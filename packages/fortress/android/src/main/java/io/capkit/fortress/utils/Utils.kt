package io.capkit.fortress.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * Utility helpers for the Fortress plugin.
 *
 * This object is intentionally empty and serves as a placeholder
 * for future shared utility functions.
 *
 * Keeping a dedicated utils package helps maintain a clean
 * separation between core logic and helper code.
 */
object Utils {
  /**
   * Produces a SHA-256 hex digest.
   */
  fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(value.toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { byte -> "%02x".format(byte) }
  }

  /**
   * Returns a non-PII stable device identifier hash.
   */
  fun deviceIdentifierHash(context: Context): String {
    val androidId =
      Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "android-unknown-device"
    return sha256Hex(androidId)
  }

  /**
   * Returns a JSON string literal with minimal escaping.
   *
   * NOTE:
   * - Do NOT use for building general JSON documents.
   * - This helper exists to produce deterministic payload strings for backend verification.
   */
  fun jsonString(value: String): String {
    val escaped =
      buildString {
        for (ch in value) {
          when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
              // Control chars must be escaped
              if (ch.code < 0x20) {
                append("\\u")
                append(ch.code.toString(16).padStart(4, '0'))
              } else {
                append(ch)
              }
            }
          }
        }
      }

    return "\"$escaped\""
  }
}
