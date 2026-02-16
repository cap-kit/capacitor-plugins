package io.capkit.rank.utils

/**
 * Collection of input validation helpers for the Rank plugin.
 *
 * These functions are pure and side-effect free.
 * They MUST NOT depend on Capacitor APIs or platform state.
 */
object RankValidators {
  /**
   * Validates that the provided Android package name
   * matches the standard applicationId pattern.
   */
  fun validatePackageName(value: String?): String? {
    if (value.isNullOrBlank()) return null

    val packageRegex = Regex("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+$")
    return if (packageRegex.matches(value)) value else null
  }

  /**
   * Validates a developer identifier for Play Store navigation.
   * Allows alphanumeric characters, underscore and hyphen.
   */
  fun validateDevId(value: String?): String? {
    if (value.isNullOrBlank()) return null

    val trimmed = value.trim()
    val devRegex = Regex("^[a-zA-Z0-9_-]+$")
    return if (devRegex.matches(trimmed)) trimmed else null
  }

  /**
   * Validates a Play Store collection identifier.
   * Allows lowercase letters, digits and underscore.
   */
  fun validateCollectionName(value: String?): String? {
    if (value.isNullOrBlank()) return null

    val trimmed = value.trim()
    val collectionRegex = Regex("^[a-z0-9_]+$")
    return if (collectionRegex.matches(trimmed)) trimmed else null
  }

  /**
   * Validates Play Store search terms.
   * Trims whitespace and ensures the string is not empty.
   * Control characters are rejected.
   */
  fun validateSearchTerms(value: String?): String? {
    if (value.isNullOrBlank()) return null

    val trimmed = value.trim()

    // Reject control characters
    if (trimmed.any { it.isISOControl() }) return null

    return if (trimmed.isNotEmpty()) trimmed else null
  }
}
