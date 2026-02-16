package io.capkit.sslpinning

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import io.capkit.sslpinning.utils.SSLPinningLogger

/**
 * Capacitor bridge for the SSLPinning plugin (Android).
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Call the native implementation
 * - Resolve or reject PluginCall
 * - Map native errors to JS-facing error codes
 */
@CapacitorPlugin(
  name = "SSLPinning",
  permissions = [
    Permission(
      alias = "network",
      strings = [android.Manifest.permission.INTERNET],
    ),
  ],
)
class SSLPinningPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration read from capacitor.config.ts.
   * * CONTRACT:
   * - Initialized exactly once in `load()`.
   * - Treated as read-only afterwards.
   */
  private lateinit var config: SSLPinningConfig

  /**
   * Native implementation layer containing core Android logic.
   *
   * CONTRACT:
   * - Owned by the Plugin layer.
   * - MUST NOT access PluginCall or Capacitor bridge APIs directly.
   */
  private lateinit var implementation: SSLPinningImpl

  // ---------------------------------------------------------------------------
  // Companion Object
  // ---------------------------------------------------------------------------

  private companion object {
    /**
     * Account type identifier for internal plugin identification.
     */
    const val ACCOUNT_TYPE = "io.capkit.sslpinning"

    /**
     * Human-readable account name for the plugin.
     */
    const val ACCOUNT_NAME = "SSLPinning"
  }

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This method initializes the configuration container and the native
   * implementation layer, ensuring all dependencies are injected.
   */
  override fun load() {
    super.load()

    config = SSLPinningConfig(this)
    implementation = SSLPinningImpl(context)
    implementation.updateConfig(config)

    SSLPinningLogger.verbose = config.verboseLogging
    SSLPinningLogger.debug("Plugin loaded")
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native SSLPinningError values
   * to JavaScript-facing error codes.
   */
  private fun reject(
    call: PluginCall,
    error: SSLPinningError,
  ) {
    val code =
      when (error) {
        is SSLPinningError.Unavailable -> "UNAVAILABLE"
        is SSLPinningError.PermissionDenied -> "PERMISSION_DENIED"
        is SSLPinningError.InitFailed -> "INIT_FAILED"
        is SSLPinningError.UnknownType -> "UNKNOWN_TYPE"
        is SSLPinningError.NoPinningConfig -> "NO_PINNING_CONFIG"
        is SSLPinningError.CertNotFound -> "CERT_NOT_FOUND"
        is SSLPinningError.TrustEvaluationFailed -> "TRUST_EVALUATION_FAILED"
      }

    call.reject(error.message, code)
  }

  // ---------------------------------------------------------------------------
  // SSL Pinning (single fingerprint)
  // ---------------------------------------------------------------------------

  /**
   * Validates the SSL certificate of a HTTPS endpoint
   * using a single fingerprint.
   */
  @PluginMethod
  fun checkCertificate(call: PluginCall) {
    val url: String? = call.getString("url")
    val fingerprint: String? = call.getString("fingerprint")

    if (url.isNullOrBlank()) {
      call.reject("Missing url", "UNKNOWN_TYPE")
      return
    }

    execute {
      try {
        // Calling the implementation layer
        val result: Map<String, Any> =
          implementation.checkCertificate(
            urlString = url,
            fingerprintFromArgs = fingerprint,
          )

        val jsResult = JSObject()
        for ((key, value) in result) {
          jsResult.put(key, value)
        }

        call.resolve(jsResult)
      } catch (error: SSLPinningError) {
        reject(call, error)
      } catch (error: Exception) {
        call.reject(
          error.message ?: "SSL pinning failed",
          "INIT_FAILED",
        )
      }
    }
  }

  // ---------------------------------------------------------------------------
  // SSL Pinning (multiple fingerprints)
  // ---------------------------------------------------------------------------

  /**
   * Validates the SSL certificate of a HTTPS endpoint
   * using multiple allowed fingerprints.
   */
  @PluginMethod
  fun checkCertificates(call: PluginCall) {
    val url: String? = call.getString("url")

    val jsArray: JSArray? = call.getArray("fingerprints")

    // Parsing JSArray to a clean Kotlin List
    val fingerprints: List<String>? =
      if (jsArray != null && jsArray.length() > 0) {
        val list = ArrayList<String>()
        for (i in 0 until jsArray.length()) {
          val value = jsArray.getString(i)
          if (!value.isNullOrBlank()) {
            list.add(value)
          }
        }
        if (list.isNotEmpty()) list else null
      } else {
        null
      }

    if (url.isNullOrBlank()) {
      call.reject("Missing url", "UNKNOWN_TYPE")
      return
    }

    execute {
      try {
        val result: Map<String, Any> =
          implementation.checkCertificates(
            urlString = url,
            fingerprintsFromArgs = fingerprints,
          )

        val jsResult = JSObject()
        for ((key, value) in result) {
          jsResult.put(key, value)
        }

        call.resolve(jsResult)
      } catch (error: SSLPinningError) {
        reject(call, error)
      } catch (error: Exception) {
        call.reject(
          error.message ?: "SSL pinning failed",
          "INIT_FAILED",
        )
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Version
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version.
   *
   * NOTE:
   * - This method is guaranteed not to fail
   * - Therefore it does NOT use TestError
   * - Version is injected at build time from package.json
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }
}
