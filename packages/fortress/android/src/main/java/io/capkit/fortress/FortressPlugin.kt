package io.capkit.fortress

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
import io.capkit.fortress.error.ErrorMessages
import io.capkit.fortress.error.NativeError
import io.capkit.fortress.impl.BiometricAuth
import io.capkit.fortress.logger.Logger

/**
 * Capacitor bridge for the Integrity plugin (Android).
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
  private var lastSecurityStatus: JSObject? = null

  // ---------------------------------------------------------------------------
  // Companion Object
  // ---------------------------------------------------------------------------

  private companion object {
    /**
     * Account type identifier for internal plugin identification.
     */
    const val ACCOUNT_TYPE = "io.capkit.fortress"

    /**
     * Human-readable account name for the plugin.
     */
    const val ACCOUNT_NAME = "Fortress"
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

    config = Config(this)
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

    // Initial privacy screen enforcement if enabled in capacitor.config.ts
    if (config.enablePrivacyScreen) {
      implementation.lock(activity)
    }

    captureInitialSecurityStatus()

    Logger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // Lifecycle Handlers
  override fun onStop(owner: LifecycleOwner) {
    // 1. Register background timestamp for grace-period evaluation.
    implementation.setSessionBackgroundTimestamp()

    if (config.enablePrivacyScreen) {
      // 2. Enable privacy protection while app is in background.
      implementation.setPrivacyProtection(activity, true)

      // 3. Hide app content to harden recents/task-switcher snapshots.
      implementation.setContentVisibility(activity, false)
    }
  }

  override fun onStart(owner: LifecycleOwner) {
    val lockAfterMs = config.lockAfterMs.toLong()

    // 1. Valuta se la sessione è scaduta mentre l'app era in stop
    implementation.evaluateSessionBackgroundGracePeriod(lockAfterMs)

    if (config.enablePrivacyScreen) {
      // 2. Restore content visibility.
      implementation.setContentVisibility(activity, true)

      // 3. Keep privacy protection only while vault is locked.
      val locked = implementation.isLocked(activity)
      implementation.setPrivacyProtection(activity, locked)
    }

    notifySecurityStateIfChanged()
    notifyListeners("onAppResume", null)
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
    val message = error.message ?: ErrorMessages.INTERNAL_ERROR
    call.reject(message, error.errorCode)
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
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }

  @PluginMethod
  fun configure(call: PluginCall) {
    try {
      implementation.configure(config)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

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

  @PluginMethod
  fun getValue(call: PluginCall) {
    try {
      val key = call.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      val value = implementation.getValue(key)
      call.resolve(JSObject().apply { put("value", value) })
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

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

  @PluginMethod
  fun clearAll(call: PluginCall) {
    try {
      implementation.clearAll()
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

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

  @PluginMethod
  fun lock(call: PluginCall) {
    try {
      implementation.lock(activity)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun isLocked(call: PluginCall) {
    try {
      // Pass the activity ereditated from the Plugin class
      val isLocked = implementation.isLocked(activity)
      call.resolve(JSObject().apply { put("isLocked", isLocked) })
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun getSession(call: PluginCall) {
    try {
      val session = implementation.getSession()
      call.resolve(
        JSObject().apply {
          put("isLocked", session.isLocked)
          put("lastActiveAt", session.lastActiveAt)
        },
      )
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

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
            JSObject().apply {
              put("success", true)
              put("signature", signature)
            },
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
      call.resolve(
        JSObject().apply {
          put("keysExist", keysExist)
        },
      )
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun createKeys(call: PluginCall) {
    try {
      val keyAlias = call.getString("keyAlias")
      val publicKey = implementation.createKeys(keyAlias)
      call.resolve(
        JSObject().apply {
          put("publicKey", publicKey)
        },
      )
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
            JSObject().apply {
              put("publicKey", pair.first)
              put("signature", pair.second)
            },
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
          call.resolve(
            JSObject().apply {
              put("signature", signature)
            },
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
  fun generateChallengePayload(call: PluginCall) {
    try {
      val nonce = call.getString("nonce") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      if (nonce.isEmpty()) {
        throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
      }

      val payload = implementation.generateChallengePayload(nonce)
      call.resolve(
        JSObject().apply {
          put("payload", payload)
        },
      )
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
      call.resolve(JSObject().apply { put("value", value) })
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
      call.resolve(JSObject().apply { put("obfuscated", obfuscated) })
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
      call.resolve(JSObject().apply { put("exists", exists) })
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  @PluginMethod
  fun checkStatus(call: PluginCall) {
    val status = implementation.checkBiometricStatus(context)
    lastSecurityStatus = status
    call.resolve(status)
  }
}
