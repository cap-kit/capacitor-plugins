package io.capkit.settings.config

import android.content.Context
import com.getcapacitor.Plugin

/**
 * Plugin configuration holder.
 *
 * This class is responsible for reading and parsing configuration values
 * defined under the `Settings` key in `capacitor.config.ts`.
 *
 * Configuration is read once during plugin initialization and treated as
 * immutable runtime input.
 */
class SettingsConfig(
  plugin: Plugin,
) {
  /**
   * Android application context.
   * Exposed for native components that may require it.
   */
  val context: Context

  /**
   * Enables verbose / debug logging for the plugin.
   *
   * When enabled, additional logs are printed to Logcat via [Logger.debug].
   *
   * Default: false
   */
  val verboseLogging: Boolean

  init {
    context = plugin.context

    val config = plugin.getConfig()

    // Parse verboseLogging (default: false)
    verboseLogging = config.getBoolean("verboseLogging", false)
  }
}
