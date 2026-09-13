package io.capkit.authentication.google

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @file GoogleSignInModeTest.kt
 * RED-GREEN contract for One Tap UX selection: with
 * `autoSelect` enabled the silent One Tap attempt runs first; otherwise the full
 * Google account chooser is the direct path.
 */
class GoogleSignInModeTest {
  @Test
  fun `autoSelect true resolves to silent One Tap attempt`() {
    assertEquals(GoogleSignInMode.AUTO_SELECT, GoogleSignInModeResolver.resolve(autoSelect = true))
  }

  @Test
  fun `autoSelect false resolves to direct full button chooser`() {
    assertEquals(GoogleSignInMode.FULL_BUTTON, GoogleSignInModeResolver.resolve(autoSelect = false))
  }

  @Test
  fun `autoSelect defaults to false when not configured`() {
    assertEquals(GoogleSignInMode.FULL_BUTTON, GoogleSignInModeResolver.resolve())
  }
}
