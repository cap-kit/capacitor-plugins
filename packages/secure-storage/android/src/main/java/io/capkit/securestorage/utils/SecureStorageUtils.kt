package io.capkit.securestorage.utils

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Pure utility helpers for SecureStorage Android.
 *
 * Deterministic key-name codecs shared by the secure and insecure tiers.
 * This object contains only pure helpers and does not perform I/O.
 */
object SecureStorageUtils {
  /**
   * URL-safe base64 without padding. Used for key-name obfuscation:
   * hides names from casual inspection, never encryption.
   */
  fun encodeB64(value: String): String =
    Base64
      .getEncoder()
      .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
      .replace("+", "-")
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
      val decoded = Base64.getDecoder().decode(normalized).toString(StandardCharsets.UTF_8)
      if (encodeB64(decoded) == value) decoded else null
    } catch (_: Exception) {
      null
    }
  }
}
