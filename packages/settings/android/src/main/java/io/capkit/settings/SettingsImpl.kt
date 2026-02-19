package io.capkit.settings

import android.content.Context
import android.content.Intent
import io.capkit.settings.config.SettingsConfig
import io.capkit.settings.error.SettingsError
import io.capkit.settings.logger.SettingsLogger
import io.capkit.settings.utils.SettingsUtils

/**
 Native Android implementation for the Settings plugin.

 Responsibilities:
 - Perform platform logic
 - Throw typed SettingsError values on failure

 Forbidden:
 - Accessing PluginCall
 - Referencing Capacitor APIs
 */
class SettingsImpl(
  private val context: Context,
) {
  /**
   * Cached plugin configuration.
   * Injected once during plugin initialization.
   */
  private lateinit var config: SettingsConfig

  /**
   * Applies plugin configuration.
   *
   * This method MUST be called exactly once from the plugin's load() method.
   * It translates static configuration into runtime behavior
   * (e.g. enabling verbose logging).
   */
  fun updateConfig(newConfig: SettingsConfig) {
    this.config = newConfig
    SettingsLogger.verbose = newConfig.verboseLogging
    SettingsLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  /**
   * Opens an Android system settings screen.
   *
   * @throws SettingsError if the operation fails
   */
  fun open(option: String): Intent {
    val intent =
      SettingsUtils.resolveIntent(option, context.packageName)
        ?: throw SettingsError.Unavailable(
          "Requested setting is not available on Android",
        )

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (intent.resolveActivity(context.packageManager) == null) {
      throw SettingsError.Unavailable(
        "No activity found to handle settings intent",
      )
    }

    return intent
  }
}
