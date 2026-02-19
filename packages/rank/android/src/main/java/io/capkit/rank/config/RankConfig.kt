package io.capkit.rank

import android.content.Context
import com.getcapacitor.Plugin

/**
 * Plugin configuration holder for the Rank plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.Rank` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization in the load() phase.
 * - Configuration values are read-only at runtime.
 * - Consumed only by native code.
 */
class RankConfig(
  plugin: Plugin,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Android application context.
   * Accessible for native implementation components that require system services.
   */
  val context: Context = plugin.context

  /**
   * Enables verbose native logging via RankLogger.
   *
   * When true, additional debug information and lifecycle events are printed to Logcat.
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * The Android Package Name used for Play Store redirection.
   *
   * If provided, this value overrides the host application's package name
   * during store navigation.
   * Default: null (falls back to host app package)
   */
  val androidPackageName: String?

  /**
   * Global policy for review request resolution.
   *
   * If true, the `requestReview` method resolves the promise immediately
   * without waiting for the Google Play review flow to complete.
   * Default: false
   */
  val fireAndForget: Boolean

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    // Access the plugin-specific configuration object
    val config = plugin.getConfig()

    // Extract verboseLogging flag
    verboseLogging = config.getBoolean("verboseLogging", false)

    // Extract and validate the Android Package Name
    val apm = config.getString("androidPackageName")
    androidPackageName = if (!apm.isNullOrBlank()) apm else null

    // Extract the fireAndForget resolution policy
    fireAndForget = config.getBoolean("fireAndForget", false)
  }
}
