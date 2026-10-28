package io.capkit.biometrics.utils

import io.capkit.biometrics.error.BiometricsError
import io.capkit.biometrics.storage.SecureAccessControl
import io.capkit.biometrics.storage.SecureStorageKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * JVM unit tests for the secure store validation helpers.
 *
 * The helpers are pure: they must run without Android framework state and
 * signal every rejection through [BiometricsError.InvalidInput].
 */
class BiometricsValidatorsTest {
  // ---------------------------------------------------------------------------
  // requireSecureStoreKey
  // ---------------------------------------------------------------------------

  @Test
  fun requireSecureStoreKeyReturnsValidKeyUnchanged() {
    val key = "session.token"
    assertEquals(key, BiometricsValidators.requireSecureStoreKey(key))
  }

  @Test
  fun requireSecureStoreKeyRejectsNull() {
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.requireSecureStoreKey(null)
    }
  }

  @Test
  fun requireSecureStoreKeyRejectsEmpty() {
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.requireSecureStoreKey("")
    }
  }

  @Test
  fun requireSecureStoreKeyRejectsBlank() {
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.requireSecureStoreKey("   ")
    }
  }

  @Test
  fun requireSecureStoreKeyRejectsOverlongKey() {
    val key = "k".repeat(SecureStorageKeys.MAX_KEY_LENGTH + 1)
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.requireSecureStoreKey(key)
    }
  }

  @Test
  fun requireSecureStoreKeyAcceptsKeyAtLengthLimit() {
    val key = "k".repeat(SecureStorageKeys.MAX_KEY_LENGTH)
    assertEquals(key, BiometricsValidators.requireSecureStoreKey(key))
  }

  // ---------------------------------------------------------------------------
  // requireSecureStoreValue
  // ---------------------------------------------------------------------------

  @Test
  fun requireSecureStoreValueReturnsValueUnchanged() {
    val value = "{\"token\":\"abc\"}"
    assertEquals(value, BiometricsValidators.requireSecureStoreValue(value))
  }

  @Test
  fun requireSecureStoreValueAllowsEmptyPayload() {
    assertEquals("", BiometricsValidators.requireSecureStoreValue(""))
  }

  @Test
  fun requireSecureStoreValueRejectsNull() {
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.requireSecureStoreValue(null)
    }
  }

  // ---------------------------------------------------------------------------
  // parseAccessControl
  // ---------------------------------------------------------------------------

  @Test
  fun parseAccessControlResolvesBiometryAny() {
    assertEquals(
      SecureAccessControl.BIOMETRY_ANY,
      BiometricsValidators.parseAccessControl("biometryAny"),
    )
  }

  @Test
  fun parseAccessControlResolvesBiometryCurrentSet() {
    assertEquals(
      SecureAccessControl.BIOMETRY_CURRENT_SET,
      BiometricsValidators.parseAccessControl("biometryCurrentSet"),
    )
  }

  @Test
  fun parseAccessControlRejectsNull() {
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.parseAccessControl(null)
    }
  }

  @Test
  fun parseAccessControlRejectsUnknownValue() {
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.parseAccessControl("biometry")
    }
  }

  @Test
  fun parseAccessControlIsCaseSensitive() {
    assertThrows(BiometricsError.InvalidInput::class.java) {
      BiometricsValidators.parseAccessControl("BiometryAny")
    }
  }

  // ---------------------------------------------------------------------------
  // parseMaxAttempts
  // ---------------------------------------------------------------------------

  @Test
  fun parseMaxAttemptsDefaultsToSingleAttempt() {
    assertEquals(1, BiometricsValidators.parseMaxAttempts(null))
  }

  @Test
  fun parseMaxAttemptsClampsNonPositiveValuesToOne() {
    assertEquals(1, BiometricsValidators.parseMaxAttempts(0))
    assertEquals(1, BiometricsValidators.parseMaxAttempts(-3))
  }

  @Test
  fun parseMaxAttemptsKeepsInBoundValue() {
    assertEquals(3, BiometricsValidators.parseMaxAttempts(3))
  }

  @Test
  fun parseMaxAttemptsClampsToDeviceMaximum() {
    assertEquals(5, BiometricsValidators.parseMaxAttempts(99))
  }

  @Test
  fun parseMaxAttemptsAcceptsDeviceMaximum() {
    assertEquals(5, BiometricsValidators.parseMaxAttempts(5))
  }
}
