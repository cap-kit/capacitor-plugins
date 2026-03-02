package io.capkit.fortress

import android.content.Context
import io.capkit.fortress.Config
import io.capkit.fortress.logger.Logger

/**
 * Platform-specific native implementation for the Fortress plugin.
 *
 * This class contains pure Android logic and MUST NOT depend
 * directly on Capacitor bridge APIs.
 *
 * The Capacitor plugin class is responsible for:
 * - reading configuration
 * - handling PluginCall objects
 * - delegating logic to this implementation
 */
class Fortress(
  private val context: Context,
) {
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Cached immutable plugin configuration.
   */
  private lateinit var config: Config

  /**
   * Applies static plugin configuration.
   */
  fun updateConfig(newConfig: Config) {
    this.config = newConfig
    Logger.verbose = newConfig.verboseLogging
    Logger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }
}
