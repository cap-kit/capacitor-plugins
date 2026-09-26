package io.capkit.people.config

import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing the People configuration
 * section in capacitor.config.ts.
 *
 * The keys mirror the TypeScript `PeopleConfig` interface
 * (packages/people/src/definitions.ts).
 */
@Serializable
private data class RawPeopleConfig(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
)

/**
 * Plugin configuration holder for the People plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.People` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization in the load() phase.
 * - Configuration values are read-only at runtime.
 * - Consumed only by native code.
 */
class PeopleConfig(
  plugin: Plugin,
) {
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  /**
   * Enables verbose native logging via PeopleLogger.
   *
   * When true, additional debug information and lifecycle events are printed to Logcat.
   * This setting is read-only and applied during plugin initialization.
   *
   * Default: false
   */
  val verboseLogging: Boolean

  // -----------------------------------------------------------------------------
  // Initialization
  // -----------------------------------------------------------------------------

  init {
    val configObject = plugin.config.getConfigJSON()
    val parsedConfig = parseConfig(configObject?.toString())

    verboseLogging = parsedConfig.verboseLogging
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(jsonString: String?): RawPeopleConfig {
      if (jsonString.isNullOrBlank()) return RawPeopleConfig()
      return try {
        json.decodeFromString<RawPeopleConfig>(jsonString)
      } catch (_: Exception) {
        RawPeopleConfig()
      }
    }
  }
}
