package io.capkit.test

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.test.utils.TestLogger

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

  // --- ---

  /**
   * Echoes a string back to JavaScript.
   *
   * @param call Capacitor plugin call containing the `value` parameter.
   */
  @PluginMethod
  fun echo(call: PluginCall) {
    var value = call.getString("value") ?: ""
    TestLogger.debug("Echoing value: $value")

    // Append the custom message from the configuration
    value += config.customMessage

    val ret = JSObject()
    ret.put("value", implementation.echo(value))
    call.resolve(ret)
  }

  // ---  Version ---

  /**
   * Returns the plugin version.
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
      startActivityForResult(call, intent, "openSettingsResult")
      call.resolve()
    } catch (e: Exception) {
      call.reject(
        "Failed to open settings",
        "UNAVAILABLE",
      )
    }
  }

  @ActivityCallback
  private fun openSettingsResult(
    call: PluginCall,
    result: ActivityResult,
  ) {
    // No-op, just to satisfy the callback requirement if needed
    call.resolve()
  }
}
