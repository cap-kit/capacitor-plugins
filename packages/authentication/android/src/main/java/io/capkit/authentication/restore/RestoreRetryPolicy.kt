package io.capkit.authentication.restore

import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException

/**
 * @file RestoreRetryPolicy.kt
 * Pure retry policy for `createRestoreCredential`: exactly one retry, without cloud
 * backup, is allowed — and only when E2EE is unavailable on the device. No other
 * failure is retried, keeping the surface deterministic. Side-effect free.
 */
object RestoreRetryPolicy {
  /**
   * @param exception the failure raised by `createCredential`.
   * @return `true` when a single retry with `isCloudBackupEnabled=false` is safe.
   */
  fun shouldRetryWithoutCloudBackup(exception: Throwable): Boolean = exception is E2eeUnavailableException
}
