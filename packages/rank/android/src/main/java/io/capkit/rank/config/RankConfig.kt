package io.capkit.rank

import android.content.Context
import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class RankConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("androidPackageName")
  val androidPackageName: String? = null,
  @SerialName("fireAndForget")
  val fireAndForget: Boolean = false,
)

/**
 * Plugin configuration holder for the Rank plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.Rank` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization in the load() phase.
 * - Configuration values are read-only at runtime.
 * - Consumed only by native code.
 */
class RankConfig(
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
   * Enables verbose native logging via RankLogger.
   *
   * When true, additional debug information and lifecycle events are printed to Logcat.
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * The Android Package Name used for Play Store redirection.
   *
   * If provided, this value overrides the host application's package name
   * during store navigation.
   * Default: null (falls back to host app package)
   */
  val androidPackageName: String?

  /**
   * Global policy for review request resolution.
   *
   * If true, the `requestReview` method resolves the promise immediately
   * without waiting for the Google Play review flow to complete.
   * Default: false
   */
  val fireAndForget: Boolean

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val parsedConfig = parseConfig(plugin)

    verboseLogging = parsedConfig.verboseLogging
    androidPackageName = parsedConfig.androidPackageName?.ifBlank { null }
    fireAndForget = parsedConfig.fireAndForget
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(plugin: Plugin): RankConfigData =
      try {
        val configJsonObject = plugin.config.getConfigJSON()
        if (configJsonObject != null) {
          json.decodeFromString<RankConfigData>(configJsonObject.toString())
        } else {
          RankConfigData()
        }
      } catch (_: Exception) {
        RankConfigData()
      }
  }
}
