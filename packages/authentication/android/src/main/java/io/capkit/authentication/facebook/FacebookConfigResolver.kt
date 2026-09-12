package io.capkit.authentication.facebook

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * @file FacebookConfigResolver.kt
 * Pure resolver extracting the typed [FacebookConfig] from the raw Capacitor config
 * JSON delivered by the bridge (`facebook` sub-object, `AppleConfigResolver` /
 * `GoogleConfigResolver` parity). Native-only read; unknown keys are ignored so the
 * config contract can grow without breaking older binaries. Side-effect free.
 */
object FacebookConfigResolver {
  private val json =
    Json {
      ignoreUnknownKeys = true
    }

  /**
   * Resolves the `facebook` sub-object from the plugin config JSON.
   *
   * @param configJson the raw configuration JSON from the Capacitor bridge.
   * @return the typed [FacebookConfig], or `null` when the `facebook` block is
   *         absent, lacks the required app id, or the JSON is malformed (caller
   *         decides the failure surface — the validator maps `null` to `INIT_FAILED`).
   */
  fun resolve(configJson: String): FacebookConfig? =
    runCatching {
      json.parseToJsonElement(configJson).jsonObject["facebook"]?.jsonObject
    }.getOrNull()?.let { facebookObject ->
      runCatching {
        json.decodeFromJsonElement(FacebookConfig.serializer(), facebookObject)
      }.getOrNull()
    }
}
