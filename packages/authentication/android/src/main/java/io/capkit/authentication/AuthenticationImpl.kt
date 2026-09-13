package io.capkit.authentication

import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import com.google.android.gms.auth.api.identity.AuthorizationResult
import io.capkit.authentication.apple.AppleAuthUrlBuilder
import io.capkit.authentication.apple.AppleConfig
import io.capkit.authentication.apple.AppleConfigResolver
import io.capkit.authentication.apple.AppleConfigValidator
import io.capkit.authentication.apple.AppleSignInActivity
import io.capkit.authentication.apple.AppleSignInResultMapper
import io.capkit.authentication.config.AuthenticationConfig
import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.facebook.FacebookConfig
import io.capkit.authentication.facebook.FacebookConfigResolver
import io.capkit.authentication.facebook.FacebookConfigValidator
import io.capkit.authentication.facebook.FacebookErrorMapper
import io.capkit.authentication.facebook.FacebookSignInError
import io.capkit.authentication.facebook.FacebookSignInGateway
import io.capkit.authentication.facebook.FacebookSignInImpl
import io.capkit.authentication.google.GoogleConfig
import io.capkit.authentication.google.GoogleConfigResolver
import io.capkit.authentication.google.GoogleConfigValidator
import io.capkit.authentication.google.GoogleSignInImpl
import io.capkit.authentication.google.GoogleSignInModeResolver
import io.capkit.authentication.google.TokenRefreshClient
import io.capkit.authentication.logger.AuthenticationLogger
import io.capkit.authentication.model.SocialAuthResultUser
import io.capkit.authentication.restore.RestoreCredentialImpl
import io.capkit.authentication.vault.TokenBundle
import io.capkit.authentication.vault.TokenVault
import java.util.UUID

/**
 * Platform-specific native implementation for the Authentication plugin.
 *
 * This class contains pure Android logic and MUST NOT depend directly on
 * Capacitor bridge APIs or PluginCall objects. It owns the per-provider secure
 * vault and orchestrates the Credential Manager sign-in, token refresh, restore
 * credential and session-management adapters.
 */
