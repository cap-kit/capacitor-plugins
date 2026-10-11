package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError

/**
 * @file StateNonceValidator.kt
 * Validates the OAuth `state` and OpenID Connect `nonce` returned by the Apple
 * flow against the values this sign-in requested.
 *
 * A mismatch — or a missing value where one was expected — maps to the plugin
 * `INVALID_INPUT` error; the caller aborts the flow. Pure and side-effect free.
 */
object StateNonceValidator {
  /**
   * @param state the `state` value returned by the callback (may be absent).
   * @param expectedState the `state` value issued in the authorization URL.
   * @param nonce the `nonce` claim extracted from the id_token (may be absent).
   * @param expectedNonce the `nonce` value issued in the authorization URL.
   * @return an [AuthenticationError.InvalidInput] describing the first mismatch,
   *         or `null` when both values match their expectations.
   */
  fun validate(
    state: String?,
    expectedState: String,
    nonce: String? = null,
    expectedNonce: String? = null,
  ): AuthenticationError? =
    when {
      state == null -> AuthenticationError.InvalidInput("OAuth state is missing.")
      state != expectedState -> AuthenticationError.InvalidInput("OAuth state mismatch.")
      expectedNonce != null && nonce == null ->
        AuthenticationError.InvalidInput("OpenID nonce claim is missing.")
      expectedNonce != null && nonce != expectedNonce ->
        AuthenticationError.InvalidInput("OpenID nonce mismatch.")
      else -> null
    }
}
