package io.capkit.splashmotion.utils

import android.view.Gravity
import android.widget.ImageView

/**
 * Pure validation and normalization helpers for the SplashMotion plugin.
 *
 * All functions are side-effect free so they can be exercised headless
 * in JVM unit tests.
 */
object SplashMotionValidators {
  /**
   * Normalizes a haptic style name to one of the supported values.
   *
   * @return "light", "medium" or "heavy". Any other value (including null)
   * falls back to "medium".
   */
  fun normalizeHapticStyle(style: String?): String =
    when (style?.lowercase()) {
      "light" -> "light"
      "heavy" -> "heavy"
      else -> "medium"
    }

  /**
   * Parses a hex color string into an ARGB int.
   *
   * Accepted formats: `#RGB`, `#RRGGBB` and `#RRGGBBAA` (RGBA order, the
   * format documented in the plugin's TypeScript API and used by the iOS
   * implementation). Pure Kotlin: unit-testable without the Android SDK.
   *
   * @return the parsed color, or null when the input is null or invalid.
   */
  fun parseColor(value: String?): Int? {
    val hex = value?.trim()?.removePrefix("#") ?: return null
    if (hex.isEmpty()) {
      return null
    }
    if (hex.any { digit -> digit.digitToIntOrNull(16) == null }) {
      return null
    }
    return when (hex.length) {
      3 -> {
        val r = hex[0].digitToInt(16) * 0x11
        val g = hex[1].digitToInt(16) * 0x11
        val b = hex[2].digitToInt(16) * 0x11
        argb(alpha = 0xFF, red = r, green = g, blue = b)
      }
      6 ->
        argb(
          alpha = 0xFF,
          red = hex.substring(0, 2).toInt(16),
          green = hex.substring(2, 4).toInt(16),
          blue = hex.substring(4, 6).toInt(16),
        )
      8 ->
        argb(
          alpha = hex.substring(6, 8).toInt(16),
          red = hex.substring(0, 2).toInt(16),
          green = hex.substring(2, 4).toInt(16),
          blue = hex.substring(4, 6).toInt(16),
        )
      else -> null
    }
  }

  private fun argb(
    alpha: Int,
    red: Int,
    green: Int,
    blue: Int,
  ): Int = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

  /**
   * Maps a config scale type name to the matching ImageView.ScaleType.
   *
   * @return the mapped ScaleType, or FIT_XY for null/unknown values.
   */
  fun parseScaleType(value: String?): ImageView.ScaleType =
    when (value?.uppercase()) {
      "CENTER" -> ImageView.ScaleType.CENTER
      "CENTER_CROP" -> ImageView.ScaleType.CENTER_CROP
      "CENTER_INSIDE" -> ImageView.ScaleType.CENTER_INSIDE
      "FIT_CENTER" -> ImageView.ScaleType.FIT_CENTER
      "FIT_END" -> ImageView.ScaleType.FIT_END
      "FIT_START" -> ImageView.ScaleType.FIT_START
      "MATRIX" -> ImageView.ScaleType.MATRIX
      else -> ImageView.ScaleType.FIT_XY
    }

  /**
   * Maps a spinner style name to the matching Android ProgressBar style
   * attribute, mirroring the official plugin mapping.
   *
   * Accepted values (case-insensitive): horizontal, small, large, inverse,
   * smallInverse, largeInverse.
   *
   * @return the mapped android.R.attr style, or progressBarStyleLarge for
   * null/unknown values.
   */
  fun parseSpinnerStyleAttr(value: String?): Int =
    when (value?.lowercase()) {
      "horizontal" -> android.R.attr.progressBarStyleHorizontal
      "small" -> android.R.attr.progressBarStyleSmall
      "inverse" -> android.R.attr.progressBarStyleInverse
      "smallinverse" -> android.R.attr.progressBarStyleSmallInverse
      "largeinverse" -> android.R.attr.progressBarStyleLargeInverse
      else -> android.R.attr.progressBarStyleLarge
    }

  /**
   * Clamps a duration in milliseconds to a sane non-negative range.
   *
   * @return the clamped duration, or [fallback] when the input is null.
   */
  fun clampDuration(
    value: Long?,
    fallback: Long,
  ): Long {
    if (value == null) {
      return fallback
    }
    return value.coerceIn(0, 60_000)
  }

  /**
   * Effective launch minimum hold: the configured `launchShowDuration`
   * clamped so it never cuts a loaded launch lottie short. The animation
   * duration (ms) only ever extends the hold — a shorter animation must
   * not shorten the configured minimum, and a longer one must play fully
   * before the splash is released.
   */
  fun effectiveLaunchMinimumHoldMs(
    configuredDurationMs: Long?,
    lottieDurationMs: Long,
  ): Long = maxOf(clampDuration(configuredDurationMs, 0L), lottieDurationMs.coerceAtLeast(0L))

