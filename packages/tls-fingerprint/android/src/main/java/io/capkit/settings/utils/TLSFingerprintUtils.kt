package io.capkit.tlsfingerprint.utils

import java.net.URL
import java.security.MessageDigest
import java.security.cert.Certificate

/**
 * Utility helpers for SSL pinning logic (Android).
 *
 * Pure utilities:
 * - No Capacitor dependency
 * - No side effects
 * - Fully testable
 */
object TLSFingerprintUtils {
  /**
   * Validates and returns a HTTPS URL.
   *
   * Non-HTTPS URLs are explicitly rejected
   * to prevent insecure usage.
   */
  fun httpsUrl(value: String): URL? =
    try {
      val url = URL(value)
      if (url.protocol == "https") url else null
    } catch (_: Exception) {
      null
    }

  /**
   * Normalizes a fingerprint string by:
   * - Removing colon separators
   * - Removing all whitespace
   * - Converting to lowercase
   *
   * Example:
   * "AA:BB:CC" → "aabbcc"
   * "AA BB CC" → "aabbcc"
   */
  fun normalizeFingerprint(value: String): String =
    value
      .replace(":", "")
      .replace(" ", "")
      .lowercase()

  /**
   * Validates that a fingerprint string is a valid SHA-256 hex format.
   *
   * Valid fingerprint:
   * - Exactly 64 hexadecimal characters (after normalization)
   * - Contains only [a-f0-9]
   */
  fun isValidFingerprintFormat(value: String): Boolean {
    val normalized = normalizeFingerprint(value)
    return normalized.length == 64 && normalized.matches(Regex("^[a-f0-9]+$"))
  }

  /**
   * Validates a fingerprint and returns an error message if invalid, or null if valid.
   */
  fun validateFingerprint(value: String): String? {
    if (value.isBlank()) {
      return "Fingerprint cannot be blank"
    }
    val normalized = normalizeFingerprint(value)
    if (normalized.length != 64) {
      return "Invalid fingerprint: must be 64 hex characters"
    }
    if (!normalized.matches(Regex("^[a-f0-9]+$"))) {
      return "Invalid fingerprint: must contain only hex characters [a-f0-9]"
    }
    return null
  }

  /**
   * Computes the SHA-256 fingerprint of an X.509 certificate.
   *
   * Output format:
   * "aa:bb:cc:dd:..."
   */
  fun sha256Fingerprint(cert: Certificate): String {
    val digest =
      MessageDigest
        .getInstance("SHA-256")
        .digest(cert.encoded)

    return digest.joinToString(separator = ":") {
      "%02x".format(it)
    }
  }
}
