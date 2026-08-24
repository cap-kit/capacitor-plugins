package io.capkit.device

import android.content.Context
import io.capkit.device.logger.DeviceLogger

/**
 * Platform-specific native implementation for the Device plugin.
 *
 * This class contains pure Android logic and MUST NOT depend directly on
 * Capacitor bridge APIs or PluginCall objects.
 *
 * Responsibilities:
 * - Hosting pure Android device-information logic.
 * - Translating configuration into native behavior.
 */
class DeviceImpl(
  private val context: Context,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: DeviceConfig

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * This method MUST be called exactly once during the plugin [DevicePlugin.load]
   * phase. It initializes internal state and configures logging verbosity.
   *
   * @param newConfig The immutable configuration instance.
   */
  fun updateConfig(newConfig: DeviceConfig) {
    this.config = newConfig
    DeviceLogger.verbose = newConfig.verboseLogging
    DeviceLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }
}
