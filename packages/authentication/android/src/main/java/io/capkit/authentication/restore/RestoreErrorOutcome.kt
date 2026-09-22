package io.capkit.authentication.restore

import io.capkit.authentication.error.AuthenticationError

/**
 * @file RestoreErrorOutcome.kt
 * Typed outcome of the Credential-Manager failure translation. A `Reject` carries
 * the native [AuthenticationError] to reject with; `RetryWithoutCloudBackup`
 * instructs the caller to retry `createRestoreCredential` one time with cloud
 * backup disabled (E2EE unavailable). Pure data.
 */
sealed interface RestoreErrorOutcome {
  data class Reject(
    val error: AuthenticationError,
  ) : RestoreErrorOutcome

  data object RetryWithoutCloudBackup : RestoreErrorOutcome
}
