package io.capkit.tlsfingerprint

import android.content.Context
import io.capkit.tlsfingerprint.config.TLSFingerprintConfig
import io.capkit.tlsfingerprint.error.TLSFingerprintError
import io.capkit.tlsfingerprint.error.TLSFingerprintErrorMessages
import io.capkit.tlsfingerprint.logger.TLSFingerprintLogger
import io.capkit.tlsfingerprint.model.TLSFingerprintResultModel
import io.capkit.tlsfingerprint.utils.TLSFingerprintUtils
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
 * Native Android implementation for the TLSFingerprint plugin.
 *
 * Responsibilities:
 * - Perform platform-specific SSL pinning logic
 * - Interact with system networking APIs
 * - Throw typed TLSFingerprintError values on failure
 *
 * Forbidden:
 * - Accessing PluginCall
 * - Referencing Capacitor APIs
 * - Constructing JavaScript payloads
 */
class TLSFingerprintImpl(
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
   * Cached plugin configuration.
   * Injected once during plugin initialization.
   */
  private lateinit var config: TLSFingerprintConfig

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
  fun updateConfig(newConfig: TLSFingerprintConfig) {
    this.config = newConfig
    TLSFingerprintLogger.verbose = newConfig.verboseLogging

    TLSFingerprintLogger.debug(
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
   *
   * @throws TLSFingerprintError.Unavailable if no fingerprint available.
   */
  @Throws(TLSFingerprintError::class)
  fun checkCertificate(
    urlString: String,
    fingerprintFromArgs: String?,
  ): TLSFingerprintResultModel {
    val fingerprint =
      fingerprintFromArgs ?: config.fingerprint

    if (fingerprint != null) {
      return performCheck(
        urlString = urlString,
        fingerprints = listOf(fingerprint),
      )
    }

    throw TLSFingerprintError.Unavailable(
      TLSFingerprintErrorMessages.NO_FINGERPRINTS_PROVIDED,
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
   * @throws TLSFingerprintError.Unavailable if no fingerprints available.
   */
  @Throws(TLSFingerprintError::class)
  fun checkCertificates(
    urlString: String,
    fingerprintsFromArgs: List<String>?,
  ): TLSFingerprintResultModel {
    val fingerprints =
      fingerprintsFromArgs?.takeIf { it.isNotEmpty() }
        ?: config.fingerprints.takeIf { it.isNotEmpty() }

    if (fingerprints != null) {
      return performCheck(
        urlString = urlString,
        fingerprints = fingerprints,
      )
    }

    throw TLSFingerprintError.Unavailable(
      TLSFingerprintErrorMessages.NO_FINGERPRINTS_PROVIDED,
    )
  }

  // -----------------------------------------------------------------------------
  // Shared implementation
  // -----------------------------------------------------------------------------

  /**
   * Determines and executes the SSL fingerprint validation for the given request.
   *
   * Evaluation order:
   *
   * 1. Excluded domain → bypass validation entirely.
   * 2. Fingerprint-based validation.
   *
   * @throws TLSFingerprintError when validation fails.
   */
  @Throws(TLSFingerprintError::class)
  private fun performCheck(
    urlString: String,
    fingerprints: List<String>,
  ): TLSFingerprintResultModel {
    val url =
      TLSFingerprintUtils.httpsUrl(urlString)
        ?: throw TLSFingerprintError.UnknownType(
          TLSFingerprintErrorMessages.INVALID_URL_MUST_BE_HTTPS,
        )

    val host = url.host

    // -----------------------------------------------------------------------------
    // EXCLUDED DOMAIN MODE
    // -----------------------------------------------------------------------------

    /**
     * If the request host matches an excluded domain,
     * fingerprint validation is bypassed.
     * The connection uses a permissive TrustManager.
     * System trust chain is NOT explicitly evaluated.
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
      TLSFingerprintLogger.debug("TLSFingerprint excluded domain:", host)

      try {
        val certificate = getCertificate(url)
        val actualFingerprint =
          TLSFingerprintUtils.normalizeFingerprint(
            TLSFingerprintUtils.sha256Fingerprint(certificate),
          )

        return TLSFingerprintResultModel(
          actualFingerprint = actualFingerprint,
          fingerprintMatched = true,
          excludedDomain = true,
          mode = "excluded",
          errorCode = "EXCLUDED_DOMAIN",
          error = TLSFingerprintErrorMessages.EXCLUDED_DOMAIN,
        )
      } catch (e: SocketTimeoutException) {
        throw TLSFingerprintError.Timeout(TLSFingerprintErrorMessages.TIMEOUT)
      } catch (e: IOException) {
        throw TLSFingerprintError.NetworkError(TLSFingerprintErrorMessages.NETWORK_ERROR)
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
    val certificate: Certificate
    try {
      certificate = getCertificate(url)
    } catch (e: SocketTimeoutException) {
      throw TLSFingerprintError.Timeout(TLSFingerprintErrorMessages.TIMEOUT)
    } catch (e: IOException) {
      throw TLSFingerprintError.NetworkError(TLSFingerprintErrorMessages.NETWORK_ERROR)
    }

    val actualFingerprint =
      TLSFingerprintUtils.normalizeFingerprint(
        TLSFingerprintUtils.sha256Fingerprint(certificate),
      )

    val normalizedExpected =
      fingerprints.map {
        TLSFingerprintUtils.normalizeFingerprint(it)
      }

    val matchedFingerprint =
      normalizedExpected.firstOrNull {
        it == actualFingerprint
      }

    val matched = matchedFingerprint != null

    val errorCode = if (matched) "" else "PINNING_FAILED"
    val errorMessage = if (matched) "" else TLSFingerprintErrorMessages.PINNING_FAILED

    TLSFingerprintLogger.debug(
      "TLSFingerprint matched:",
      matched.toString(),
    )

    return TLSFingerprintResultModel(
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
}
