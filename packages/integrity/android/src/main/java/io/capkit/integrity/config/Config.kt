package io.capkit.integrity.config

import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Configuration for the optional integrity block page.
 *
 * This configuration controls the availability and source
 * of a developer-provided HTML page that may be presented
 * to the end user when the host application decides to do so.
 *
 * @property enabled Enables the block page feature.
 * @property url URL or local path of the HTML page to present.
 * @property preventTapJacking Enables tap-jacking prevention (Android only).
 */
@Serializable
data class BlockPageConfig(
  @SerialName("enabled")
  val enabled: Boolean = false,
  @SerialName("url")
  val url: String? = null,
  @SerialName("preventTapJacking")
  val preventTapJacking: Boolean = false,
)

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class RawConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("blockPage")
  val blockPage: BlockPageConfig? = null,
)

/**
 * Plugin configuration container.
 *
 * This class is responsible for reading and exposing
 * static configuration values defined under the
 * `Integrity` key in capacitor.config.ts.
 *
 * Configuration rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime input
 * - Accessible only from native code
 *
 * @property verboseLogging Enables verbose native logging.
 * @property blockPage Optional block page configuration.
 */
class Config(
  plugin: Plugin,
) {
  // -----------------------------------------------------------------------------
  // Public Configuration Values
  // -----------------------------------------------------------------------------

  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information
   * is printed to Logcat.
   *
   * @default false
   */
  val verboseLogging: Boolean

  /**
   * Optional configuration for the integrity block page.
   *
   * Controls the availability and source of a developer-provided
   * HTML page that may be presented to the end user when the host
   * application decides to do so.
   *
   * @see BlockPageConfig
   */
  val blockPage: BlockPageConfig?

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val configObject = plugin.config.getConfigJSON()
    val parsedConfig = parseConfig(configObject?.toString())

    verboseLogging = parsedConfig.verboseLogging

    // Sanitizamos la propiedad `url` de blockPage si viene vacía o en blanco
    blockPage =
      parsedConfig.blockPage?.let { bp ->
        bp.copy(url = bp.url?.takeIf { it.isNotBlank() })
      }
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(jsonString: String?): RawConfigData {
      if (jsonString.isNullOrBlank()) return RawConfigData()
      return try {
        json.decodeFromString<RawConfigData>(jsonString)
      } catch (_: Exception) {
        RawConfigData()
      }
    }
  }
}
