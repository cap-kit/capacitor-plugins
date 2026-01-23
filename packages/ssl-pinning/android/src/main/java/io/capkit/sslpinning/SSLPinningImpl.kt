package io.capkit.sslpinning

import io.capkit.sslpinning.utils.SSLPinningLogger
import io.capkit.sslpinning.utils.SSLPinningUtils
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SSLPinningImpl(
  private val config: SSLPinningConfig,
) {
  // ---- Single fingerprint ----

  /**
   * Validates the SSL certificate of a HTTPS endpoint using a single SHA-256 fingerprint.
   *
   * This method:
   * - Opens a TLS connection to the given URL
   * - Extracts the leaf certificate presented by the server
   * - Computes its SHA-256 fingerprint
   * - Compares it against the provided or configured fingerprint
   *
   * NOTE:
   * - No HTTP request body is sent
   * - The certificate trust chain is NOT validated
   * - Only the leaf certificate fingerprint is checked
   */
  fun checkCertificate(
    urlString: String,
    fingerprintFromArgs: String?,
    callback: (Map<String, Any>) -> Unit,
  ) {
    val fingerprint =
      fingerprintFromArgs ?: config.fingerprint

    if (fingerprint == null) {
      callback(
        mapOf(
          "fingerprintMatched" to false,
          "error" to "No fingerprint provided (args or config)",
        ),
      )
      return
    }

    performCheck(urlString, listOf(fingerprint), callback)
  }

  // ---- Multiple fingerprints ----

  /**
   * Validates the SSL certificate of a HTTPS endpoint using multiple allowed fingerprints.
   *
   * The certificate is considered valid if **any** of the provided fingerprints
   * matches the server's leaf certificate fingerprint.
   *
   * This is typically used to support certificate rotation.
   */
  fun checkCertificates(
    urlString: String,
    fingerprintsFromArgs: List<String>?,
    callback: (Map<String, Any>) -> Unit,
  ) {
    val fingerprints =
      fingerprintsFromArgs?.takeIf { it.isNotEmpty() }
        ?: config.fingerprints.takeIf { it.isNotEmpty() }

    if (fingerprints == null) {
      callback(
        mapOf(
          "fingerprintMatched" to false,
          "error" to "No fingerprints provided (args or config)",
        ),
      )
      return
    }

    performCheck(urlString, fingerprints, callback)
  }

  // ---- Shared implementation ----

  /**
   * Shared internal implementation for SSL pinning validation.
   *
   * This method performs the actual TLS handshake and fingerprint comparison.
   * It is intentionally isolated to avoid duplication between
   * single and multi fingerprint modes.
   */
  private fun performCheck(
    urlString: String,
    fingerprints: List<String>,
    callback: (Map<String, Any>) -> Unit,
  ) {
    val url = SSLPinningUtils.httpsUrl(urlString)
    if (url == null) {
      callback(
        mapOf(
          "fingerprintMatched" to false,
          "error" to "Invalid HTTPS URL",
          "errorCode" to "UNKNOWN_TYPE",
        ),
      )
      return
    }

    try {
      val cert = getCertificate(url)
      val actualFingerprint =
        SSLPinningUtils.normalizeFingerprint(
          SSLPinningUtils.sha256Fingerprint(cert),
        )

      val normalizedExpected =
        fingerprints.map { SSLPinningUtils.normalizeFingerprint(it) }

      val matchedFingerprint =
        normalizedExpected.firstOrNull { it == actualFingerprint }

      val matched = matchedFingerprint != null

      SSLPinningLogger.debug("SSLPinning matched:", matched.toString())

      callback(
        mapOf(
          "actualFingerprint" to actualFingerprint,
          "fingerprintMatched" to matched,
          "matchedFingerprint" to (matchedFingerprint ?: ""),
        ),
      )
    } catch (e: Exception) {
      SSLPinningLogger.error("Certificate check failed", e)
      callback(
        mapOf(
          "fingerprintMatched" to false,
          "error" to e.message.orEmpty(),
          "errorCode" to "INIT_FAILED",
        ),
      )
    }
  }

  // ---- Certificate retrieval ----

  /**
   * Opens a TLS connection and extracts the server leaf certificate.
   *
   * A permissive TrustManager is intentionally used to allow
   * inspection of the certificate without enforcing trust validation.
   *
   * SECURITY NOTE:
   * This does NOT bypass SSL pinning security, because the fingerprint
   * comparison is performed manually after extraction.
   */
  private fun getCertificate(url: URL): Certificate {
    val trustManagers =
      arrayOf<TrustManager>(
        object : X509TrustManager {
          override fun getAcceptedIssuers() = arrayOf<java.security.cert.X509Certificate>()

          override fun checkClientTrusted(
            certs: Array<java.security.cert.X509Certificate>,
            authType: String,
          ) {}

          override fun checkServerTrusted(
            certs: Array<java.security.cert.X509Certificate>,
            authType: String,
          ) {}
        },
      )

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustManagers, java.security.SecureRandom())

    val connection = url.openConnection() as HttpsURLConnection
    connection.sslSocketFactory = sslContext.socketFactory
    connection.connect()

    val cert = connection.serverCertificates.first()
    connection.disconnect()

    return cert
  }
}
