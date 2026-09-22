package io.capkit.fortress.impl

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.getcapacitor.JSObject
import io.capkit.fortress.error.ErrorMessages
import io.capkit.fortress.error.NativeError
import java.util.concurrent.atomic.AtomicBoolean

class BiometricAuth(
  private val context: Context,
) {
  data class PromptOptions(
    val title: String?,
    val subtitle: String?,
    val description: String?,
    val negativeButtonText: String?,
    val confirmationRequired: Boolean?,
  )

  /**
   * Triggers the biometric prompt using configuration-driven text and settings.
   *
   */
  fun unlock(
    activity: FragmentActivity,
    allowPasscode: Boolean,
    promptText: String, // Added parameter from Config
    promptOptions: PromptOptions?,
    completion: (Result<Unit>) -> Unit,
  ) {
    val authenticators =
      if (allowPasscode) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
      } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
      }

    val canAuthenticateResult = BiometricManager.from(context).canAuthenticate(authenticators)
    if (canAuthenticateResult != BiometricManager.BIOMETRIC_SUCCESS) {
      completion(Result.failure(mapCanAuthenticateError(canAuthenticateResult)))
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
            if (completed.compareAndSet(false, true)) {
              completion(Result.success(Unit))
            }
          }

          override fun onAuthenticationError(
            errorCode: Int,
            errString: CharSequence,
          ) {
            if (completed.compareAndSet(false, true)) {
              completion(Result.failure(mapPromptError(errorCode, errString.toString())))
            }
          }
        },
      )

    val promptBuilder =
      BiometricPrompt
        .PromptInfo
        .Builder()
        .setTitle(promptOptions?.title ?: "Authenticate")
        .setSubtitle(promptOptions?.subtitle ?: "Access your secure vault")
        .setDescription(promptOptions?.description)
        .setAllowedAuthenticators(authenticators)

    if (promptOptions?.confirmationRequired != null) {
      promptBuilder.setConfirmationRequired(promptOptions.confirmationRequired)
    }

    /**
     * Android Constraint: setNegativeButtonText MUST NOT be called if
     * DEVICE_CREDENTIAL is included in authenticators.
     */
    if (!allowPasscode) {
      promptBuilder.setNegativeButtonText(promptOptions?.negativeButtonText ?: promptText)
    }

    val handler = Handler(Looper.getMainLooper())
    handler.post {
      biometricPrompt.authenticate(promptBuilder.build())
    }
  }

  fun checkStatus(context: Context): JSObject {
    val biometricManager = BiometricManager.from(context)
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    val strongAuthResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

    val isBiometricsAvailable =
      strongAuthResult == BiometricManager.BIOMETRIC_SUCCESS ||
        strongAuthResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

    val isBiometricsEnabled = strongAuthResult == BiometricManager.BIOMETRIC_SUCCESS

    val biometryType =
      if (!isBiometricsAvailable) {
        "none"
      } else {
        resolveBiometryType(context)
      }

    val status = JSObject()
    status.put("isDeviceSecure", keyguardManager.isDeviceSecure)
    status.put("isBiometricsAvailable", isBiometricsAvailable)
    status.put("isBiometricsEnabled", isBiometricsEnabled)
    status.put("biometryType", biometryType)
    return status
  }

  private fun resolveBiometryType(context: Context): String {
    val packageManager = context.packageManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
      packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
    ) {
      return "faceId"
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
      packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)
    ) {
      return "iris"
    }
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
      return "fingerprint"
    }
    return "none"
  }

  private fun mapCanAuthenticateError(code: Int): NativeError =
    when (code) {
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
      BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
      BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
      BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
        NativeError.InitFailed(ErrorMessages.INIT_FAILED)
      else ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
    }

  private fun mapPromptError(
    errorCode: Int,
    errString: String,
  ): NativeError =
    when (errorCode) {
      BiometricPrompt.ERROR_TIMEOUT ->
        NativeError.Timeout(ErrorMessages.TIMEOUT)

      BiometricPrompt.ERROR_USER_CANCELED,
      BiometricPrompt.ERROR_NEGATIVE_BUTTON,
      BiometricPrompt.ERROR_CANCELED,
      ->
        NativeError.Cancelled(ErrorMessages.CANCELLED)
      BiometricPrompt.ERROR_LOCKOUT,
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
      ->
        // Lockout raised to security violation to distinguish from missing hardware
        NativeError.SecurityViolation(ErrorMessages.SECURITY_VIOLATION)
      BiometricPrompt.ERROR_NO_BIOMETRICS,
      BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
      BiometricPrompt.ERROR_HW_NOT_PRESENT,
      BiometricPrompt.ERROR_HW_UNAVAILABLE,
      ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
      else ->
        NativeError.InitFailed(errString.ifBlank { ErrorMessages.INIT_FAILED })
    }
}
