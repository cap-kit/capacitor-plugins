package io.capkit.splashmotion

import android.app.Activity
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.splashmotion.config.SplashMotionConfig
import io.capkit.splashmotion.error.SplashMotionError
import io.capkit.splashmotion.error.SplashMotionErrorMessages
import io.capkit.splashmotion.logger.SplashMotionLogger
import io.capkit.splashmotion.model.BackgroundGradientOptionsData
import io.capkit.splashmotion.model.EntranceOptionsData
import io.capkit.splashmotion.model.HapticOptionsData
import io.capkit.splashmotion.model.HideOptionsData
import io.capkit.splashmotion.model.ImageOptionsData
import io.capkit.splashmotion.model.LottieOptionsData
import io.capkit.splashmotion.model.ShowOptionsData
import io.capkit.splashmotion.model.SpinnerOptionsData
import io.capkit.splashmotion.model.SplashMotionVersionResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capacitor bridge for the SplashMotion plugin.
 *
 * CONTRACT:
 * - This class is the ONLY entry point from JavaScript.
 * - All PluginCall instances MUST be resolved or rejected exactly once.
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Invoke the native implementation
 * - Resolve or reject PluginCall exactly once
 */
@CapacitorPlugin(name = "SplashMotion")
@Suppress("unused")
class SplashMotionPlugin : Plugin() {
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
  private lateinit var config: SplashMotionConfig

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - MUST NOT access PluginCall or Capacitor APIs
   */
  private lateinit var implementation: SplashMotionImpl

  /**
   * Serializer instance configured to encode result models into JSObject string payloads.
   *
   * NOTE: encodeDefaults is intentionally NOT set so nullable/defaulted fields
   * are omitted from the emitted JSON, matching the optional TypeScript types.
   */
  private val json = Json

  private fun currentHostActivity(): Activity? = activity ?: bridge.activity

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

    config = SplashMotionConfig(this)
    implementation = SplashMotionImpl(context)
    implementation.updateConfig(config)

    // Parity with the official splash-screen plugin: when the WebView is
    // below the configured minimum and an error path exists, the app will
    // load the error page and no JS will ever call hide(); holding a launch
    // splash there would leave it stuck on screen forever.
    if (!bridge.isMinimumWebViewInstalled() && bridge.config.errorPath != null && !config.launchAutoHide) {
      SplashMotionLogger.warn("Minimum WebView not installed; skipping launch splash")
    } else {
      implementation.showLaunchSplash(currentHostActivity())
    }

    SplashMotionLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   */
  private fun reject(
    call: PluginCall,
    error: SplashMotionError,
  ) {
    val code =
      when (error) {
        is SplashMotionError.Unavailable -> "UNAVAILABLE"
        is SplashMotionError.Cancelled -> "CANCELLED"
        is SplashMotionError.PermissionDenied -> "PERMISSION_DENIED"
        is SplashMotionError.InitFailed -> "INIT_FAILED"
        is SplashMotionError.InvalidInput -> "INVALID_INPUT"
        is SplashMotionError.UnknownType -> "UNKNOWN_TYPE"
        is SplashMotionError.NotFound -> "NOT_FOUND"
        is SplashMotionError.Conflict -> "CONFLICT"
        is SplashMotionError.Timeout -> "TIMEOUT"
      }

    val message = error.message ?: SplashMotionErrorMessages.INTERNAL_ERROR
    call.reject(message, code)
  }

  /**
   * Centralizes error handling for a native throwable.
   */
  private fun handleError(
    call: PluginCall,
    throwable: Throwable,
  ) {
    if (throwable is SplashMotionError) {
      reject(call, throwable)
    } else {
      val message = throwable.message ?: SplashMotionErrorMessages.UNEXPECTED_NATIVE_ERROR
      reject(call, SplashMotionError.InitFailed(message))
    }
  }

  // ---------------------------------------------------------------------------
  // Splash Motion
  // ---------------------------------------------------------------------------

