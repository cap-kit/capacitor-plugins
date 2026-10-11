package io.capkit.authentication.facebook

import kotlinx.serialization.Serializable

/**
 * @file FacebookConfig.kt
 * Typed `facebook` configuration sub-object for the Authentication plugin (Android).
 *
 * Read-only and native-only (configuration-integrity): the JSON is decoded from the
 * Capacitor bridge config at call time and never mutated; raw values never reach JS.
 *
 * `facebookAppId` is required (absent → `INIT_FAILED`, blank → `INVALID_INPUT`).
 * `facebookClientToken` is optional but non-blank when present;
 * `facebookVersion` defaults to the Web Graph version `v17.0` (native SDKs pin their
 * own); `scopes` default to `public_profile email`.
 */
@Serializable
data class FacebookConfig(
  val facebookAppId: String,
  val facebookClientToken: String? = null,
  val facebookVersion: String = DEFAULT_VERSION,
  val scopes: List<String> = DEFAULT_SCOPES,
) {
  companion object {
    /** Default Graph API version used by the Web provider; native SDKs pin their own. */
    const val DEFAULT_VERSION = "v17.0"

    /** Default read permissions, matching the Web provider defaults. */
    val DEFAULT_SCOPES = listOf("public_profile", "email")
  }
}
