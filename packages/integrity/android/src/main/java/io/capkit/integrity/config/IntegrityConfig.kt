package io.capkit.integrity.config

import com.getcapacitor.Plugin

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
 */
class IntegrityConfig(
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
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * Enables the native integrity block page.
   *
   * Default: false
   */
  val blockPageEnabled: Boolean

  /**
   * URL used by the native block page when enabled.
   */
  val blockPageUrl: String?

  init {
    val config = plugin.getConfig()

    // Verbose logging flag
    verboseLogging =
      config.getBoolean("verboseLogging", false)

    val blockPage = config.getObject("blockPage")

    blockPageEnabled = blockPage?.getBoolean("enabled") ?: false

    blockPageUrl = blockPage?.getString("url")
  }
}
