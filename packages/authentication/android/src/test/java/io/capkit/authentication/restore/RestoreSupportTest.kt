package io.capkit.authentication.restore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file RestoreSupportTest.kt
 * RED-GREEN gate for the Restore Credentials device floor: Restore Credentials is
 * API 28+ (Android 9, GMS core 242200000+, credentials 1.5.0+); below that the
 * surface reports `available=false` rather than crashing.
 */
class RestoreSupportTest {
  @Test
  fun `restore supported from API 28 onwards`() {
    assertTrue(RestoreSupport.isRestoreSupported(28))
    assertTrue(RestoreSupport.isRestoreSupported(35))
  }

  @Test
  fun `restore unavailable below API 28`() {
    assertFalse(RestoreSupport.isRestoreSupported(27))
    assertFalse(RestoreSupport.isRestoreSupported(24))
  }

  @Test
  fun `minimum SDK constant matches the contract`() {
    assertTrue(RestoreSupport.MIN_RESTORE_SDK_INT == 28)
  }
}
