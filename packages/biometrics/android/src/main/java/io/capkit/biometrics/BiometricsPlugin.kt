package io.capkit.biometrics

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.biometrics.config.BiometricsConfig
import io.capkit.biometrics.error.BiometricsError
import io.capkit.biometrics.error.BiometricsErrorMessages
import io.capkit.biometrics.logger.BiometricsLogger
import io.capkit.biometrics.model.AuthenticationTypeResult
import io.capkit.biometrics.model.BiometricStrengthResult
import io.capkit.biometrics.model.BiometricTypeResult
import io.capkit.biometrics.model.BiometricTypesResult
import io.capkit.biometrics.model.BiometricsVersionResult
import io.capkit.biometrics.model.HasDeviceCredentialResult
import io.capkit.biometrics.model.IsAvailableResult
import io.capkit.biometrics.model.IsDeviceSecureResult
import io.capkit.biometrics.model.IsEnrolledResult
import io.capkit.biometrics.model.IsSecureDataSavedResult
import io.capkit.biometrics.model.SecureDataResult
import io.capkit.biometrics.storage.GetSecureDataOptions
import io.capkit.biometrics.storage.SecureStorageImpl
import io.capkit.biometrics.storage.SetSecureDataOptions
import io.capkit.biometrics.utils.BiometricsValidators
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Capacitor bridge for the Biometrics plugin.
 *
 * CONTRACT:
 * - This class is the ONLY entry point from JavaScript.
 * - All PluginCall instances MUST be resolved or rejected exactly once.
 */
