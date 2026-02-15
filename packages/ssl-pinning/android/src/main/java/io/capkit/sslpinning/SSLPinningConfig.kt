package io.capkit.sslpinning

import android.content.Context
import com.getcapacitor.Plugin

/**
 * Plugin configuration container.
 *
 * This class is responsible for reading and exposing
 * static configuration values defined under the
 * `SSLPinning` key in capacitor.config.ts.
 *
 * Configuration rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime input
 * - Accessible only from native code
 */
class SSLPinningConfig(
  plugin: Plugin,
) {
  /**
   * Android application context.
   * Exposed for native components that may require it.
   */
  val context: Context = plugin.context

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

  init {
    val config = plugin.getConfig()

    // Verbose logging flag
    verboseLogging =
      config.getBoolean("verboseLogging", false)

    // Single fingerprint (optional)
    val fp = config.getString("fingerprint")
    fingerprint =
      if (!fp.isNullOrBlank()) fp else null

    // Multiple fingerprints (optional)
    fingerprints =
      config
        .getArray("fingerprints")
        ?.toList()
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
  }
}
