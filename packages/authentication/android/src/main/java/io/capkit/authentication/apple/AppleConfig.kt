package io.capkit.authentication.apple

import kotlinx.serialization.Serializable

/**
 * @file AppleConfig.kt
 * Typed `apple` configuration sub-object for the Authentication plugin (Android).
 *
 * Read-only and native-only (configuration-integrity): the JSON is decoded from the
 * Capacitor bridge config at call time and never mutated; raw values never reach JS.
 *
 * Web parity note: default scopes mirror the Web provider's `name email` set.
 */
@Serializable
data class AppleConfig(
  val clientId: String,
  val redirectURI: String? = null,
  val scopes: List<String> = DEFAULT_SCOPES,
  val nonce: String? = null,
  val state: String? = null,
) {
  companion object {
    /** Default Apple scopes, matching the Web provider defaults. */
    val DEFAULT_SCOPES = listOf("name", "email")
  }
}
