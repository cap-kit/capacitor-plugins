package io.capkit.privacyscreen.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for the input validation helpers of the PrivacyScreen plugin.
 *
 * The helpers are pure: they must run without Android framework state and
 * must not throw for any input — unknown values normalize, never fail.
 */
class PrivacyScreenValidatorsTest {
  // ---------------------------------------------------------------------------
  // normalizeHiddenMode
  // ---------------------------------------------------------------------------

  @Test
  fun normalizeHiddenModeKeepsDocumentedDimValue() {
    assertEquals("dim", PrivacyScreenValidators.normalizeHiddenMode("dim"))
  }

  @Test
  fun normalizeHiddenModeKeepsDocumentedSplashValue() {
    assertEquals("splash", PrivacyScreenValidators.normalizeHiddenMode("splash"))
  }

  @Test
  fun normalizeHiddenModeDefaultsDocumentedNoneValue() {
    assertEquals("none", PrivacyScreenValidators.normalizeHiddenMode("none"))
  }

  @Test
  fun normalizeHiddenModeFallsBackToNoneForUnknownValues() {
    assertEquals("none", PrivacyScreenValidators.normalizeHiddenMode("opaque"))
    assertEquals("none", PrivacyScreenValidators.normalizeHiddenMode(""))
  }

  @Test
  fun normalizeHiddenModeFallsBackToNoneForNull() {
    assertEquals("none", PrivacyScreenValidators.normalizeHiddenMode(null))
  }

  @Test
  fun normalizeHiddenModeIsCaseSensitive() {
    assertEquals("none", PrivacyScreenValidators.normalizeHiddenMode("DIM"))
    assertEquals("none", PrivacyScreenValidators.normalizeHiddenMode("Splash"))
  }
}
