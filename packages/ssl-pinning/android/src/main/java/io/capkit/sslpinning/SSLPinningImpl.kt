package io.capkit.sslpinning

import android.content.Context
import io.capkit.sslpinning.utils.SSLPinningLogger
import io.capkit.sslpinning.utils.SSLPinningUtils
import java.net.URL
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Native Android implementation for the SSLPinning plugin.
 *
 * Responsibilities:
 * - Perform platform-specific SSL pinning logic
 * - Interact with system networking APIs
 * - Throw typed SSLPinningError values on failure
 *
 * Forbidden:
 * - Accessing PluginCall
 * - Referencing Capacitor APIs
 * - Constructing JavaScript payloads
 */
class SSLPinningImpl(
  private val context: Context,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached list of pinned certificates loaded from assets.
   *
   * This avoids repeated I/O operations when certificate-based
   * pinning is used multiple times during the app lifecycle.
   */
  private var cachedPinnedCerts: List<X509Certificate>? = null

  /**
   * Cached plugin configuration.
   * Injected once during plugin initialization.
   */
  private lateinit var config: SSLPinningConfig

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies plugin configuration.
   *
   * This method MUST be called exactly once from the plugin's load() method.
   * It translates static configuration into runtime behavior
   * (e.g. enabling verbose logging).
   */
  fun updateConfig(newConfig: SSLPinningConfig) {
    this.config = newConfig
    SSLPinningLogger.verbose = newConfig.verboseLogging
    SSLPinningLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Single fingerprint
  // ---------------------------------------------------------------------------

  /**
   * Validates the SSL certificate of a HTTPS endpoint
   * using a single SHA-256 fingerprint.
   *
   * Resolution order:
   * 1. Runtime fingerprint argument
   * 2. Static configuration fingerprint
   *
   * @throws SSLPinningError.Unavailable if no fingerprint is available.
   */
  @Throws(SSLPinningError::class)
  fun checkCertificate(
    urlString: String,
    fingerprintFromArgs: String?,
  ): Map<String, Any> {
    val fingerprint =
      fingerprintFromArgs ?: config.fingerprint

    if (fingerprint == null) {
      throw SSLPinningError.Unavailable(
        "No fingerprint provided (args or config)",
      )
    }

    return performCheck(
      urlString = urlString,
      fingerprints = listOf(fingerprint),
    )
  }

  // ---------------------------------------------------------------------------
  // Multiple fingerprints
  // ---------------------------------------------------------------------------

  /**
   * Validates the SSL certificate of a HTTPS endpoint
   * using multiple allowed SHA-256 fingerprints.
   *
   * A match is considered valid if ANY provided fingerprint matches.
   *
   * @throws SSLPinningError.Unavailable if no fingerprints are available.
   */
  @Throws(SSLPinningError::class)
  fun checkCertificates(
    urlString: String,
    fingerprintsFromArgs: List<String>?,
  ): Map<String, Any> {
    val fingerprints =
      fingerprintsFromArgs?.takeIf { it.isNotEmpty() }
        ?: config.fingerprints.takeIf { it.isNotEmpty() }

    if (fingerprints == null) {
      throw SSLPinningError.Unavailable(
        "No fingerprints provided (args or config)",
      )
    }

    return performCheck(
      urlString = urlString,
      fingerprints = fingerprints,
    )
  }

  // ---------------------------------------------------------------------------
  // Shared implementation
  // ---------------------------------------------------------------------------

  /**
   * Determines and executes the SSL pinning strategy for the given request.
   *
   * Evaluation order:
   *
   * 1. Excluded domain → bypass pinning entirely.
   * 2. Certificate-based pinning (cert mode).
   * 3. Fingerprint-based pinning.
   *
   * Certificate-based pinning is activated ONLY when:
   * - No fingerprints are provided
   * - One or more certificates are configured
   *
   * @throws SSLPinningError when validation fails.
   */
  @Throws(SSLPinningError::class)
  private fun performCheck(
    urlString: String,
    fingerprints: List<String>,
  ): Map<String, Any> {
    val url =
      SSLPinningUtils.httpsUrl(urlString)
        ?: throw SSLPinningError.UnknownType(
          "Invalid HTTPS URL",
        )

    val host = url.host

    // ---------------------------------------------------------------------------
    // EXCLUDED DOMAIN MODE
    // ---------------------------------------------------------------------------

    /**
     * If the request host matches an excluded domain,
     * SSL pinning is bypassed completely.
     *
     * Matching rules:
     * - Exact match
     * - Subdomain match
     */
    if (config.excludedDomains.any { excluded ->
        val excludedLower = excluded.lowercase().trim()
        val hostLower = host.lowercase().trim()
        // Match exact domain or any subdomain (e.g., api.example.com matches example.com)
        hostLower == excludedLower || hostLower.endsWith(".$excludedLower")
      }
    ) {
      SSLPinningLogger.debug("SSLPinning excluded domain:", host)

      return mapOf(
        "fingerprintMatched" to true,
        "excludedDomain" to true,
        "mode" to "excluded",
      )
    }

    // ---------------------------------------------------------------------------
    // CERT MODE
    // ---------------------------------------------------------------------------

    /**
     * Certificate-based SSL pinning.
     *
     * This mode validates the full server certificate chain
     * against pinned X.509 certificates bundled with the app.
     */
    if (fingerprints.isEmpty() && config.certs.isNotEmpty()) {
      // Use helper to get certificates from memory or load them from assets if needed
      val pinnedCerts = getPinnedCertsInternal()

      if (pinnedCerts.isEmpty()) {
        throw SSLPinningError.CertNotFound(
          "No valid pinned certificates found",
        )
      }

      try {
        val certificate =
          getCertificateWithPinnedCerts(url, pinnedCerts)

        val actualFingerprint =
          SSLPinningUtils.normalizeFingerprint(
            SSLPinningUtils.sha256Fingerprint(certificate),
          )

        return mapOf(
          "fingerprintMatched" to true,
          "actualFingerprint" to actualFingerprint,
          "mode" to "cert",
        )
      } catch (e: Exception) {
        throw SSLPinningError.TrustEvaluationFailed(
          e.message ?: "Trust evaluation failed",
        )
      }
    }

    // ---------------------------------------------------------------------------
    // FINGERPRINT MODE
    // ---------------------------------------------------------------------------

    /**
     * Fingerprint-based pinning.
     *
     * Only the leaf certificate fingerprint is compared.
     * The system trust chain is NOT evaluated in this mode.
     */
    if (fingerprints.isEmpty()) {
      throw SSLPinningError.NoPinningConfig(
        "No fingerprint provided (args or config)",
      )
    }

    val certificate = getCertificate(url)

    val actualFingerprint =
      SSLPinningUtils.normalizeFingerprint(
        SSLPinningUtils.sha256Fingerprint(certificate),
      )

    val normalizedExpected =
      fingerprints.map {
        SSLPinningUtils.normalizeFingerprint(it)
      }

    val matchedFingerprint =
      normalizedExpected.firstOrNull {
        it == actualFingerprint
      }

    val matched = matchedFingerprint != null

    SSLPinningLogger.debug(
      "SSLPinning matched:",
      matched.toString(),
    )

    return mapOf(
      "actualFingerprint" to actualFingerprint,
      "fingerprintMatched" to matched,
      "matchedFingerprint" to (matchedFingerprint ?: ""),
      "mode" to "fingerprint",
    )
  }

  // ---------------------------------------------------------------------------
  // Certificate retrieval
  // ---------------------------------------------------------------------------

  /**
   * Opens a TLS connection and extracts
   * the server leaf certificate.
   *
   * SECURITY MODEL:
   * - A permissive TrustManager is used intentionally.
   * - The system trust chain is NOT validated.
   * - Validation is performed manually via fingerprint comparison.
   */
  @Throws(Exception::class)
  private fun getCertificate(url: URL): Certificate {
    val trustManagers =
      arrayOf<TrustManager>(
        object : X509TrustManager {
          override fun getAcceptedIssuers() = emptyArray<X509Certificate>()

          override fun checkClientTrusted(
            certs: Array<X509Certificate>,
            authType: String,
          ) {}

          override fun checkServerTrusted(
            certs: Array<X509Certificate>,
            authType: String,
          ) {}
        },
      )

    val sslContext =
      SSLContext.getInstance("TLS")

    sslContext.init(
      null,
      trustManagers,
      java.security.SecureRandom(),
    )

    val connection =
      url.openConnection() as HttpsURLConnection

    connection.sslSocketFactory =
      sslContext.socketFactory

    connection.connect()

    val certificate =
      connection.serverCertificates.first()

    connection.disconnect()

    return certificate
  }

  /**
   * Opens a TLS connection using a TrustManager configured
   * with pinned X.509 certificates.
   *
   * SECURITY MODEL:
   * - The system trust chain is replaced with pinned anchors.
   * - The handshake succeeds ONLY if the server chain
   *   can be validated against pinned certificates.
   */
  @Throws(Exception::class)
  private fun getCertificateWithPinnedCerts(
    url: URL,
    certs: List<X509Certificate>,
  ): Certificate {
    val keyStore =
      java.security.KeyStore.getInstance(
        java.security.KeyStore.getDefaultType(),
      )

    keyStore.load(null)

    certs.forEachIndexed { index, cert ->
      keyStore.setCertificateEntry("cert_$index", cert)
    }

    val tmf =
      javax.net.ssl.TrustManagerFactory.getInstance(
        javax.net.ssl.TrustManagerFactory
          .getDefaultAlgorithm(),
      )

    tmf.init(keyStore)

    val sslContext =
      SSLContext.getInstance("TLS")

    sslContext.init(null, tmf.trustManagers, null)

    val connection =
      url.openConnection() as HttpsURLConnection

    connection.sslSocketFactory =
      sslContext.socketFactory

    connection.connect()

    val certificate =
      connection.serverCertificates.first()

    connection.disconnect()

    return certificate
  }

  /**
   * Internal helper to retrieve pinned certificates.
   * Uses cached values if available to avoid redundant asset I/O.
   */
  private fun getPinnedCertsInternal(): List<X509Certificate> {
    if (cachedPinnedCerts == null) {
      cachedPinnedCerts = SSLPinningUtils.loadPinnedCertificates(context, config.certs)
    }
    return cachedPinnedCerts ?: emptyList()
  }
}
