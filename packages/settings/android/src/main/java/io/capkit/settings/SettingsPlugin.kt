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
  /**
   * Plugin configuration parsed from capacitor.config.ts.
   */
  private lateinit var config: SettingsConfig

  /**
   * Native implementation containing Android-specific logic.
   */
  private lateinit var implementation: SettingsImpl

  /**
   * Plugin lifecycle entry point.
   *
   * Called exactly once when the plugin is loaded.
   */
  override fun load() {
    super.load()

    config = SettingsConfig(this)
    implementation = SettingsImpl(context)
    implementation.updateConfig(config)
  }

  // --- Open Settings ---

  /**
   * Opens a platform-specific settings screen (legacy key).
   */
  @PluginMethod
  fun open(call: PluginCall) {
    val option = call.getString("optionAndroid") ?: ""
    val result = implementation.open(option)
    call.resolve(result.toJS())
  }

  /**
   * Opens an Android settings screen.
   */
  @PluginMethod
  fun openAndroid(call: PluginCall) {
    val option = call.getString("option") ?: ""
    val result = implementation.open(option)
    call.resolve(result.toJS())
  }

  // --- Version ---

  /**
   * Returns the native plugin version.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }

  /**
   * Converts a native Result into a JSObject.
   */
  private fun SettingsImpl.Result.toJS(): JSObject {
    val obj = JSObject()
    obj.put("success", success)
    if (error != null) obj.put("error", error)
    if (code != null) obj.put("code", code)
    return obj
  }
}
