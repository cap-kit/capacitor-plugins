package io.capkit.biometrics.storage

import androidx.biometric.BiometricPrompt
import io.capkit.biometrics.error.BiometricsError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the BiometricPrompt-to-native-error mapping of the
 * secure store.
 *
 * The mapping mirrors `BiometricsImpl.mapPromptError`: user dismissals must
 * surface as CANCELLED, missing hardware/enrollment as UNAVAILABLE and
 * lockouts/failures as INIT_FAILED once the bridge maps the sealed error.
 *
 * NOTE: BiometricPrompt.ERROR_* values are compile-time constants, so they
 * are inlined and the JVM test never instantiates Android classes.
 */
class SecurePromptErrorsTest {
  @Test
  fun userCancelledMapsToCancelled() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_USER_CANCELED, "any")
    assertTrue(error is BiometricsError.Cancelled)
  }

  @Test
  fun negativeButtonMapsToCancelled() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_NEGATIVE_BUTTON, "any")
    assertTrue(error is BiometricsError.Cancelled)
  }

  @Test
  fun systemCanceledMapsToCancelled() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_CANCELED, "any")
    assertTrue(error is BiometricsError.Cancelled)
  }

  @Test
  fun lockoutMapsToInitFailed() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_LOCKOUT, "any")
    assertTrue(error is BiometricsError.InitFailed)
    assertEquals(
      "Biometric authentication temporarily locked out.",
      error.message,
    )
  }

  @Test
  fun permanentLockoutMapsToInitFailed() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_LOCKOUT_PERMANENT, "any")
    assertTrue(error is BiometricsError.InitFailed)
  }

  @Test
  fun promptTimeoutMapsToInitFailed() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_TIMEOUT, "any")
    assertTrue(error is BiometricsError.InitFailed)
  }

  @Test
  fun noBiometricsEnrolledMapsToUnavailable() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_NO_BIOMETRICS, "any")
    assertTrue(error is BiometricsError.Unavailable)
  }

  @Test
  fun missingHardwareMapsToUnavailable() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_HW_NOT_PRESENT, "any")
    assertTrue(error is BiometricsError.Unavailable)
  }

  @Test
  fun hardwareUnavailableMapsToUnavailable() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_HW_UNAVAILABLE, "any")
    assertTrue(error is BiometricsError.Unavailable)
  }

  @Test
  fun missingDeviceCredentialMapsToUnavailable() {
    val error = SecurePromptErrors.map(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL, "any")
    assertTrue(error is BiometricsError.Unavailable)
  }

  @Test
  fun unknownErrorCodeFallsBackToInitFailedWithSystemMessage() {
    val error = SecurePromptErrors.map(999, "Vendor specific failure")
    assertTrue(error is BiometricsError.InitFailed)
    assertEquals("Vendor specific failure", error.message)
  }

  @Test
  fun unknownErrorCodeWithBlankMessageUsesFallbackMessage() {
    val error = SecurePromptErrors.map(999, "")
    assertTrue(error is BiometricsError.InitFailed)
    assertEquals("Biometric authentication failed.", error.message)
  }
}
