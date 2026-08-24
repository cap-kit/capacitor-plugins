package io.capkit.device

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.device.logger.DeviceLogger
import io.capkit.device.model.DevicePluginVersionResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capacitor bridge for the Device plugin.
 *
 * This class acts as the boundary between JavaScript and native Android code.
 * It handles input parsing, configuration management, and delegates execution
 * to the platform-specific implementation.
 */
@CapacitorPlugin(name = "Device")
class DevicePlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration read from capacitor.config.ts.
   * * CONTRACT:
   * - Initialized exactly once in `load()`.
   * - Treated as read-only afterwards.
   */
  private lateinit var config: DeviceConfig

  /**
   * Native implementation layer containing core Android logic.
   *
   * CONTRACT:
   * - Owned by the Plugin layer.
   * - MUST NOT access PluginCall or Capacitor bridge APIs directly.
   */
  private lateinit var implementation: DeviceImpl

  /**
   * Serializer instance configured to encode result models into JSObject string payloads.
   *
   * NOTE: encodeDefaults is intentionally NOT set so nullable/defaulted fields
   * are omitted from the emitted JSON, matching the optional TypeScript types.
   */
  private val json = Json

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

    config = DeviceConfig(this)
    implementation = DeviceImpl(context)
    implementation.updateConfig(config)

    DeviceLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // ---------------------------------------------------------------------------
  // Helper Methods for Serialization
  // ---------------------------------------------------------------------------

  /**
   * Converts a serializable result model directly into a Capacitor JSObject.
   */
  private inline fun <reified T> toJSObject(value: T): JSObject {
    val jsonString = json.encodeToString(value)
    return JSObject(jsonString)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   * Ensure consistency with the JS DeviceErrorCode enum.
   */
  private fun reject(
    call: PluginCall,
    error: DeviceError,
  ) {
    val code =
      when (error) {
        is DeviceError.Unavailable -> "UNAVAILABLE"
        is DeviceError.Cancelled -> "CANCELLED"
        is DeviceError.PermissionDenied -> "PERMISSION_DENIED"
        is DeviceError.InitFailed -> "INIT_FAILED"
        is DeviceError.InvalidInput -> "INVALID_INPUT"
        is DeviceError.UnknownType -> "UNKNOWN_TYPE"
        is DeviceError.NotFound -> "NOT_FOUND"
        is DeviceError.Conflict -> "CONFLICT"
        is DeviceError.Timeout -> "TIMEOUT"
      }

    // Always use the message from the DeviceError instance
    val message = error.message ?: "Unknown native error"
    call.reject(message, code)
  }

  // ---------------------------------------------------------------------------
  // Version Information
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version synchronized from package.json.
   *
   * This information is used for diagnostics and ensuring parity between
   * the JavaScript and native layers.
   *
   * @param call The bridge call to resolve with version data.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    call.resolve(toJSObject(DevicePluginVersionResult(BuildConfig.PLUGIN_VERSION)))
  }
}
