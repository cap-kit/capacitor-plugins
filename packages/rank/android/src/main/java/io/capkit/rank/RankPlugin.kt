package io.capkit.rank

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.rank.error.RankErrorMessages
import io.capkit.rank.utils.RankLogger
import io.capkit.rank.utils.RankValidators

/**
 * Capacitor bridge for the Rank plugin.
 *
 * This class acts as the boundary between JavaScript and native Android code.
 * It handles input parsing, configuration management, and delegates execution
 * to the platform-specific implementation.
 */
@CapacitorPlugin(name = "Rank")
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
   * Rejects the call with a message and a standardized error code.
   * Ensure consistency with the JS RankErrorCode enum.
   */
  private fun reject(
    call: PluginCall,
    error: RankError,
  ) {
    val code =
      when (error) {
        is RankError.Unavailable -> "UNAVAILABLE"
        is RankError.Cancelled -> "CANCELLED"
        is RankError.PermissionDenied -> "PERMISSION_DENIED"
        is RankError.InitFailed -> "INIT_FAILED"
        is RankError.InvalidInput -> "INVALID_INPUT"
        is RankError.UnknownType -> "UNKNOWN_TYPE"
        is RankError.NotFound -> "NOT_FOUND"
        is RankError.Conflict -> "CONFLICT"
        is RankError.Timeout -> "TIMEOUT"
      }

    // Always use the message from the RankError instance
    val message = error.message ?: "Unknown native error"
    call.reject(message, code)
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
    implementation.checkReviewEnvironment { diagnostic ->
      if (diagnostic.error != null) {
        reject(call, diagnostic.error)
        return@checkReviewEnvironment
      }

      val ret = JSObject()
      ret.put("canRequestReview", diagnostic.canRequestReview)

      if (!diagnostic.canRequestReview && diagnostic.reason != null) {
        ret.put("reason", diagnostic.reason)
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
        reject(call, RankError.Unavailable(RankErrorMessages.ACTIVITY_NOT_AVAILABLE))
      }
      return
    }

    // RULE: UI operations MUST be executed on the main thread
    currentActivity.runOnUiThread {
      implementation.requestReview(currentActivity) { error: RankError? ->
        // Ensure we only respond if we haven't already resolved via fireAndForget
        if (!fireAndForget) {
          if (error != null) {
            RankLogger.error("Review flow failed", error)
            reject(call, error)
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
    val rawPackageName = call.getString("packageName") ?: config.androidPackageName
    val packageName = RankValidators.validatePackageName(rawPackageName)

    if (packageName == null) {
      reject(call, RankError.InvalidInput(RankErrorMessages.INVALID_ANDROID_PACKAGE_NAME))
      return
    }

    val currentActivity = activity
    if (currentActivity == null) {
      reject(call, RankError.Unavailable(RankErrorMessages.ACTIVITY_NOT_AVAILABLE))
      return
    }

    currentActivity.runOnUiThread {
      try {
        implementation.openStore(packageName)
        call.resolve()
      } catch (e: Exception) {
        reject(call, RankError.InitFailed(e.message ?: RankErrorMessages.NATIVE_OPERATION_FAILED))
      }
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
    val rawPackageName =
      call.getString("appId")
        ?: call.getString("packageName")
        ?: config.androidPackageName

    val packageName = RankValidators.validatePackageName(rawPackageName)

    if (packageName == null) {
      reject(call, RankError.InvalidInput(RankErrorMessages.INVALID_ANDROID_PACKAGE_NAME))
      return
    }

    val currentActivity = activity
    if (currentActivity == null) {
      reject(call, RankError.Unavailable(RankErrorMessages.ACTIVITY_NOT_AVAILABLE))
      return
    }

    currentActivity.runOnUiThread {
      try {
        implementation.openStoreListing(packageName)
        call.resolve()
      } catch (e: Exception) {
        reject(call, RankError.InitFailed(e.message ?: RankErrorMessages.NATIVE_OPERATION_FAILED))
      }
    }
  }

  /**
   * Opens a specific app collection on the Play Store.
   * * @param call PluginCall containing the required 'name' string.
   */
  @PluginMethod
  fun openCollection(call: PluginCall) {
    val rawName = call.getString("name")
    val name = RankValidators.validateCollectionName(rawName)

    if (name == null) {
      reject(call, RankError.InvalidInput(RankErrorMessages.INVALID_COLLECTION_NAME))
      return
    }

    val currentActivity = activity
    if (currentActivity == null) {
      reject(call, RankError.Unavailable(RankErrorMessages.ACTIVITY_NOT_AVAILABLE))
      return
    }

    currentActivity.runOnUiThread {
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
    val rawTerms = call.getString("terms")
    val terms = RankValidators.validateSearchTerms(rawTerms)

    if (terms == null) {
      reject(call, RankError.InvalidInput(RankErrorMessages.INVALID_SEARCH_TERMS))
      return
    }

    val currentActivity = activity
    if (currentActivity == null) {
      reject(call, RankError.Unavailable(RankErrorMessages.ACTIVITY_NOT_AVAILABLE))
      return
    }

    currentActivity.runOnUiThread {
      implementation.search(terms)
      call.resolve()
    }
  }

  /**
   * Opens the developer page on the store.
   * * @param call PluginCall containing the required 'devId' string.
   */
  @PluginMethod
  fun openDevPage(call: PluginCall) {
    val rawDevId = call.getString("devId")
    val devId = RankValidators.validateDevId(rawDevId)

    if (devId == null) {
      reject(call, RankError.InvalidInput(RankErrorMessages.INVALID_DEVELOPER_ID))
      return
    }

    val currentActivity = activity
    if (currentActivity == null) {
      reject(call, RankError.Unavailable(RankErrorMessages.ACTIVITY_NOT_AVAILABLE))
      return
    }

    currentActivity.runOnUiThread {
      implementation.openDevPage(devId)
      call.resolve()
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
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }
}
