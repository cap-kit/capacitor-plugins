package io.capkit.rank

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import io.capkit.rank.utils.RankLogger

/**
 * Capacitor bridge for the Rank plugin.
 *
 * This class acts as the boundary between JavaScript and native Android code.
 * It handles input parsing, configuration management, and delegates execution
 * to the platform-specific implementation.
 */
@CapacitorPlugin(
  name = "Rank",
  permissions = [
    Permission(
      alias = "network",
      strings = [android.Manifest.permission.INTERNET],
    ),
  ],
)
class RankPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration read from capacitor.config.ts.
   * * CONTRACT:
   * - Initialized exactly once in `load()`.
   * - Treated as read-only afterwards.
   */
  private lateinit var config: RankConfig

  /**
   * Native implementation layer containing core Android logic.
   *
   * CONTRACT:
   * - Owned by the Plugin layer.
   * - MUST NOT access PluginCall or Capacitor bridge APIs directly.
   */
  private lateinit var implementation: RankImpl

  // ---------------------------------------------------------------------------
  // Companion Object
  // ---------------------------------------------------------------------------

  private companion object {
    /**
     * Account type identifier for internal plugin identification.
     */
    const val ACCOUNT_TYPE = "io.capkit.rank"

    /**
     * Human-readable account name for the plugin.
     */
    const val ACCOUNT_NAME = "Rank"
  }

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This method initializes the configuration container and the native
   * implementation layer, ensuring all dependencies are injected.
   */
  override fun load() {
    super.load()

    config = RankConfig(this)
    implementation = RankImpl(context)
    implementation.updateConfig(config)

    RankLogger.verbose = config.verboseLogging
    RankLogger.debug("Plugin loaded")

    // RULE: Perform pre-warm of native resources to improve UX
    implementation.preloadReviewInfo()
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native RankError sealed class instances to standardized
   * JavaScript-facing error codes.
   *
   * * @param call The PluginCall to reject.
   * * @param error The native error encountered.
   */
  private fun reject(
    call: PluginCall,
    error: RankError,
  ) {
    val code =
      when (error) {
        is RankError.Unavailable -> "UNAVAILABLE"
        is RankError.PermissionDenied -> "PERMISSION_DENIED"
        is RankError.InitFailed -> "INIT_FAILED"
        is RankError.UnknownType -> "UNKNOWN_TYPE"
      }

    call.reject(error.message, code)
  }

  // ---------------------------------------------------------------------------
  // Availability
  // ---------------------------------------------------------------------------

  /**
   * Checks if the device supports the Google Play Review API.
   */
  @PluginMethod
  fun isAvailable(call: PluginCall) {
    implementation.isAvailable { available ->
      val ret = JSObject()
      ret.put("value", available)
      call.resolve(ret)
    }
  }

  /**
   * Performs a diagnostic check to determine whether the Google Play
   * In-App Review dialog can be displayed in the current environment_attach.
   *
   * This method does NOT trigger the review flow and does NOT represent
   * an error condition. It is intended purely for runtime diagnostics.
   *
   * Typical cases where the review dialog cannot be shown include:
   * - The Google Play Store app is not installed or is disabled
   * - The app was not installed from the official Play Store
   * - The device or environment is not Play Store–certified (e.g. emulator)
   *
   * The result is returned as a structured object instead of rejecting
   * the call, allowing consumers to make UI decisions (e.g. disabling
   * the review button) without treating this as a failure.
   */
  @PluginMethod
  fun checkReviewEnvironment(call: PluginCall) {
    implementation.checkReviewEnvironment { canRequest ->
      val ret = JSObject()
      ret.put("canRequestReview", canRequest)

      if (!canRequest) {
        ret.put("reason", "PLAY_STORE_NOT_AVAILABLE")
      }

      call.resolve(ret)
    }
  }

  // ---------------------------------------------------------------------------
  // Product Page (Fallback for Android)
  // ---------------------------------------------------------------------------

  /**
   * Android fallback for presentProductPage.
   * Redirects to the Play Store app as no native internal overlay exists.
   */
  @PluginMethod
  fun presentProductPage(call: PluginCall) {
    openStore(call)
  }

  // ---------------------------------------------------------------------------
  // Public Plugin Methods
  // ---------------------------------------------------------------------------

  /**
   * Requests the display of the native Google Play In-App Review flow.
   *
   * This method extracts the 'fireAndForget' policy to determine if the
   * promise should resolve immediately or wait for the review flow results.
   *
   * * @param call PluginCall containing:
   * - fireAndForget (Boolean, optional): Override for the global resolution policy.
   */
  @PluginMethod
  fun requestReview(call: PluginCall) {
    val fireAndForget = call.getBoolean("fireAndForget") ?: config.fireAndForget

    // If fireAndForget is enabled, resolve the call immediately
    if (fireAndForget) {
      call.resolve()
    }

    val currentActivity = activity
    if (currentActivity == null) {
      // Edge case: plugin invoked while Activity is not available
      RankLogger.error("Cannot request review: Activity is null")
      if (!fireAndForget) {
        call.reject("Activity not available", "INIT_FAILED")
      }
      return
    }

    // RULE: UI operations MUST be executed on the main thread
    currentActivity.runOnUiThread {
      implementation.requestReview(currentActivity) { error: Exception? ->
        // Ensure we only respond if we haven't already resolved via fireAndForget
        if (!fireAndForget) {
          if (error != null) {
            RankLogger.error("Review flow failed", error)
            call.reject(error.message, "INIT_FAILED")
          } else {
            call.resolve()
          }
        }
      }
    }
  }

  /**
   * Navigates the user to the Google Play Store page for the application.
   *
   * * @param call PluginCall containing:
   * - packageName (String, optional): The target app package. Defaults to the host app.
   */
  @PluginMethod
  fun openStore(call: PluginCall) {
    val packageName = call.getString("packageName") ?: config.androidPackageName

    try {
      implementation.openStore(packageName)
      call.resolve()
    } catch (e: Exception) {
      call.reject(
        e.message,
        "INIT_FAILED",
      )
    }
  }

  /**
   * Opens the App Store listing page.
   * * This method resolves the target identifier by checking the 'appId' parameter,
   * then 'packageName', and finally falling back to the configured static ID.
   * * @param call PluginCall containing optional 'appId' or 'packageName'.
   */
  @PluginMethod
  fun openStoreListing(call: PluginCall) {
    val appId = call.getString("appId") ?: call.getString("packageName") ?: config.androidPackageName
    activity.runOnUiThread {
      try {
        implementation.openStoreListing(appId ?: context.packageName)
        call.resolve()
      } catch (e: Exception) {
        call.reject(e.message, "INIT_FAILED")
      }
    }
  }

  /**
   * Opens a specific app collection on the Play Store.
   * * @param call PluginCall containing the required 'name' string.
   */
  @PluginMethod
  fun openCollection(call: PluginCall) {
    val name = call.getString("name") ?: return call.reject("Collection name is missing")
    activity.runOnUiThread {
      implementation.openCollection(name)
      call.resolve()
    }
  }

  /**
   * Performs a store search for the given terms.
   * * @param call PluginCall containing the required 'terms' string.
   */
  @PluginMethod
  fun search(call: PluginCall) {
    val terms = call.getString("terms") ?: return call.reject("Terms are missing")
    implementation.search(terms)
    call.resolve()
  }

  /**
   * Opens the developer page on the store.
   * * @param call PluginCall containing the required 'devId' string.
   */
  @PluginMethod
  fun openDevPage(call: PluginCall) {
    val devId = call.getString("devId") ?: return call.reject("devId is missing")
    implementation.openDevPage(devId)
    call.resolve()
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
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }
}