class AuthenticationImpl(
  context: Context,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Android application context for the plugin (the bridge Activity context for
   * interactive Credential Manager flows).
   */
  private val context: Context = context

  /**
   * Cached plugin configuration container. Provided once via [updateConfig].
   */
  private lateinit var config: AuthenticationConfig

  /**
   * Typed Google configuration resolved by `initialize()` (read-only afterwards).
   */
  private var googleConfig: GoogleConfig? = null

  /**
   * Typed Apple configuration resolved by `initialize()` (read-only afterwards).
   */
  private var appleConfig: AppleConfig? = null

  /** Per-provider secure vault for the `google` provider. */
  private val vault: TokenVault by lazy { TokenVault(context, PROVIDER_GOOGLE) }

  /** Per-provider secure vault for the `apple` provider. */
  private val appleVault: TokenVault by lazy { TokenVault(context, PROVIDER_APPLE) }

  /**
   * Typed Facebook configuration resolved by `initialize()` (read-only afterwards).
   */
  private var facebookConfig: FacebookConfig? = null

  /** Per-provider secure vault for the `facebook` provider (`auth_vault_facebook` ns). */
  private val facebookVault: TokenVault by lazy { TokenVault(context, PROVIDER_FACEBOOK) }

  /** Facebook SDK surface bound during plugin `load()`. */
  private var facebookGateway: FacebookSignInGateway? = null

  /** Pure Facebook sign-in core owning the single-flight gate and vault hooks. */
  private var facebookSignIn: FacebookSignInImpl? = null

  /** Awaited facebook sign-in surface; set per interactive start, cleared on delivery. */
  private var facebookSignInCallback: FacebookSignInCallback? = null

  /** Credential Manager One Tap sign-in adapter (single-flight). */
  private val signIn: GoogleSignInImpl by lazy { GoogleSignInImpl(context) }

  /** OAuth2 token-endpoint refresh transport. */
  private val refreshClient: TokenRefreshClient by lazy { TokenRefreshClient() }

  /** Restore Credentials (Zero Tap) adapter. */
  private val restore: RestoreCredentialImpl by lazy { RestoreCredentialImpl(context) }

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * MUST be called exactly once during the plugin [io.capkit.authentication.AuthenticationPlugin.load]
   * phase; configures logging verbosity. Provider configuration is applied
   * separately by [initialize].
   */
  fun updateConfig(newConfig: AuthenticationConfig) {
    this.config = newConfig
    AuthenticationLogger.verbose = newConfig.verboseLogging
    AuthenticationLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Google authorization chain (full token set)
  // ---------------------------------------------------------------------------

  /**
   * Attaches the bridge hook that launches the Google authorization pending intent
   * (owned by the plugin's Activity). MUST be called during plugin `load()`.
   */
  fun attachAuthorizationLauncher(launcher: GoogleSignInImpl.AuthorizationLauncher) {
    signIn.attachAuthorizationLauncher(launcher)
  }

  /**
   * Completes a sign-in with the `AuthorizationResult` recovered from the launched
   * authorization intent (access token + server auth code over the idToken).
   */
  fun handleAuthorizationResult(authResult: AuthorizationResult) {
    signIn.handleAuthorizationResult(authResult)
  }

  /** Completes a sign-in as user-cancelled when the authorization intent returned non-OK. */
  fun handleAuthorizationCanceled() {
    signIn.handleAuthorizationCanceled()
  }

  /** Completes a sign-in as failed when the authorization intent cannot be recovered. */
  fun handleAuthorizationFailed(throwable: Throwable) {
    signIn.handleAuthorizationFailed(throwable)
  }

  // ---------------------------------------------------------------------------
  // Facebook authorization chain (SDK CallbackManager flow)
  // ---------------------------------------------------------------------------

  /**
   * Typed callback surface for the interactive Facebook flow (plugin-owned).
   *
   * @see startFacebookSignIn
   */
  interface FacebookSignInCallback {
    /** The normalized token set produced by a successful facebook sign-in. */
    fun onResult(bundle: TokenBundle)

    /** A typed native error (exact ten-code set — zero new codes). */
    fun onError(error: AuthenticationError)
  }

  /**
   * Binds the concrete Facebook SDK gateway to the pure sign-in core and
   * registers the outcome listener. MUST be called during plugin `load()`.
   *
   * The listener routes SDK outcomes through [FacebookSignInImpl] (persistence,
   * single-flight release, ten-code mapping) before delivering
   * the awaited [FacebookSignInCallback].
   */
  fun bindFacebookSignIn(gateway: FacebookSignInGateway) {
    facebookGateway = gateway
    val core =
      FacebookSignInImpl(
        gateway = gateway,
        persist = { facebookVault.store(it) },
        clearVault = { facebookVault.delete() },
      )
    facebookSignIn = core
    gateway.register(
      object : FacebookSignInGateway.Listener {
        override fun onLoginSuccess(
          accessToken: String,
          userId: String?,
          grantedPermissions: Collection<String>?,
          declinedPermissions: Collection<String>?,
          expiresAtMillis: Long?,
          profile: SocialAuthResultUser?,
        ) {
          val bundle =
            core.completeSuccess(
              accessToken,
              userId,
              grantedPermissions,
              declinedPermissions,
              expiresAtMillis,
              profile,
            )
          takeFacebookCallback()?.onResult(bundle)
            ?: AuthenticationLogger.debug(
              "Facebook login completed without a pending call.",
              bundle.accessToken.orEmpty(),
            )
        }

        override fun onCancelled() {
          core.completeCancellation()
          takeFacebookCallback()?.onError(
            AuthenticationError.UserCancelled("The user cancelled the Facebook sign-in flow."),
          ) ?: AuthenticationLogger.debug("Facebook login cancelled without a pending call.")
        }

        override fun onError(error: FacebookSignInError) {
          val mapped = core.completeFailure(error)
          takeFacebookCallback()?.onError(mapped)
            ?: AuthenticationLogger.debug(
              "Facebook login failed without a pending call.",
              mapped.message.orEmpty(),
            )
        }
      },
    )
  }

  /**
   * Starts the interactive Facebook login surface (single-flight): resolves the
   * facebook configuration, adopts the awaited [FacebookSignInCallback] and
   * launches the SDK dialog through the bound gateway.
   *
   * @param callback the awaited outcome surface (the plugin keeps the bridge call).
   * @return the mapped `CONFLICT` error when a facebook sign-in is already in
   *         flight, or `null` when the flow started.
   * @throws [AuthenticationError] when the configuration is missing/invalid or
   *         the sign-in core is not bound (never a new code).
   */
  fun startFacebookSignIn(callback: FacebookSignInCallback): AuthenticationError? {
    val core = requireFacebookSignIn()
    val config = requireFacebookConfig()
    // Initialize the SDK from the runtime config BEFORE the dialog opens: the
    // gateway constructor never touches the SDK, so the
    // surface is only created here, once, when a real config exists. A host
    // without a facebook app id keeps the plugin loaded and fails with the
    // typed MISSING_CONFIGURATION on launch instead of crashing load().
    facebookGateway?.initialize(config)
    // Adopt the awaited surface BEFORE launching so an in-flight dialog never
    // races ahead of the plugin's saved call (call-lifecycle rule).
    facebookSignInCallback = callback
    val startError = core.begin(config.scopes)
    if (startError != null) {
      // CONFLICT: the conflicting flow owns the awaited surface; reject directly.
      facebookSignInCallback = null
      return startError
    }
    AuthenticationLogger.debug("Facebook sign-in started with scopes:", config.scopes.joinToString())
    return null
  }

  /**
   * Forwards the plugin's `onActivityResult` surface into the Facebook
   * CallbackManager. Returns `true` only when the event belongs to the facebook
   * flow (request code `0xface`).
   */
  fun handleFacebookActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ): Boolean {
    val gateway = facebookGateway ?: return false
    return gateway.onActivityResult(requestCode, resultCode, data)
  }

  // ---------------------------------------------------------------------------
  // Provider helpers
  // ---------------------------------------------------------------------------

  /**
   * Validates the provider key surfaced by JavaScript.
   *
   * @param provider the JS `AuthProvider` value.
   * @throws [AuthenticationError.InvalidInput] for unsupported providers
   *         (mirrors the Web facade's invalid-provider rejection).
   */
  private fun requireProvider(provider: String?) {
    if (provider != PROVIDER_GOOGLE && provider != PROVIDER_APPLE && provider != PROVIDER_FACEBOOK) {
      throw AuthenticationError.InvalidInput("Unsupported provider: $provider")
    }
  }

  /**
   * Validates that the provider is Google. Restore Credentials (Zero Tap) are a
   * Google-only capability; any other provider is rejected (restore is not
   * implemented for Apple).
   *
   * @param provider the JS `AuthProvider` value.
   * @throws [AuthenticationError.InvalidInput] for non-Google providers.
   */
  private fun requireGoogleProvider(provider: String?) {
    if (provider != PROVIDER_GOOGLE) {
      throw AuthenticationError.InvalidInput("Unsupported provider: $provider")
    }
  }

  /**
   * Resolves and validates the raw Apple configuration JSON.
   *
   * @param configJson the raw `plugins.Authentication` JSON from the bridge.
   * @return the typed [AppleConfig].
   * @throws [AuthenticationError] when the configuration is absent or invalid.
   */
  private fun resolveAppleConfig(configJson: String?): AppleConfig {
    val resolved = AppleConfigResolver.resolve(configJson.orEmpty())
    AppleConfigValidator.validate(resolved)?.let { throw it }
    return checkNotNull(resolved)
  }

  /**
   * Resolves and validates the raw Google configuration JSON.
   *
   * @param configJson the raw `plugins.Authentication` JSON from the bridge.
   * @return the typed [GoogleConfig].
   * @throws [AuthenticationError] when the configuration is absent or invalid.
   */
  private fun resolveGoogleConfig(configJson: String?): GoogleConfig {
    val resolved = GoogleConfigResolver.resolve(configJson.orEmpty())
    GoogleConfigValidator.validate(resolved)?.let { throw it }
    return checkNotNull(resolved)
  }

  // ---------------------------------------------------------------------------
  // Facade: initialize / signIn / session
  // ---------------------------------------------------------------------------

  /**
   * Initializes the provider configuration (eager validation, unlike the Web
   * facade's lazy sign-in validation).
   *
   * @param provider the JS `AuthProvider` value.
   * @param configJson the raw `plugins.Authentication` configuration JSON.
   * @throws [AuthenticationError] on invalid configuration.
   */
  fun initialize(
    provider: String?,
    configJson: String?,
  ) {
    requireProvider(provider)
    when (provider) {
      PROVIDER_GOOGLE -> {
        val resolved = resolveGoogleConfig(configJson)
        googleConfig = resolved
        AuthenticationLogger.debug("Google configuration applied. clientId:", resolved.serverClientId)
      }
      PROVIDER_APPLE -> {
        val resolved = resolveAppleConfig(configJson)
        appleConfig = resolved
        AuthenticationLogger.debug("Apple configuration applied. clientId:", resolved.clientId)
      }
      PROVIDER_FACEBOOK -> {
        val resolved = resolveFacebookConfig(configJson)
        facebookConfig = resolved
        val gateway = facebookGateway
        if (gateway != null) {
          gateway.initialize(resolved)
        } else {
          AuthenticationLogger.debug("Facebook gateway not bound; configuration validated only.")
        }
        AuthenticationLogger.debug("Facebook configuration applied. appId:", resolved.facebookAppId)
      }
    }
  }

  /**
   * Signs the user in with Google (Credential Manager One Tap).
   *
   * @param provider the JS `AuthProvider` value.
   * @param autoSelect the JS option overriding the configured autoSelect flag.
   * @param callback the typed result surface (TokenBundle from the vault).
   */
  fun signIn(
    provider: String?,
    autoSelect: Boolean?,
    callback: GoogleSignInImpl.SignInCallback,
  ) {
    requireProvider(provider)
    when (provider) {
      PROVIDER_GOOGLE -> {
        val config = requireGoogleConfig()
        val mode = GoogleSignInModeResolver.resolve(autoSelect ?: config.autoSelect)
        signIn.signIn(config, vault, mode, callback)
      }
      PROVIDER_APPLE -> {
        // Apple starts an Activity flow (see buildAppleSignInIntent); it never
        // uses the Google Credential Manager sign-in path.
        throw AuthenticationError.InvalidInput("Apple sign-in must use the WebView Activity flow.")
      }
      PROVIDER_FACEBOOK -> {
        // Facebook uses the SDK CallbackManager flow; the plugin routes through
        // startFacebookSignIn + handleFacebookActivityResult, never this path.
        throw AuthenticationError.InvalidInput("Facebook sign-in must use the CallbackManager flow.")
      }
    }
  }

  /**
   * Builds the launch intent for the Apple WebView sign-in flow.
   *
   * Resolves the Apple configuration, generates ephemeral state/nonce values
   * when the configuration does not pin them, builds the authorization URL, and
   * packs the Activity extras. The Activity is launched
   * by the plugin; its result is consumed by [handleAppleSignInResult].
   *
   * @return the configured [AppleSignInActivity] intent.
   * @throws [AuthenticationError] when the Apple configuration is missing.
   */
  fun buildAppleSignInIntent(): Intent {
    val config = requireAppleConfig()
    val redirectUri =
      config.redirectURI
        ?: throw AuthenticationError.InvalidInput("apple.redirectURI must not be blank.")
    val state = config.state ?: generateEphemeralValue()
    val nonce = config.nonce ?: generateEphemeralValue()
    val authorizeUrl =
      AppleAuthUrlBuilder.buildAuthUrl(
        clientId = config.clientId,
        redirectUri = redirectUri,
        state = state,
        nonce = nonce,
        scopes = config.scopes,
      )
    return AppleSignInActivity.newIntent(
      context = context,
      authorizeUrl = authorizeUrl,
      redirectUri = redirectUri,
      expectedState = state,
      expectedNonce = nonce,
    )
  }

  /**
   * Completes an Apple sign-in from the Activity result surfaced through the
   * plugin `@ActivityCallback`.
   *
   * Maps the result through [AppleSignInResultMapper] (which validates state and
   * nonce and classifies errors with no new codes), stores the parsed credential
   * in the `apple` vault, and returns the bundle for the plugin result.
   *
   * @param resultCode the Activity result code.
   * @param payloadJson the decoded form-post JSON returned by the Activity.
   * @param errorCode the raw SDK error code, when the Activity failed with one.
   * @param errorMessage the raw SDK error message, when one was surfaced.
   * @param expectedState the `state` issued in the authorization URL (echoed
   *        back by the Activity result extras).
   * @param expectedNonce the `nonce` issued in the authorization URL (echoed
   *        back by the Activity result extras), or `null` when none was issued.
   * @return the stored [TokenBundle] on success.
   * @throws [AuthenticationError] mapped from the outcome (never new codes).
   */
  fun handleAppleSignInResult(
    resultCode: Int,
    payloadJson: String?,
    errorCode: String?,
    errorMessage: String?,
    expectedState: String,
    expectedNonce: String?,
  ): TokenBundle {
    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = resultCode,
        payload = payloadJson,
        expectedState = expectedState,
        expectedNonce = expectedNonce,
        errorCode = errorCode,
        errorMessage = errorMessage,
      )

    return when (outcome) {
      is AppleSignInResultMapper.Outcome.Success -> {
        val bundle =
          TokenBundle(
            accessToken = null,
            idToken = outcome.idToken,
            authorizationCode = outcome.authorizationCode,
            user = outcome.user,
          )
        appleVault.store(bundle)
        bundle
      }
      is AppleSignInResultMapper.Outcome.UserCancelled ->
        throw AuthenticationError.UserCancelled("The user cancelled the Apple sign-in flow.")
      is AppleSignInResultMapper.Outcome.Failed -> throw outcome.error
    }
  }

  /**
   * Ends the provider session, clearing the secure vault (and Apple's WebView
   * cookies).
   *
   * @param provider the JS `AuthProvider` value.
   */
  fun signOut(provider: String?) {
    requireProvider(provider)
    when (provider) {
      PROVIDER_GOOGLE -> vault.delete()
      PROVIDER_APPLE -> {
        clearAppleCookies()
        appleVault.delete()
      }
      PROVIDER_FACEBOOK -> {
        // SDK logout plus vault clear; when the gateway is not
        // bound, at least the vaulted session is cleared.
        val core = facebookSignIn
        if (core != null) {
          core.signOut()
        } else {
          facebookVault.delete()
        }
      }
    }
  }

  /**
   * Alias for [signOut] mirroring the Web facade.
   *
   * @param provider the JS `AuthProvider` value.
   */
  fun logout(provider: String?) {
    requireProvider(provider)
    when (provider) {
      PROVIDER_GOOGLE -> vault.delete()
      PROVIDER_APPLE -> {
        clearAppleCookies()
        appleVault.delete()
      }
      PROVIDER_FACEBOOK -> {
        // SDK logout plus vault clear; when the gateway is not
        // bound, at least the vaulted session is cleared.
        val core = facebookSignIn
        if (core != null) {
          core.signOut()
        } else {
          facebookVault.delete()
        }
      }
    }
  }

  /**
   * Returns the stored access token for the provider, if any.
   *
   * @param provider the JS `AuthProvider` value.
   * @return the vaulted access token, or `null` when none is stored.
   */
  fun getCurrentAccessToken(provider: String?): String? {
    requireProvider(provider)
    return when (provider) {
      PROVIDER_GOOGLE -> vault.read()?.accessToken
      PROVIDER_APPLE -> appleVault.read()?.accessToken
      PROVIDER_FACEBOOK -> facebookVault.read()?.accessToken
      else -> null
    }
  }

  /**
   * Refreshes the access token through the OAuth2 token endpoint using the
   * vaulted refresh token.
   *
   * @param provider the JS `AuthProvider` value.
   * @param callback the typed result surface.
   */
  fun refreshToken(
    provider: String?,
    callback: TokenRefreshClient.RefreshCallback,
  ) {
    requireProvider(provider)
    when (provider) {
      PROVIDER_GOOGLE -> {
        val config = requireGoogleConfig()
        val existing = vault.read() ?: TokenBundle(accessToken = null)
        refreshClient.refresh(config.serverClientId, existing, vault, callback)
      }
      PROVIDER_APPLE -> {
        // Apple issues no refresh token to the client; THE authorization code
        // must be exchanged server-side. Mirrors the Web provider's rejection
        // with no new error codes.
        callback.onError(AuthenticationError.InvalidInput("Apple issues no refresh token."))
      }
      PROVIDER_FACEBOOK -> {
        // Facebook issues no refresh token to the client: the JS facade must
        // re-run the interactive flow.
        // Exact message shared with the Web provider (FacebookErrorMapper).
        callback.onError(
          AuthenticationError.InvalidInput(FacebookErrorMapper.REFRESH_NOT_SUPPORTED_MESSAGE),
        )
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Facade: restore credentials
  // ---------------------------------------------------------------------------

  /**
   * Creates a Restore Credential (Zero Tap) for the provider.
   *
   * @param requestJson the WebAuthn-style credential request JSON.
   * @param isCloudBackupEnabled whether the credential may be cloud backed up.
   * @param callback the typed result surface.
   */
  fun createRestoreCredential(
    requestJson: String?,
    isCloudBackupEnabled: Boolean,
    callback: RestoreCredentialImpl.RestoreCredentialCallback,
  ) {
    requireGoogleProvider(PROVIDER_GOOGLE)
    restore.create(requestJson, isCloudBackupEnabled, callback)
  }

  /**
   * Retrieves a previously created restore credential, if any.
   *
   * @param requestJson the WebAuthn-style request JSON for the restore option.
   * @param callback the typed result surface.
   */
  fun getRestoreCredential(
    requestJson: String?,
    callback: RestoreCredentialImpl.RestoreCredentialCallback,
  ) {
    requireGoogleProvider(PROVIDER_GOOGLE)
    restore.get(requestJson, callback)
  }

  /**
   * Clears a previously created restore credential.
   *
   * @param callback the typed result surface.
   */
  fun clearRestoreCredential(callback: RestoreCredentialImpl.RestoreCredentialCallback) {
    requireGoogleProvider(PROVIDER_GOOGLE)
    restore.clear(callback)
  }

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  /**
   * Resolves and validates the raw Facebook configuration JSON.
   *
   * @param configJson the raw `plugins.Authentication` JSON from the bridge.
   * @return the typed [FacebookConfig].
   * @throws [AuthenticationError] when the configuration is absent or invalid.
   */
  private fun resolveFacebookConfig(configJson: String?): FacebookConfig {
    val resolved = FacebookConfigResolver.resolve(configJson.orEmpty())
    FacebookConfigValidator.validate(resolved)?.let { throw it }
    return checkNotNull(resolved)
  }

  /**
   * @return the resolved Google configuration.
   * @throws [AuthenticationError.InitFailed] when `initialize()` was not called.
   */
  private fun requireGoogleConfig(): GoogleConfig {
    val resolved = googleConfig
    if (resolved == null) {
      throw AuthenticationError.InitFailed("Authentication provider is not initialized.")
    }
    return resolved
  }

  /**
   * @return the resolved Apple configuration.
   * @throws [AuthenticationError.InitFailed] when `initialize()` was not called.
   */
  private fun requireAppleConfig(): AppleConfig {
    val resolved = appleConfig
    if (resolved == null) {
      throw AuthenticationError.InitFailed("Authentication provider is not initialized.")
    }
    return resolved
  }

  /**
   * @return the resolved Facebook configuration.
   * @throws [AuthenticationError.InitFailed] when `initialize()` was not called.
   */
  private fun requireFacebookConfig(): FacebookConfig {
    val resolved = facebookConfig
    if (resolved == null) {
      throw AuthenticationError.InitFailed("Authentication provider is not initialized.")
    }
    return resolved
  }

  /**
   * @return the bound Facebook sign-in core.
   * @throws [AuthenticationError.InitFailed] when `bindFacebookSignIn` was not
   *         called during plugin `load()`.
   */
  private fun requireFacebookSignIn(): FacebookSignInImpl {
    val core = facebookSignIn
    if (core == null) {
      throw AuthenticationError.InitFailed("Facebook sign-in is not bound.")
    }
    return core
  }

  /**
   * Consumes the awaited facebook callback surface. The first delivered outcome
   * adopts it; every later outcome (stray activity results) is dropped so a call
   * is never resolved twice (call-lifecycle guarantee).
   */
  private fun takeFacebookCallback(): FacebookSignInCallback? {
    val pending = facebookSignInCallback
    facebookSignInCallback = null
    return pending
  }

  /** Clears every WebView cookie (Apple session cookies). */
  private fun clearAppleCookies() {
    runCatching { CookieManager.getInstance().removeAllCookies(null) }
  }

  /**
   * Generates an ephemeral OAuth `state`/`nonce` value when the configuration
   * does not pin one. Uses a v4 UUID (cryptographically random) so every Apple
   * sign-in carries fresh CSRF/replay protection.
   */
  private fun generateEphemeralValue(): String = UUID.randomUUID().toString()

  private companion object {
    const val PROVIDER_GOOGLE = "google"
    const val PROVIDER_APPLE = "apple"
    const val PROVIDER_FACEBOOK = "facebook"
  }
}
