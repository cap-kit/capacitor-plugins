package io.capkit.authentication.facebook

import io.capkit.authentication.error.AuthenticationError

/**
 * @file FacebookConfigValidator.kt
 * Pure validation gate for the resolved [FacebookConfig].
 * Absent configuration is an initialization failure (`INIT_FAILED`,
 * "Missing 'facebook' configuration; provide appId."); a blank required value or an
 * explicitly empty scope set is caller input failure (`INVALID_INPUT`, `isBlank`
 * parity with `AppleConfigValidator` / `GoogleConfigValidator`). The client token
 * is OPTIONAL, but blank when present is caller error. Side-effect free.
 */
object FacebookConfigValidator {
  /**
   * @param config the resolved [FacebookConfig], possibly `null` when absent.
   * @return the mapped [AuthenticationError] when invalid, or `null` when valid.
   */
  fun validate(config: FacebookConfig?): AuthenticationError? =
    when {
      config == null ->
        AuthenticationError.InitFailed("Missing 'facebook' configuration; provide appId.")
      config.facebookAppId.isBlank() ->
        AuthenticationError.InvalidInput("facebook.appId must not be blank.")
      config.facebookClientToken?.isBlank() == true ->
        AuthenticationError.InvalidInput("facebook.clientToken must not be blank.")
      // The DTO applies the default scope set, so only an EXPLICIT empty list arrives here.
      config.scopes.isEmpty() ->
        AuthenticationError.InvalidInput("facebook.scopes must not be empty.")
      else -> null
    }
}
