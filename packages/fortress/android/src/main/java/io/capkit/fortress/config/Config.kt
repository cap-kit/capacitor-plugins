package io.capkit.fortress.config

import com.getcapacitor.Plugin

/**
 * Plugin configuration container.
 *
 * This class is responsible for reading and exposing
 * static configuration values defined under the
 * `Fortress` key in capacitor.config.ts.
 *
 * Configuration rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime input
 * - Accessible only from native code
 *
 * @property verboseLogging Enables verbose native logging.
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
    const val LOCK_AFTER_MS = "lockAfterMs"
    const val ENABLE_PRIVACY_SCREEN = "enablePrivacyScreen"
    const val OBFUSCATION_PREFIX = "obfuscationPrefix"
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
  val lockAfterMs: Int
  val enablePrivacyScreen: Boolean
  val obfuscationPrefix: String

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val config = plugin.config

    // Verbose logging flag
    verboseLogging =
      config.getBoolean(Keys.VERBOSE_LOGGING, false)

    lockAfterMs =
      config.getInt(Keys.LOCK_AFTER_MS, 60000)

    enablePrivacyScreen =
      config.getBoolean(Keys.ENABLE_PRIVACY_SCREEN, true)

    obfuscationPrefix =
      config.getString(Keys.OBFUSCATION_PREFIX, "ftrss_")
  }
}
