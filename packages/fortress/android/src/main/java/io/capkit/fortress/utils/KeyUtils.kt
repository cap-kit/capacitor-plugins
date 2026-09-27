package io.capkit.fortress.utils

/**
 * Key-formatting helpers for Fortress storage namespaces.
 *
 * Responsibilities:
 * - Apply deterministic obfuscation prefixing for standard storage keys
 * - Apply global prefixing for secure-storage keys
 */
object KeyUtils {
  /**
   * Applies both the global prefix and the obfuscation prefix.
   */
  fun obfuscate(
    key: String,
    obfuscationPrefix: String,
    globalPrefix: String = "",
  ): String = "${globalPrefix}${obfuscationPrefix}$key"

  /**
   * Applies only the global prefix for secure storage keys.
   */
  fun formatSecureKey(
    key: String,
    globalPrefix: String = "",
  ): String = "${globalPrefix}$key"

  /**
   * URL-safe base64 without padding. Used for key-name obfuscation:
   * hides names from casual inspection, never encryption.
   */
  fun encodeB64(value: String): String =
    android.util.Base64
      .encodeToString(
        value.toByteArray(Charsets.UTF_8),
        android.util.Base64.NO_WRAP,
      ).replace("+", "-")
      .replace("/", "_")
      .trimEnd('=')

  /**
   * Best-effort inverse of [encodeB64]: returns null unless the value
   * round-trips cleanly, so plain names are never misdecoded.
   */
  fun decodeB64(value: String): String? {
    if (value.any {
        it !in 'A'..'Z' &&
          it !in 'a'..'z' &&
          it !in '0'..'9' &&
          it != '-' &&
          it != '_' &&
          it != '+' &&
          it != '/' &&
          it != '='
      }
    ) {
      return null
    }
    return try {
      var normalized = value.replace("-", "+").replace("_", "/")
      val remainder = normalized.length % 4
      if (remainder > 0) {
        normalized += "=".repeat(4 - remainder)
      }
      val decoded =
        android.util.Base64
          .decode(normalized, android.util.Base64.DEFAULT)
          .toString(Charsets.UTF_8)
      if (encodeB64(decoded) == value) decoded else null
    } catch (_: Exception) {
      null
    }
  }
}
