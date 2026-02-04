package io.capkit.sslpinning

import android.content.Context
import io.capkit.sslpinning.utils.SSLPinningLogger
import io.capkit.sslpinning.utils.SSLPinningUtils
import java.net.URL
import java.security.cert.Certificate
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
  /**
   * Cached plugin configuration.
   * Injected once during plugin initialization.
   */
  private lateinit var config: SSLPinningConfig

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
   * Performs the actual SSL pinning validation.
   *
   * This method:
   * - Validates the HTTPS URL
   * - Opens a TLS connection
   * - Extracts the server leaf certificate
   * - Compares its SHA-256 fingerprint
   *   against the expected ones
   *
   * IMPORTANT:
   * - The system trust chain is NOT evaluated
   * - Only fingerprint matching determines acceptance
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

    return try {
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

      mapOf(
        "actualFingerprint" to actualFingerprint,
        "fingerprintMatched" to matched,
        "matchedFingerprint" to (matchedFingerprint ?: ""),
      )
    } catch (e: SSLPinningError) {
      throw e
    } catch (e: Exception) {
      SSLPinningLogger.error(
        "Certificate check failed",
        e,
      )
      throw SSLPinningError.InitFailed(
        e.message ?: "SSL pinning failed",
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Certificate retrieval
  // ---------------------------------------------------------------------------

  /**
   * Opens a TLS connection and extracts
   * the server leaf certificate.
   *
   * A permissive TrustManager is intentionally used
   * to allow certificate inspection without enforcing
   * system trust validation.
   */
  @Throws(Exception::class)
  private fun getCertificate(url: URL): Certificate {
    val trustManagers =
      arrayOf<TrustManager>(
        object : X509TrustManager {
          override fun getAcceptedIssuers() = emptyArray<java.security.cert.X509Certificate>()

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
}
