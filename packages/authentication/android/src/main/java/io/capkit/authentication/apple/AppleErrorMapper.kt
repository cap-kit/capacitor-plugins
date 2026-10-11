package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError

/**
 * Typed Apple sign-in failure categories consumed by [AppleErrorMapper]. These are
 * flow-level outcomes — the mapper translates them onto the shared ten-code
 * `AuthenticationErrorCode` set; no new codes exist.
 */
enum class AppleSignInError {
  /** The user dismissed or closed the sign-in UI. */
  USER_CANCELLED,

  /** The nonce returned by the flow does not match the one issued. */
  INVALID_NONCE,

  /** The state returned by the flow does not match the one issued. */
  INVALID_STATE,

  /** The apple configuration (clientId / redirectURI) is missing or malformed. */
  MISSING_CONFIGURATION,

  /** The WebView/popup failed for a network or server-side reason. */
  NETWORK,

  /** Any other, unclassified failure. */
  UNKNOWN,
}

/**
 * @file AppleErrorMapper.kt
 * Maps Apple sign-in failures onto the plugin's native [AuthenticationError]
 * model, which [io.capkit.authentication.utils.ErrorCodeMapper] resolves to the
 * shared ten-code JS set. Semantics mirror the Web reducer (`mapAppleSdkError` in
 * `src/web.ts`): cancellation → `USER_CANCELLED`, nonce/state problems →
 * `INVALID_INPUT`, configuration/network/unknown → `INIT_FAILED`. No new error
 * codes SHALL be introduced. Pure and side-effect free.
 */
object AppleErrorMapper {
  private val CANCELLATION_CODES = setOf("popup_closed_by_user", "user_cancelled_authorize", "access_denied")

  /**
   * Maps a typed [AppleSignInError] category onto a native [AuthenticationError].
   */
  fun map(error: AppleSignInError): AuthenticationError =
    when (error) {
      AppleSignInError.USER_CANCELLED ->
        AuthenticationError.UserCancelled("The user cancelled the Apple sign-in flow.")
      AppleSignInError.INVALID_NONCE ->
        AuthenticationError.InvalidInput("The Apple sign-in returned an invalid nonce.")
      AppleSignInError.INVALID_STATE ->
        AuthenticationError.InvalidInput("The Apple sign-in returned an invalid state.")
      AppleSignInError.MISSING_CONFIGURATION ->
        AuthenticationError.InitFailed("Apple configuration is missing or malformed.")
      AppleSignInError.NETWORK ->
        AuthenticationError.InitFailed("The Apple sign-in flow failed over the network.")
      AppleSignInError.UNKNOWN ->
        AuthenticationError.InitFailed("The Apple sign-in flow failed for an unknown reason.")
    }

  /**
   * Maps a raw SDK/WebView error string (as surfaced by `appleid.auth.js` or the
   * injected WebView callback) onto a native [AuthenticationError], mirroring the
   * Web reducer's code classification.
   *
   * @param code the SDK-provided error code string.
   * @param message the SDK-provided human message, used for the error text.
   */
  fun mapSdkError(
    code: String,
    message: String? = null,
  ): AuthenticationError {
    val detail = message ?: "Apple sign-in failed"
    return when {
      code in CANCELLATION_CODES -> AuthenticationError.UserCancelled(detail)
      code == "invalid_nonce" -> AuthenticationError.InvalidInput(detail)
      else -> AuthenticationError.InitFailed(detail)
    }
  }
}
