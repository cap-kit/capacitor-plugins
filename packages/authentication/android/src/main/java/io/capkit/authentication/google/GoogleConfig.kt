package io.capkit.authentication.google

import kotlinx.serialization.Serializable

/**
 * @file GoogleConfig.kt
 * Typed `google` configuration sub-object for the Authentication plugin (Android).
 *
 * Read-only and native-only (configuration-integrity): the JSON is decoded from the
 * Capacitor bridge config at call time and never mutated; raw values never reach JS.
 *
 * Web parity note: default scopes mirror the Web provider's `openid email profile`.
 */
@Serializable
data class GoogleConfig(
  val serverClientId: String,
  val scopes: List<String> = DEFAULT_SCOPES,
  val autoSelect: Boolean = false,
  val nonce: String? = null,
) {
  companion object {
    /** Default OpenID Connect scopes, matching the Web provider defaults. */
    val DEFAULT_SCOPES = listOf("openid", "email", "profile")
  }
}
