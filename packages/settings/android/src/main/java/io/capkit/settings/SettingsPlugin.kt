package io.capkit.settings

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.settings.config.SettingsConfig
import io.capkit.settings.error.SettingsError
import io.capkit.settings.logger.SettingsLogger

/**
 * Capacitor bridge for the Settings plugin (Android).
 *
 * Responsibilities:
 * - read configuration
 * - extract PluginCall input
 * - delegate logic to SettingsImpl
 * - resolve calls with standardized results
 *
 * This class MUST NOT contain platform logic.
 */
@CapacitorPlugin(
  name = "Settings",
)
class SettingsPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration parsed from capacitor.config.ts.
   */
  private lateinit var config: SettingsConfig

  /**
   * Native implementation containing platform-specific logic only.
   *
   * IMPORTANT:
   * - Must NOT access PluginCall
   * - Must NOT reference Capacitor APIs
   */
  private lateinit var implementation: SettingsImpl

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read static configuration
   * - initialize native resources
   * - inject configuration into the implementation
   */
  override fun load() {
    super.load()

    config = SettingsConfig(this)
    implementation = SettingsImpl(context)
    implementation.updateConfig(config)

    SettingsLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   * Ensure consistency with the JS SettingsErrorCode enum.
   */
  private fun reject(
    call: PluginCall,
    error: SettingsError,
  ) {
    val code =
      when (error) {
        is SettingsError.Unavailable -> "UNAVAILABLE"
        is SettingsError.Cancelled -> "CANCELLED"
        is SettingsError.PermissionDenied -> "PERMISSION_DENIED"
        is SettingsError.InitFailed -> "INIT_FAILED"
        is SettingsError.InvalidInput -> "INVALID_INPUT"
        is SettingsError.UnknownType -> "UNKNOWN_TYPE"
        is SettingsError.NotFound -> "NOT_FOUND"
        is SettingsError.Conflict -> "CONFLICT"
        is SettingsError.Timeout -> "TIMEOUT"
      }

    // Always use the message from the SettingsError instance
    val message = error.message ?: "Unknown native error"
    call.reject(message, code)
  }

  // ---------------------------------------------------------------------------
  // Open
  // ---------------------------------------------------------------------------

  /**
   * Opens a platform-specific settings screen (legacy key).
   */
  @PluginMethod
  fun open(call: PluginCall) {
    val option = call.getString("optionAndroid")
    if (option.isNullOrBlank()) {
      reject(call, SettingsError.InvalidInput("`optionAndroid` must be provided and not empty."))
      return
    }
    handleOpen(call, option)
  }

  // ---------------------------------------------------------------------------
  // Open (Android)
  // ---------------------------------------------------------------------------

  /**
   * Opens an Android settings screen.
   */
  @PluginMethod
  fun openAndroid(call: PluginCall) {
    val option = call.getString("option")
    if (option.isNullOrBlank()) {
      reject(call, SettingsError.InvalidInput("`option` must be provided and not empty."))
      return
    }
    handleOpen(call, option)
  }

  private fun handleOpen(
    call: PluginCall,
    option: String,
  ) {
    try {
      val intent = implementation.open(option)
      bridge.activity.runOnUiThread {
        bridge.activity.startActivity(intent)
      }
      call.resolve()
    } catch (error: SettingsError) {
      reject(call, error)
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