  /**
   * Shows the splash screen with optional Lottie animation and haptic feedback.
   */
  @PluginMethod
  fun show(call: PluginCall) {
    try {
      val options = parseShowOptions(call)
      implementation.show(currentHostActivity(), options)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Hides the splash screen with an optional fade out and haptic feedback.
   */
  @PluginMethod
  fun hide(call: PluginCall) {
    try {
      val fadeOutDuration = parseOptionalLong(call, "fadeOutDuration")
      val haptic = parseHapticOptions(call.getObject("haptic"))
      implementation.hide(currentHostActivity(), HideOptionsData(fadeOutDuration, haptic))
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  // ---------------------------------------------------------------------------
  // Option Parsing
  // ---------------------------------------------------------------------------

  private fun parseShowOptions(call: PluginCall): ShowOptionsData =
    ShowOptionsData(
      autoHide = call.getBoolean("autoHide"),
      fadeInDuration = parseOptionalLong(call, "fadeInDuration"),
      fadeOutDuration = parseOptionalLong(call, "fadeOutDuration"),
      showDuration = parseOptionalLong(call, "showDuration"),
      lottie = parseLottieOptions(call.getObject("lottie")),
      image = parseImageOptions(call.getObject("image")),
      backgroundImage = parseImageOptions(call.getObject("backgroundImage")),
      brandingImage = parseImageOptions(call.getObject("brandingImage")),
      entrance = parseEntranceOptions(call.getObject("entrance")),
      backgroundGradient = parseGradientOptions(call.getObject("backgroundGradient")),
      spinner = parseSpinnerOptions(call.getObject("spinner")),
      haptic = parseHapticOptions(call.getObject("haptic")),
    )

  private fun parseImageOptions(options: JSObject?): ImageOptionsData? {
    if (options == null) {
      return null
    }
    val source = options.getString("source") ?: return null
    return ImageOptionsData(
      source = source,
      darkSource = options.getString("darkSource"),
      gravity = options.getString("gravity"),
      scaleType = options.getString("scaleType"),
      alpha = parseOptionalDouble(options, "alpha"),
      margin = parseOptionalDouble(options, "margin"),
    )
  }

  private fun parseEntranceOptions(options: JSObject?): EntranceOptionsData? {
    if (options == null) {
      return null
    }
    return EntranceOptionsData(
      type = options.getString("type"),
      durationMs = parseOptionalLong(options, "durationMs"),
    )
  }

  private fun parseGradientOptions(options: JSObject?): BackgroundGradientOptionsData? {
    if (options == null) {
      return null
    }
    val top = options.getString("top") ?: return null
    val bottom = options.getString("bottom") ?: return null
    return BackgroundGradientOptionsData(
      top = top,
      bottom = bottom,
      topDark = options.getString("topDark"),
      bottomDark = options.getString("bottomDark"),
    )
  }

  private fun parseSpinnerOptions(options: JSObject?): SpinnerOptionsData? {
    if (options == null) {
      return null
    }
    return SpinnerOptionsData(
      show = parseOptionalBoolean(options, "show"),
      style = options.getString("style"),
      color = options.getString("color"),
    )
  }

  private fun parseLottieOptions(options: JSObject?): LottieOptionsData? {
    if (options == null) {
      return null
    }
    val source = options.getString("source") ?: return null
    return LottieOptionsData(
      source = source,
      loop = parseOptionalBoolean(options, "loop"),
      speed = parseOptionalDouble(options, "speed"),
    )
  }

  private fun parseHapticOptions(options: JSObject?): HapticOptionsData? {
    if (options == null) {
      return null
    }
    return HapticOptionsData(style = options.getString("style"))
  }

  private fun parseOptionalLong(
    call: PluginCall,
    key: String,
  ): Long? {
    if (!call.getData().has(key)) {
      return null
    }
    return try {
      call.getInt(key)?.toLong() ?: call.getLong(key)
    } catch (_: Exception) {
      null
    }
  }

  private fun parseOptionalLong(
    options: JSObject,
    key: String,
  ): Long? {
    if (!options.has(key)) {
      return null
    }
    return try {
      options.getInt(key).toLong()
    } catch (_: Exception) {
      try {
        options.getLong(key)
      } catch (_: Exception) {
        null
      }
    }
  }

  private fun parseOptionalBoolean(
    options: JSObject,
    key: String,
  ): Boolean? = if (options.has(key)) options.getBoolean(key) else null

  private fun parseOptionalDouble(
    options: JSObject,
    key: String,
  ): Double? = if (options.has(key)) options.getDouble(key) else null

  // ---------------------------------------------------------------------------
  // Version Information
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version synchronized from package.json.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    try {
      call.resolve(toJSObject(SplashMotionVersionResult(BuildConfig.PLUGIN_VERSION)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }
}
