package io.capkit.authentication.facebook

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import io.capkit.authentication.model.DisplayName
import io.capkit.authentication.model.SocialAuthResultUser
import org.json.JSONObject

/**
 * @file FacebookSignInGateway.kt
 * Concrete binding of the [FacebookSdkGateway] seam to the `facebook-login`
 * 18.3.0 artifacts: the CallbackManager/LoginManager
 * surface, SDK initialization from the runtime [FacebookConfig], and the Graph
 * `/me` profile fetch. Owned by the plugin Activity; every interactive event
 * flows through [onActivityResult] (request code `0xface`, registered in
 * `@CapacitorPlugin(requestCodes = [...])` so the Capacitor bridge dispatches
 * the facebook dialogs to the plugin — never the JS layer).
 *
 * This class is pure SDK glue: no business decisions live here. Outcomes are
 * surfaced as typed [FacebookSignInError] categories to the [Listener]; the
 * orchestration core ([FacebookSignInImpl]) owns persistence and the
 * single-flight gate. SDK 18.3.0 declares its own `FacebookActivity`,
 * `CustomTabActivity` and `FacebookInitProvider` via manifest merge — the plugin
 * manifest is unchanged.
 *
 * Load safety: the constructor must never touch the SDK. The
 * CallbackManager/LoginManager surface is created lazily in [initialize] only
 * after `FacebookSdk.sdkInitialize` succeeded, so a host app without a facebook
 * app id still loads the plugin (Google/Apple/version keep working); the first
 * `launchLogin` on an uninitialized surface fails with
 * [FacebookSignInError.MISSING_CONFIGURATION] instead of throwing.
 */
