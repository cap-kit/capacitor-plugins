package io.capkit.tlsfingerprint.config

import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class RawConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("fingerprint")
  val fingerprint: String? = null,
  @SerialName("fingerprints")
  val fingerprints: List<String> = emptyList(),
  @SerialName("excludedDomains")
  val excludedDomains: List<String> = emptyList(),
)

/**
 * Plugin configuration container.
 *
 * This class is responsible for reading and exposing
 * static configuration values defined under the
 * `TLSFingerprint` key in capacitor.config.ts.
 *
 * Configuration rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime input
 * - Accessible only from native code
 */
class TLSFingerprintConfig(
  plugin: Plugin,
) {
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information
   * is printed to Logcat.
   *
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * Default SHA-256 fingerprint used by checkCertificate()
   * when no fingerprint is provided at runtime.
   */
  val fingerprint: String?

  /**
   * Default SHA-256 fingerprints used by checkCertificates()
   * when no fingerprints are provided at runtime.
   */
  val fingerprints: List<String>

  /**
   * Domains or URL prefixes excluded from SSL pinning.
   *
   * Any request whose host matches one of these values
   * MUST bypass SSL pinning checks.
   */
  val excludedDomains: List<String>

  // -----------------------------------------------------------------------------
  // Initialization
  // -----------------------------------------------------------------------------

  init {
    val configObject = plugin.config.getConfigJSON()
    val parsedConfig = parseConfig(configObject?.toString())

    verboseLogging = parsedConfig.verboseLogging
    fingerprint = parsedConfig.fingerprint?.takeIf { it.isNotBlank() }
    fingerprints = parsedConfig.fingerprints.filter { it.isNotBlank() }
    excludedDomains = parsedConfig.excludedDomains.filter { it.isNotBlank() }
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
