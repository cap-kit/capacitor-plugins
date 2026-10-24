package io.capkit.fortress

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.fortress.config.Config
import io.capkit.fortress.config.RuntimeConfigStore
import io.capkit.fortress.error.ErrorMessages
import io.capkit.fortress.error.NativeError
import io.capkit.fortress.impl.BiometricAuth
import io.capkit.fortress.logger.Logger
import io.capkit.fortress.model.AuthenticateWithChallengeResult
import io.capkit.fortress.model.BiometricKeysExistResult
import io.capkit.fortress.model.CreateKeysResult
import io.capkit.fortress.model.CreateSignatureResult
import io.capkit.fortress.model.DeviceSecurityStatusResult
import io.capkit.fortress.model.FortressRuntimeConfig
import io.capkit.fortress.model.FortressSessionResult
import io.capkit.fortress.model.GenerateChallengePayloadResult
import io.capkit.fortress.model.GetManyResult
import io.capkit.fortress.model.HasKeyResult
import io.capkit.fortress.model.IsLockedResult
import io.capkit.fortress.model.KeysResult
import io.capkit.fortress.model.ObfuscatedKeyResult
import io.capkit.fortress.model.PluginVersionResult
import io.capkit.fortress.model.PrivacyScreenActionResult
import io.capkit.fortress.model.PrivacyScreenStatus
import io.capkit.fortress.model.RegisterWithChallengeResult
import io.capkit.fortress.model.ValueResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capacitor bridge for the Fortress plugin (Android).
 *
 * CONTRACT:
 * - This class is the ONLY entry point from JavaScript.
 * - All PluginCall instances MUST be resolved or rejected exactly once.
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Invoke the native implementation
 * - Resolve or reject PluginCall exactly once
 * - Map native NativeError to JS-facing error codes
 *
 * Forbidden:
 * - Platform-specific business logic
 * - Direct system API usage outside lifecycle-bound orchestration
 * - Throwing uncaught exceptions
 */