@CapacitorPlugin(name = "Biometrics")
@Suppress("unused")
class BiometricsPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration.
   *
   * CONTRACT:
   * - Initialized exactly once in `load()`
   * - Treated as read-only afterwards
   */
  private lateinit var config: BiometricsConfig

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - MUST NOT access PluginCall or Capacitor APIs
   */
  private lateinit var implementation: BiometricsImpl

  /**
   * Biometric-bound secure store implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - MUST NOT access PluginCall or Capacitor APIs
   */
  private lateinit var secureStorage: SecureStorageImpl

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

    config = BiometricsConfig(this)
    implementation = BiometricsImpl(context)
    implementation.updateConfig(config)
    secureStorage = SecureStorageImpl(context)

    BiometricsLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   */
  private fun reject(
    call: PluginCall,
    error: BiometricsError,
  ) {
    val code =
      when (error) {
        is BiometricsError.Unavailable -> "UNAVAILABLE"
        is BiometricsError.Cancelled -> "CANCELLED"
        is BiometricsError.PermissionDenied -> "PERMISSION_DENIED"
        is BiometricsError.InitFailed -> "INIT_FAILED"
        is BiometricsError.InvalidInput -> "INVALID_INPUT"
        is BiometricsError.UnknownType -> "UNKNOWN_TYPE"
        is BiometricsError.NotFound -> "NOT_FOUND"
        is BiometricsError.Conflict -> "CONFLICT"
        is BiometricsError.Timeout -> "TIMEOUT"
      }

    val message = error.message ?: BiometricsErrorMessages.INTERNAL_ERROR
    call.reject(message, code)
  }

  /**
   * Maps implementation throwables to bridge rejections.
   *
   * The standalone contract surfaces only two JS error codes: user cancellation
   * and unavailability; everything unexpected becomes INIT_FAILED.
   */
  private fun handleError(
    call: PluginCall,
    throwable: Throwable,
  ) {
    if (throwable is BiometricsError) {
      reject(call, throwable)
      return
    }
    val message = throwable.message ?: BiometricsErrorMessages.UNEXPECTED_NATIVE_ERROR
    if (message.contains("cancelled", ignoreCase = true)) {
      reject(call, BiometricsError.Cancelled(message))
    } else if (message.contains("unavailable", ignoreCase = true)) {
      reject(call, BiometricsError.Unavailable(message))
    } else {
      reject(call, BiometricsError.InitFailed(message))
    }
  }

  private fun parsePromptOptions(call: PluginCall): BiometricsImpl.PromptOptions =
    BiometricsImpl.PromptOptions(
      title = call.getString("title"),
      subtitle = call.getString("subtitle"),
      description = call.getString("description") ?: call.getString("reason"),
      negativeButtonText = call.getString("cancelButtonText"),
      confirmationRequired = null,
    )

  // ---------------------------------------------------------------------------
  // Biometrics
  // ---------------------------------------------------------------------------

  /**
   * Authenticates the user with biometrics or device credentials.
   */
  @PluginMethod
  fun authenticate(call: PluginCall) {
    val hostActivity = currentHostActivity() as? androidx.fragment.app.FragmentActivity
    if (hostActivity == null) {
      reject(call, BiometricsError.Unavailable(BiometricsErrorMessages.UNAVAILABLE))
      return
    }

    val allowPasscode = call.data.optBoolean("allowDeviceCredential", true)
    val promptOptions = parsePromptOptions(call)

    implementation.authenticate(hostActivity, allowPasscode, promptOptions) { result ->
      result
        .onSuccess {
          call.resolve()
        }.onFailure { error ->
          handleError(call, error)
        }
    }
  }

  /**
   * Cancels an ongoing authentication prompt, if any.
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
   * Reports whether biometric authentication can be used.
   */
  @PluginMethod
  fun isAvailable(call: PluginCall) {
    try {
      val status = implementation.capabilityStatus(context)
      call.resolve(toJSObject(IsAvailableResult(isAvailable = status.isAvailable)))
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
      val status = implementation.capabilityStatus(context)
      call.resolve(toJSObject(IsEnrolledResult(isEnrolled = status.isEnrolled)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns the primary biometry modality.
   */
  @PluginMethod
  fun getBiometricType(call: PluginCall) {
    try {
      val status = implementation.capabilityStatus(context)
      call.resolve(toJSObject(BiometricTypeResult(biometryType = status.biometryType)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Returns every biometry modality known to the hardware.
   */
  @PluginMethod
  fun getBiometricTypes(call: PluginCall) {
    try {
      val status = implementation.capabilityStatus(context)
      call.resolve(toJSObject(BiometricTypesResult(biometryTypes = status.biometryTypes)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports whether a device credential is set.
   */
  @PluginMethod
  fun hasDeviceCredential(call: PluginCall) {
    try {
      val status = implementation.capabilityStatus(context)
      call.resolve(
        toJSObject(HasDeviceCredentialResult(hasDeviceCredential = status.isDeviceSecure)),
      )
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports whether the device is secured by a lock-screen credential.
   */
  @PluginMethod
  fun isDeviceSecure(call: PluginCall) {
    try {
      val status = implementation.capabilityStatus(context)
      call.resolve(
        toJSObject(IsDeviceSecureResult(isDeviceSecure = status.isDeviceSecure)),
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
      val status = implementation.capabilityStatus(context)
      val level =
        if (status.strongBiometryIsAvailable) {
          "strong"
        } else if (status.isAvailable) {
          "weak"
        } else {
          "none"
        }
      call.resolve(toJSObject(BiometricStrengthResult(strengthLevel = level)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports which credential satisfied the last successful ceremony.
   */
  @PluginMethod
  fun getAuthenticationType(call: PluginCall) {
    try {
      call.resolve(
        toJSObject(AuthenticationTypeResult(authenticationType = implementation.lastAuthenticationType)),
      )
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Opens the system biometric enrollment screen (API 30+).
   */
  @PluginMethod
  fun enroll(call: PluginCall) {
    try {
      if (implementation.enroll(currentHostActivity())) {
        call.resolve()
      } else {
        reject(call, BiometricsError.Unavailable(BiometricsErrorMessages.UNAVAILABLE))
      }
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  // ---------------------------------------------------------------------------
  // Secure Store
  // ---------------------------------------------------------------------------

  /**
   * Stores a string value encrypted under a biometry-bound key, protecting
   * the write with a biometric prompt carrying the CryptoObject.
   */
  @PluginMethod
  fun setSecureData(call: PluginCall) {
    val hostActivity = currentHostActivity() as? androidx.fragment.app.FragmentActivity
    if (hostActivity == null) {
      reject(call, BiometricsError.Unavailable(BiometricsErrorMessages.UNAVAILABLE))
      return
    }

    try {
      val options =
        SetSecureDataOptions(
          key = BiometricsValidators.requireSecureStoreKey(call.getString("key")),
          value = BiometricsValidators.requireSecureStoreValue(call.getString("value")),
          accessControl = BiometricsValidators.parseAccessControl(call.getString("accessControl")),
          title = call.getString("title"),
          negativeButtonText = call.getString("negativeButtonText"),
          maxAttempts = BiometricsValidators.parseMaxAttempts(call.getInt("maxAttempts")),
        )

      secureStorage.setSecureData(hostActivity, options) { result ->
        result
          .onSuccess {
            call.resolve()
          }.onFailure { error ->
            handleError(call, error)
          }
      }
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reads a biometry-protected value behind a biometric prompt carrying the
   * CryptoObject. Missing or unrecoverable data rejects with NOT_FOUND.
   */
  @PluginMethod
  fun getSecureData(call: PluginCall) {
    val hostActivity = currentHostActivity() as? androidx.fragment.app.FragmentActivity
    if (hostActivity == null) {
      reject(call, BiometricsError.Unavailable(BiometricsErrorMessages.UNAVAILABLE))
      return
    }

    try {
      val options =
        GetSecureDataOptions(
          key = BiometricsValidators.requireSecureStoreKey(call.getString("key")),
          title = call.getString("title"),
          subtitle = call.getString("subtitle"),
          // `reason` is the iOS-only prompt text; reuse it as description
          // fallback, mirroring the existing authenticate option handling.
          description = call.getString("description") ?: call.getString("reason"),
          negativeButtonText = call.getString("negativeButtonText"),
          maxAttempts = BiometricsValidators.parseMaxAttempts(call.getInt("maxAttempts")),
        )

      secureStorage.getSecureData(hostActivity, options) { result ->
        result
          .onSuccess { value ->
            call.resolve(toJSObject(SecureDataResult(value)))
          }.onFailure { error ->
            handleError(call, error)
          }
      }
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Deletes the value stored for the key. Never prompts.
   */
  @PluginMethod
  fun deleteSecureData(call: PluginCall) {
    try {
      val key = BiometricsValidators.requireSecureStoreKey(call.getString("key"))
      secureStorage.deleteSecureData(key)
      call.resolve()
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }

  /**
   * Reports whether a value exists for the key, without prompting.
   */
  @PluginMethod
  fun isSecureDataSaved(call: PluginCall) {
    try {
      val key = BiometricsValidators.requireSecureStoreKey(call.getString("key"))
      call.resolve(toJSObject(IsSecureDataSavedResult(isSaved = secureStorage.isSecureDataSaved(key))))
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
      call.resolve(toJSObject(BiometricsVersionResult(BuildConfig.PLUGIN_VERSION)))
    } catch (error: Throwable) {
      handleError(call, error)
    }
  }
}