  /**
   * Resolves the splash background color honoring dark mode.
   *
   * When [isDark] is true and a dark variant is provided, the dark variant
   * wins; otherwise the light color is used. Both are parsed leniently.
   *
   * @return the resolved color, or null when nothing valid is available.
   */
  fun resolveSplashColor(
    light: String?,
    dark: String?,
    isDark: Boolean,
  ): Int? {
    if (isDark && dark != null) {
      return parseColor(dark)
    }
    return parseColor(light)
  }

  /**
   * Maps a splash gravity name to the matching Android gravity flags.
   *
   * Accepted values: topStart, top, topEnd, start, center, end,
   * bottomStart, bottom, bottomEnd (case-insensitive). Pure Kotlin:
   * unit-testable without the Android SDK.
   *
   * @return the mapped gravity flags, or CENTER for null/unknown values.
   */
  fun parseGravity(value: String?): Int =
    when (value?.lowercase()) {
      "topstart" -> Gravity.TOP or Gravity.START
      "top" -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
      "topend" -> Gravity.TOP or Gravity.END
      "start" -> Gravity.CENTER_VERTICAL or Gravity.START
      "end" -> Gravity.CENTER_VERTICAL or Gravity.END
      "bottomstart" -> Gravity.BOTTOM or Gravity.START
      "bottom" -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
      "bottomend" -> Gravity.BOTTOM or Gravity.END
      else -> Gravity.CENTER
    }

  /**
   * Normalizes an image opacity value to the 0..1 range.
   *
   * @return the clamped opacity as a Float, or 1f when the input is null.
   */
  fun normalizeAlpha(value: Double?): Float = (value ?: 1.0).coerceIn(0.0, 1.0).toFloat()

  /**
   * Normalizes an image margin value in dp to the documented 0-200 range.
   *
   * @return the clamped margin, or [fallback] when the input is null.
   */
  fun normalizeMargin(
    value: Double?,
    fallback: Double,
  ): Double = (value ?: fallback).coerceIn(0.0, 200.0)

  /**
   * Normalizes an entrance animation type name to one of the supported
   * values.
   *
   * @return "none", "fade" or "scale". Any other value (including null)
   * falls back to "none".
   */
  fun normalizeEntranceType(type: String?): String =
    when (type?.lowercase()) {
      "fade" -> "fade"
      "scale" -> "scale"
      else -> "none"
    }

  /**
   * Clamps an entrance animation duration in milliseconds to the
   * documented 0-2000 range.
   *
   * @return the clamped duration, or 300 when the input is null.
   */
  fun clampEntranceDuration(value: Long?): Long = value?.coerceIn(0, 2_000) ?: 300

  /**
   * Resolves the splash gradient colors honoring dark mode.
   *
   * When [isDark] is true and both dark variants are valid colors, the
   * dark pair wins; otherwise the light pair is used. Lenient: an invalid
   * dark pair silently falls back to the light pair.
   *
   * @return the top/bottom color pair, or null when no valid pair is
   * available (the gradient is then ignored and the flat background color
   * applies).
   */
  fun resolveGradient(
    top: String?,
    bottom: String?,
    topDark: String?,
    bottomDark: String?,
    isDark: Boolean,
  ): Pair<Int, Int>? {
    if (isDark) {
      val darkTop = parseColor(topDark)
      val darkBottom = parseColor(bottomDark)
      if (darkTop != null && darkBottom != null) {
        return darkTop to darkBottom
      }
    }
    val lightTop = parseColor(top) ?: return null
    val lightBottom = parseColor(bottom) ?: return null
    return lightTop to lightBottom
  }

  /**
   * Selects the animation JSON entry inside a dotLottie (zip) archive.
   *
   * Selection rule, matching the dotLottie spec layout:
   * 1. the first entry under `animations/` ending in `.json`;
   * 2. otherwise the first `.json` entry that is not `manifest.json`.
   *
   * @return the selected entry name, or null when no candidate exists.
   */
  fun selectDotLottieEntry(names: List<String>): String? {
    val animationEntry =
      names.firstOrNull { name ->
        name.startsWith("animations/") && name.endsWith(".json")
      }
    if (animationEntry != null) {
      return animationEntry
    }
    return names.firstOrNull { name ->
      name.endsWith(".json") && !name.equals("manifest.json", ignoreCase = true)
    }
  }
}
