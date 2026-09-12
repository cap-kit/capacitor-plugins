package io.capkit.authentication.facebook

import io.capkit.authentication.error.AuthenticationError

/**
 * Typed Facebook flow-level failure categories consumed by [FacebookErrorMapper].
 * These are outcome classes — the mapper translates them onto the shared ten-code
 * `AuthenticationErrorCode` set; no new codes exist.
 */
enum class FacebookSignInError {
  /** The user dismissed or closed the Facebook login dialog. */
  USER_CANCELLED,

  /** The facebook configuration (app id) is missing or malformed. */
  MISSING_CONFIGURATION,

  /** The Android key hash does not match the Meta developer settings (`1349094`). */
  KEY_HASH_MISMATCH,

  /** The Graph `/me` request failed for a network or server-side reason. */
  GRAPH_NETWORK,

  /** The flow returned blank or malformed input. */
  MALFORMED_INPUT,

  /** `refreshToken('facebook')` — Facebook issues no refresh token. */
  REFRESH_NOT_SUPPORTED,

  /** Any other, unclassified failure. */
  UNKNOWN,
}

/**
 * @file FacebookErrorMapper.kt
 * Maps Facebook sign-in failures onto the plugin's native [AuthenticationError]
 * model, which [io.capkit.authentication.utils.ErrorCodeMapper] resolves to the
 * shared ten-code JS set: cancellation → `USER_CANCELLED`;
 * missing config / Android keyhash `1349094` → actionable `INIT_FAILED`;
 * Graph/network → `UNAVAILABLE`; blank/malformed and no-refresh → `INVALID_INPUT`
 * (the latter with the exact "Facebook issues no refresh token" message). Zero new
 * error codes. Pure and side-effect free.
 */
object FacebookErrorMapper {
  /** The Android key-hash value in the SDK's registration-failure message (`FBLoginException`). */
  const val ANDROID_KEY_HASH_MISMATCH_CODE = "1349094"

  /** Exact no-refresh rejection message, shared across platforms. */
  const val REFRESH_NOT_SUPPORTED_MESSAGE = "Facebook issues no refresh token"

  private const val KEY_HASH_ACTIONABLE_MESSAGE =
    "Facebook sign-in failed: verify key hash matches Facebook developer settings."

  /**
   * Maps a typed [FacebookSignInError] category onto a native [AuthenticationError].
   */
  fun map(error: FacebookSignInError): AuthenticationError =
    when (error) {
      FacebookSignInError.USER_CANCELLED ->
        AuthenticationError.UserCancelled("The user cancelled the Facebook sign-in flow.")
      FacebookSignInError.MISSING_CONFIGURATION ->
        AuthenticationError.InitFailed("Facebook configuration is missing or malformed.")
      FacebookSignInError.KEY_HASH_MISMATCH ->
        AuthenticationError.InitFailed(KEY_HASH_ACTIONABLE_MESSAGE)
      FacebookSignInError.GRAPH_NETWORK ->
        AuthenticationError.Unavailable("The Facebook Graph request failed over the network.")
      FacebookSignInError.MALFORMED_INPUT ->
        AuthenticationError.InvalidInput("The Facebook sign-in returned malformed input.")
      FacebookSignInError.REFRESH_NOT_SUPPORTED ->
        AuthenticationError.InvalidInput(REFRESH_NOT_SUPPORTED_MESSAGE)
      FacebookSignInError.UNKNOWN ->
        AuthenticationError.InitFailed("The Facebook sign-in flow failed for an unknown reason.")
    }

  /**
   * True when an SDK exception message is the known Android key-hash registration
   * failure (message contains `1349094`).
   */
  fun isKeyHashMismatch(rawMessage: String?): Boolean = rawMessage?.contains(ANDROID_KEY_HASH_MISMATCH_CODE) == true

  /**
   * Classifies a raw SDK exception message onto the ten-code set:
   * keyhash `1349094` → actionable `INIT_FAILED`; blank/null message → `INVALID_INPUT`;
   * anything else (Graph/network) → `UNAVAILABLE`. Mirrors the Web reducer's
   * classification so all platforms report the same code.
   */
  fun mapSdkMessage(rawMessage: String?): AuthenticationError =
    when {
      isKeyHashMismatch(rawMessage) ->
        AuthenticationError.InitFailed(KEY_HASH_ACTIONABLE_MESSAGE)
      rawMessage.isNullOrBlank() ->
        AuthenticationError.InvalidInput("The Facebook SDK returned an empty error message.")
      else ->
        AuthenticationError.Unavailable(rawMessage)
    }
}
