package io.capkit.fortress

import android.content.Context
import android.util.Base64
import androidx.fragment.app.FragmentActivity
import com.getcapacitor.JSObject
import io.capkit.fortress.config.Config
import io.capkit.fortress.error.ErrorMessages
import io.capkit.fortress.error.NativeError
import io.capkit.fortress.impl.BiometricAuth
import io.capkit.fortress.impl.PrivacyScreen
import io.capkit.fortress.impl.SecureStorage
import io.capkit.fortress.impl.SessionManager
import io.capkit.fortress.impl.SessionState
import io.capkit.fortress.impl.StandardStorage
import io.capkit.fortress.logger.Logger
import io.capkit.fortress.utils.KeyUtils
import io.capkit.fortress.utils.KeystoreHelper
import io.capkit.fortress.utils.Utils

/**
 * Platform-specific native implementation for the Fortress plugin.
 *
 * This class contains pure Android logic and MUST NOT depend
 * directly on Capacitor bridge APIs.
 *
 * The Capacitor plugin class is responsible for:
 * - reading configuration
 * - handling PluginCall objects
 * - delegating logic to this implementation
 */
class Fortress(
  private val context: Context,
) {
  private companion object {
    const val DEFAULT_BIOMETRIC_KEY_ALIAS = "biometric_keypair"
  }
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Cached immutable plugin configuration.
   */
  private lateinit var config: Config
  private val secureStorage = SecureStorage(context)
  private val standardStorage = StandardStorage(context)
  private val biometricAuth = BiometricAuth(context)
  private val sessionManager = SessionManager()
  private val privacyScreen = PrivacyScreen()
  private var lastSuccessfulAuthAtMs: Long = 0
  private var failedBiometricAttempts: Int = 0
  private var lockoutUntilMs: Long = 0

  /**
   * Applies static plugin configuration.
   */
  fun updateConfig(newConfig: Config) {
    this.config = newConfig
    Logger.verbose = newConfig.verboseLogging
    Logger.setLevel(newConfig.logLevel)
    Logger.debug(
      "Configuration applied. Log level:",
      newConfig.logLevel,
    )
  }

  fun configure(config: Config) {
    updateConfig(config)

    if (config.encryptionAlgorithm != "AES-GCM") {
      throw NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
    }

    if (config.cryptoStrategy != "auto" && config.cryptoStrategy != "ecc" && config.cryptoStrategy != "rsa") {
      throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
    }

    if (config.keySize != 2048 && config.keySize != 4096) {
      throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
    }
  }

  private fun shouldUseCachedAuthentication(): Boolean {
    if (!config.allowCachedAuthentication) {
      return false
    }

    val timeoutMs = config.cachedAuthenticationTimeoutMs.toLong()
    if (timeoutMs <= 0 || lastSuccessfulAuthAtMs <= 0) {
      return false
    }

    val freshnessTimeout = config.requireFreshAuthenticationMs.toLong()
    if (freshnessTimeout > 0 && System.currentTimeMillis() - lastSuccessfulAuthAtMs > freshnessTimeout) {
      return false
    }

    return System.currentTimeMillis() - lastSuccessfulAuthAtMs <= timeoutMs
  }

  private fun markAuthenticationSuccess() {
    lastSuccessfulAuthAtMs = System.currentTimeMillis()
    failedBiometricAttempts = 0
    lockoutUntilMs = 0
  }

  private fun clearAuthenticationCache() {
    lastSuccessfulAuthAtMs = 0
    failedBiometricAttempts = 0
    lockoutUntilMs = 0
  }

  private fun assertNotLockedOut() {
    if (System.currentTimeMillis() < lockoutUntilMs) {
      throw NativeError.SecurityViolation(ErrorMessages.SECURITY_VIOLATION)
    }
  }

  private fun recordBiometricFailure(error: Throwable) {
    val maxAttempts = config.maxBiometricAttempts
    val lockoutDuration = config.lockoutDurationMs
    if (maxAttempts <= 0 || lockoutDuration <= 0) {
      return
    }

    if (error is NativeError.Cancelled) {
      return
    }

    failedBiometricAttempts += 1
    if (failedBiometricAttempts >= maxAttempts) {
      lockoutUntilMs = System.currentTimeMillis() + lockoutDuration.toLong()
      failedBiometricAttempts = 0
    }
  }

  fun setValue(
    key: String,
    value: String,
  ) {
    ensureSecureVaultAccessible()
    // Correctly passing the hardware-backed security requirement from config
    secureStorage.set(key, value, requireStrongBox = config.requireStrongBox)
  }

  fun setMany(values: List<JSObject>) {
    val operations =
      values.map { item ->
        val key = item.getString("key") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
        val value = item.getString("value") ?: throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
        val secure = item.getBoolean("secure") ?: true
        SetManyOperation(key = key, value = value, secure = secure)
      }

    if (operations.any { it.secure }) {
      ensureSecureVaultAccessible()
    }

    val snapshot = HashMap<String, String?>()
    operations.forEach { operation ->
      val marker = operationMarker(operation)
      if (!snapshot.containsKey(marker)) {
        snapshot[marker] = readOperationValue(operation)
      }
    }

    try {
      operations.forEach { operation ->
        if (operation.secure) {
          secureStorage.set(operation.key, operation.value, requireStrongBox = config.requireStrongBox)
        } else {
          setInsecureValue(operation.key, operation.value)
        }
      }
    } catch (error: Throwable) {
      rollbackSetMany(snapshot)
      throw error
    }
  }

  fun getValue(key: String): String? {
    ensureSecureVaultAccessible()
    return secureStorage.get(key)
  }

  fun removeValue(key: String) {
    secureStorage.remove(key)
  }

  fun clearAll() {
    secureStorage.clearAll()
  }

  fun unlock(
    activity: FragmentActivity,
    promptOptions: BiometricAuth.PromptOptions?,
    completion: (Result<Unit>) -> Unit,
  ) {
    try {
      assertNotLockedOut()
    } catch (error: Throwable) {
      completion(Result.failure(error))
      return
    }

    if (shouldUseCachedAuthentication()) {
      if (isPrivacyScreenEnabled()) {
        privacyScreen.unlock(activity)
      }
      sessionManager.unlock()
      completion(Result.success(Unit))
      return
    }

    biometricAuth.unlock(
      activity = activity,
      allowPasscode = config.allowDevicePasscode,
      promptText = config.biometricPromptText,
      promptOptions = promptOptions,
    ) { result ->
      result
        .onSuccess {
          markAuthenticationSuccess()
          if (isPrivacyScreenEnabled()) {
            privacyScreen.unlock(activity)
          }
          sessionManager.unlock()
          completion(Result.success(Unit))
        }.onFailure { error ->
          recordBiometricFailure(error)
          completion(Result.failure(error))
        }
    }
  }

  fun lock(activity: android.app.Activity?) {
    if (isPrivacyScreenEnabled()) {
      privacyScreen.lock(activity)
    }
    sessionManager.lock() // Ensure sessionManager is updated to LOCKED
    clearAuthenticationCache()
  }

  fun isLocked(activity: android.app.Activity?): Boolean {
    val locked = sessionManager.evaluateLockState(config.lockAfterMs.toLong())
    if (locked && isPrivacyScreenEnabled()) {
      privacyScreen.lock(activity)
    }
    return locked
  }

  fun getSession(): SessionState = sessionManager.getSession()

  fun resetSession(activity: android.app.Activity?) {
    // Reset session state and force vault lock with activity reference
    sessionManager.reset()
    if (isPrivacyScreenEnabled()) {
      privacyScreen.lock(activity)
    }
    clearAuthenticationCache()
  }

  /**
   * Updates the activity timestamp and checks if the session has expired.
   */
  fun touchSession(activity: android.app.Activity?) {
    val locked = sessionManager.evaluateLockState(config.lockAfterMs.toLong())
    if (locked) {
      if (isPrivacyScreenEnabled()) {
        privacyScreen.lock(activity)
      }
      return
    }

    sessionManager.touch()
  }

  fun createSignature(
    activity: FragmentActivity,
    payload: String,
    keyAlias: String?,
    promptMessage: String?,
    promptOptions: BiometricAuth.PromptOptions?,
    completion: (Result<String>) -> Unit,
  ) {
    try {
      assertNotLockedOut()
    } catch (error: Throwable) {
      completion(Result.failure(error))
      return
    }

    if (payload.isEmpty()) {
      completion(Result.failure(NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)))
      return
    }

    try {
      ensureSecureVaultAccessible()
    } catch (error: Throwable) {
      completion(Result.failure(error))
      return
    }

    val resolvedAlias = keyAlias ?: DEFAULT_BIOMETRIC_KEY_ALIAS
    // Explicit registration state check
    val isRegistered = KeystoreHelper.hasKeyPair(resolvedAlias)
    if (!isRegistered) {
      completion(Result.failure(NativeError.NotFound(ErrorMessages.NOT_FOUND)))
      return
    }

    biometricAuth.unlock(
      activity = activity,
      allowPasscode = config.allowDevicePasscode,
      promptText = promptMessage ?: config.biometricPromptText,
      promptOptions = promptOptions,
    ) { result ->
      result
        .onSuccess {
          markAuthenticationSuccess()
          try {
            val rawPayload = payload.toByteArray(Charsets.UTF_8)
            val signatureBytes = KeystoreHelper.sign(resolvedAlias, rawPayload)
            val signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
            completion(Result.success(signature))
          } catch (error: KeystoreHelper.KeystoreError) {
            val mapped =
              when (error) {
                is KeystoreHelper.KeystoreError.KeyNotFound -> NativeError.NotFound(ErrorMessages.NOT_FOUND)
                else -> NativeError.SecurityViolation(ErrorMessages.SECURITY_VIOLATION)
              }
            completion(Result.failure(mapped))
          } catch (error: Throwable) {
            completion(Result.failure(NativeError.InitFailed(ErrorMessages.INIT_FAILED)))
          }
        }.onFailure { error ->
          recordBiometricFailure(error)
          completion(Result.failure(error))
        }
    }
  }

  fun biometricKeysExist(keyAlias: String?): Boolean {
    val resolvedAlias = keyAlias ?: DEFAULT_BIOMETRIC_KEY_ALIAS
    return KeystoreHelper.hasKeyPair(resolvedAlias)
  }

  fun createKeys(keyAlias: String?): String {
    val resolvedAlias = keyAlias ?: DEFAULT_BIOMETRIC_KEY_ALIAS

    try {
      KeystoreHelper.deleteKeyPair(resolvedAlias)
    } catch (_: Throwable) {
    }

    return try {
      KeystoreHelper.getOrCreateKeyPair(
        alias = resolvedAlias,
        requireStrongBox = config.requireStrongBox,
        cryptoStrategy = config.cryptoStrategy,
        keySize = config.keySize,
      )
    } catch (_: KeystoreHelper.KeystoreError) {
      throw NativeError.InitFailed(ErrorMessages.INIT_FAILED)
    }
  }

  fun deleteKeys(keyAlias: String?) {
    val resolvedAlias = keyAlias ?: DEFAULT_BIOMETRIC_KEY_ALIAS
    try {
      KeystoreHelper.deleteKeyPair(resolvedAlias)
    } catch (_: Throwable) {
    }
  }

  fun registerWithChallenge(
    activity: FragmentActivity,
    challenge: String,
    keyAlias: String?,
    promptMessage: String?,
    promptOptions: BiometricAuth.PromptOptions?,
    completion: (Result<Pair<String, String>>) -> Unit,
  ) {
    try {
      assertNotLockedOut()
    } catch (error: Throwable) {
      completion(Result.failure(error))
      return
    }

    if (challenge.isEmpty()) {
      completion(Result.failure(NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)))
      return
    }

    val resolvedAlias = keyAlias ?: DEFAULT_BIOMETRIC_KEY_ALIAS
    val publicKey =
      try {
        createKeys(resolvedAlias)
      } catch (error: Throwable) {
        completion(Result.failure(error))
        return
      }

    biometricAuth.unlock(
      activity = activity,
      allowPasscode = config.allowDevicePasscode,
      promptText = promptMessage ?: config.biometricPromptText,
      promptOptions = promptOptions,
    ) { result ->
      result
        .onSuccess {
          markAuthenticationSuccess()
          try {
            val signatureBytes = KeystoreHelper.sign(resolvedAlias, challenge.toByteArray(Charsets.UTF_8))
            val signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
            completion(Result.success(Pair(publicKey, signature)))
          } catch (error: KeystoreHelper.KeystoreError) {
            val mapped =
              when (error) {
                is KeystoreHelper.KeystoreError.KeyNotFound -> NativeError.NotFound(ErrorMessages.NOT_FOUND)
                else -> NativeError.SecurityViolation(ErrorMessages.SECURITY_VIOLATION)
              }
            completion(Result.failure(mapped))
          } catch (_: Throwable) {
            completion(Result.failure(NativeError.InitFailed(ErrorMessages.INIT_FAILED)))
          }
        }.onFailure { error ->
          recordBiometricFailure(error)
          completion(Result.failure(error))
        }
    }
  }

  fun authenticateWithChallenge(
    activity: FragmentActivity,
    challenge: String,
    keyAlias: String?,
    promptMessage: String?,
    promptOptions: BiometricAuth.PromptOptions?,
    completion: (Result<String>) -> Unit,
  ) {
    try {
      assertNotLockedOut()
    } catch (error: Throwable) {
      completion(Result.failure(error))
      return
    }

    if (challenge.isEmpty()) {
      completion(Result.failure(NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)))
      return
    }

    val resolvedAlias = keyAlias ?: DEFAULT_BIOMETRIC_KEY_ALIAS
    if (!KeystoreHelper.hasKeyPair(resolvedAlias)) {
      completion(Result.failure(NativeError.NotFound(ErrorMessages.NOT_FOUND)))
      return
    }

    biometricAuth.unlock(
      activity = activity,
      allowPasscode = config.allowDevicePasscode,
      promptText = promptMessage ?: config.biometricPromptText,
      promptOptions = promptOptions,
    ) { result ->
      result
        .onSuccess {
          markAuthenticationSuccess()
          try {
            val signatureBytes = KeystoreHelper.sign(resolvedAlias, challenge.toByteArray(Charsets.UTF_8))
            val signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
            completion(Result.success(signature))
          } catch (error: KeystoreHelper.KeystoreError) {
            val mapped =
              when (error) {
                is KeystoreHelper.KeystoreError.KeyNotFound -> NativeError.NotFound(ErrorMessages.NOT_FOUND)
                else -> NativeError.SecurityViolation(ErrorMessages.SECURITY_VIOLATION)
              }
            completion(Result.failure(mapped))
          } catch (_: Throwable) {
            completion(Result.failure(NativeError.InitFailed(ErrorMessages.INIT_FAILED)))
          }
        }.onFailure { error ->
          recordBiometricFailure(error)
          completion(Result.failure(error))
        }
    }
  }

  fun generateChallengePayload(nonce: String): String {
    if (nonce.isEmpty()) {
      throw NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
    }

    val timestamp = System.currentTimeMillis()
    val deviceIdentifierHash = Utils.deviceIdentifierHash(context)

    // Optimization: Ensure strictly ordered keys and atomic string building
    // to prevent any mismatch with backend re-serialization.
    val payload =
      buildString {
        append("{")
        append("\"deviceIdentifierHash\":").append(Utils.jsonString(deviceIdentifierHash)).append(",")
        append("\"nonce\":").append(Utils.jsonString(nonce)).append(",")
        append("\"timestamp\":").append(timestamp)
        append("}")
      }

    return payload
  }

  fun setInsecureValue(
    key: String,
    value: String,
  ) {
    standardStorage.set(key = KeyUtils.obfuscate(key, config.obfuscationPrefix), value = value)
  }

  fun getInsecureValue(key: String): String? = standardStorage.get(KeyUtils.obfuscate(key, config.obfuscationPrefix))

  fun removeInsecureValue(key: String) {
    standardStorage.remove(KeyUtils.obfuscate(key, config.obfuscationPrefix))
  }

  fun getObfuscatedKey(key: String): String = KeyUtils.obfuscate(key, config.obfuscationPrefix)

  fun hasKey(
    key: String,
    secure: Boolean,
  ): Boolean =
    if (secure) {
      secureStorage.hasKey(key)
    } else {
      standardStorage.hasKey(KeyUtils.obfuscate(key, config.obfuscationPrefix))
    }

  fun setSessionLockCallback(callback: (Boolean) -> Unit) {
    sessionManager.onLockStatusChanged = callback
  }

  fun setSessionBackgroundTimestamp() {
    sessionManager.setBackgroundTimestamp()
  }

  fun evaluateSessionBackgroundGracePeriod(lockAfterMs: Long) {
    sessionManager.evaluateBackgroundGracePeriod(lockAfterMs)
  }

  fun setContentVisibility(
    activity: android.app.Activity?,
    visible: Boolean,
  ) {
    if (!isPrivacyScreenEnabled()) {
      return
    }
    privacyScreen.setContentVisibility(activity, visible)
  }

  fun setPrivacyProtection(
    activity: android.app.Activity?,
    enabled: Boolean,
  ) {
    if (!isPrivacyScreenEnabled()) {
      privacyScreen.unlock(activity)
      return
    }

    if (enabled) {
      privacyScreen.lock(activity)
    } else {
      privacyScreen.unlock(activity)
    }
  }

  fun checkBiometricStatus(context: Context): JSObject = biometricAuth.checkStatus(context)

  private data class SetManyOperation(
    val key: String,
    val value: String,
    val secure: Boolean,
  )

  private fun ensureSecureVaultAccessible() {
    val freshnessTimeout = config.requireFreshAuthenticationMs.toLong()
    if (freshnessTimeout > 0) {
      val now = System.currentTimeMillis()
      val stale = lastSuccessfulAuthAtMs <= 0 || now - lastSuccessfulAuthAtMs > freshnessTimeout
      if (stale) {
        sessionManager.lock()
        throw NativeError.VaultLocked(ErrorMessages.VAULT_LOCKED)
      }
    }

    val locked = sessionManager.evaluateLockState(config.lockAfterMs.toLong())
    if (locked) {
      throw NativeError.VaultLocked(ErrorMessages.VAULT_LOCKED)
    }
  }

  private fun isPrivacyScreenEnabled(): Boolean = config.enablePrivacyScreen

  private fun operationMarker(operation: SetManyOperation): String =
    if (operation.secure) {
      "secure:${operation.key}"
    } else {
      "insecure:${operation.key}"
    }

  private fun readOperationValue(operation: SetManyOperation): String? =
    if (operation.secure) {
      secureStorage.get(operation.key)
    } else {
      standardStorage.get(KeyUtils.obfuscate(operation.key, config.obfuscationPrefix))
    }

  private fun rollbackSetMany(snapshot: Map<String, String?>) {
    snapshot.forEach { (marker, previousValue) ->
      val secure = marker.startsWith("secure:")
      val key = marker.substringAfter(':')

      try {
        if (secure) {
          if (previousValue == null) {
            secureStorage.remove(key)
          } else {
            secureStorage.set(key, previousValue, requireStrongBox = config.requireStrongBox)
          }
        } else {
          val storageKey = KeyUtils.obfuscate(key, config.obfuscationPrefix)
          if (previousValue == null) {
            standardStorage.remove(storageKey)
          } else {
            standardStorage.set(storageKey, previousValue)
          }
        }
      } catch (_: Throwable) {
      }
    }
  }
}
