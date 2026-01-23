package io.capkit.sslpinning.utils

import java.net.URL
import java.security.MessageDigest
import java.security.cert.Certificate

object SSLPinningUtils {
  /**
   * Validates that the provided string is a valid HTTPS URL.
   *
   * Non-HTTPS URLs are explicitly rejected to prevent insecure usage.
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
   * This allows consistent comparison across platforms.
   */
  fun normalizeFingerprint(value: String): String {
    return value.replace(":", "").lowercase()
  }

  /**
   * Computes the SHA-256 fingerprint of an X.509 certificate.
   *
   * The returned format uses colon-separated hexadecimal pairs,
   * matching common OpenSSL representations.
   */
  fun sha256Fingerprint(cert: Certificate): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(cert.encoded)
    return digest.joinToString(":") { "%02x".format(it) }
  }
}
