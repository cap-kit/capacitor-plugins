package io.capkit.authentication.utils

import java.io.ByteArrayOutputStream

/**
 * @file Base64Url.kt
 * RFC 4648 base64url codec (URL-safe alphabet, unpadded output), pure Kotlin with
 * no Android dependency: safe below API 26 (where `java.util.Base64` is absent)
 * and JVM-testable. Encoding matches `java.util.Base64.getUrlEncoder().withoutPadding()`;
 * decoding accepts padded or unpadded input and returns `null` for illegal characters
 * (so JWT extraction never crashes — security baseline).
 */
object Base64Url {
  private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

  /**
   * Encodes bytes as unpadded base64url (RFC 4648 section 5).
   *
   * @param input the raw bytes.
   * @return the base64url string without trailing '=' padding.
   */
  fun encode(input: ByteArray): String {
    val out = StringBuilder((input.size + 2) / 3 * 4)
    var i = 0
    while (i < input.size) {
      val b0 = input[i].toInt() and 0xFF
      val b1 = if (i + 1 < input.size) input[i + 1].toInt() and 0xFF else -1
      val b2 = if (i + 2 < input.size) input[i + 2].toInt() and 0xFF else -1
      out.append(ALPHABET[b0 ushr 2])
      out.append(ALPHABET[((b0 shl 4) or (if (b1 >= 0) b1 ushr 4 else 0)) and 0x3F])
      if (b1 >= 0) out.append(ALPHABET[((b1 shl 2) or (if (b2 >= 0) b2 ushr 6 else 0)) and 0x3F])
      if (b2 >= 0) out.append(ALPHABET[b2 and 0x3F])
      i += 3
    }
    return out.toString()
  }

  /**
   * Decodes base64url data, padded or unpadded.
   *
   * @param input the base64url string.
   * @return the decoded bytes, or `null` for illegal characters or invalid length.
   */
  fun decode(input: String): ByteArray? {
    val cleaned = input.trimEnd('=')
    if (cleaned.isEmpty()) return ByteArray(0)
    if (cleaned.length % 4 == 1) return null
    val out = ByteArrayOutputStream(cleaned.length * 3 / 4)
    var buffer = 0
    var bits = 0
    for (ch in cleaned) {
      val value =
        when (ch) {
          in 'A'..'Z' -> ch - 'A'
          in 'a'..'z' -> ch - 'a' + 26
          in '0'..'9' -> ch - '0' + 52
          '-' -> 62
          '_' -> 63
          else -> return null
        }
      buffer = (buffer shl 6) or value
      bits += 6
      if (bits >= 8) {
        bits -= 8
        out.write((buffer shr bits) and 0xFF)
      }
    }
    return out.toByteArray()
  }
}
