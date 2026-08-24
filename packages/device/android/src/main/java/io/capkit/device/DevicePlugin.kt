package io.capkit.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.device.logger.DeviceLogger
import io.capkit.device.model.DevicePluginVersionResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

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
  // Constants
  // ---------------------------------------------------------------------------

  private companion object {
    /**
     * Name of the event emitted when the battery charging state changes.
     */
    const val BATTERY_CHARGING_STATE_CHANGE_EVENT = "batteryChargingStateChange"
  }

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

  /**
   * Last charging state observed by the battery listener, used to filter
   * duplicate notifications. Null until the first broadcast is received.
   */
  private var lastBatteryChargingState: Boolean? = null

  /**
   * Receiver tracking ACTION_BATTERY_CHANGED broadcasts so that
   * `batteryChargingStateChange` is emitted only on real state transitions.
   */
  private val batteryStateReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?,
      ) {
        if (intent == null || !Intent.ACTION_BATTERY_CHANGED.equals(intent.action)) {
          return
        }
        val charging = isChargingIntent(intent)
        val baseline = lastBatteryChargingState
        if (baseline == null) {
          // Baseline observation captured on the first broadcast: never emit.
          lastBatteryChargingState = charging
          return
        }
        if (baseline != charging) {
          lastBatteryChargingState = charging
          notifyBatteryChargingStateChange(intent)
        }
      }
    }

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
   * - start battery state tracking
   */
  override fun load() {
    super.load()

    config = DeviceConfig(this)
    implementation = DeviceImpl(context)
    implementation.updateConfig(config)

    registerBatteryReceiver()

    DeviceLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  override fun handleOnDestroy() {
    try {
      context.unregisterReceiver(batteryStateReceiver)
    } catch (_: IllegalArgumentException) {
      DeviceLogger.debug("Battery receiver was not registered.")
    }
  }

  /**
   * Registers the battery state receiver using the export flag required by
   * the current Android version (RECEIVER_NOT_EXPORTED on API 33+).
   */
  private fun registerBatteryReceiver() {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.registerReceiver(context, batteryStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    } else {
      context.registerReceiver(batteryStateReceiver, filter)
    }
  }

  /**
   * Emits `batteryChargingStateChange` with the level and charging state
   * carried by the battery broadcast that triggered the transition.
   */
  private fun notifyBatteryChargingStateChange(batteryIntent: Intent) {
    val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val batteryLevel = if (level >= 0 && scale > 0) level / scale.toFloat() else -1f

    val data = JSObject()
    data.put("batteryLevel", batteryLevel)
    data.put("isCharging", isChargingIntent(batteryIntent))
    notifyListeners(BATTERY_CHARGING_STATE_CHANGE_EVENT, data)
  }

  /**
   * Extracts the charging state from a battery broadcast intent.
   */
  private fun isChargingIntent(intent: Intent): Boolean {
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
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
  // Plugin Methods
  // ---------------------------------------------------------------------------

  /**
   * Returns an unique identifier for the device.
   */
  @PluginMethod
  fun getId(call: PluginCall) {
    try {
      val result = JSObject()
      result.put("identifier", implementation.getUuid())
      call.resolve(result)
    } catch (error: DeviceError) {
      reject(call, error)
    }
  }

  /**
   * Returns information about the underlying device, OS, and platform.
   *
   * Field order mirrors the official reference implementation: memory and
   * disk metrics first, then identity fields, then WebView information.
   */
  @PluginMethod
  fun getInfo(call: PluginCall) {
    val result = JSObject()
    result.put("memUsed", implementation.getMemUsed())
    result.put("diskFree", implementation.getDiskFree())
    result.put("diskTotal", implementation.getDiskTotal())
    result.put("realDiskFree", implementation.getRealDiskFree())
    result.put("realDiskTotal", implementation.getRealDiskTotal())
    result.put("model", implementation.getModel())
    result.put("operatingSystem", "android")
    result.put("osVersion", implementation.getOsVersion())
    result.put("androidSDKVersion", implementation.getAndroidSDKVersion())
    result.put("platform", implementation.getPlatform())
    result.put("manufacturer", implementation.getManufacturer())
    result.put("isVirtual", implementation.isVirtual())
    result.put("name", implementation.getName())
    result.put("webViewVersion", implementation.getWebViewVersion())
    call.resolve(result)
  }

  /**
   * Returns information about the device battery.
   */
  @PluginMethod
  fun getBatteryInfo(call: PluginCall) {
    val result = JSObject()
    result.put("batteryLevel", implementation.getBatteryLevel())
    result.put("isCharging", implementation.isCharging())
    call.resolve(result)
  }

  /**
   * Returns the two-character language code of the current locale.
   */
  @PluginMethod
  fun getLanguageCode(call: PluginCall) {
    val result = JSObject()
    result.put("value", Locale.getDefault().language)
    call.resolve(result)
  }

  /**
   * Returns the BCP 47 language tag of the current locale.
   */
  @PluginMethod
  fun getLanguageTag(call: PluginCall) {
    val result = JSObject()
    result.put("value", Locale.getDefault().toLanguageTag())
    call.resolve(result)
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
