package io.capkit.test

import android.content.Context
import io.capkit.test.utils.Logger

/**
 * Platform-specific native implementation for the Test plugin.
 *
 * This class contains pure Android logic and MUST NOT depend
 * directly on Capacitor bridge APIs.
 *
 * The Capacitor plugin class is responsible for:
 * - reading configuration
 * - handling PluginCall objects
 * - delegating logic to this implementation
 */
class Test(
  private val context: Context,
) {
  /**
   * Cached plugin configuration.
   * Provided once during initialization.
   */
  private lateinit var config: TestConfig

  /**
   * Applies the plugin configuration.
   *
   * This method should be called exactly once during plugin load
   * and is responsible for translating configuration values into
   * runtime behavior (e.g. logging verbosity).
   */
  fun updateConfig(newConfig: TestConfig) {
    this.config = newConfig
    Logger.verbose = this.config.verboseLogging
    Logger.debug("Configuration updated. Verbose logging: ${this.config.verboseLogging}")
  }

  /**
   * Echoes the provided value.
   *
   * This method represents a simple synchronous native operation
   * and is intentionally side-effect free.
   */
  fun echo(value: String): String {
    Logger.debug(value)
    return value
  }

  companion object {
    /**
     * Account type identifier (example constant).
     */
    const val ACCOUNT_TYPE = "io.capkit.test"

    /**
     * Human-readable account name (example constant).
     */
    const val ACCOUNT_NAME = "Test"
  }
}
