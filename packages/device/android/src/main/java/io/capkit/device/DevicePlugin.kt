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
import io.capkit.device.config.DeviceConfig
import io.capkit.device.error.ErrorMessages
import io.capkit.device.error.NativeError
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
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.registerReceiver(context, batteryStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
      } else {
        context.registerReceiver(batteryStateReceiver, filter)
      }
    } catch (e: SecurityException) {
      DeviceLogger.warn("Failed to register battery receiver: ${e.message}")
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
    error: NativeError,
  ) {
    val code =
      when (error) {
        is NativeError.Unavailable -> "UNAVAILABLE"
        is NativeError.Cancelled -> "CANCELLED"
        is NativeError.PermissionDenied -> "PERMISSION_DENIED"
        is NativeError.InitFailed -> "INIT_FAILED"
        is NativeError.InvalidInput -> "INVALID_INPUT"
        is NativeError.UnknownType -> "UNKNOWN_TYPE"
        is NativeError.NotFound -> "NOT_FOUND"
        is NativeError.Conflict -> "CONFLICT"
        is NativeError.Timeout -> "TIMEOUT"
      }

    // Always use the message from the NativeError instance
    val message = error.message ?: ErrorMessages.INTERNAL_ERROR
    call.reject(message, code)
  }

  /**
   * Handles unexpected throwables from the Impl layer.
   * If the throwable is already a NativeError, delegates to [reject].
   * Otherwise wraps it in an InitFailed rejection.
   */
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
    } catch (e: NativeError) {
      reject(call, e)
    } catch (e: Exception) {
      handleError(call, e)
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
    try {
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
    } catch (e: NativeError) {
      handleError(call, e)
    } catch (e: Exception) {
      handleError(call, e)
    }
  }

  /**
   * Returns information about the device battery.
   */
  @PluginMethod
  fun getBatteryInfo(call: PluginCall) {
    try {
      val result = JSObject()
      result.put("batteryLevel", implementation.getBatteryLevel())
      result.put("isCharging", implementation.isCharging())
      call.resolve(result)
    } catch (e: NativeError) {
      handleError(call, e)
    } catch (e: Exception) {
      handleError(call, e)
    }
  }

  /**
   * Returns the two-character language code of the current locale.
   */
  @PluginMethod
  fun getLanguageCode(call: PluginCall) {
    try {
      val result = implementation.getLanguageCode()
      call.resolve(JSObject().put("value", result))
    } catch (e: NativeError) {
      reject(call, e)
    } catch (e: Exception) {
      handleError(call, e)
    }
  }

  /**
   * Returns the BCP 47 language tag of the current locale.
   */
  @PluginMethod
  fun getLanguageTag(call: PluginCall) {
    try {
      val result = implementation.getLanguageTag()
      call.resolve(JSObject().put("value", result))
    } catch (e: NativeError) {
      reject(call, e)
    } catch (e: Exception) {
      handleError(call, e)
    }
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
