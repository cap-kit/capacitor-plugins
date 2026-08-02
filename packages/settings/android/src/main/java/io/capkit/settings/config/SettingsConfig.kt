package io.capkit.settings.config

import android.content.Context
import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class SettingsConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
)

/**
 * Plugin configuration holder for the Settings plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.Settings` key in `capacitor.config.ts`.
 *
 * Configuration is read once during plugin initialization and treated as
 * immutable runtime input.
 */
class SettingsConfig(
  plugin: Plugin,
) {
  /**
   * Android application context.
   * Exposed for native components that may require it.
   */
  val context: Context = plugin.context

  /**
   * Enables verbose / debug logging for the plugin.
   *
   * When enabled, additional logs are printed to Logcat via [Logger.debug].
   *
   * Default: false
   */
  val verboseLogging: Boolean

  init {
    verboseLogging = parseConfig(plugin).verboseLogging
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(plugin: Plugin): SettingsConfigData =
      try {
        val configJsonObject = plugin.config.getConfigJSON()
        if (configJsonObject != null) {
          json.decodeFromString<SettingsConfigData>(configJsonObject.toString())
        } else {
          SettingsConfigData()
        }
      } catch (_: Exception) {
        SettingsConfigData()
      }
  }
}
