package io.capkit.authentication.restore

import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file RestoreRetryPolicyTest.kt
 * RED-GREEN retry policy for createRestoreCredential: only E2EE unavailability
 * triggers the single retry without cloud backup (isCloudBackupEnabled=false).
 */
class RestoreRetryPolicyTest {
  @Test
  fun `retries without cloud backup only for E2EE unavailability`() {
    assertTrue(RestoreRetryPolicy.shouldRetryWithoutCloudBackup(E2eeUnavailableException("no e2ee")))
  }

  @Test
  fun `does not retry for any other failure`() {
    assertFalse(RestoreRetryPolicy.shouldRetryWithoutCloudBackup(GetCredentialUnknownException("x")))
    assertFalse(RestoreRetryPolicy.shouldRetryWithoutCloudBackup(RuntimeException("x")))
  }
}
