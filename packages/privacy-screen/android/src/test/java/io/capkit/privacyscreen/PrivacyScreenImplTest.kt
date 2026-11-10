package io.capkit.privacyscreen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Headless JVM tests for the protection-state logic of [PrivacyScreenImpl].
 *
 * Every test passes a null activity: the implementation must treat a missing
 * host activity as a no-op for all window/scrim side effects while still
 * tracking the protection state. UI-bound behavior (FLAG_SECURE, scrim
 * views) cannot be asserted without an instrumented device and is verified
 * on-device via the example app.
 */
class PrivacyScreenImplTest {
  @Test
  fun protectionStartsDisabled() {
    val impl = PrivacyScreenImpl(context = null)
    assertFalse(impl.isEnabled())
  }

  @Test
  fun enableTracksStateWithoutActivity() {
    val impl = PrivacyScreenImpl(context = null)
    impl.enable(activity = null, dimBackground = true, hiddenMode = "dim")
    assertTrue(impl.isEnabled())
  }

  @Test
  fun disableClearsStateWithoutActivity() {
    val impl = PrivacyScreenImpl(context = null)
    impl.enable(activity = null, dimBackground = false, hiddenMode = "none")
    impl.disable(activity = null)
    assertFalse(impl.isEnabled())
  }

  @Test
  fun lifecycleCallbacksAreNoOpsWhenDisabled() {
    val impl = PrivacyScreenImpl(context = null)
    // Neither callback may throw or resurrect the protection state.
    impl.onActivityHidden(activity = null)
    impl.onActivityShown(activity = null)
    assertFalse(impl.isEnabled())
  }

  @Test
  fun lifecycleCallbacksAreNoOpsWithNullActivityWhileEnabled() {
    val impl = PrivacyScreenImpl(context = null)
    impl.enable(activity = null, dimBackground = false, hiddenMode = "splash")
    impl.onActivityHidden(activity = null)
    impl.onActivityShown(activity = null)
    assertTrue(impl.isEnabled())
  }

  @Test
  fun enableCanBeToggledRepeatedly() {
    val impl = PrivacyScreenImpl(context = null)
    impl.enable(activity = null, dimBackground = true, hiddenMode = "dim")
    impl.disable(activity = null)
    impl.enable(activity = null, dimBackground = false, hiddenMode = "splash")
    assertTrue(impl.isEnabled())
    impl.disable(activity = null)
    assertFalse(impl.isEnabled())
  }
}
