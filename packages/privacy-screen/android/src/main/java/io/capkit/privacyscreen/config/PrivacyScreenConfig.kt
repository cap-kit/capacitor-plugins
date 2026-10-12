package io.capkit.privacyscreen.config

import android.content.Context
import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class PrivacyScreenConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("enabled")
  val enabled: Boolean = false,
)

/**
 * Plugin configuration holder for the PrivacyScreen plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.PrivacyScreen` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization in the load() phase.
 * - Configuration values are read-only at runtime.
 * - Consumed only by native code.
 */
class PrivacyScreenConfig(
  plugin: Plugin,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Android application context.
   * Accessible for native implementation components that require system services.
   */
  val context: Context = plugin.context

  /**
   * Enables verbose native logging via PrivacyScreenLogger.
   *
   * When true, additional debug information and lifecycle events are printed to Logcat.
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * Enables privacy protection automatically when the native plugin
   * loads, without waiting for the first `enable()` call.
   * Default: false
   */
  val enabled: Boolean

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val parsedConfig = parseConfig(plugin)

    verboseLogging = parsedConfig.verboseLogging
    enabled = parsedConfig.enabled
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(plugin: Plugin): PrivacyScreenConfigData =
      try {
        val configJsonObject = plugin.config.getConfigJSON()
        if (configJsonObject != null) {
          json.decodeFromString<PrivacyScreenConfigData>(configJsonObject.toString())
        } else {
          PrivacyScreenConfigData()
        }
      } catch (_: Exception) {
        PrivacyScreenConfigData()
      }
  }
}
