package io.capkit.settings

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

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
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native SettingsError instances to JS-facing SettingsErrorCode values.
   *
   * IMPORTANT:
   * - This is the ONLY place where native errors are translated
   * - SettingsImpl must NEVER know about JS or Capacitor error codes
   */
  private fun reject(
    call: PluginCall,
    error: SettingsError,
  ) {
    val code =
      when (error) {
        is SettingsError.Unavailable -> "UNAVAILABLE"
        is SettingsError.PermissionDenied -> "PERMISSION_DENIED"
        is SettingsError.InitFailed -> "INIT_FAILED"
        is SettingsError.UnknownType -> "UNKNOWN_TYPE"
      }

    call.reject(error.message, code)
  }

  // ---------------------------------------------------------------------------
  // Open
  // ---------------------------------------------------------------------------

  /**
   * Opens a platform-specific settings screen (legacy key).
   */
  @PluginMethod
  fun open(call: PluginCall) {
    val option = call.getString("optionAndroid") ?: ""
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
    val option = call.getString("option") ?: ""
    handleOpen(call, option)
  }

  private fun handleOpen(
    call: PluginCall,
    option: String,
  ) {
    try {
      implementation.open(option)
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
