package io.capkit.settings

import android.content.Context
import android.content.Intent
import io.capkit.settings.utils.SettingsLogger
import io.capkit.settings.utils.SettingsUtils

/**
 * Platform-specific native implementation for the Settings plugin (Android).
 *
 * This class contains ONLY Android platform logic and MUST NOT:
 * - depend on Capacitor APIs
 * - reference PluginCall
 * - perform JS-related validation
 *
 * The Capacitor plugin (SettingsPlugin) is responsible for:
 * - reading configuration
 * - extracting call parameters
 * - resolving results to JavaScript
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
   * The operation is considered successful if:
   * - the option maps to a valid Intent
   * - an Activity exists to handle the Intent
   *
   * No Activity result is awaited, by design.
   *
   * @param option JavaScript-facing settings key
   * @return standardized Result object (state-based)
   */
  fun open(option: String): Result {
    SettingsLogger.debug("Requested Android settings option:", option)

    val intent =
      SettingsUtils.resolveIntent(option, context.packageName)
        ?: return Result(
          success = false,
          error = "Requested setting is not available on Android",
          code = "UNAVAILABLE",
        )

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
      SettingsLogger.debug("Android settings activity started:", intent.action ?: "unknown")
      Result(success = true)
    } else {
      SettingsLogger.error("No activity found to handle intent for option: $option")
      Result(
        success = false,
        error = "Cannot open settings URL",
        code = "UNAVAILABLE",
      )
    }
  }

  /**
   * Standardized result returned by Settings operations.
   *
   * This mirrors the iOS result model and the public TypeScript API.
   */
  data class Result(
    val success: Boolean,
    val error: String? = null,
    val code: String? = null,
  )
}
