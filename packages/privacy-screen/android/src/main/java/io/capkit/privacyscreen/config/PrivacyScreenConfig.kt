package io.capkit.privacyscreen.config

import android.content.Context
import com.getcapacitor.Plugin
import io.capkit.privacyscreen.utils.PrivacyScreenValidators
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class PrivacyScreenConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("enabled")
  val enabled: Boolean = false,
  // `color` accepts a hex string or `{ light: hex, dark: hex }`; parsed
  // manually below because kotlinx has no union type.
  @SerialName("color")
  val color: JsonElement? = null,
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

  /**
   * Hex color for the light appearance (`color` string or `color.light`),
   * validated with [PrivacyScreenValidators.isValidHexColor]; null when
   * absent or invalid. Falls back to [colorDark] usage rules: each variant
   * is used as-is for its appearance, and a single variant covers both when
   * only one is set.
   * Default: null
   */
  val colorLight: String?

  /**
   * Hex color for the dark appearance (`color` string or `color.dark`),
   * validated with [PrivacyScreenValidators.isValidHexColor]; null when
   * absent or invalid.
   * Default: null
   */
  val colorDark: String?

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val parsedConfig = parseConfig(plugin)

    verboseLogging = parsedConfig.verboseLogging
    enabled = parsedConfig.enabled
    val colors = normalizeColor(parsedConfig.color)
    colorLight = colors.first
    colorDark = colors.second
  }

  /**
   * Normalizes the `color` option into (light, dark) hex pairs:
   * a string applies to both appearances; an object maps `light`/`dark`.
   * Invalid hex values are dropped (the platform default applies for that
   * appearance).
   */
  private fun normalizeColor(raw: JsonElement?): Pair<String?, String?> =
    when {
      raw == null -> null to null
      raw is JsonPrimitive -> {
        val content = raw.jsonPrimitive.content
        val hex = content.takeIf { PrivacyScreenValidators.isValidHexColor(it) }
        hex to hex
      }
      raw is JsonObject -> {
        val lightRaw = (raw["light"] as? JsonPrimitive)?.content
        val darkRaw = (raw["dark"] as? JsonPrimitive)?.content
        val light = lightRaw?.takeIf { PrivacyScreenValidators.isValidHexColor(it) }
        val dark = darkRaw?.takeIf { PrivacyScreenValidators.isValidHexColor(it) }
        light to dark
      }
      else -> null to null
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
