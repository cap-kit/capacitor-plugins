package io.capkit.authentication.google

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException

/**
 * @file SignInFallbackPolicy.kt
 * Pure fallback policy (google-provider "Full-button fallback"): the silent One Tap
 * attempt falls back to the full Google account chooser ONLY when autoSelect was
 * attempted AND the failure means nothing could be auto-selected — user
 * cancellation or no credentialed account. Other failures abort the flow.
 */
object SignInFallbackPolicy {
  /**
   * @param mode the attempted sign-in mode.
   * @param error the failure raised by the Credential Manager.
   * @return `true` when the full-button chooser should be presented next.
   */
  fun shouldFallbackToFullButton(
    mode: GoogleSignInMode,
    error: Throwable,
  ): Boolean = mode == GoogleSignInMode.AUTO_SELECT && isAutoSelectUnavailable(error)

  private fun isAutoSelectUnavailable(error: Throwable): Boolean =
    error is GetCredentialCancellationException ||
      error is CreateCredentialCancellationException ||
      error is NoCredentialException
}
