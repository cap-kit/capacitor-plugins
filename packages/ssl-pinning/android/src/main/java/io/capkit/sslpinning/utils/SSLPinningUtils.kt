package io.capkit.sslpinning.utils

import android.content.Context
import java.net.URL
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

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
   * - Converting to lowercase
   *
   * Example:
   * "AA:BB:CC" → "aabbcc"
   */
  fun normalizeFingerprint(value: String): String =
    value
      .replace(":", "")
      .lowercase()

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

  /**
   * Loads X.509 certificates from the app assets.
   *
   * Certificates must be placed under:
   * assets/certs/
   *
   * Invalid or unreadable certificates are silently ignored.
   */
  fun loadPinnedCertificates(
    context: Context,
    certFileNames: List<String>,
  ): List<X509Certificate> {
    val certificates = mutableListOf<X509Certificate>()
    val assetManager = context.assets
    val certFactory = CertificateFactory.getInstance("X.509")

    for (fileName in certFileNames) {
      val assetPath = "certs/$fileName"
      try {
        assetManager.open(assetPath).use { inputStream ->
          val cert = certFactory.generateCertificate(inputStream)
          if (cert is X509Certificate) {
            certificates.add(cert)
          }
        }
      } catch (e: Exception) {
        // Errors are handled by the caller (SSLPinningImpl)
      }
    }
    return certificates
  }
}
