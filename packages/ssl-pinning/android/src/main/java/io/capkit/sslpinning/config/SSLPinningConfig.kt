package io.capkit.sslpinning.config

import com.getcapacitor.Plugin
import io.capkit.sslpinning.logger.SSLPinningLogger
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Plugin configuration container.
 *
 * This class is responsible for reading and exposing
 * static configuration values defined under the
 * `SSLPinning` key in capacitor.config.ts.
 *
 * Configuration rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime input
 * - Accessible only from native code
 *
 * Validation:
 * - Certificate files are validated at load time
 * - Missing/invalid certs cause fail-fast errors
 */
class SSLPinningConfig(
  plugin: Plugin,
) {
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information
   * is printed to Logcat.
   *
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * Default SHA-256 fingerprint used by checkCertificate()
   * when no fingerprint is provided at runtime.
   */
  val fingerprint: String?

  /**
   * Default SHA-256 fingerprints used by checkCertificates()
   * when no fingerprints are provided at runtime.
   */
  val fingerprints: List<String>

  /**
   * Certificate filenames used for SSL pinning.
   *
   * Files are expected to be located under:
   * assets/certs/
   *
   * This is the global fallback used when no domain-specific
   * certificates are configured via `certsByDomain`.
   */
  val certs: List<String>

  /**
   * Per-domain certificate configuration.
   *
   * Maps a domain (or subdomain pattern) to a list of
   * certificate filenames to use for that domain.
   */
  val certsByDomain: Map<String, List<String>>

  /**
   * Optional manifest file for certificate auto-discovery.
   *
   * The manifest is a JSON file containing either:
   * - { "certs": ["a.cer", "b.cer"] }
   * - { "certsByDomain": { "example.com": ["cert.cer"] } }
   * - Both, which extend/override the explicit config values
   */
  val certsManifest: String?

  /**
   * Domains or URL prefixes excluded from SSL pinning.
   *
   * Any request whose host matches one of these values
   * MUST bypass SSL pinning checks.
   */
  val excludedDomains: List<String>

  /**
   * Cached validation status for certificate files.
   * Populated during initialization.
   */
  private val certValidationCache: Map<String, Boolean>

  // -----------------------------------------------------------------------------
  // Initialization
  // -----------------------------------------------------------------------------

  init {
    val config = plugin.getConfig()
    val context = plugin.context

    // Verbose logging flag
    verboseLogging =
      config.getBoolean("verboseLogging", false)

    SSLPinningLogger.verbose = verboseLogging

    // Single fingerprint (optional)
    val fp = config.getString("fingerprint")
    fingerprint =
      if (!fp.isNullOrBlank()) fp else null

    // Multiple fingerprints (optional)
    fingerprints =
      config
        .getArray("fingerprints")
        ?.toList()
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    // Certificate files (optional)
    val explicitCerts =
      config
        .getArray("certs")
        ?.toList()
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    // Per-domain certificates (optional)
    val explicitCertsByDomain =
      parseCertsByDomainFromConfig(config)

    // Manifest (optional)
    val manifestPath = config.getString("certsManifest")

    // Load and merge manifest if present
    val (manifestCerts, manifestCertsByDomain) =
      if (!manifestPath.isNullOrBlank()) {
        loadManifest(context, manifestPath)
      } else {
        Pair(emptyList(), emptyMap<String, List<String>>())
      }

    // Merge: explicit config takes precedence over manifest
    certs = explicitCerts.ifEmpty { manifestCerts }

    val mergedCertsByDomain = mutableMapOf<String, List<String>>()
    mergedCertsByDomain.putAll(manifestCertsByDomain)
    mergedCertsByDomain.putAll(explicitCertsByDomain)
    certsByDomain = mergedCertsByDomain.toMap()

    // Store manifest path
    certsManifest = manifestPath

    // Excluded domains (optional)
    excludedDomains =
      config
        .getArray("excludedDomains")
        ?.toList()
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    // Validate certificate files at load time (fail-fast)
    certValidationCache = validateCertFiles(context, certs, certsByDomain)
  }

  // -----------------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------------

  /**
   * Checks if a certificate file exists and is valid.
   */
  fun isCertValid(fileName: String): Boolean = certValidationCache[fileName] ?: false

  /**
   * Returns all certificate file names from config (global + domain-specific).
   */
  fun getAllCertFileNames(): Set<String> {
    val domainCerts = certsByDomain.values.flatten()
    return (certs + domainCerts).toSet()
  }

  private fun parseCertsByDomainFromConfig(config: com.getcapacitor.PluginConfig): Map<String, List<String>> {
    val jsonObject = config.getObject("certsByDomain") as? org.json.JSONObject
    if (jsonObject == null) return emptyMap()

    val result = mutableMapOf<String, List<String>>()
    val keys = jsonObject.keys()

    while (keys.hasNext()) {
      val key = keys.next()
      val jsonArray = jsonObject.getJSONArray(key)
      val certList = mutableListOf<String>()
      for (i in 0 until jsonArray.length()) {
        val cert = jsonArray.getString(i)
        if (cert.isNotBlank()) {
          certList.add(cert)
        }
      }
      if (certList.isNotEmpty()) {
        result[key] = certList
      }
    }
    return result
  }

  private fun loadManifest(
    context: android.content.Context,
    manifestPath: String,
  ): Pair<List<String>, Map<String, List<String>>> {
    val assetPath = "certs/$manifestPath"

    return try {
      context.assets.open(assetPath).use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
          val json = reader.readText()
          parseManifestJson(json)
        }
      }
    } catch (e: Exception) {
      SSLPinningLogger.error("Failed to load manifest: $assetPath", e)
      Pair(emptyList(), emptyMap())
    }
  }

  private fun parseManifestJson(json: String): Pair<List<String>, Map<String, List<String>>> =
    try {
      val result = org.json.JSONObject(json)
      val certsList = mutableListOf<String>()
      val certsByDomainMap = mutableMapOf<String, List<String>>()

      // Parse global certs
      if (result.has("certs")) {
        val certsArray = result.getJSONArray("certs")
        for (i in 0 until certsArray.length()) {
          val cert = certsArray.getString(i)
          if (cert.isNotBlank()) {
            certsList.add(cert)
          }
        }
      }

      // Parse certsByDomain
      if (result.has("certsByDomain")) {
        val domainObj = result.getJSONObject("certsByDomain")
        val keys = domainObj.keys()
        while (keys.hasNext()) {
          val domain = keys.next()
          val certArray = domainObj.getJSONArray(domain)
          val certList = mutableListOf<String>()
          for (i in 0 until certArray.length()) {
            val cert = certArray.getString(i)
            if (cert.isNotBlank()) {
              certList.add(cert)
            }
          }
          if (certList.isNotEmpty()) {
            certsByDomainMap[domain] = certList
          }
        }
      }

      Pair(certsList, certsByDomainMap)
    } catch (e: Exception) {
      SSLPinningLogger.error("Failed to parse manifest JSON", e)
      Pair(emptyList(), emptyMap())
    }

  /**
   * Validates all configured certificate files at load time.
   * Returns a map of filename -> isValid.
   */
  private fun validateCertFiles(
    context: android.content.Context,
    globalCerts: List<String>,
    domainCerts: Map<String, List<String>>,
  ): Map<String, Boolean> {
    val result = mutableMapOf<String, Boolean>()
    val allCerts = (globalCerts + domainCerts.values.flatten()).distinct()

    for (certFile in allCerts) {
      val isValid = isCertFileValid(context, certFile)
      result[certFile] = isValid
      if (!isValid) {
        SSLPinningLogger.error("Certificate validation failed: $certFile")
      }
    }

    return result
  }

  /**
   * Checks if a certificate file exists and can be parsed.
   */
  private fun isCertFileValid(
    context: android.content.Context,
    fileName: String,
  ): Boolean {
    val assetPath = "certs/$fileName"
    return try {
      context.assets.open(assetPath).use { inputStream ->
        val certFactory =
          java.security.cert.CertificateFactory
            .getInstance("X.509")
        val cert = certFactory.generateCertificate(inputStream)
        cert is java.security.cert.X509Certificate
      }
    } catch (e: Exception) {
      false
    }
  }
}
