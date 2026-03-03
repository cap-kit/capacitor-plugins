package io.capkit.fortress

import android.content.Context
import androidx.fragment.app.FragmentActivity
import io.capkit.fortress.config.Config
import io.capkit.fortress.impl.BiometricAuth
import io.capkit.fortress.impl.PrivacyScreen
import io.capkit.fortress.impl.SecureStorage
import io.capkit.fortress.impl.SessionManager
import io.capkit.fortress.impl.SessionState
import io.capkit.fortress.impl.StandardStorage
import io.capkit.fortress.logger.Logger
import io.capkit.fortress.utils.KeyUtils

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

  /**
   * Applies static plugin configuration.
   */
  fun updateConfig(newConfig: Config) {
    this.config = newConfig
    Logger.verbose = newConfig.verboseLogging
    Logger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  fun configure(config: Config) {
    updateConfig(config)
  }

  fun setValue(
    key: String,
    value: String,
  ) {
    secureStorage.set(key, value)
  }

  fun getValue(key: String): String? = secureStorage.get(key)

  fun removeValue(key: String) {
    secureStorage.remove(key)
  }

  fun clearAll() {
    secureStorage.clearAll()
  }

  fun unlock(
    activity: FragmentActivity,
    completion: (Result<Unit>) -> Unit,
  ) {
    biometricAuth.unlock(activity = activity, allowPasscode = true) { result ->
      result
        .onSuccess {
          privacyScreen.unlock()
          completion(Result.success(Unit))
        }.onFailure { error ->
          completion(Result.failure(error))
        }
    }
  }

  fun lock() {
    privacyScreen.lock()
  }

  fun isLocked(): Boolean = sessionManager.getSession().isLocked

  fun getSession(): SessionState = sessionManager.getSession()

  fun resetSession() {
    sessionManager.getSession()
  }

  fun touchSession() {
    sessionManager.getSession()
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
}
