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
}
