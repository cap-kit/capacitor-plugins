package io.capkit.authentication.facebook

import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.model.SocialAuthResultUser
import io.capkit.authentication.vault.TokenBundle

/**
 * SDK surface seam for the Android Facebook flow. The wiring slice binds this
 * to the real `facebook-login` 18.3.0 artifacts (CallbackManager / LoginManager
 * / GraphRequest via the plugin Activity). The pure orchestration core in
 * [FacebookSignInImpl] depends only on this interface, keeping every behavioral
 * decision JVM-testable without the SDK on the classpath.
 */
interface FacebookSdkGateway {
  /**
   * Launches the native Facebook login dialog with the requested read permissions.
   *
   * @param scopes the read permissions to request (defaults applied by the caller).
   */
  fun launchLogin(scopes: List<String>)

  /** Logs the current Facebook session out of the SDK. */
  fun logOut()
}

/**
 * @file FacebookSignInImpl.kt
 * Pure Android Facebook sign-in orchestration core: the single-flight
 * guard (existing `CONFLICT` semantics), the typed
 * outcome mapping onto [FacebookErrorMapper] (ten codes, zero new), and the vault
 * persist/clear hooks. The SDK dialog surface is injected as [FacebookSdkGateway]
 * — the wiring slice binds CallbackManager/LoginManager/GraphRequest,
 * `FacebookActivity` manifest merge and the plugin call lifecycle. Side-effect free
 * apart from the injected hooks; the in-flight flag is released in a finally-equivalent
 * (call-lifecycle guarantee).
 */
class FacebookSignInImpl(
  private val gateway: FacebookSdkGateway,
  private val persist: (TokenBundle) -> Unit,
  private val clearVault: () -> Unit,
) {
  private var inFlight = false

  /** True while an interactive Facebook login is running (single-flight). */
  val isInFlight: Boolean
    get() = inFlight

  /**
   * Begins the interactive flow if none is running.
   *
   * @param scopes the read permissions to request.
   * @return `null` when the flow started, or the mapped `CONFLICT` error when a
   *         sign-in is already in progress (existing single-flight code).
   */
  fun begin(scopes: List<String>): AuthenticationError? {
    if (inFlight) {
      return AuthenticationError.Conflict("A Facebook sign-in is already in progress.")
    }
    inFlight = true
    gateway.launchLogin(scopes)
    return null
  }

  /**
   * Completes the flow with a successful SDK login result: maps the token/profile
   * primitives onto the [TokenBundle] (four slots + `picture`), persists it through
   * the injected hook and releases the single-flight gate.
   */
  fun completeSuccess(
    accessToken: String,
    userId: String? = null,
    grantedPermissions: Collection<String>? = null,
    declinedPermissions: Collection<String>? = null,
    expiresAtMillis: Long? = null,
    profile: SocialAuthResultUser? = null,
  ): TokenBundle {
    val bundle =
      FacebookTokenMapper.mapSignInResult(
        accessToken = accessToken,
        userId = userId,
        grantedPermissions = grantedPermissions,
        declinedPermissions = declinedPermissions,
        expiresAtMillis = expiresAtMillis,
        profile = profile,
      )
    try {
      persist(bundle)
    } finally {
      inFlight = false
    }
    return bundle
  }

  /** Completes the flow as user-cancelled: releases the gate, persists nothing. */
  fun completeCancellation() {
    inFlight = false
  }

  /**
   * Completes the flow as a typed failure: releases the gate and maps the
   * [FacebookSignInError] onto the ten-code native error model.
   */
  fun completeFailure(error: FacebookSignInError): AuthenticationError {
    inFlight = false
    return FacebookErrorMapper.map(error)
  }

  /** Signs out: SDK logout plus vault clear. */
  fun signOut() {
    gateway.logOut()
    clearVault()
  }
}
