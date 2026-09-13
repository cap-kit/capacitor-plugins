package io.capkit.authentication

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.google.android.gms.auth.api.identity.Identity
import io.capkit.authentication.apple.AppleSignInActivity
import io.capkit.authentication.config.AuthenticationConfig
import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.error.AuthenticationErrorMessages
import io.capkit.authentication.facebook.FacebookSignInGateway
import io.capkit.authentication.google.GoogleSignInImpl
import io.capkit.authentication.google.TokenRefreshClient
import io.capkit.authentication.logger.AuthenticationLogger
import io.capkit.authentication.model.AuthenticationPluginVersionResult
import io.capkit.authentication.model.GetCurrentAccessTokenResult
import io.capkit.authentication.model.RestoreResult
import io.capkit.authentication.restore.RestoreCredentialImpl
import io.capkit.authentication.utils.ErrorCodeMapper
import io.capkit.authentication.utils.TokenMapper
import io.capkit.authentication.vault.TokenBundle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capacitor bridge for the Authentication plugin.
 *
 * This class acts as the boundary between JavaScript and native Android code.
 * It handles input parsing, configuration management, and delegates execution
 * to the platform-specific implementation.
 */
@CapacitorPlugin(
  name = "Authentication",
  requestCodes = [FACEBOOK_CALLBACK_REQUEST_CODE],
)
class AuthenticationPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration read from capacitor.config.ts.
   * * CONTRACT:
   * - Initialized exactly once in `load()`.
   * - Treated as read-only afterwards.
   */
  private lateinit var config: AuthenticationConfig

  /**
   * Native implementation layer containing core Android logic.
   *
   * CONTRACT:
   * - Owned by the Plugin layer.
   * - MUST NOT access PluginCall or Capacitor bridge APIs directly.
   */
  private lateinit var implementation: AuthenticationImpl

  /**
   * Serializer instance configured to encode result models into JSObject string payloads.
   *
   * NOTE: encodeDefaults is intentionally NOT set so nullable/defaulted fields
   * are omitted from the emitted JSON, matching the optional TypeScript types.
   */
  private val json = Json

  /**
   * Launches the Google authorization pending intent and recovers the token
   * result, mirroring the Capawesome reference flow:
   * `ActivityResultContracts.StartIntentSenderForResult` ->
   * `Identity.getAuthorizationClient(...).getAuthorizationResultFromIntent(...)`.
   */
  private var authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

  private companion object {
    const val PROVIDER_GOOGLE = "google"
    const val PROVIDER_APPLE = "apple"
    const val PROVIDER_FACEBOOK = "facebook"
  }

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read static configuration
   * - initialize native resources
   * - inject configuration into the implementation
   */
  override fun load() {
    super.load()

    config = AuthenticationConfig(this)
    implementation = AuthenticationImpl(context)
    implementation.updateConfig(config)

    authorizationLauncher =
      activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleAuthorizationActivityResult,
      )
    implementation.attachAuthorizationLauncher(::launchAuthorizationIntent)

    // Binds the facebook SDK gateway to the pure sign-in core. The gateway
    // constructor is SDK-free: the CallbackManager/
    // LoginManager surface is created lazily when startFacebookSignIn
    // initializes the SDK with the runtime config. No manifest changes are
    // required: the SDK 18.3.0 artifacts merge
    // FacebookActivity/CustomTabActivity/FacebookInitProvider themselves,
    // and the facebook request code is registered above so the
    // bridge dispatches dialog results to handleOnActivityResult.
    val facebookGateway = FacebookSignInGateway(activity)
    implementation.bindFacebookSignIn(facebookGateway)
    AuthenticationLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  /**
   * Handles the Google authorization activity result: recovers the
   * `AuthorizationResult` from the returned intent (success) or completes the
   * flow as cancelled/failed.
   */
  private fun handleAuthorizationActivityResult(result: ActivityResult) {
    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
      try {
        val authResult =
          Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(result.data)
        implementation.handleAuthorizationResult(authResult)
      } catch (e: Throwable) {
        implementation.handleAuthorizationFailed(e)
      }
    } else {
      implementation.handleAuthorizationCanceled()
    }
  }

  /**
   * Entry point for the Google authorization pending intent, kept as a named
   * helper for wiring symmetry with the Capawesome reference.
   *
   * @param pendingIntent the pending intent from `AuthorizationResult.getPendingIntent()`.
   */
  private fun launchAuthorizationIntent(pendingIntent: PendingIntent) {
    authorizationLauncher?.launch(IntentSenderRequest.Builder(pendingIntent).build())
  }

  /**
   * Forwards the Facebook SDK dialog result (request code `0xface`) into the
   * CallbackManager bound during [load]. Other request codes are intentionally
   * ignored — the bridge already filtered them by the registered set.
   *
   * @param requestCode the Activity request code.
   * @param resultCode the Activity result code.
   * @param data the result intent (facebook login data).
   */
  override fun handleOnActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ) {
    if (requestCode == FACEBOOK_CALLBACK_REQUEST_CODE) {
      implementation.handleFacebookActivityResult(requestCode, resultCode, data)
    }
  }

  // ---------------------------------------------------------------------------
  // Helper Methods for Serialization
  // ---------------------------------------------------------------------------

  /**
   * Converts a serializable result model directly into a Capacitor JSObject.
   */
  private inline fun <reified T> toJSObject(value: T): JSObject {
    val jsonString = json.encodeToString(value)
    return JSObject(jsonString)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   * Codes are derived from [ErrorCodeMapper] to guarantee cross-platform parity
   * with the JS AuthenticationErrorCode const object.
   */
  private fun reject(
    call: PluginCall,
    error: AuthenticationError,
  ) {
    val code = ErrorCodeMapper.mapCode(error)

    // Always prefer the message from the AuthenticationError instance,
    // falling back to the canonical internal error message.
    val message = error.message ?: AuthenticationErrorMessages.INTERNAL_ERROR
    call.reject(message, code)
  }

  /**
   * Centralizes error handling for a native throwable.
   *
   * - AuthenticationError instances are mapped through [reject] directly.
   * - Any other Throwable is wrapped as an initialization failure so
   *   unexpected native exceptions never escape the bridge unmapped.
   */
  private fun handleError(
    call: PluginCall,
    throwable: Throwable,
  ) {
    if (throwable is AuthenticationError) {
      reject(call, throwable)
    } else {
      val message = throwable.message ?: AuthenticationErrorMessages.UNEXPECTED_NATIVE_ERROR
      reject(call, AuthenticationError.InitFailed(message))
    }
  }

  /**
   * Wraps the implementation-callback surface: resolve or reject via [handleError].
   */
  private fun respondError(
    call: PluginCall,
    error: AuthenticationError,
  ) {
    reject(call, error)
  }

  // ---------------------------------------------------------------------------
  // Version Information
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version synchronized from package.json.
   *
   * @param call The bridge call to resolve with version data.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    try {
      call.resolve(toJSObject(AuthenticationPluginVersionResult(BuildConfig.PLUGIN_VERSION)))
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  // ---------------------------------------------------------------------------
  // Facade: initialize / signIn
  // ---------------------------------------------------------------------------

  /**
   * Initializes the provider, eagerly validating the Google configuration.
   *
   * @param call The bridge call carrying the provider key; resolves void.
   */
  @PluginMethod
  fun initialize(call: PluginCall) {
    try {
      implementation.initialize(providerOf(call), config.rawConfigJson)
      call.resolve()
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  /**
   * Signs the user in with the requested provider.
   *
   * Google: Credential Manager One Tap via the authorization pending intent.
   * Apple: launches the [AppleSignInActivity] WebView flow and completes the
   * call from [handleAppleSignInResult] via `@ActivityCallback`.
   *
   * Resolves the normalized token set; the interactive flow is single-flight so
   * overlapping calls reject with CONFLICT.
   *
   * @param call The bridge call carrying the provider key and optional options.
   */
  @PluginMethod
  fun signIn(call: PluginCall) {
    try {
      if (providerOf(call) == PROVIDER_FACEBOOK) {
        facebookSignIn(call)
        return
      }
      if (providerOf(call) == PROVIDER_APPLE) {
        val intent = implementation.buildAppleSignInIntent()
        startActivityForResult(call, intent, "handleAppleSignInResult")
        return
      }
      val autoSelect = call.getObject("options")?.getBoolean("autoSelect")
      implementation.signIn(
        provider = providerOf(call),
        autoSelect = autoSelect,
        callback =
          object : GoogleSignInImpl.SignInCallback {
            override fun onResult(bundle: TokenBundle) {
              call.resolve(socialAuthResult(PROVIDER_GOOGLE, bundle))
            }

            override fun onError(error: AuthenticationError) {
              respondError(call, error)
            }
          },
      )
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  /**
   * Facebook interactive sign-in through the SDK CallbackManager flow.
   *
   * The bridge call is saved ONLY after the flow starts: a CONFLICT or
   * configuration failure rejects the live call before anything is saved, so
   * the bridge's saved-call map is never leaked (call-lifecycle rule).
   * Capacitor v8 retains the same [call] object (saveCall stores it by id and
   * returns void); the dialog result arrives on handleOnActivityResult (request
   * code `0xface`) and resolves the saved call exactly once.
   */
  private fun facebookSignIn(call: PluginCall) {
    val startError =
      implementation.startFacebookSignIn(
        callback =
          object : AuthenticationImpl.FacebookSignInCallback {
            override fun onResult(bundle: TokenBundle) {
              bridge.releaseCall(call)
              call.resolve(socialAuthResult(PROVIDER_FACEBOOK, bundle))
            }

            override fun onError(error: AuthenticationError) {
              bridge.releaseCall(call)
              respondError(call, error)
            }
          },
      )
    if (startError != null) {
      respondError(call, startError)
      return
    }
    bridge.saveCall(call)
    AuthenticationLogger.debug("Facebook sign-in call saved:", call.callbackId)
  }

  /**
   * Completes the Apple WebView sign-in started by [signIn].
   *
   * The Activity echoes the expected state/nonce back in its result extras; the
   * implementation validates both and stores the parsed credential in the
   * `apple` vault before the call resolves.
   *
   * @param call the saved bridge call (Capacitor v8 restores it by id).
   * @param result the Activity result from [AppleSignInActivity].
   */
  @ActivityCallback
  private fun handleAppleSignInResult(
    call: PluginCall,
    result: ActivityResult,
  ) {
    val savedCall = bridge.getSavedCall(call.callbackId) ?: call
    try {
      val data = result.data
      val bundle =
        implementation.handleAppleSignInResult(
          resultCode = result.resultCode,
          payloadJson = data?.getStringExtra(AppleSignInActivity.EXTRA_PAYLOAD),
          errorCode = data?.getStringExtra(AppleSignInActivity.EXTRA_ERROR_CODE),
          errorMessage = data?.getStringExtra(AppleSignInActivity.EXTRA_ERROR_MESSAGE),
          expectedState = data?.getStringExtra(AppleSignInActivity.EXTRA_EXPECTED_STATE).orEmpty(),
          expectedNonce = data?.getStringExtra(AppleSignInActivity.EXTRA_EXPECTED_NONCE),
        )
      savedCall.resolve(socialAuthResult(PROVIDER_APPLE, bundle))
    } catch (e: Throwable) {
      handleError(savedCall, e)
    }
  }

  // ---------------------------------------------------------------------------
  // Facade: signOut / logout / getCurrentAccessToken / refreshToken
  // ---------------------------------------------------------------------------

  /**
   * Ends the provider session: clears the secure vault and Apple's WebView
   * cookies when applicable.
   *
   * @param call The bridge call carrying the provider key; resolves void.
   */
  @PluginMethod
  fun signOut(call: PluginCall) {
    try {
      implementation.signOut(providerOf(call))
      call.resolve()
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  /**
   * Alias for [signOut]. Clears the session and persisted tokens.
   *
   * @param call The bridge call carrying the provider key; resolves void.
   */
  @PluginMethod
  fun logout(call: PluginCall) {
    try {
      implementation.logout(providerOf(call))
      call.resolve()
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  /**
   * Returns the stored access token for the provider, if any.
   *
   * @param call The bridge call carrying the provider key.
   */
  @PluginMethod
  fun getCurrentAccessToken(call: PluginCall) {
    try {
      val accessToken = implementation.getCurrentAccessToken(providerOf(call))
      call.resolve(toJSObject(GetCurrentAccessTokenResult(accessToken)))
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  /**
   * Refreshes the access token through the token endpoint using the vaulted
   * refresh token; resolves the normalized token set.
   *
   * @param call The bridge call carrying the provider key.
   */
  @PluginMethod
  fun refreshToken(call: PluginCall) {
    try {
      implementation.refreshToken(
        provider = providerOf(call),
        callback =
          object : TokenRefreshClient.RefreshCallback {
            override fun onResult(bundle: TokenBundle) {
              call.resolve(socialAuthResult(providerOf(call) ?: PROVIDER_GOOGLE, bundle))
            }

            override fun onError(error: AuthenticationError) {
              respondError(call, error)
            }
          },
      )
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  // ---------------------------------------------------------------------------
  // Facade: restore credentials
  // ---------------------------------------------------------------------------

  /**
   * Creates a Google Restore (Zero Tap) credential.
   *
   * @param call The bridge call carrying `requestJson` and `isCloudBackupEnabled`.
   */
  @PluginMethod
  fun createRestoreCredential(call: PluginCall) {
    try {
      val isCloudBackupEnabled = call.getBoolean("isCloudBackupEnabled") ?: true
      implementation.createRestoreCredential(
        requestJson = call.getString("requestJson"),
        isCloudBackupEnabled = isCloudBackupEnabled,
        callback = restoreCallback(call),
      )
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  /**
   * Retrieves a previously created restore credential, if any.
   *
   * @param call The bridge call carrying the `requestJson` for the restore option.
   */
  @PluginMethod
  fun getRestoreCredential(call: PluginCall) {
    try {
      implementation.getRestoreCredential(
        requestJson = call.getString("requestJson"),
        callback = restoreCallback(call),
      )
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  /**
   * Clears a previously created restore credential.
   *
   * @param call The bridge call; resolves with the availability surface.
   */
  @PluginMethod
  fun clearRestoreCredential(call: PluginCall) {
    try {
      implementation.clearRestoreCredential(callback = restoreCallback(call))
    } catch (e: Throwable) {
      handleError(call, e)
    }
  }

  // ---------------------------------------------------------------------------
  // Private: parsing helpers
  // ---------------------------------------------------------------------------

  /**
   * Extracts the JS provider key from the call payload.
   */
  private fun providerOf(call: PluginCall): String? = call.getString("provider")

  /**
   * Builds the normalized `SocialAuthResult` JSObject from a token bundle.
   *
   * Token fields are shaped through [TokenMapper] (the pure contract) so
   * absent optional fields are omitted, matching the TypeScript `OAuthTokenSet`.
   * Apple bundles surface `authorizationCode`/`user` (homogeneous shape);
   * Google bundles omit both.
   */
  private fun socialAuthResult(
    provider: String,
    bundle: TokenBundle,
  ): JSObject =
    JSObject().apply {
      put("provider", provider)
      val tokens = JSObject()
      TokenMapper
        .map(
          accessToken = bundle.accessToken,
          refreshToken = bundle.refreshToken,
          idToken = bundle.idToken,
          serverAuthCode = bundle.serverAuthCode,
          authorizationCode = bundle.authorizationCode,
          userId = bundle.userId,
          grantedPermissions = bundle.grantedPermissions,
          declinedPermissions = bundle.declinedPermissions,
          expiresAt = bundle.expiresAt,
        ).forEach { (key, value) -> tokens.put(key, value) }
      // The structured `user` profile is encoded through the @Serializable model
      // (not a flat string) so the nested object matches the TS shape.
      bundle.user?.let { tokens.put("user", toJSObject(it)) }
      put("tokens", tokens)
    }

  /**
   * Shared callback surface for the three restore-credential methods.
   */
  private fun restoreCallback(call: PluginCall): RestoreCredentialImpl.RestoreCredentialCallback =
    object : RestoreCredentialImpl.RestoreCredentialCallback {
      override fun onResult(
        available: Boolean,
        requestJson: String?,
      ) {
        call.resolve(toJSObject(RestoreResult(available, requestJson)))
      }

      override fun onError(error: AuthenticationError) {
        respondError(call, error)
      }
    }
}

/**
 * Request code registered for the Facebook SDK CallbackManager flow. `0xface`
 * equals the facebook SDK's own login request code; registering it in
 * `@CapacitorPlugin(requestCodes = [...])` makes the Capacitor bridge dispatch
 * the dialog results to [AuthenticationPlugin.handleOnActivityResult] instead of
 * the JS layer. Declared at file scope so it is a compile-time constant in the
 * class annotation (a companion const cannot be referenced there).
 */
private const val FACEBOOK_CALLBACK_REQUEST_CODE = 0xface
