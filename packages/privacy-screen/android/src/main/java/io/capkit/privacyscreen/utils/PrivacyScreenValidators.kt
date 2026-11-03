package io.capkit.privacyscreen.utils

/**
 * Collection of input validation helpers for the PrivacyScreen plugin.
 *
 * These functions are pure and side-effect free.
 * They MUST NOT depend on Capacitor APIs or platform state.
 */
object PrivacyScreenValidators {
  /**
   * Normalizes the `privacyModeOnActivityHidden` session option.
   *
   * Only the documented values `dim` and `splash` are honored; every other
   * raw value (including null, the documented default and wrong casing)
   * falls back to `none`, which keeps the hidden-activity scrim disabled.
   *
   * The comparison is case-sensitive on purpose: the JS contract sends
   * exact lowercase values.
   */
  fun normalizeHiddenMode(raw: String?): String = if (raw == "dim" || raw == "splash") raw else "none"

  /**
   * Returns whether the raw static `color` option is a valid hex color.
   *
   * Accepted shapes: `#RRGGBB`, `#AARRGGBB` and the same shapes without the
   * leading `#`. Any other value (including CSS color names and `rgba()`
   * strings) is rejected so the scrim keeps its default black.
   *
   * The comparison is case-insensitive on purpose: hex digits may be written
   * in either case and both are unambiguous.
   */
  fun isValidHexColor(raw: String?): Boolean = raw != null && Regex("^#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$").matches(raw)

  /**
   * Parses a validated hex color into an Android color int.
   *
   * Accepts the same shapes as [isValidHexColor]; 6-digit input is treated
   * as fully opaque. MUST only be called with values that passed
   * [isValidHexColor].
   */
  fun parseHexColor(raw: String): Int {
    val digits = raw.removePrefix("#")
    val argb = if (digits.length == 6) "FF$digits" else digits
    return argb.toLong(16).toInt()
  }
}
