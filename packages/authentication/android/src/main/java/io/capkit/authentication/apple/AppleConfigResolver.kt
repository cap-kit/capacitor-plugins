package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * @file AppleConfigResolver.kt
 * Pure resolver extracting the typed [AppleConfig] from the raw Capacitor config
 * JSON delivered by the bridge. Native-only read; unknown keys are ignored so the
 * config contract can grow without breaking older binaries. Side-effect free.
 */
object AppleConfigResolver {
  private val json =
    Json {
      ignoreUnknownKeys = true
    }

  /**
   * Resolves the `apple` sub-object from the plugin config JSON.
   *
   * @param configJson the raw configuration JSON from the Capacitor bridge.
   * @return the typed [AppleConfig], or `null` when the `apple` block is absent
   *         or the JSON is malformed (caller decides the failure surface).
   */
  fun resolve(configJson: String): AppleConfig? =
    runCatching {
      json.parseToJsonElement(configJson).jsonObject["apple"]?.jsonObject
    }.getOrNull()?.let { appleObject ->
      runCatching {
        json.decodeFromJsonElement(AppleConfig.serializer(), appleObject)
      }.getOrNull()
    }
}

/**
 * @file AppleConfigValidator.kt
 * Pure validation gate for the resolved [AppleConfig]. Absent configuration is an
 * initialization failure; a blank client id or redirect URI is caller input
 * failure. Side-effect free.
 */
object AppleConfigValidator {
  /**
   * @param config the resolved [AppleConfig], possibly `null` when absent.
   * @return the mapped [AuthenticationError] when invalid, or `null` when valid.
   */
  fun validate(config: AppleConfig?): AuthenticationError? =
    when {
      config == null ->
        AuthenticationError.InitFailed("Missing 'apple' configuration; provide clientId.")
      config.clientId.isBlank() ->
        AuthenticationError.InvalidInput("apple.clientId must not be blank.")
      config.redirectURI?.isBlank() == true ->
        AuthenticationError.InvalidInput("apple.redirectURI must not be blank.")
      else -> null
    }
}
