package io.capkit.authentication.google

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file SignInFallbackPolicyTest.kt
 * RED-GREEN policy for the full-button fallback (google-provider scenario
 * "Full-button fallback"): the silent One Tap attempt is only retried through the
 * full chooser when autoSelect was actually attempted and the failure means "no
 * credential could be auto-selected" (cancellation or no credentialed account).
 */
class SignInFallbackPolicyTest {
  @Test
  fun `falls back after silent attempt cancelled by the user`() {
    assertTrue(isFallback(GoogleSignInMode.AUTO_SELECT, GetCredentialCancellationException("c")))
    assertTrue(isFallback(GoogleSignInMode.AUTO_SELECT, CreateCredentialCancellationException("c")))
  }

  @Test
  fun `falls back after silent attempt finds no credentialed account`() {
    assertTrue(isFallback(GoogleSignInMode.AUTO_SELECT, NoCredentialException("none")))
  }

  @Test
  fun `does not fall back from the full button mode`() {
    assertFalse(isFallback(GoogleSignInMode.FULL_BUTTON, GetCredentialCancellationException("c")))
    assertFalse(isFallback(GoogleSignInMode.FULL_BUTTON, NoCredentialException("none")))
  }

  @Test
  fun `does not fall back on non-cancellation failures`() {
    assertFalse(isFallback(GoogleSignInMode.AUTO_SELECT, GetCredentialUnknownException("u")))
    assertFalse(isFallback(GoogleSignInMode.AUTO_SELECT, RuntimeException("boom")))
  }

  private fun isFallback(
    mode: GoogleSignInMode,
    error: Throwable,
  ): Boolean = SignInFallbackPolicy.shouldFallbackToFullButton(mode, error)
}
