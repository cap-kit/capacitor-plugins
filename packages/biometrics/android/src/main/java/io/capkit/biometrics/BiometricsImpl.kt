package io.capkit.biometrics

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.capkit.biometrics.config.BiometricsConfig
import io.capkit.biometrics.logger.BiometricsLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Platform-specific native implementation for the Biometrics plugin.
 *
 * Pure Android logic extracted from the Fortress biometric engine and
 * adapted to the standalone contract. MUST NOT depend on Capacitor bridge
 * APIs or PluginCall objects.
 */
class BiometricsImpl(
  private val context: Context,
) {
  data class PromptOptions(
    val title: String?,
    val subtitle: String?,
    val description: String?,
    val negativeButtonText: String?,
    val confirmationRequired: Boolean?,
  )

  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: BiometricsConfig

  @Volatile private var activePrompt: BiometricPrompt? = null

  /**
   * Credential class of the last successful ceremony in this process
   * (`biometric`, `deviceCredential`, `unknown`).
   */
  @Volatile var lastAuthenticationType: String = "unknown"
    private set

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * MUST be called exactly once during the plugin load phase.
   */
  fun updateConfig(newConfig: BiometricsConfig) {
    this.config = newConfig
    BiometricsLogger.verbose = newConfig.verboseLogging
    BiometricsLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  /**
   * Runs the system biometric ceremony.
   *
   * @param activity Host fragment activity.
   * @param allowPasscode Whether device credentials join the authenticator set.
   * @param promptOptions Dialog customization.
   * @param completion Result callback, invoked exactly once.
   */
  fun authenticate(
    activity: FragmentActivity,
    allowPasscode: Boolean,
    promptOptions: PromptOptions?,
    completion: (Result<Unit>) -> Unit,
  ) {
    val authenticators =
      if (allowPasscode) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
          BiometricManager.Authenticators.DEVICE_CREDENTIAL
      } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
      }

    val canAuthenticateResult = BiometricManager.from(context).canAuthenticate(authenticators)
    if (canAuthenticateResult != BiometricManager.BIOMETRIC_SUCCESS) {
      completion(Result.failure(mapAvailabilityError(canAuthenticateResult)))
      return
    }

    val completed = AtomicBoolean(false)
    val executor = ContextCompat.getMainExecutor(activity)

    val biometricPrompt =
      BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            activePrompt = null
            lastAuthenticationType = mapAuthenticationType(result)
            if (completed.compareAndSet(false, true)) {
              completion(Result.success(Unit))
            }
          }

          override fun onAuthenticationError(
            errorCode: Int,
            errString: CharSequence,
          ) {
            activePrompt = null
            if (completed.compareAndSet(false, true)) {
              completion(Result.failure(mapPromptError(errorCode, errString.toString())))
            }
          }
        },
      )
    activePrompt = biometricPrompt

    val promptBuilder =
      BiometricPrompt
        .PromptInfo
        .Builder()
        .setTitle(promptOptions?.title ?: "Authenticate")
        .setSubtitle(promptOptions?.subtitle)
        .setDescription(promptOptions?.description)
        .setAllowedAuthenticators(authenticators)

    if (promptOptions?.confirmationRequired != null) {
      promptBuilder.setConfirmationRequired(promptOptions.confirmationRequired)
    }

    // DEVICE_CREDENTIAL and a negative button are mutually exclusive.
    if (!allowPasscode) {
      promptBuilder.setNegativeButtonText(promptOptions?.negativeButtonText ?: "Cancel")
    }

    val handler = Handler(Looper.getMainLooper())
    handler.post {
      biometricPrompt.authenticate(promptBuilder.build())
    }
  }

  /**
   * Dismisses the in-flight prompt, if any. The pending completion fires
   * with a cancellation error; safe to call when idle.
   */
  fun cancelActiveAuthentication() {
    activePrompt?.cancelAuthentication()
    activePrompt = null
  }

  // ---------------------------------------------------------------------------
  // Capability status
  // ---------------------------------------------------------------------------

  data class CapabilityStatus(
    val isAvailable: Boolean,
    val isEnrolled: Boolean,
    val isDeviceSecure: Boolean,
    val biometryType: String,
    val biometryTypes: List<String>,
    val strongBiometryIsAvailable: Boolean,
  )

  fun capabilityStatus(context: Context): CapabilityStatus {
    val biometricManager = BiometricManager.from(context)
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    val strongResult =
      biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    val available =
      strongResult == BiometricManager.BIOMETRIC_SUCCESS ||
        strongResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    val enrolled = strongResult == BiometricManager.BIOMETRIC_SUCCESS
    val types = biometryTypes(context)

    return CapabilityStatus(
      isAvailable = available,
      isEnrolled = enrolled,
      isDeviceSecure = keyguardManager.isDeviceSecure,
      biometryType = if (!available || types.isEmpty()) "none" else types.first(),
      biometryTypes = if (!available) emptyList() else types,
      strongBiometryIsAvailable =
        biometricManager.canAuthenticate(
          BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS,
    )
  }

  private fun biometryTypes(context: Context): List<String> {
    val packageManager = context.packageManager
    val types = mutableListOf<String>()
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
      types.add("fingerprint")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
      packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
    ) {
      types.add("face")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
      packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)
    ) {
      types.add("iris")
    }
    return types
  }

  /**
   * Opens the system biometric enrollment screen (API 30+).
   *
   * @return false when the platform cannot enroll (caller rejects UNAVAILABLE).
   */
  fun enroll(activity: android.app.Activity?): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || activity == null) {
      return false
    }
    return try {
      val intent =
        Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
          putExtra(
            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
              BiometricManager.Authenticators.BIOMETRIC_WEAK,
          )
        }
      activity.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun mapAuthenticationType(result: BiometricPrompt.AuthenticationResult): String =
    try {
      when (result.authenticationType) {
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "deviceCredential"
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "biometric"
        else -> "unknown"
      }
    } catch (_: Exception) {
      "unknown"
    }

  private fun mapAvailabilityError(code: Int): Throwable =
    when (code) {
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
      BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
      BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
      -> IllegalStateException("Biometrics unavailable on this device.")
      else -> IllegalStateException("Biometric check failed.")
    }

  private fun mapPromptError(
    errorCode: Int,
    errString: String,
  ): Throwable =
    when (errorCode) {
      BiometricPrompt.ERROR_USER_CANCELED,
      BiometricPrompt.ERROR_NEGATIVE_BUTTON,
      BiometricPrompt.ERROR_CANCELED,
      -> IllegalStateException("Biometric authentication was cancelled.")
      BiometricPrompt.ERROR_LOCKOUT,
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
      BiometricPrompt.ERROR_TIMEOUT,
      -> IllegalStateException("Biometric authentication temporarily locked out.")
      BiometricPrompt.ERROR_NO_BIOMETRICS,
      BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
      BiometricPrompt.ERROR_HW_NOT_PRESENT,
      BiometricPrompt.ERROR_HW_UNAVAILABLE,
      -> IllegalStateException("Biometrics unavailable on this device.")
      else ->
        IllegalStateException(errString.ifBlank { "Biometric authentication failed." })
    }
}
