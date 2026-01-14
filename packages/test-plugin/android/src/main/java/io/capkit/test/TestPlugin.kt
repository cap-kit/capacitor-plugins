package io.capkit.test

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Capacitor bridge for the Test plugin.
 *
 * This class exposes native Android functionality to JavaScript
 * and delegates all business logic to the implementation class.
 */
@CapacitorPlugin(
  name = "Test",
)
class TestPlugin : Plugin() {
  private lateinit var config: TestConfig

  override fun load() {
    super.load()
    Logger.debug("Loading plugin")
    config = TestConfig(this)
  }

  /** Plugin version (injected by Gradle). */
  private val pluginVersion: String = BuildConfig.PLUGIN_VERSION

  /** Native implementation (business logic). */
  private val implementation = Test()

  /**
   * Echoes a string back to JavaScript.
   *
   * @param call Capacitor plugin call containing the `value` parameter.
   */
  @PluginMethod
  fun echo(call: PluginCall) {
    var value = call.getString("value") ?: ""
    Logger.debug("Echoing value: $value")

    // Append the custom message from the configuration
    value += config.customMessage

    val ret = JSObject()
    ret.put("value", implementation.echo(value))
    call.resolve(ret)
  }

  /**
   * Returns the plugin version.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", pluginVersion)
    call.resolve(ret)
  }
}
