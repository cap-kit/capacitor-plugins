package io.capkit.people.config

import com.getcapacitor.Plugin

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
    // Access the plugin-specific configuration object provided by Capacitor bridge
    val config = plugin.getConfig()

    // Extract verboseLogging flag with a safe default fallback
    verboseLogging = config.getBoolean("verboseLogging", false)
  }
}