@CapacitorPlugin(
  name = "Fortress",
)
@Suppress("unused")
class FortressPlugin :
  Plugin(),
  DefaultLifecycleObserver {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration.
   *
   * CONTRACT:
   * - Initialized exactly once in `load()`
   * - Treated as read-only afterward
   * - MUST NOT be mutated at runtime
   * - MUST NOT be accessed by the Impl layer
   */
  private lateinit var config: Config
  private lateinit var staticConfigBaseline: JSObject

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - Lifetime == plugin lifetime
   * - MUST NOT access PluginCall or Capacitor APIs
   * - MUST NOT perform UI operations
   */
  private lateinit var implementation: Fortress
  private lateinit var runtimeConfigStore: RuntimeConfigStore
  private var lastSecurityStatus: JSObject? = null
  private var overlayUnlockInProgress = false

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

  private fun currentActivityOrNull(): android.app.Activity? = activity ?: bridge.activity

  /**
   * Converts a serializable result model directly into a Capacitor JSObject.
   */
  private inline fun <reified T> toJSObject(value: T): JSObject {
    val jsonString = json.encodeToString(value)
    return JSObject(jsonString)
  }

  // ---------------------------------------------------------------------------
  // Companion Object
  // ---------------------------------------------------------------------------

  private companion object {
    const val REASON_SECURITY_STATE_CHANGED = "security_state_changed"
    const val REASON_KEYPAIR_INVALIDATED = "keypair_invalidated"
    const val REASON_KEYS_DELETED = "keys_deleted"
  }

  // -----------------------------------------------------------------------------
  // Lifecycle
  // -----------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This method initializes the configuration container and the native
   * implementation layer, ensuring all dependencies are injected.
   */
  override fun load() {
    super.load()

    staticConfigBaseline = Config(this).toRuntimeOverrides()
    config = Config(this)
    runtimeConfigStore = RuntimeConfigStore(context)
    runtimeConfigStore.loadOverrides()?.let { config.applyRuntimeOverrides(it) }
    implementation = Fortress(context)
    implementation.updateConfig(config)

    // Register Lifecycle Observer
    bridge.activity.runOnUiThread {
      ProcessLifecycleOwner
        .get()
        .lifecycle
        .addObserver(this)
    }

    // Connect Session Callback to Capacitor Events
    implementation.setSessionLockCallback { isLocked ->
      if (isLocked) {
        notifyListeners("sessionLocked", null)
      } else {
        notifyListeners("sessionUnlocked", null)
      }
      notifyLockStatusChanged(isLocked)
    }

    implementation.setPrivacyScreenTapCallback {
      val hostActivity = activity as? FragmentActivity ?: return@setPrivacyScreenTapCallback
      if (overlayUnlockInProgress) {
        return@setPrivacyScreenTapCallback
      }

      overlayUnlockInProgress = true
      implementation.unlock(hostActivity, null) { result ->
        overlayUnlockInProgress = false
        result.onFailure { error ->
          Logger.warn("Overlay tap unlock failed: ${error.message}")
        }
      }
    }

    // Initial privacy protection sync for current foreground state.
    // ProcessLifecycle onStart may not fire immediately when observer is
    // registered while app is already in foreground.
    val hostActivity = currentActivityOrNull()
    if (implementation.isPrivacyScreenActive()) {
      implementation.setContentVisibility(hostActivity, true)
      val locked = implementation.isLocked(hostActivity)
      implementation.setPrivacyProtection(hostActivity, locked)
    } else {
      implementation.setPrivacyProtection(hostActivity, false)
    }

    captureInitialSecurityStatus()

    Logger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // Lifecycle Handlers
  override fun onPause(owner: LifecycleOwner) {
    if (implementation.isPrivacyScreenActive()) {
      val hostActivity = currentActivityOrNull()
      implementation.setWindowSecure(hostActivity, true)
      implementation.setPrivacyProtection(hostActivity, true)
      implementation.setContentVisibility(hostActivity, false)
    }
  }

  /**
   * Capacitor activity lifecycle hook.
   *
   * This fires earlier than ProcessLifecycleOwner callbacks on some OEM builds
   * and helps ensure privacy protection is applied before recents snapshot.
   */
  override fun handleOnPause() {
    super.handleOnPause()
    if (implementation.isPrivacyScreenActive()) {
      val hostActivity = currentActivityOrNull()
      implementation.setWindowSecure(hostActivity, true)
      implementation.setPrivacyProtection(hostActivity, true)
      implementation.setContentVisibility(hostActivity, false)
    }
  }

  override fun handleOnResume() {
    super.handleOnResume()
    val hostActivity = currentActivityOrNull()
    if (implementation.isPrivacyScreenActive()) {
      implementation.setContentVisibility(hostActivity, true)
      val locked = implementation.isLocked(hostActivity)
      implementation.setPrivacyProtection(hostActivity, locked)
    }
    syncScreenshotCallback()
  }

  override fun onStop(owner: LifecycleOwner) {
    val hostActivity = currentActivityOrNull()

    // 1. Register background timestamp for grace-period evaluation.
    implementation.setSessionBackgroundTimestamp()

    if (implementation.isPrivacyScreenActive()) {
      // 2. Enable privacy protection while app is in background.
      implementation.setPrivacyProtection(hostActivity, true)

      // 3. Hide app content to harden recents/task-switcher snapshots.
      implementation.setContentVisibility(hostActivity, false)
    }
  }

  override fun onStart(owner: LifecycleOwner) {
    val hostActivity = currentActivityOrNull()
    val lockAfterMs = config.lockAfterMs.toLong()

    // 1. Evaluate whether session expired while the app was in stop/background.
    implementation.evaluateSessionBackgroundGracePeriod(lockAfterMs)

    if (implementation.isPrivacyScreenActive()) {
      // 2. Restore content visibility.
      implementation.setContentVisibility(hostActivity, true)

      // 3. Keep privacy protection only while vault is locked.
      val locked = implementation.isLocked(hostActivity)
      implementation.setPrivacyProtection(hostActivity, locked)
    }

    notifySecurityStateIfChanged()
    notifyListeners("onAppResume", null)
  }

  /**
   * Reconciles the API 34+ screen-capture callback with the effective
   * privacy state. Re-registers on every resume so activity recreation
   * never leaves a stale handle behind. No-op below API 34.
   */
  private fun syncScreenshotCallback() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      return
    }
    val hostActivity = currentActivityOrNull()
    (screenCaptureCallback as? android.app.Activity.ScreenCaptureCallback)?.let { previous ->
      try {
        hostActivity?.unregisterScreenCaptureCallback(previous)
      } catch (_: Exception) {
      }
      screenCaptureCallback = null
    }
    if (hostActivity == null || !implementation.isPrivacyScreenActive()) {
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

  private fun parsePromptOptions(call: PluginCall): BiometricAuth.PromptOptions? {
    val promptOptions = call.getObject("promptOptions") ?: return null

    return BiometricAuth.PromptOptions(
      title = promptOptions.getString("title"),
      subtitle = promptOptions.getString("subtitle"),
      description = promptOptions.getString("description"),
      negativeButtonText = promptOptions.getString("negativeButtonText"),
      confirmationRequired = promptOptions.getBool("confirmationRequired"),
    )
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native NativeError values to JavaScript-facing error codes.
   *
   * CONTRACT:
   * - This method is the ONLY place where native errors
   *   are translated into JS-visible failures.
   * - Error codes MUST be:
   *   - stable
   *   - documented
   *   - identical across platforms
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
        is NativeError.NotFound -> "NOT_FOUND"
        is NativeError.Conflict -> "CONFLICT"
        is NativeError.Timeout -> "TIMEOUT"
        is NativeError.SecurityViolation -> "SECURITY_VIOLATION"
        is NativeError.VaultLocked -> "VAULT_LOCKED"
      }

    val message = error.message ?: ErrorMessages.INTERNAL_ERROR
    call.reject(message, code)
  }

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
  // Events
  // ---------------------------------------------------------------------------

  private fun notifyLockStatusChanged(isLocked: Boolean) {
    val data = JSObject()
    data.put("isLocked", isLocked)
    notifyListeners("onLockStatusChanged", data)
  }

  private fun captureInitialSecurityStatus() {
    lastSecurityStatus = implementation.checkBiometricStatus(context)
  }

  private fun notifySecurityStateIfChanged() {
    val currentStatus = implementation.checkBiometricStatus(context)
    val previousStatus = lastSecurityStatus

    if (previousStatus == null || !areSecurityStatusesEqual(previousStatus, currentStatus)) {
      notifyListeners("onSecurityStateChanged", currentStatus)

      if (previousStatus != null && didSecurityPostureDowngrade(previousStatus, currentStatus)) {
        notifyVaultInvalidated(REASON_SECURITY_STATE_CHANGED)
      }

      lastSecurityStatus = currentStatus
    }
  }

  private fun notifyVaultInvalidated(reason: String) {
    val payload = JSObject()
    payload.put("reason", reason)
    notifyListeners("onVaultInvalidated", payload)
  }

  private fun areSecurityStatusesEqual(
    previous: JSObject,
    current: JSObject,
  ): Boolean =
    previous.getBool("isBiometricsAvailable") == current.getBool("isBiometricsAvailable") &&
      previous.getBool("isBiometricsEnabled") == current.getBool("isBiometricsEnabled") &&
      previous.getBool("isDeviceSecure") == current.getBool("isDeviceSecure") &&
      previous.getString("biometryType") == current.getString("biometryType")

  private fun didSecurityPostureDowngrade(
    previous: JSObject,
    current: JSObject,
  ): Boolean {
    val wasDeviceSecure = previous.getBool("isDeviceSecure") ?: false
    val isDeviceSecure = current.getBool("isDeviceSecure") ?: false

    val wasBiometricsEnabled = previous.getBool("isBiometricsEnabled") ?: false
    val isBiometricsEnabled = current.getBool("isBiometricsEnabled") ?: false

    return (wasDeviceSecure && !isDeviceSecure) || (wasBiometricsEnabled && !isBiometricsEnabled)
  }

  // ---------------------------------------------------------------------------
  // Version
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version.
   *
   * NOTE:
   * - This method is guaranteed not to fail
   * - Version is injected at build time from package.json
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    call.resolve(toJSObject(PluginVersionResult(version = BuildConfig.PLUGIN_VERSION)))
  }

  /**
   * Returns the runtime configuration currently used by the plugin.
   */
  @PluginMethod
  fun getRuntimeConfig(call: PluginCall) {
    call.resolve(
      toJSObject(
        FortressRuntimeConfig(
          verboseLogging = config.verboseLogging,
          logLevel = config.logLevel,
          lockAfterMs = config.lockAfterMs,
          enablePrivacyScreen = config.enablePrivacyScreen,
          privacyOverlayText = config.privacyOverlayText,
          privacyOverlayImageName = config.privacyOverlayImageName,
          privacyOverlayShowText = config.privacyOverlayShowText,
          privacyOverlayShowImage = config.privacyOverlayShowImage,
          privacyOverlayTextColor = config.privacyOverlayTextColor,
          privacyOverlayBackgroundOpacity = config.privacyOverlayBackgroundOpacity,
          privacyOverlayTheme = config.privacyOverlayTheme,
          obfuscateKeys = config.obfuscateKeys,
          privacyScreenEnabled = implementation.isPrivacyScreenActive(),
          fallbackStrategy = config.fallbackStrategy,
          allowCachedAuthentication = config.allowCachedAuthentication,
          cachedAuthenticationTimeoutMs = config.cachedAuthenticationTimeoutMs,
          maxBiometricAttempts = config.maxBiometricAttempts,
          lockoutDurationMs = config.lockoutDurationMs,
          requireFreshAuthenticationMs = config.requireFreshAuthenticationMs,
          encryptionAlgorithm = config.encryptionAlgorithm,
          persistSessionState = config.persistSessionState,
        ),
      ),
    )
  }

  /**
   * Applies runtime configuration already parsed at plugin load.
   */
  @PluginMethod
  fun configure(call: PluginCall) {
    try {
      config.applyRuntimeOverrides(call.data)

      implementation.configure(config)
      runtimeConfigStore.saveOverrides(config.toRuntimeOverrides())
      implementation.setPrivacyScreenManualOverride(null)

      if (implementation.isPrivacyScreenActive()) {
        val locked = implementation.isLocked(activity)
        implementation.setPrivacyProtection(activity, locked)
      } else {
        implementation.setPrivacyProtection(activity, false)
      }

      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun resetRuntimeConfig(call: PluginCall) {
    try {
      config = Config(this)
      config.applyRuntimeOverrides(staticConfigBaseline)
      implementation.configure(config)
      runtimeConfigStore.clearOverrides()
      implementation.setPrivacyScreenManualOverride(null)

      if (implementation.isPrivacyScreenActive()) {
        val locked = implementation.isLocked(activity)
        implementation.setPrivacyProtection(activity, locked)
      } else {
        implementation.setPrivacyProtection(activity, false)
      }

      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Stores a secure value in the encrypted vault.
   */
  @PluginMethod
  fun setValue(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      val value = call.getString("value") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      implementation.setValue(key, value)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Stores multiple secure values in a single operation.
   */
  @PluginMethod
  fun setMany(call: PluginCall) {
    val values =
      call.getArray("values")?.toList<JSObject>() ?: run {
        call.reject(ErrorMessages.INVALID_INPUT)
        return
      }

    try {
      implementation.setMany(values)
      call.resolve()
    } catch (e: Exception) {
      handleError(call, e)
    }
  }

  /**
   * Reads a secure value from the encrypted vault.
   */
  @PluginMethod
  fun getValue(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      val value = implementation.getValue(key)
      call.resolve(toJSObject(ValueResult(value = value)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Removes a secure value from the encrypted vault.
   */
  @PluginMethod
  fun removeValue(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      implementation.removeValue(key)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Clears all secure values managed by the plugin.
   */
  @PluginMethod
  fun clearAll(call: PluginCall) {
    try {
      implementation.clearAll()
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Unlocks the vault using biometric/device-credential authentication.
   */
  @PluginMethod
  fun unlock(call: PluginCall) {
    val hostActivity = activity as? FragmentActivity
    if (hostActivity == null) {
      reject(call, NativeError.Unavailable(ErrorMessages.UNAVAILABLE))
      return
    }

    val promptOptions = parsePromptOptions(call)
    implementation.unlock(hostActivity, promptOptions) { result ->
      result
        .onSuccess {
          call.resolve()
        }.onFailure { error ->
          if (error is NativeError.NotFound) {
            notifyVaultInvalidated(REASON_KEYPAIR_INVALIDATED)
          }
          handleError(call, error)
        }
    }
  }

  /**
   * Locks the vault and applies privacy protection when configured.
   */
  @PluginMethod
  fun lock(call: PluginCall) {
    try {
      implementation.lock(activity)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Verifies identity without touching vault or session state.
   *
   * Unlike `unlock()`, a success here does NOT unlock the vault, update
   * the session, or hide the privacy overlay. `allowDeviceCredential`,
   * when present, overrides the configured fallback strategy for this
   * call only; `reason` (or legacy `promptMessage`) feeds the prompt
   * description.
   */
  @PluginMethod
  fun authenticate(call: PluginCall) {
    val hostActivity = activity as? FragmentActivity
    if (hostActivity == null) {
      reject(call, NativeError.Unavailable(ErrorMessages.UNAVAILABLE))
      return
    }

    val baseOptions = parsePromptOptions(call)
    val reason = call.getString("reason") ?: call.getString("promptMessage")
    val promptOptions =
      if (reason != null && baseOptions?.description == null) {
        (
          baseOptions ?: BiometricAuth.PromptOptions(null, null, null, null, null)
        ).copy(description = reason)
      } else {
        baseOptions
      }
    val allowPasscode =
      call.data.optBoolean("allowDeviceCredential", implementation.resolveAllowPasscode())

    implementation.authenticateIdentity(hostActivity, promptOptions, allowPasscode) { result ->
      result
        .onSuccess {
          call.resolve()
        }.onFailure { error ->
          handleError(call, error)
        }
    }
  }

  /**
   * Cancels an ongoing interactive authentication prompt, if any.
   */
  @PluginMethod
  fun cancelAuthentication(call: PluginCall) {
    try {
      implementation.cancelActiveAuthentication()
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports whether biometric authentication can currently be used.
   */
  @PluginMethod
  fun isAvailable(call: PluginCall) {
    try {
      val status = implementation.checkBiometricStatus(context)
      call.resolve(JSObject().put("isAvailable", status.getBool("isBiometricsAvailable") ?: false))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports whether the user enrolled biometrics.
   */
  @PluginMethod
  fun isEnrolled(call: PluginCall) {
    try {
      val status = implementation.checkBiometricStatus(context)
      call.resolve(JSObject().put("isEnrolled", status.getBool("isBiometricsEnabled") ?: false))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns the primary biometry modality of the device.
   */
  @PluginMethod
  fun getBiometricType(call: PluginCall) {
    try {
      val status = implementation.checkBiometricStatus(context)
      call.resolve(JSObject().put("biometryType", status.getString("biometryType") ?: "none"))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns every biometry modality known to the device hardware.
   */
  @PluginMethod
  fun getBiometricTypes(call: PluginCall) {
    try {
      val status = implementation.checkBiometricStatus(context)
      call.resolve(JSObject().put("biometryTypes", toStringList(status, "biometryTypes")))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports whether the user set a device credential usable as fallback.
   */
  @PluginMethod
  fun hasDeviceCredential(call: PluginCall) {
    try {
      val status = implementation.checkBiometricStatus(context)
      call.resolve(
        JSObject().put("hasDeviceCredential", status.getBool("isDeviceSecure") ?: false),
      )
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports the strength class of the available biometry.
   */
  @PluginMethod
  fun getBiometricStrengthLevel(call: PluginCall) {
    try {
      val status = implementation.checkBiometricStatus(context)
      val strong = status.getBool("strongBiometryIsAvailable") ?: false
      val available = status.getBool("isBiometricsAvailable") ?: false
      val level =
        if (strong) {
          "strong"
        } else if (available) {
          "weak"
        } else {
          "none"
        }
      call.resolve(JSObject().put("strengthLevel", level))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports which credential satisfied the last successful native ceremony.
   */
  @PluginMethod
  fun getAuthenticationType(call: PluginCall) {
    try {
      call.resolve(JSObject().put("authenticationType", implementation.getAuthenticationType()))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Opens the system biometric enrollment screen (Android API 30+).
   */
  @PluginMethod
  fun enroll(call: PluginCall) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      reject(call, NativeError.Unavailable(ErrorMessages.UNAVAILABLE))
      return
    }
    try {
      val intent =
        Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
          putExtra(
            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
              BiometricManager.Authenticators.BIOMETRIC_WEAK,
          )
        }
      currentActivityOrNull()?.startActivity(intent) ?: run {
        reject(call, NativeError.Unavailable(ErrorMessages.UNAVAILABLE))
        return
      }
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns whether the vault is currently locked.
   */
  @PluginMethod
  fun isLocked(call: PluginCall) {
    try {
      // Use the activity inherited from the Capacitor Plugin base class.
      val isLocked = implementation.isLocked(activity)
      call.resolve(toJSObject(IsLockedResult(isLocked = isLocked)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Enables privacy-screen protection independently of the vault lock state.
   *
   * The official `PrivacyScreenConfig` shape (`android` / `ios` display knobs)
   * is accepted for API compatibility; visual style stays Fortress-driven.
   * Explicit manual control detaches privacy from the follow-lock policy
   * until configure()/resetRuntimeConfig() re-attaches it.
   */
  @PluginMethod
  fun enable(call: PluginCall) {
    try {
      implementation.setPrivacyScreenManualOverride(true)
      implementation.setPrivacyProtection(currentActivityOrNull(), true)
      syncScreenshotCallback()
      call.resolve(toJSObject(PrivacyScreenActionResult(success = true)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Disables privacy-screen protection independently of the vault lock state.
   *
   * Use only when the current screen must stay visible in system previews.
   */
  @PluginMethod
  fun disable(call: PluginCall) {
    try {
      implementation.setPrivacyScreenManualOverride(false)
      implementation.setPrivacyProtection(currentActivityOrNull(), false)
      syncScreenshotCallback()
      call.resolve(toJSObject(PrivacyScreenActionResult(success = true)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns the current privacy-screen enabled state, independent from
   * the vault lock state.
   */
  @PluginMethod
  fun isEnabled(call: PluginCall) {
    try {
      call.resolve(toJSObject(PrivacyScreenStatus(enabled = implementation.isPrivacyScreenActive())))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns the current session state snapshot.
   */
  @PluginMethod
  fun getSession(call: PluginCall) {
    try {
      val session = implementation.getSession()
      call.resolve(
        toJSObject(
          FortressSessionResult(
            isLocked = session.isLocked,
            lastActiveAt = session.lastActiveAt,
          ),
        ),
      )
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Resets session state and enforces locked vault semantics.
   */
  @PluginMethod
  fun resetSession(call: PluginCall) {
    try {
      // Pass the activity instance provided by the Capacitor Plugin class
      implementation.resetSession(activity)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Refreshes session activity timestamp when vault is unlocked.
   */
  @PluginMethod
  fun touchSession(call: PluginCall) {
    try {
      implementation.touchSession(activity)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun createSignature(call: PluginCall) {
    val payload = call.getString("payload")
    if (payload.isNullOrEmpty()) {
      reject(call, NativeError.InvalidInput(ErrorMessages.INVALID_INPUT))
      return
    }

    val hostActivity = activity as? FragmentActivity
    if (hostActivity == null) {
      reject(call, NativeError.Unavailable(ErrorMessages.UNAVAILABLE))
      return
    }

    val keyAlias = call.getString("keyAlias")
    val promptMessage = call.getString("promptMessage")
    val promptOptions = parsePromptOptions(call)

    implementation.createSignature(
      activity = hostActivity,
      payload = payload,
      keyAlias = keyAlias,
      promptMessage = promptMessage,
      promptOptions = promptOptions,
    ) { result ->
      result
        .onSuccess { signature ->
          call.resolve(
            toJSObject(
              CreateSignatureResult(
                success = true,
                signature = signature,
              ),
            ),
          )
        }.onFailure { error ->
          if (error is NativeError.NotFound) {
            notifyVaultInvalidated(REASON_KEYPAIR_INVALIDATED)
          }
          handleError(call, error)
        }
    }
  }

  @PluginMethod
  fun biometricKeysExist(call: PluginCall) {
    try {
      val keyAlias = call.getString("keyAlias")
      val keysExist = implementation.biometricKeysExist(keyAlias)
      call.resolve(toJSObject(BiometricKeysExistResult(keysExist = keysExist)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun createKeys(call: PluginCall) {
    try {
      val keyAlias = call.getString("keyAlias")
      val publicKey = implementation.createKeys(keyAlias)
      call.resolve(toJSObject(CreateKeysResult(publicKey = publicKey)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun deleteKeys(call: PluginCall) {
    try {
      val keyAlias = call.getString("keyAlias")
      val hadKeys = implementation.biometricKeysExist(keyAlias)
      implementation.deleteKeys(keyAlias)

      if (hadKeys) {
        notifyVaultInvalidated(REASON_KEYS_DELETED)
      }

      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun registerWithChallenge(call: PluginCall) {
    val challenge = call.getString("challenge")
    if (challenge.isNullOrEmpty()) {
      reject(call, NativeError.InvalidInput(ErrorMessages.INVALID_INPUT))
      return
    }

    val hostActivity = activity as? FragmentActivity
    if (hostActivity == null) {
      reject(call, NativeError.Unavailable(ErrorMessages.UNAVAILABLE))
      return
    }

    val keyAlias = call.getString("keyAlias")
    val promptMessage = call.getString("promptMessage")
    val promptOptions = parsePromptOptions(call)

    implementation.registerWithChallenge(
      activity = hostActivity,
      challenge = challenge,
      keyAlias = keyAlias,
      promptMessage = promptMessage,
      promptOptions = promptOptions,
    ) { result ->
      result
        .onSuccess { pair ->
          call.resolve(
            toJSObject(
              RegisterWithChallengeResult(
                publicKey = pair.first,
                signature = pair.second,
              ),
            ),
          )
        }.onFailure { error ->
          if (error is NativeError.NotFound) {
            notifyVaultInvalidated(REASON_KEYPAIR_INVALIDATED)
          }
          handleError(call, error)
        }
    }
  }

  @PluginMethod
  fun authenticateWithChallenge(call: PluginCall) {
    val challenge = call.getString("challenge")
    if (challenge.isNullOrEmpty()) {
      reject(call, NativeError.InvalidInput(ErrorMessages.INVALID_INPUT))
      return
    }

    val hostActivity = activity as? FragmentActivity
    if (hostActivity == null) {
      reject(call, NativeError.Unavailable(ErrorMessages.UNAVAILABLE))
      return
    }

    val keyAlias = call.getString("keyAlias")
    val promptMessage = call.getString("promptMessage")
    val promptOptions = parsePromptOptions(call)

    implementation.authenticateWithChallenge(
      activity = hostActivity,
      challenge = challenge,
      keyAlias = keyAlias,
      promptMessage = promptMessage,
      promptOptions = promptOptions,
    ) { result ->
      result
        .onSuccess { signature ->
          call.resolve(toJSObject(AuthenticateWithChallengeResult(signature = signature)))
        }.onFailure { error ->
          if (error is NativeError.NotFound) {
            notifyVaultInvalidated(REASON_KEYPAIR_INVALIDATED)
          }
          handleError(call, error)
        }
    }
  }

  @PluginMethod
  fun generateChallengePayload(call: PluginCall) {
    try {
      val nonce = call.getString("nonce") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      if (nonce.isEmpty()) {
        throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      }

      val payload = implementation.generateChallengePayload(nonce)
      call.resolve(toJSObject(GenerateChallengePayloadResult(payload = payload)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun setInsecureValue(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      val value = call.getString("value") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      implementation.setInsecureValue(key, value)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun getInsecureValue(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      val value = implementation.getInsecureValue(key)
      call.resolve(toJSObject(ValueResult(value = value)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun removeInsecureValue(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      implementation.removeInsecureValue(key)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun getObfuscatedKey(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      val obfuscated = implementation.getObfuscatedKey(key)
      call.resolve(toJSObject(ObfuscatedKeyResult(obfuscated = obfuscated)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun hasKey(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      val secure = call.getBoolean("secure", true) ?: true
      val exists = implementation.hasKey(key, secure)
      call.resolve(toJSObject(HasKeyResult(exists = exists)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun checkStatus(call: PluginCall) {
    val status = implementation.checkBiometricStatus(context)
    lastSecurityStatus = status
    call.resolve(toJSObject(toDeviceSecurityStatus(status)))
  }

  private fun toDeviceSecurityStatus(status: JSObject): DeviceSecurityStatusResult =
    DeviceSecurityStatusResult(
      isBiometricsAvailable = status.getBool("isBiometricsAvailable") ?: false,
      isBiometricsEnabled = status.getBool("isBiometricsEnabled") ?: false,
      isDeviceSecure = status.getBool("isDeviceSecure") ?: false,
      biometryType = status.getString("biometryType") ?: "none",
      biometryTypes = toStringList(status, "biometryTypes"),
      strongBiometryIsAvailable = status.getBool("strongBiometryIsAvailable") ?: false,
    )

  private fun toStringList(
    status: JSObject,
    key: String,
  ): List<String> {
    val values = mutableListOf<String>()
    try {
      val array = status.optJSONArray(key) ?: return values
      for (index in 0 until array.length()) {
        array.optString(index, null)?.let { values.add(it) }
      }
    } catch (_: Exception) {
      // Best effort only; malformed payloads yield an empty list.
    }
    return values
  }

  /**
   * Lists keys in secure or insecure storage.
   */
  @PluginMethod
  fun keys(call: PluginCall) {
    try {
      val secure = call.data.optBoolean("secure", true)
      call.resolve(toJSObject(KeysResult(keys = implementation.keys(secure))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reads several keys in one call. Missing keys map to null.
   */
  @PluginMethod
  fun getMany(call: PluginCall) {
    val keys =
      call.getArray("keys")?.toList<String>() ?: run {
        call.reject(ErrorMessages.INVALID_INPUT)
        return
      }
    try {
      val secure = call.data.optBoolean("secure", true)
      call.resolve(toJSObject(GetManyResult(values = implementation.getMany(keys, secure))))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * iCloud Keychain does not exist on Android; no-op for API parity.
   */
  @PluginMethod
  fun setSynchronize(call: PluginCall) {
    try {
      implementation.setSynchronize(call.data.optBoolean("synchronize", false))
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Always false on Android; iCloud Keychain does not exist here.
   */
  @PluginMethod
  fun getSynchronize(call: PluginCall) {
    try {
      val result = JSObject()
      result.put("synchronize", implementation.isSynchronized())
      call.resolve(result)
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * iOS Keychain accessibility has no Android equivalent; no-op.
   */
  @PluginMethod
  fun setDefaultKeychainAccess(call: PluginCall) {
    try {
      implementation.setDefaultKeychainAccess(call.getString("access") ?: "")
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Overrides the detected biometry type for development/testing flows.
   *
   * Accepted values: `none`, `touchId`, `faceId`, `fingerprint`, `iris`.
   */
  @PluginMethod
  fun setBiometryType(call: PluginCall) {
    val biometryType = call.getString("biometryType")
    if (biometryType == null ||
      (
        biometryType != "none" &&
          biometryType != "touchId" &&
          biometryType != "faceId" &&
          biometryType != "fingerprint" &&
          biometryType != "iris"
      )
    ) {
      reject(call, NativeError.InvalidInput(ErrorMessages.INVALID_INPUT))
      return
    }

    implementation.setBiometryType(biometryType)
    val status = implementation.checkBiometricStatus(context)
    lastSecurityStatus = status
    notifyListeners("onSecurityStateChanged", status)
    call.resolve()
  }

  /**
   * Overrides biometric enrollment state for development/testing flows.
   */
  @PluginMethod
  fun setBiometryIsEnrolled(call: PluginCall) {
    val isBiometricsEnabled = call.getBoolean("isBiometricsEnabled")
    if (isBiometricsEnabled == null) {
      reject(call, NativeError.InvalidInput(ErrorMessages.INVALID_INPUT))
      return
    }

    implementation.setBiometryIsEnrolled(isBiometricsEnabled)
    val status = implementation.checkBiometricStatus(context)
    lastSecurityStatus = status
    notifyListeners("onSecurityStateChanged", status)
    call.resolve()
  }

  /**
   * Overrides device secure-state for development/testing flows.
   */
  @PluginMethod
  fun setDeviceIsSecure(call: PluginCall) {
    val isDeviceSecure = call.getBoolean("isDeviceSecure")
    if (isDeviceSecure == null) {
      reject(call, NativeError.InvalidInput(ErrorMessages.INVALID_INPUT))
      return
    }

    implementation.setDeviceIsSecure(isDeviceSecure)
    val status = implementation.checkBiometricStatus(context)
    lastSecurityStatus = status
    notifyListeners("onSecurityStateChanged", status)
    call.resolve()
  }
}
