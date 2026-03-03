package io.capkit.fortress.impl

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.capkit.fortress.error.ErrorMessages
import io.capkit.fortress.error.NativeError
import java.util.concurrent.atomic.AtomicBoolean

class BiometricAuth(
  private val context: Context,
) {
  fun unlock(
    activity: FragmentActivity,
    allowPasscode: Boolean,
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
        .setTitle("Authenticate")
        .setSubtitle("Access your secure vault")
        .setAllowedAuthenticators(authenticators)

    if (!allowPasscode) {
      promptBuilder.setNegativeButtonText("Cancel")
    }

    biometricPrompt.authenticate(promptBuilder.build())
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
      BiometricPrompt.ERROR_USER_CANCELED,
      BiometricPrompt.ERROR_NEGATIVE_BUTTON,
      BiometricPrompt.ERROR_CANCELED,
      BiometricPrompt.ERROR_TIMEOUT,
      ->
        NativeError.Cancelled(ErrorMessages.CANCELLED)
      BiometricPrompt.ERROR_LOCKOUT,
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
      ->
        NativeError.Unavailable(ErrorMessages.UNAVAILABLE)
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
