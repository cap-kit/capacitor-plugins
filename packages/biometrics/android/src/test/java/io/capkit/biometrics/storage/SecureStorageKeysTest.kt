package io.capkit.biometrics.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM unit tests for the pure key derivation helpers of the secure store.
 */
class SecureStorageKeysTest {
  @Test
  fun keystoreAliasUsesDedicatedPrefix() {
    assertEquals(
      "BiometricsSecureKey_session.token",
      SecureStorageKeys.keystoreAlias("session.token"),
    )
  }

  @Test
  fun prefsEntryUsesSecurePrefix() {
    assertEquals(
      "secure_session.token",
      SecureStorageKeys.prefsEntry("session.token"),
    )
  }

  @Test
  fun keystoreAliasAndPrefsEntryDifferPerKey() {
    val firstAlias = SecureStorageKeys.keystoreAlias("a")
    val secondAlias = SecureStorageKeys.keystoreAlias("b")
    assertFalse(firstAlias == secondAlias)
  }

  @Test
  fun accessControlResolvesBiometryAny() {
    assertEquals(
      SecureAccessControl.BIOMETRY_ANY,
      SecureAccessControl.fromRaw("biometryAny"),
    )
  }

  @Test
  fun accessControlResolvesBiometryCurrentSet() {
    assertEquals(
      SecureAccessControl.BIOMETRY_CURRENT_SET,
      SecureAccessControl.fromRaw("biometryCurrentSet"),
    )
  }

  @Test
  fun accessControlReturnsNullForUnknownRaw() {
    assertNull(SecureAccessControl.fromRaw("deviceCredential"))
    assertNull(SecureAccessControl.fromRaw(null))
  }

  @Test
  fun biometryAnySurvivesEnrollmentChanges() {
    assertEquals(false, SecureAccessControl.BIOMETRY_ANY.invalidatedByEnrollment)
  }

  @Test
  fun biometryCurrentSetInvalidatesOnEnrollmentChanges() {
    assertEquals(true, SecureAccessControl.BIOMETRY_CURRENT_SET.invalidatedByEnrollment)
  }
}
