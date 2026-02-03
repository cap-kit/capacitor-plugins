package io.capkit.sslpinning.utils

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
object SSLPinningUtils {
  /**
   * Validates and returns a HTTPS URL.
   *
   * Non-HTTPS URLs are explicitly rejected
   * to prevent insecure usage.
   */
  fun httpsUrl(value: String): URL? {
    return try {
      val url = URL(value)
      if (url.protocol == "https") url else null
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Normalizes a fingerprint string by:
   * - Removing colon separators
   * - Converting to lowercase
   *
   * Example:
   * "AA:BB:CC" → "aabbcc"
   */
  fun normalizeFingerprint(value: String): String {
    return value
      .replace(":", "")
      .lowercase()
  }

  /**
   * Computes the SHA-256 fingerprint of an X.509 certificate.
   *
   * Output format:
   * "aa:bb:cc:dd:..."
   */
  fun sha256Fingerprint(cert: Certificate): String {
    val digest =
      MessageDigest.getInstance("SHA-256")
        .digest(cert.encoded)

    return digest.joinToString(separator = ":") {
      "%02x".format(it)
    }
  }
}
