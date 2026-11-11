package io.capkit.splashmotion.utils

import android.view.Gravity
import android.widget.ImageView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Headless JVM tests for the pure validation helpers of [SplashMotionValidators].
 */
class SplashMotionValidatorsTest {
  @Test
  fun hapticStyleNormalization() {
    assertEquals("light", SplashMotionValidators.normalizeHapticStyle("light"))
    assertEquals("light", SplashMotionValidators.normalizeHapticStyle("LIGHT"))
    assertEquals("heavy", SplashMotionValidators.normalizeHapticStyle("heavy"))
    assertEquals("medium", SplashMotionValidators.normalizeHapticStyle("medium"))
    assertEquals("medium", SplashMotionValidators.normalizeHapticStyle(null))
    assertEquals("medium", SplashMotionValidators.normalizeHapticStyle("unknown"))
  }

  @Test
  fun colorParsing() {
    assertEquals(-16777216, SplashMotionValidators.parseColor("#000000"))
    assertEquals(-1, SplashMotionValidators.parseColor("#FFFFFF"))
    // 8-digit format is RRGGBBAA (matches the iOS implementation contract):
    // #FF000080 = red FF, green 00, blue 00, alpha 80.
    assertEquals(-2130771968, SplashMotionValidators.parseColor("#FF000080"))
    assertNull(SplashMotionValidators.parseColor(null))
    assertNull(SplashMotionValidators.parseColor(""))
    assertNull(SplashMotionValidators.parseColor("not-a-color"))
    assertNull(SplashMotionValidators.parseColor("#12345"))
  }

  @Test
  fun scaleTypeParsing() {
    assertEquals(ImageView.ScaleType.CENTER_CROP, SplashMotionValidators.parseScaleType("CENTER_CROP"))
    assertEquals(ImageView.ScaleType.CENTER, SplashMotionValidators.parseScaleType("center"))
    assertEquals(ImageView.ScaleType.FIT_XY, SplashMotionValidators.parseScaleType(null))
    assertEquals(ImageView.ScaleType.FIT_XY, SplashMotionValidators.parseScaleType("unknown"))
  }

  @Test
  fun gravityParsing() {
    assertEquals(Gravity.TOP or Gravity.START, SplashMotionValidators.parseGravity("topStart"))
    assertEquals(Gravity.TOP or Gravity.CENTER_HORIZONTAL, SplashMotionValidators.parseGravity("TOP"))
    assertEquals(Gravity.BOTTOM or Gravity.END, SplashMotionValidators.parseGravity("bottomEnd"))
    assertEquals(Gravity.CENTER_VERTICAL or Gravity.END, SplashMotionValidators.parseGravity("end"))
    assertEquals(Gravity.CENTER, SplashMotionValidators.parseGravity(null))
    assertEquals(Gravity.CENTER, SplashMotionValidators.parseGravity("unknown"))
  }

  @Test
  fun spinnerStyleParsing() {
    val large = android.R.attr.progressBarStyleLarge
    val smallInverse = android.R.attr.progressBarStyleSmallInverse
    val largeInverse = android.R.attr.progressBarStyleLargeInverse
    // Official mapping: default is progressBarStyleLarge.
    assertEquals(large, SplashMotionValidators.parseSpinnerStyleAttr(null))
    assertEquals(large, SplashMotionValidators.parseSpinnerStyleAttr("unknown"))
    assertEquals(large, SplashMotionValidators.parseSpinnerStyleAttr("large"))
    assertEquals(large, SplashMotionValidators.parseSpinnerStyleAttr("LARGE"))
    assertEquals(android.R.attr.progressBarStyleHorizontal, SplashMotionValidators.parseSpinnerStyleAttr("horizontal"))
    assertEquals(android.R.attr.progressBarStyleSmall, SplashMotionValidators.parseSpinnerStyleAttr("small"))
    assertEquals(android.R.attr.progressBarStyleInverse, SplashMotionValidators.parseSpinnerStyleAttr("inverse"))
    assertEquals(smallInverse, SplashMotionValidators.parseSpinnerStyleAttr("smallInverse"))
    assertEquals(largeInverse, SplashMotionValidators.parseSpinnerStyleAttr("largeInverse"))
  }

  @Test
  fun alphaNormalization() {
    assertEquals(1f, SplashMotionValidators.normalizeAlpha(null))
    assertEquals(0f, SplashMotionValidators.normalizeAlpha(0.0))
    assertEquals(0.5f, SplashMotionValidators.normalizeAlpha(0.5))
    assertEquals(1f, SplashMotionValidators.normalizeAlpha(1.0))
    assertEquals(0f, SplashMotionValidators.normalizeAlpha(-0.5))
    assertEquals(1f, SplashMotionValidators.normalizeAlpha(2.0))
  }

  @Test
  fun marginNormalization() {
    assertEquals(16.0, SplashMotionValidators.normalizeMargin(null, 16.0), 0.0)
    assertEquals(0.0, SplashMotionValidators.normalizeMargin(-5.0, 16.0), 0.0)
    assertEquals(32.0, SplashMotionValidators.normalizeMargin(32.0, 16.0), 0.0)
    assertEquals(200.0, SplashMotionValidators.normalizeMargin(500.0, 16.0), 0.0)
  }

