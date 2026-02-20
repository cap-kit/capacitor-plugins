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
 *
 * NOTE: Certificate loading involves I/O and is NOT pure.
 * It is provided as a separate utility function.
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

  /**
   * Determines the effective certificate list for a given host.
   *
   * Matching rules (in order of precedence):
   * 1. Exact domain match (e.g., "api.example.com")
   * 2. Subdomain match - most specific wins (longest key)
   * 3. Fallback to global certs
   *
   * @param host The request host (e.g., "api.example.com")
   * @param certsByDomain Per-domain certificate configuration
   * @param globalCerts Global fallback certificates
   * @return The effective list of certificate file names
   */
  fun getEffectiveCertsForHost(
    host: String,
    certsByDomain: Map<String, List<String>>,
    globalCerts: List<String>,
  ): List<String> {
    val hostLower = host.lowercase().trim()

    // 1. Try exact match
    certsByDomain[hostLower]?.let { return it }

    // 2. Try subdomain match - find most specific (longest key)
    val matchingSubdomains =
      certsByDomain.keys
        .filter { key ->
          hostLower.endsWith(".$key") || hostLower == key
        }.sortedByDescending { it.length }

    matchingSubdomains.firstOrNull()?.let { bestMatch ->
      return certsByDomain[bestMatch] ?: globalCerts
    }

    // 3. Fallback to global certs
    return globalCerts
  }

  /**
   * Checks if any configured certificate is invalid.
   * Returns the first invalid certificate filename, or null if all are valid.
   */
  fun findFirstInvalidCert(
    certFileNames: List<String>,
    isValidChecker: (String) -> Boolean,
  ): String? = certFileNames.firstOrNull { !isValidChecker(it) }

  /**
   * Loads X.509 certificates from the app assets.
   *
   * Certificates must be placed under:
   * assets/certs/
   *
   * @param context Android context
   * @param certFileNames List of certificate file names
   * @return List of loaded X509Certificates (empty on any failure)
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
