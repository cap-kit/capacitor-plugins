package io.capkit.biometrics.storage

import androidx.biometric.BiometricPrompt
import io.capkit.biometrics.error.BiometricsError

/**
 * Maps AndroidX BiometricPrompt error codes to the plugin's native error model.
 *
 * The mapping mirrors `BiometricsImpl.mapPromptError` so prompt failures in
 * the storage flows surface exactly like the standalone authenticate flow:
 * user dismissals map to Cancelled, missing hardware/enrollment to Unavailable
 * and lockouts/failures to InitFailed. The bridge layer then maps the sealed
 * error to the stable JS code.
 *
 * MUST NOT depend on Capacitor APIs or Android framework state beyond the
 * androidx error-code constants.
 */
internal object SecurePromptErrors {
  fun map(
    errorCode: Int,
    errString: String,
  ): BiometricsError =
    when (errorCode) {
      BiometricPrompt.ERROR_USER_CANCELED,
      BiometricPrompt.ERROR_NEGATIVE_BUTTON,
      BiometricPrompt.ERROR_CANCELED,
      -> BiometricsError.Cancelled("Biometric authentication was cancelled.")
      BiometricPrompt.ERROR_LOCKOUT,
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
      BiometricPrompt.ERROR_TIMEOUT,
      -> BiometricsError.InitFailed("Biometric authentication temporarily locked out.")
      BiometricPrompt.ERROR_NO_BIOMETRICS,
      BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
      BiometricPrompt.ERROR_HW_NOT_PRESENT,
      BiometricPrompt.ERROR_HW_UNAVAILABLE,
      -> BiometricsError.Unavailable("Biometrics unavailable on this device.")
      else -> BiometricsError.InitFailed(errString.ifBlank { "Biometric authentication failed." })
    }
}
