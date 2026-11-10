package io.capkit.localnetwork

import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import io.capkit.localnetwork.config.LocalNetworkConfig
import io.capkit.localnetwork.error.ErrorMessages
import io.capkit.localnetwork.error.NativeError
import io.capkit.localnetwork.logger.LocalNetworkLogger
import io.capkit.localnetwork.permission.LocalNetworkPermission

/**
 * Capacitor bridge for the LocalNetwork plugin.
 *
 * Exposes the Android 17+ local-network runtime permission
 * (`android.permission.ACCESS_LOCAL_NETWORK`, API 37) through the standard
 * `checkPermissions()` / `requestPermissions()` contract.
 *
 * On SDKs below API 37 the permission is unknown to `PackageManager` and is
 * always reported as granted, so no prompt can ever appear there.
 */
@CapacitorPlugin(
  name = "LocalNetwork",
  permissions = [
    Permission(
      alias = LocalNetworkPermission.ALIAS,
      strings = [LocalNetworkPermission.NAME],
    ),
  ],
)
class LocalNetworkPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration read from capacitor.config.ts.
   *
   * CONTRACT:
   * - Initialized exactly once in `load()`.
   * - Treated as read-only afterwards.
   */
  private lateinit var config: LocalNetworkConfig

  /**
   * Last reported permission state to avoid emitting duplicate events.
   */
  private var lastPermissionState: String? = null

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read static configuration
   * - configure logging verbosity
   */
  override fun load() {
    super.load()

    config = LocalNetworkConfig(this)
    LocalNetworkLogger.verbose = config.verboseLogging

    LocalNetworkLogger.debug("Plugin loaded. Verbose logging: ", config.verboseLogging.toString())
    lastPermissionState = getCurrentPermissionStateString()
  }

  /**
   * Check for permission changes when returning from system settings.
   */
  override fun handleOnResume() {
    super.handleOnResume()
    val currentState = getCurrentPermissionStateString()
    if (lastPermissionState != null && lastPermissionState != currentState && hasListeners("permissionChange")) {
      val data =
        JSObject().apply {
          put(LocalNetworkPermission.ALIAS, currentState)
        }
      notifyListeners("permissionChange", data)
    }
    lastPermissionState = currentState
  }

  private fun getCurrentPermissionStateString(): String {
    if (!LocalNetworkPermission.isEnforced()) {
      return PermissionState.GRANTED.toString()
    }
    return getPermissionState(LocalNetworkPermission.ALIAS)?.toString() ?: PermissionState.PROMPT.toString()
  }

  // ---------------------------------------------------------------------------
  // Permissions
  // ---------------------------------------------------------------------------

  /**
   * Reports the current state of the local-network permission.
   *
   * On SDKs below API 37 the permission is not enforced and is reported as
   * `granted` without touching `PackageManager`.
   *
   * Annotated with [PermissionCallback] because Capacitor invokes this method
   * by name after an interactive permission request.
   */
  @PluginMethod
  @PermissionCallback
  override fun checkPermissions(call: PluginCall) {
    if (!LocalNetworkPermission.isEnforced()) {
      resolveGranted(call)
      return
    }

    super.checkPermissions(call)
  }

  /**
   * Requests the local-network permission.
   *
   * On SDKs below API 37 there is nothing to request: the call resolves as
   * `granted` immediately. On API 37+ it delegates to the standard Capacitor
   * permission flow, which ends in [checkPermissions].
   */
  @PluginMethod
  override fun requestPermissions(call: PluginCall) {
    if (!LocalNetworkPermission.isEnforced()) {
      resolveGranted(call)
      return
    }

    super.requestPermissions(call)
  }

  /**
   * Resolves the call reporting the local-network permission as granted.
   */
  private fun resolveGranted(call: PluginCall) {
    LocalNetworkLogger.debug("Local-network permission not enforced on this SDK; reporting granted.")

    val ret =
      JSObject().apply {
        put(LocalNetworkPermission.ALIAS, PermissionState.GRANTED.toString())
      }
    call.resolve(ret)
  }

  // ---------------------------------------------------------------------------
  // Plugin Info
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version injected at build time from package.json
   * into [BuildConfig.PLUGIN_VERSION].
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret =
      JSObject().apply {
        put("version", BuildConfig.PLUGIN_VERSION)
      }
    call.resolve(ret)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   * Ensure consistency with the JS LocalNetworkErrorCode object.
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
  @Suppress("unused")
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
}
