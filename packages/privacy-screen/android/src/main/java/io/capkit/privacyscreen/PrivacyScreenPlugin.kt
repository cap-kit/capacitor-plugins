package io.capkit.privacyscreen

import android.os.Build
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.privacyscreen.config.PrivacyScreenConfig
import io.capkit.privacyscreen.error.PrivacyScreenError
import io.capkit.privacyscreen.error.PrivacyScreenErrorMessages
import io.capkit.privacyscreen.logger.PrivacyScreenLogger
import io.capkit.privacyscreen.model.PrivacyScreenActionResult
import io.capkit.privacyscreen.model.PrivacyScreenStatus
import io.capkit.privacyscreen.model.PrivacyScreenVersionResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capacitor bridge for the PrivacyScreen plugin.
 *
 * CONTRACT:
 * - This class is the ONLY entry point from JavaScript.
 * - All PluginCall instances MUST be resolved or rejected exactly once.
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Invoke the native implementation
 * - Resolve or reject PluginCall exactly once
 * - Emit `screenshotTaken` events (API 34+)
 */
@CapacitorPlugin(name = "PrivacyScreen")
@Suppress("unused")
class PrivacyScreenPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration.
   *
   * CONTRACT:
   * - Initialized exactly once in `load()`
   * - Treated as read-only afterward
   */
  private lateinit var config: PrivacyScreenConfig

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - MUST NOT access PluginCall or Capacitor APIs
   */
  private lateinit var implementation: PrivacyScreenImpl

  /**
   * Opaque handle for the API 34+ screen-capture callback.
   *
   * Typed as Any to avoid class-verification issues on older runtimes;
   * every use is guarded by an SDK_INT check.
   */
  private var screenCaptureCallback: Any? = null

  /**
   * Serializer instance configured to encode result models into JSObject string payloads.
   *
   * NOTE: encodeDefaults is intentionally NOT set so nullable/defaulted fields
   * are omitted from the emitted JSON, matching the optional TypeScript types.
   */
  private val json = Json

  private fun currentHostActivity(): android.app.Activity? = activity ?: bridge.activity

  /**
   * Converts a serializable result model directly into a Capacitor JSObject.
   */
  private inline fun <reified T> toJSObject(value: T): JSObject {
    val jsonString = json.encodeToString(value)
    return JSObject(jsonString)
  }

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   */
  override fun load() {
    super.load()

    config = PrivacyScreenConfig(this)
    implementation = PrivacyScreenImpl(context)
    implementation.updateConfig(config)

    if (config.enabled) {
      implementation.enable(currentHostActivity(), false, "none")
    }

    PrivacyScreenLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  override fun handleOnPause() {
    super.handleOnPause()
    implementation.onActivityHidden(currentHostActivity())
  }

  override fun handleOnResume() {
    super.handleOnResume()
    val hostActivity = currentHostActivity()
    implementation.onActivityShown(hostActivity)
    syncScreenshotCallback()
  }

  /**
   * Reconciles the API 34+ screen-capture callback with the effective
   * protection state. Re-registers on every resume so activity recreation
   * never leaves a stale handle behind. No-op below API 34.
   */
  private fun syncScreenshotCallback() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      return
    }
    val hostActivity = currentHostActivity()
    (screenCaptureCallback as? android.app.Activity.ScreenCaptureCallback)?.let { previous ->
      try {
        hostActivity?.unregisterScreenCaptureCallback(previous)
      } catch (_: Exception) {
      }
      screenCaptureCallback = null
    }
    if (hostActivity == null || !implementation.isEnabled()) {
      return
    }
    val callback =
      android.app.Activity.ScreenCaptureCallback {
        notifyListeners("screenshotTaken", null)
      }
    try {
      hostActivity.registerScreenCaptureCallback(hostActivity.mainExecutor, callback)
      screenCaptureCallback = callback
    } catch (_: Exception) {
    }
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   */
  private fun reject(
    call: PluginCall,
    error: PrivacyScreenError,
  ) {
    val code =
      when (error) {
        is PrivacyScreenError.Unavailable -> "UNAVAILABLE"
        is PrivacyScreenError.Cancelled -> "CANCELLED"
        is PrivacyScreenError.PermissionDenied -> "PERMISSION_DENIED"
        is PrivacyScreenError.InitFailed -> "INIT_FAILED"
        is PrivacyScreenError.InvalidInput -> "INVALID_INPUT"
        is PrivacyScreenError.UnknownType -> "UNKNOWN_TYPE"
        is PrivacyScreenError.NotFound -> "NOT_FOUND"
        is PrivacyScreenError.Conflict -> "CONFLICT"
        is PrivacyScreenError.Timeout -> "TIMEOUT"
      }

    val message = error.message ?: PrivacyScreenErrorMessages.INTERNAL_ERROR
    call.reject(message, code)
  }

  /**
   * Centralizes error handling for a native throwable.
   */
  private fun handleError(
    call: PluginCall,
    throwable: Throwable,
  ) {
    if (throwable is PrivacyScreenError) {
      reject(call, throwable)
    } else {
      val message = throwable.message ?: PrivacyScreenErrorMessages.UNEXPECTED_NATIVE_ERROR
      reject(call, PrivacyScreenError.InitFailed(message))
    }
  }

  // ---------------------------------------------------------------------------
  // Privacy Screen
  // ---------------------------------------------------------------------------

  /**
   * Enables privacy-screen protection.
   *
   * Accepts the official `PrivacyScreenConfig` shape; only the `android`
   * knobs are honored natively (`dimBackground`, `privacyModeOnActivityHidden`).
   */
  @PluginMethod
  fun enable(call: PluginCall) {
    try {
      val androidOptions = call.getObject("android")
      val dimBackground = androidOptions?.getBoolean("dimBackground") ?: false
      val hiddenMode = androidOptions?.getString("privacyModeOnActivityHidden") ?: "none"
      implementation.enable(currentHostActivity(), dimBackground, hiddenMode)
      syncScreenshotCallback()
      call.resolve(toJSObject(PrivacyScreenActionResult(success = true)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Disables privacy-screen protection.
   */
  @PluginMethod
  fun disable(call: PluginCall) {
    try {
      implementation.disable(currentHostActivity())
      syncScreenshotCallback()
      call.resolve(toJSObject(PrivacyScreenActionResult(success = true)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns the current privacy-screen enabled state.
   */
  @PluginMethod
  fun isEnabled(call: PluginCall) {
    try {
      call.resolve(toJSObject(PrivacyScreenStatus(enabled = implementation.isEnabled())))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  // ---------------------------------------------------------------------------
  // Version Information
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version synchronized from package.json.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    try {
      call.resolve(toJSObject(PrivacyScreenVersionResult(BuildConfig.PLUGIN_VERSION)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }
}
