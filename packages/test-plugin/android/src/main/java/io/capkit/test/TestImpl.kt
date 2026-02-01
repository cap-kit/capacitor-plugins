package io.capkit.test

import android.content.Context
import io.capkit.test.utils.TestLogger

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
class TestImpl(
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
    TestLogger.verbose = newConfig.verboseLogging
    TestLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  /**
   * Echoes the provided value.
   *
   * This method represents a simple synchronous native operation
   * and is intentionally side-effect free.
   */
  fun echo(value: String): String {
    TestLogger.debug("Echoing value:", value)
    return value
  }

  /**
   * Opens the system app settings screen.
   *
   * Returns a Result<Unit> to explicitly model success or failure.
   */
  fun openAppSettings(): Result<Unit> {
    return try {
      // Actual intent execution is delegated to the Plugin layer
      Result.success(Unit)
    } catch (e: SecurityException) {
      Result.failure(TestError.PermissionDenied("Permission denied"))
    } catch (e: Exception) {
      Result.failure(TestError.Unavailable("Settings not available"))
    }
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
