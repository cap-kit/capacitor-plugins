package io.capkit.fortress

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
 *
 * @property verboseLogging Enables verbose native logging.
 * @property blockPage Optional block page configuration.
 */
class Config(
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
  }

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

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val config = plugin.config

    // Verbose logging flag
    verboseLogging =
      config.getBoolean(Keys.VERBOSE_LOGGING, false)
  }
}
