package io.capkit.device

import android.content.Context
import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class DeviceConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
)

/**
 * Plugin configuration holder for the Device plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.Device` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization in the load() phase.
 * - Configuration values are read-only at runtime.
 * - Consumed only by native code.
 */
class DeviceConfig(
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
   * Enables verbose native logging via DeviceLogger.
   *
   * When true, additional debug information and lifecycle events are printed to Logcat.
   * Default: false
   */
  val verboseLogging: Boolean

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val parsedConfig = parseConfig(plugin)

    verboseLogging = parsedConfig.verboseLogging
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(plugin: Plugin): DeviceConfigData =
      try {
        val configJsonObject = plugin.config.getConfigJSON()
        if (configJsonObject != null) {
          json.decodeFromString<DeviceConfigData>(configJsonObject.toString())
        } else {
          DeviceConfigData()
        }
      } catch (_: Exception) {
        DeviceConfigData()
      }
  }
}
