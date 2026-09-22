package io.capkit.authentication.google

import io.capkit.authentication.error.AuthenticationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * @file GoogleConfigResolver.kt
 * Pure resolver extracting the typed [GoogleConfig] from the raw Capacitor config
 * JSON delivered by the bridge. Native-only read; unknown keys are ignored so the
 * config contract can grow without breaking older binaries. Side-effect free.
 */
object GoogleConfigResolver {
  private val json =
    Json {
      ignoreUnknownKeys = true
    }

  /**
   * Resolves the `google` sub-object from the plugin config JSON.
   *
   * @param configJson the raw configuration JSON from the Capacitor bridge.
   * @return the typed [GoogleConfig], or `null` when the `google` block is absent
   *         or the JSON is malformed (caller decides the failure surface).
   */
  fun resolve(configJson: String): GoogleConfig? =
    runCatching {
      json.parseToJsonElement(configJson).jsonObject["google"]?.jsonObject
    }.getOrNull()?.let { googleObject ->
      runCatching {
        json.decodeFromJsonElement(GoogleConfig.serializer(), googleObject)
      }.getOrNull()
    }
}

/**
 * @file GoogleConfigValidator.kt
 * Pure validation gate for the resolved [GoogleConfig]. Absent configuration is an
 * initialization failure; a blank client id is caller input failure. Side-effect free.
 */
object GoogleConfigValidator {
  /**
   * @param config the resolved [GoogleConfig], possibly `null` when absent.
   * @return the mapped [AuthenticationError] when invalid, or `null` when valid.
   */
  fun validate(config: GoogleConfig?): AuthenticationError? =
    when {
      config == null -> AuthenticationError.InitFailed("Missing 'google' configuration; provide serverClientId.")
      config.serverClientId.isBlank() ->
        AuthenticationError.InvalidInput("google.serverClientId must not be blank.")
      else -> null
    }
}
