package io.capkit.authentication.restore

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file RestoreErrorMapperTest.kt
 * RED-GREEN contract for translating Credential Manager failures into the typed
 * plugin error surface:
 * - user cancellations (Get/Create credential) → USER_CANCELLED
 * - E2EE unavailable → retry-without-cloud-backup instruction (NOT an error)
 * - no-store credential (`NoCredentialException`) → caller handles as absence,
 *   never mapped to an error by the mapper
 */
class RestoreErrorMapperTest {
  @Test
  fun `get credential cancellation maps to USER_CANCELLED`() {
    val outcome = RestoreErrorMapper.map(GetCredentialCancellationException("cancelled"))
    assertTrue(outcome is RestoreErrorOutcome.Reject)
    assertTrue((outcome as RestoreErrorOutcome.Reject).error is AuthenticationError.UserCancelled)
  }

  @Test
  fun `create credential cancellation maps to USER_CANCELLED`() {
    val outcome = RestoreErrorMapper.map(CreateCredentialCancellationException("cancelled"))
    assertTrue(outcome is RestoreErrorOutcome.Reject)
    assertTrue((outcome as RestoreErrorOutcome.Reject).error is AuthenticationError.UserCancelled)
  }

  @Test
  fun `E2EE unavailable maps to retry instruction not an error`() {
    assertEquals(
      RestoreErrorOutcome.RetryWithoutCloudBackup,
      RestoreErrorMapper.map(E2eeUnavailableException("e2ee unavailable")),
    )
  }

  @Test
  fun `NoCredentialException is left for the caller as normal absence`() {
    assertNull(RestoreErrorMapper.map(NoCredentialException("none")))
  }

  @Test
  fun `unknown credential failures are not pre-mapped`() {
    assertNull(RestoreErrorMapper.map(GetCredentialUnknownException("unknown")))
    assertNull(RestoreErrorMapper.map(RuntimeException("boom")))
  }
}
