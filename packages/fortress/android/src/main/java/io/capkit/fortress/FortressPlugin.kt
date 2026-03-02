package io.capkit.fortress

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.fortress.error.ErrorMessages
import io.capkit.fortress.error.NativeError
import io.capkit.fortress.logger.Logger

/**
 * Capacitor bridge for the Integrity plugin (Android).
 *
 * CONTRACT:
 * - This class is the ONLY entry point from JavaScript.
 * - All PluginCall instances MUST be resolved or rejected exactly once.
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Invoke the native implementation
 * - Resolve or reject PluginCall exactly once
 * - Map native NativeError to JS-facing error codes
 *
 * Forbidden:
 * - Platform-specific business logic
 * - Direct system API usage outside lifecycle-bound orchestration
 * - Throwing uncaught exceptions
 */
@CapacitorPlugin(
  name = "Fortress",
)
class FortressPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration.
   *
   * CONTRACT:
   * - Initialized exactly once in `load()`
   * - Treated as read-only afterward
   * - MUST NOT be mutated at runtime
   * - MUST NOT be accessed by the Impl layer
   */
  private lateinit var config: Config

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - Lifetime == plugin lifetime
   * - MUST NOT access PluginCall or Capacitor APIs
   * - MUST NOT perform UI operations
   */
  private lateinit var implementation: Fortress

  // ---------------------------------------------------------------------------
  // Companion Object
  // ---------------------------------------------------------------------------

  private companion object {
    /**
     * Account type identifier for internal plugin identification.
     */
    const val ACCOUNT_TYPE = "io.capkit.fortress"

    /**
     * Human-readable account name for the plugin.
     */
    const val ACCOUNT_NAME = "Fortress"
  }

  // -----------------------------------------------------------------------------
  // Lifecycle
  // -----------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This method initializes the configuration container and the native
   * implementation layer, ensuring all dependencies are injected.
   */
  override fun load() {
    super.load()

    config = Config(this)
    implementation = Fortress(context)
    implementation.updateConfig(config)

    Logger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native NativeError values to JavaScript-facing error codes.
   *
   * CONTRACT:
   * - This method is the ONLY place where native errors
   *   are translated into JS-visible failures.
   * - Error codes MUST be:
   *   - stable
   *   - documented
   *   - identical across platforms
   */
  private fun reject(
    call: PluginCall,
    error: NativeError,
  ) {
    val message = error.message ?: ErrorMessages.INTERNAL_ERROR
    call.reject(message, error.errorCode)
  }

  private fun handleError(
    call: PluginCall,
    throwable: Throwable,
  ) {
    if (throwable is NativeError) {
      reject(call, throwable)
    } else {
      val message = throwable.message ?: ErrorMessages.UNEXPECTED_NATIVE_ERROR
      reject(call, NativeError.InitFailed(message))
    }
  }

  // ---------------------------------------------------------------------------
  // Version
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version.
   *
   * NOTE:
   * - This method is guaranteed not to fail
   * - Therefore it does NOT use TestError
   * - Version is injected at build time from package.json
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }
}
