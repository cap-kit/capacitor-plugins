package io.capkit.authentication.restore

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import io.capkit.authentication.error.AuthenticationError

/**
 * @file RestoreErrorMapper.kt
 * Pure translation of Credential Manager failures into the plugin error surface:
 *
 * - get/create credential cancellation → [AuthenticationError.UserCancelled]
 *   (cross-platform `USER_CANCELLED` parity);
 * - E2EE unavailable → [RestoreErrorOutcome.RetryWithoutCloudBackup] (create retry);
 * - everything else (including `NoCredentialException`, handled by the caller as
 *   normal absence) → `null`, i.e. not pre-mapped.
 */
object RestoreErrorMapper {
  /**
   * @param exception the failure raised by the Credential Manager.
   * @return a [RestoreErrorOutcome] when the failure has a defined mapping,
   *         or `null` when the caller must decide.
   */
  fun map(exception: Throwable): RestoreErrorOutcome? =
    when (exception) {
      is E2eeUnavailableException -> RestoreErrorOutcome.RetryWithoutCloudBackup
      is GetCredentialCancellationException, is CreateCredentialCancellationException ->
        RestoreErrorOutcome.Reject(
          AuthenticationError.UserCancelled("The user dismissed the credential flow."),
        )
      else -> null
    }
}
