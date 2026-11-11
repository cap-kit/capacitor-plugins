package io.capkit.splashmotion

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Headless JVM tests for the state logic of [SplashMotionImpl].
 *
 * Every test passes a null activity: the implementation must treat a missing
 * host activity as a no-op for all visual side effects without throwing.
 * UI-bound behavior (dialog surface, Lottie rendering, haptics) cannot be
 * asserted without an instrumented device and is verified on-device via the
 * example app.
 */
class SplashMotionImplTest {
  @Test
  fun splashStartsHidden() {
    val impl = SplashMotionImpl(context = null)
    assertFalse(impl.isVisible())
  }

  @Test
  fun showWithoutActivityIsNoOp() {
    val impl = SplashMotionImpl(context = null)
    impl.show(activity = null, options = testShowOptions())
    assertFalse(impl.isVisible())
  }

  @Test
  fun hideWithoutActivityIsNoOp() {
    val impl = SplashMotionImpl(context = null)
    impl.hide(activity = null, options = testHideOptions())
    assertFalse(impl.isVisible())
  }

  private fun testShowOptions() =
    io.capkit.splashmotion.model.ShowOptionsData(
      autoHide = true,
      fadeInDuration = 100,
      fadeOutDuration = 100,
      showDuration = 1000,
      lottie = null,
      haptic = null,
    )

  private fun testHideOptions() =
    io.capkit.splashmotion.model.HideOptionsData(
      fadeOutDuration = 100,
      haptic = null,
    )
}
