package io.capkit.test

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Capacitor bridge for the Test plugin.
 *
 * This class acts as the boundary between JavaScript and native Android code.
 * It is responsible for:
 * - reading configuration
 * - validating PluginCall input
 * - mapping JS calls to native logic
 */
@CapacitorPlugin(
  name = "Test",
)
class TestPlugin : Plugin() {
  /**
   * Plugin configuration parsed from capacitor.config.ts.
   */
  private lateinit var config: TestConfig

  /**
   * Native implementation containing platform logic.
   */
  private lateinit var implementation: TestImpl

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read configuration
   * - initialize native resources
   * - inject dependencies into the implementation
   */
  override fun load() {
    super.load()

    config = TestConfig(this)
    implementation = TestImpl(context)
    implementation.updateConfig(config)
  }

  // --- Echo ---

  /**
   * Echoes a string back to JavaScript.
   *
   * @param call Capacitor plugin call containing the `value` parameter.
   */
  @PluginMethod
  fun echo(call: PluginCall) {
    var value = call.getString("value") ?: ""

    // Append the custom message from the configuration
    value += config.customMessage

    val ret = JSObject()
    ret.put("value", implementation.echo(value))
    call.resolve(ret)
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

  // --- Settings ---

  /**
   * Opens the application details settings page.
   * Allowing the user to manually enable permissions.
   */
  @PluginMethod
  fun openAppSettings(call: PluginCall) {
    try {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      val uri = Uri.fromParts("package", context.packageName, null)
      intent.data = uri
      context.startActivity(intent)
      call.resolve()
    } catch (e: Exception) {
      // NOTE:
      // On Android, PluginCall.reject(...) is fully supported and correctly used here.
      // However, this plugin is designed with a cross-platform, state-based API mindset
      // to remain compatible with iOS when using Swift Package Manager (SPM),
      // where Promise rejection is not reliably available.
      //
      // For this reason:
      // - JavaScript consumers SHOULD NOT rely on try/catch for this method
      // - Errors should be handled by inspecting resolved result states instead
      //
      // This reject() call is intentionally kept on Android to:
      // - preserve native correctness
      // - document the platform capability
      // - avoid hiding real failures during development
      //
      // When extending this plugin, consider mirroring error semantics across platforms
      // to keep the public JS API predictable and consistent.
      call.reject(
        "Failed to open settings",
        "UNAVAILABLE",
      )
    }
  }
}
