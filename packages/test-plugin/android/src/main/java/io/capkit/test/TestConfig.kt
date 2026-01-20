package io.capkit.test

import android.content.Context
import com.getcapacitor.Plugin

/**
 * Plugin configuration holder.
 *
 * This class is responsible for reading and parsing configuration values
 * defined under the `Test` key in `capacitor.config.ts`.
 *
 * Configuration is read once during plugin initialization and treated as
 * immutable runtime input.
 */
class TestConfig(plugin: Plugin) {
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

  /**
   * Optional custom message appended to echoed values.
   *
   * This value is provided for demonstration purposes and shows how to
   * pass static configuration from JavaScript to native code.
   */
  val customMessage: String

  init {
    context = plugin.context

    val config = plugin.getConfig()

    // Parse verboseLogging (default: false)
    verboseLogging = config.getBoolean("verboseLogging", false)

    // Parse customMessage (default fallback applied)
    val cm = config.getString("customMessage")
    customMessage = if (cm.isNullOrBlank()) " (from config)" else cm
  }
}
