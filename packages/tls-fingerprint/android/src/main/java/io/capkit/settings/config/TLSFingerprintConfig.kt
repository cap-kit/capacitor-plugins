package io.capkit.tlsfingerprint.config

import com.getcapacitor.Plugin

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
  // Configuration Keys
  // -----------------------------------------------------------------------------

  /**
   * Centralized definition of configuration keys.
   * Avoids string duplication and typos.
   */
  private object Keys {
    const val VERBOSE_LOGGING = "verboseLogging"
    const val FINGERPRINT = "fingerprint"
    const val FINGERPRINTS = "fingerprints"
    const val EXCLUDED_DOMAINS = "excludedDomains"
  }

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
    val config = plugin.getConfig()

    // Verbose logging flag
    verboseLogging =
      config.getBoolean(Keys.VERBOSE_LOGGING, false)

    // Single fingerprint (optional)
    val fp = config.getString(Keys.FINGERPRINT)
    fingerprint =
      if (!fp.isNullOrBlank()) fp else null

    // Multiple fingerprints (optional)
    fingerprints =
      config
        .getArray(Keys.FINGERPRINTS)
        ?.toList()
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    excludedDomains =
      config
        .getArray(Keys.EXCLUDED_DOMAINS)
        ?.toList()
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
  }
}