  @Test
  fun entranceNormalization() {
    assertEquals("fade", SplashMotionValidators.normalizeEntranceType("fade"))
    assertEquals("scale", SplashMotionValidators.normalizeEntranceType("SCALE"))
    assertEquals("none", SplashMotionValidators.normalizeEntranceType(null))
    assertEquals("none", SplashMotionValidators.normalizeEntranceType("zoom"))
    assertEquals(300, SplashMotionValidators.clampEntranceDuration(null))
    assertEquals(0, SplashMotionValidators.clampEntranceDuration(-10))
    assertEquals(500, SplashMotionValidators.clampEntranceDuration(500))
    assertEquals(2000, SplashMotionValidators.clampEntranceDuration(9999))
  }

  @Test
  fun gradientResolutionHonorsDarkMode() {
    // Light mode: light pair wins.
    assertEquals(
      -1 to -16777216,
      SplashMotionValidators.resolveGradient("#FFFFFF", "#000000", "#111111", "#222222", false),
    )
    // Dark mode with valid dark pair: dark pair wins.
    assertEquals(
      -16777216 to -1,
      SplashMotionValidators.resolveGradient("#FFFFFF", "#000000", "#000000", "#FFFFFF", true),
    )
    // Dark mode with partially invalid dark pair: light pair applies.
    assertEquals(
      -1 to -16777216,
      SplashMotionValidators.resolveGradient("#FFFFFF", "#000000", "#000000", "invalid", true),
    )
    // Invalid light top color: gradient ignored.
    assertNull(SplashMotionValidators.resolveGradient("nope", "#000000", null, null, false))
    // Invalid light bottom color: gradient ignored.
    assertNull(SplashMotionValidators.resolveGradient("#FFFFFF", null, null, null, false))
  }

  @Test
  fun durationClamping() {
    assertEquals(200, SplashMotionValidators.clampDuration(null, 200))
    assertEquals(0, SplashMotionValidators.clampDuration(-5, 200))
    assertEquals(500, SplashMotionValidators.clampDuration(500, 200))
    assertEquals(60000, SplashMotionValidators.clampDuration(999999, 200))
  }

  @Test
  fun splashColorResolutionHonorsDarkMode() {
    // Light mode: light color wins.
    assertEquals(-1, SplashMotionValidators.resolveSplashColor("#FFFFFF", "#000000", false))
    // Dark mode with variant: dark color wins.
    assertEquals(-16777216, SplashMotionValidators.resolveSplashColor("#FFFFFF", "#000000", true))
    // Dark mode without variant: light color still applies.
    assertEquals(-1, SplashMotionValidators.resolveSplashColor("#FFFFFF", null, true))
    // Invalid values resolve to null instead of throwing ("bad" is a valid
    // 3-digit hex #BAD, so a genuinely invalid literal is used).
    assertNull(SplashMotionValidators.resolveSplashColor("not-a-color", "#000000", false))
    assertNull(SplashMotionValidators.resolveSplashColor(null, null, true))
  }

  @Test
  fun dotLottieEntrySelection() {
    // Prefers the first entry under animations/.
    assertEquals(
      "animations/splash.json",
      SplashMotionValidators.selectDotLottieEntry(
        listOf("manifest.json", "animations/splash.json", "animations/other.json"),
      ),
    )
    // Falls back to the first non-manifest JSON.
    assertEquals(
      "splash.json",
      SplashMotionValidators.selectDotLottieEntry(listOf("manifest.json", "splash.json")),
    )
    // No candidate at all.
    assertNull(SplashMotionValidators.selectDotLottieEntry(listOf("manifest.json", "images/logo.png")))
    assertNull(SplashMotionValidators.selectDotLottieEntry(emptyList()))
  }

  @Test
  fun effectiveLaunchMinimumHoldNeverCutsTheLottie() {
    // A longer lottie extends the hold: the splash must never be released
    // mid-animation.
    assertEquals(
      2000L,
      SplashMotionValidators.effectiveLaunchMinimumHoldMs(1000L, 2000L),
    )
    // A shorter lottie never shortens the configured minimum.
    assertEquals(
      2000L,
      SplashMotionValidators.effectiveLaunchMinimumHoldMs(2000L, 1000L),
    )
    // Equal values hold as configured.
    assertEquals(
      2000L,
      SplashMotionValidators.effectiveLaunchMinimumHoldMs(2000L, 2000L),
    )
    // A missing configured value falls back to the lottie duration alone.
    assertEquals(
      1500L,
      SplashMotionValidators.effectiveLaunchMinimumHoldMs(null, 1500L),
    )
    // No lottie duration captured yet (composition still loading): the
    // configured minimum applies untouched.
    assertEquals(
      2000L,
      SplashMotionValidators.effectiveLaunchMinimumHoldMs(2000L, 0L),
    )
    // Negative lottie durations are coerced, never trusted.
    assertEquals(
      2000L,
      SplashMotionValidators.effectiveLaunchMinimumHoldMs(2000L, -5L),
    )
  }
}