class FacebookSignInGateway(
  private val activity: Activity,
) : FacebookSdkGateway {
  /** Session profile fields requested through the Graph `/me` call. */
  private companion object {
    const val FIELDS_PARAM_NAME = "fields"
    const val FIELDS_PARAM = "id,name,email,picture.width(720).height(720)"
    const val KEY_ID = "id"
    const val KEY_NAME = "name"
    const val KEY_EMAIL = "email"
    const val KEY_PICTURE = "picture"
    const val DATA_KEY = "data"
    const val URL_KEY = "url"
  }

  /**
   * Outcome surface for the interactive facebook flow. Delivered on the thread
   * of the underlying SDK callback (Graph responses arrive on a background
   * thread; dialog results on the main thread) — implementations must not touch
   * the UI.
   */
  interface Listener {
    /**
     * The dialog succeeded and the `/me` profile request completed: primitives
     * are delivered in exactly the shape the pure core consumes
     * ([FacebookSignInImpl.completeSuccess]).
     */
    fun onLoginSuccess(
      accessToken: String,
      userId: String?,
      grantedPermissions: Collection<String>?,
      declinedPermissions: Collection<String>?,
      expiresAtMillis: Long?,
      profile: SocialAuthResultUser?,
    )

    /** The user dismissed the facebook login dialog. */
    fun onCancelled()

    /** The flow failed with a typed category ([FacebookSignInError], zero new codes). */
    fun onError(error: FacebookSignInError)
  }

  // The SDK surface objects are created lazily inside initialize(): constructing
  // CallbackManager/LoginManager here would throw when the Facebook SDK is not
  // initialized yet (load-safety), which would fail the whole plugin load
  // through PluginHandle.load(). The constructor is intentionally SDK-free.
  private var callbackManager: CallbackManager? = null
  private var loginManager: LoginManager? = null

  @Volatile private var listener: Listener? = null

  private val callback =
    object : FacebookCallback<LoginResult> {
      override fun onSuccess(result: LoginResult) {
        requestProfile(result.accessToken)
      }

      override fun onCancel() {
        listener?.onCancelled()
      }

      override fun onError(exception: FacebookException) {
        listener?.onError(classify(exception))
      }
    }

  /**
   * Initializes the Facebook SDK from the runtime configuration (no manifest
   * meta-data required): the application id must be set before
   * [FacebookSdk.sdkInitialize] so initialization picks up the explicit value
   * instead of a manifest lookup. Idempotent: repeated calls only re-apply the
   * same values.
   */
  fun initialize(config: FacebookConfig) {
    FacebookSdk.setApplicationId(config.facebookAppId)
    config.facebookClientToken?.let { FacebookSdk.setClientToken(it) }
    FacebookSdk.sdkInitialize(activity.applicationContext)
    ensureSdkSurface()
  }

  /**
   * Registers the [Listener]. The SDK callback binding is deferred to
   * [initialize]: registering it here would touch the uninitialized SDK and
   * break plugin `load()` (a demo app without a facebook app id would crash the
   * whole plugin — Google/Apple/version included). Only the listener is kept.
   */
  fun register(listener: Listener) {
    this.listener = listener
  }

  override fun launchLogin(scopes: List<String>) {
    val login = loginManager
    if (login == null) {
      // SDK never initialized: no facebook app id configured. Fail with the
      // existing typed category; zero new codes.
      listener?.onError(FacebookSignInError.MISSING_CONFIGURATION)
      return
    }
    login.logInWithReadPermissions(activity, scopes)
  }

  override fun logOut() {
    loginManager?.logOut()
  }

  /**
   * Forwards the plugin's `onActivityResult` event into the facebook
   * CallbackManager ([FacebookSdkGateway] dialog surface).
   *
   * @return `true` when the event was consumed by the facebook callback manager.
   */
  fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ): Boolean = callbackManager?.onActivityResult(requestCode, resultCode, data) ?: false

  /**
   * Creates the CallbackManager/LoginManager surface and binds the SDK callback
   * listener. Called from [initialize] only, AFTER `FacebookSdk.sdkInitialize`
   * succeeded — safe to touch the SDK there (load-safety). Keeps
   * `register` and the constructor free of SDK access.
   */
  private fun ensureSdkSurface() {
    if (callbackManager != null && loginManager != null) return
    val manager = CallbackManager.Factory.create()
    val login = LoginManager.getInstance()
    login.registerCallback(manager, callback)
    callbackManager = manager
    loginManager = login
  }

  /**
   * Requests the `/me` profile for the fresh access token and delivers the
   * complete outcome only after the profile lands (email and picture are not
   * present on the login result itself).
   */
  private fun requestProfile(token: AccessToken) {
    val request =
      GraphRequest.newMeRequest(token) { json, response ->
        onProfileFetched(token, json, response)
      }
    request.parameters = Bundle().apply { putString(FIELDS_PARAM_NAME, FIELDS_PARAM) }
    request.executeAsync()
  }

  private fun onProfileFetched(
    token: AccessToken,
    json: JSONObject?,
    response: GraphResponse?,
  ) {
    val listener = listener ?: return
    val expiresAtMillis = token.expires?.time
    when {
      // Key-hash mismatches surface during login (not on /me); a Graph failure
      // here is a network or server-side problem.
      response?.error != null -> listener.onError(FacebookSignInError.GRAPH_NETWORK)
      json == null -> listener.onError(FacebookSignInError.MALFORMED_INPUT)
      else ->
        listener.onLoginSuccess(
          accessToken = token.token,
          userId = token.userId,
          grantedPermissions = token.permissions?.mapNotNull { it },
          declinedPermissions = token.declinedPermissions?.mapNotNull { it },
          expiresAtMillis = expiresAtMillis,
          profile = parseProfile(json),
        )
    }
  }

  /**
   * Classifies a raw SDK login exception onto the typed category set, mirroring
   * [FacebookErrorMapper.mapSdkMessage]: the Android key-hash registration
   * failure (`1349094`) is the actionable initialization case; blank messages
   * are malformed input; everything else is treated as a Graph/network failure.
   */
  private fun classify(exception: FacebookException): FacebookSignInError =
    when {
      FacebookErrorMapper.isKeyHashMismatch(exception.message) ->
        FacebookSignInError.KEY_HASH_MISMATCH
      exception.message.isNullOrBlank() -> FacebookSignInError.MALFORMED_INPUT
      else -> FacebookSignInError.GRAPH_NETWORK
    }

  /**
   * Parses the `/me` payload into the provider-agnostic profile model. org.json
   * coercion gotcha: `optString` returns the literal string `"null"` for
   * `JSONObject.NULL` values, so every field is guarded through
   * [optStringOrNull] before coercion.
   */
  private fun parseProfile(json: JSONObject): SocialAuthResultUser =
    SocialAuthResultUser(
      id = optStringOrNull(json, KEY_ID),
      email = optStringOrNull(json, KEY_EMAIL),
      name = optStringOrNull(json, KEY_NAME)?.let { DisplayName(it) },
      picture =
        json
          .optJSONObject(KEY_PICTURE)
          ?.optJSONObject(DATA_KEY)
          ?.let { optStringOrNull(it, URL_KEY) },
    )

  /** Coerces a JSON string field, treating absent/`JSONObject.NULL`/blank as absent. */
  private fun optStringOrNull(
    json: JSONObject,
    key: String,
  ): String? = if (json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() }
}
