package io.capkit.sslpinning

import android.content.Context
import io.capkit.sslpinning.config.SSLPinningConfig
import io.capkit.sslpinning.error.SSLPinningError
import io.capkit.sslpinning.error.SSLPinningErrorMessages
import io.capkit.sslpinning.logger.SSLPinningLogger
import io.capkit.sslpinning.model.SSLPinningResultModel
import io.capkit.sslpinning.utils.SSLPinningUtils
import java.io.IOException
import java.net.SocketTimeoutException
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
  // -----------------------------------------------------------------------------
  // Constants
  // -----------------------------------------------------------------------------

  companion object {
    private const val TIMEOUT_MS = 10_000
  }

  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

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

  // -----------------------------------------------------------------------------
  // Configuration
  // -----------------------------------------------------------------------------

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

  // -----------------------------------------------------------------------------
  // Single fingerprint
  // -----------------------------------------------------------------------------

  /**
   * Validates the SSL certificate of a HTTPS endpoint
   * using a single SHA-256 fingerprint.
   *
   * Resolution order:
   * 1. Runtime fingerprint argument
   * 2. Static configuration fingerprint
   * 3. Fall back to cert mode if certificates are configured
   *
   * @throws SSLPinningError.Unavailable if no fingerprint and no certificates available.
   */
  @Throws(SSLPinningError::class)
  fun checkCertificate(
    urlString: String,
    fingerprintFromArgs: String?,
  ): SSLPinningResultModel {
    val fingerprint =
      fingerprintFromArgs ?: config.fingerprint

    if (fingerprint != null) {
      return performCheck(
        urlString = urlString,
        fingerprints = listOf(fingerprint),
      )
    }

    val hasCerts = config.certs.isNotEmpty()
    if (hasCerts) {
      return performCheck(
        urlString = urlString,
        fingerprints = emptyList(),
      )
    }

    throw SSLPinningError.Unavailable(
      SSLPinningErrorMessages.NO_FINGERPRINTS_PROVIDED,
    )
  }

  // -----------------------------------------------------------------------------
  // Multiple fingerprints
  // -----------------------------------------------------------------------------

  /**
   * Validates the SSL certificate of a HTTPS endpoint
   * using multiple allowed SHA-256 fingerprints.
   *
   * A match is considered valid if ANY provided fingerprint matches.
   *
   * Falls back to cert mode if no fingerprints are provided
   * but certificates are configured.
   *
   * @throws SSLPinningError.Unavailable if no fingerprints and no certificates available.
   */
  @Throws(SSLPinningError::class)
  fun checkCertificates(
    urlString: String,
    fingerprintsFromArgs: List<String>?,
  ): SSLPinningResultModel {
    val fingerprints =
      fingerprintsFromArgs?.takeIf { it.isNotEmpty() }
        ?: config.fingerprints.takeIf { it.isNotEmpty() }

    if (fingerprints != null) {
      return performCheck(
        urlString = urlString,
        fingerprints = fingerprints,
      )
    }

    val hasCerts = config.certs.isNotEmpty()
    if (hasCerts) {
      return performCheck(
        urlString = urlString,
        fingerprints = emptyList(),
      )
    }

    throw SSLPinningError.Unavailable(
      SSLPinningErrorMessages.NO_FINGERPRINTS_PROVIDED,
    )
  }

  // -----------------------------------------------------------------------------
  // Shared implementation
  // -----------------------------------------------------------------------------

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
  ): SSLPinningResultModel {
    val url =
      SSLPinningUtils.httpsUrl(urlString)
        ?: throw SSLPinningError.UnknownType(
          SSLPinningErrorMessages.INVALID_URL_MUST_BE_HTTPS,
        )

    val host = url.host

    // -----------------------------------------------------------------------------
    // EXCLUDED DOMAIN MODE
    // -----------------------------------------------------------------------------

    /**
     * If the request host matches an excluded domain,
     * SSL pinning is bypassed but we still validate using system trust.
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

      // Perform actual TLS handshake using system trust
      try {
        val certificate = getCertificateWithSystemTrust(url)
        val actualFingerprint =
          SSLPinningUtils.normalizeFingerprint(
            SSLPinningUtils.sha256Fingerprint(certificate),
          )

        return SSLPinningResultModel(
          actualFingerprint = actualFingerprint,
          fingerprintMatched = true,
          excludedDomain = true,
          mode = "excluded",
          errorCode = "EXCLUDED_DOMAIN",
          error = SSLPinningErrorMessages.EXCLUDED_DOMAIN,
        )
      } catch (e: SocketTimeoutException) {
        throw SSLPinningError.Timeout(SSLPinningErrorMessages.TIMEOUT)
      } catch (e: IOException) {
        throw SSLPinningError.NetworkError(SSLPinningErrorMessages.NETWORK_ERROR)
      }
    }

    // -----------------------------------------------------------------------------
    // CERT MODE
    // -----------------------------------------------------------------------------

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
          SSLPinningErrorMessages.NO_CERTS_PROVIDED,
        )
      }

      try {
        val certificate =
          getCertificateWithPinnedCerts(url, pinnedCerts)

        val actualFingerprint =
          SSLPinningUtils.normalizeFingerprint(
            SSLPinningUtils.sha256Fingerprint(certificate),
          )

        return SSLPinningResultModel(
          actualFingerprint = actualFingerprint,
          fingerprintMatched = true,
          mode = "cert",
          errorCode = "",
          error = "",
        )
      } catch (e: SocketTimeoutException) {
        throw SSLPinningError.Timeout(SSLPinningErrorMessages.TIMEOUT)
      } catch (e: IOException) {
        throw SSLPinningError.NetworkError(SSLPinningErrorMessages.NETWORK_ERROR)
      } catch (e: Exception) {
        throw SSLPinningError.TrustEvaluationFailed(
          SSLPinningErrorMessages.PINNING_FAILED,
        )
      }
    }

    // -----------------------------------------------------------------------------
    // FINGERPRINT MODE
    // -----------------------------------------------------------------------------

    /**
     * Fingerprint-based pinning.
     *
     * Only the leaf certificate fingerprint is compared.
     * The system trust chain is NOT evaluated in this mode.
     */
    if (fingerprints.isEmpty()) {
      throw SSLPinningError.NoPinningConfig(
        SSLPinningErrorMessages.NO_FINGERPRINTS_PROVIDED,
      )
    }

    val certificate: Certificate
    try {
      certificate = getCertificate(url)
    } catch (e: SocketTimeoutException) {
      throw SSLPinningError.Timeout(SSLPinningErrorMessages.TIMEOUT)
    } catch (e: IOException) {
      throw SSLPinningError.NetworkError(SSLPinningErrorMessages.NETWORK_ERROR)
    }

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

    val errorCode = if (matched) "" else "PINNING_FAILED"
    val errorMessage = if (matched) "" else SSLPinningErrorMessages.PINNING_FAILED

    SSLPinningLogger.debug(
      "SSLPinning matched:",
      matched.toString(),
    )

    return SSLPinningResultModel(
      actualFingerprint = actualFingerprint,
      fingerprintMatched = matched,
      matchedFingerprint = matchedFingerprint,
      mode = "fingerprint",
      errorCode = errorCode,
      error = errorMessage,
    )
  }

  // -----------------------------------------------------------------------------
  // Certificate retrieval
  // -----------------------------------------------------------------------------

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

    try {
      connection.sslSocketFactory =
        sslContext.socketFactory

      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS

      connection.connect()

      val certificate =
        connection.serverCertificates.first()

      return certificate
    } finally {
      try {
        connection.disconnect()
      } catch (_: Exception) {
        // Ignore disconnect errors
      }
    }
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

    try {
      connection.sslSocketFactory =
        sslContext.socketFactory

      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS

      connection.connect()

      val certificate =
        connection.serverCertificates.first()

      return certificate
    } finally {
      try {
        connection.disconnect()
      } catch (_: Exception) {
        // Ignore disconnect errors
      }
    }
  }

  /**
   * Opens a TLS connection using system default trust validation.
   *
   * This is used for excluded domains - we bypass pinning
   * but still validate the certificate chain using system anchors.
   *
   * @throws Exception if the TLS handshake fails or certificate is invalid.
   */
  @Throws(Exception::class)
  private fun getCertificateWithSystemTrust(url: URL): Certificate {
    val tmf =
      javax.net.ssl.TrustManagerFactory.getInstance(
        javax.net.ssl.TrustManagerFactory
          .getDefaultAlgorithm(),
      )

    tmf.init(null as java.security.KeyStore?)

    val sslContext =
      SSLContext.getInstance("TLS")

    sslContext.init(null, tmf.trustManagers, null)

    val connection =
      url.openConnection() as HttpsURLConnection

    try {
      connection.sslSocketFactory =
        sslContext.socketFactory

      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS

      connection.connect()

      val certificate =
        connection.serverCertificates.first()

      return certificate
    } finally {
      try {
        connection.disconnect()
      } catch (_: Exception) {
        // Ignore disconnect errors
      }
    }
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
