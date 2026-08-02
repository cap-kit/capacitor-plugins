package io.capkit.tlsfingerprint

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import io.capkit.tlsfingerprint.config.TLSFingerprintConfig
import io.capkit.tlsfingerprint.error.TLSFingerprintError
import io.capkit.tlsfingerprint.error.TLSFingerprintErrorMessages
import io.capkit.tlsfingerprint.logger.TLSFingerprintLogger
import io.capkit.tlsfingerprint.model.TLSFingerprintResultModel
import io.capkit.tlsfingerprint.utils.TLSFingerprintUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Capacitor bridge for the TLSFingerprint plugin (Android).
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Call the native implementation
 * - Resolve or reject PluginCall
 * - Map native errors to JS-facing error codes
 */
@CapacitorPlugin(
  name = "TLSFingerprint",
  permissions = [
    Permission(
      alias = "network",
      strings = [android.Manifest.permission.INTERNET],
    ),
  ],
)
class TLSFingerprintPlugin : Plugin() {
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  /**
   * Immutable plugin configuration read from capacitor.config.ts.
   * * CONTRACT:
   * - Initialized exactly once in `load()`.
   * - Treated as read-only afterwards.
   */
  private lateinit var config: TLSFingerprintConfig

  /**
   * Native implementation layer containing core Android logic.
   *
   * CONTRACT:
   * - Owned by the Plugin layer.
   * - MUST NOT access PluginCall or Capacitor bridge APIs directly.
   */
  private lateinit var implementation: TLSFingerprintImpl

  /**
   * Serializer instance configured to encode model objects into JSObject string payloads.
   */
  private val json = Json

  // -----------------------------------------------------------------------------
  // Companion Object
  // -----------------------------------------------------------------------------

  private companion object {
    /**
     * Account type identifier for internal plugin identification.
     */
    const val ACCOUNT_TYPE = "io.capkit.tlsfingerprint"

    /**
     * Human-readable account name for the plugin.
     */
    const val ACCOUNT_NAME = "TLSFingerprint"
  }

  // -----------------------------------------------------------------------------
  // Lifecycle
  // -----------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This method initializes the configuration container and the native
   * implementation layer, ensuring all dependencies are injected.
   */
  override fun load() {
    super.load()

    config = TLSFingerprintConfig(this)
    implementation = TLSFingerprintImpl(context)
    implementation.updateConfig(config)

    TLSFingerprintLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // -----------------------------------------------------------------------------
  // Helper Methods for Serialization
  // -----------------------------------------------------------------------------

  /**
   * Converts a serializable model directly into a Capacitor JSObject.
   */
  private inline fun <reified T> toJSObject(value: T): JSObject {
    val jsonString = json.encodeToString(value)
    return JSObject(jsonString)
  }

  // -----------------------------------------------------------------------------
  // Error Mapping
  // -----------------------------------------------------------------------------

  /**
   * Rejects the call with a message and a standardized error code.
   * Ensure consistency with the JS TLSFingerprintErrorCode enum.
   */
  private fun reject(
    call: PluginCall,
    error: TLSFingerprintError,
  ) {
    val code =
      when (error) {
        is TLSFingerprintError.Unavailable -> "UNAVAILABLE"
        is TLSFingerprintError.Cancelled -> "CANCELLED"
        is TLSFingerprintError.PermissionDenied -> "PERMISSION_DENIED"
        is TLSFingerprintError.InitFailed -> "INIT_FAILED"
        is TLSFingerprintError.InvalidInput -> "INVALID_INPUT"
        is TLSFingerprintError.UnknownType -> "UNKNOWN_TYPE"
        is TLSFingerprintError.NotFound -> "NOT_FOUND"
        is TLSFingerprintError.Conflict -> "CONFLICT"
        is TLSFingerprintError.Timeout -> "TIMEOUT"
        is TLSFingerprintError.NetworkError -> "NETWORK_ERROR"
        is TLSFingerprintError.InvalidConfig -> "INVALID_INPUT"
        is TLSFingerprintError.SslError -> "SSL_ERROR"
      }

    val message = error.errorMessage.ifBlank { "Unknown native error" }
    call.reject(message, code)
  }

  // -----------------------------------------------------------------------------
  // SSL Pinning (single fingerprint)
  // -----------------------------------------------------------------------------

  /**
   * Validates the SSL certificate of a HTTPS endpoint
   * using a single fingerprint.
   */
  @PluginMethod
  fun checkCertificate(call: PluginCall) {
    val url: String? = call.getString("url")
    val fingerprint: String? = call.getString("fingerprint")

    if (url.isNullOrBlank()) {
      call.reject(TLSFingerprintErrorMessages.URL_REQUIRED, "INVALID_INPUT")
      return
    }

    val parsedUrl =
      try {
        java.net.URL(url)
      } catch (e: java.net.MalformedURLException) {
        call.reject(TLSFingerprintErrorMessages.INVALID_URL, "INVALID_INPUT")
        return
      }

    if (parsedUrl.host.isNullOrBlank()) {
      call.reject(TLSFingerprintErrorMessages.NO_HOST_FOUND_IN_URL, "INVALID_INPUT")
      return
    }

    if (fingerprint != null && !TLSFingerprintUtils.isValidFingerprintFormat(fingerprint)) {
      call.reject(TLSFingerprintErrorMessages.INVALID_FINGERPRINT_FORMAT, "INVALID_INPUT")
      return
    }

    try {
      execute {
        try {
          val result: TLSFingerprintResultModel =
            implementation.checkCertificate(
              urlString = url,
              fingerprintFromArgs = fingerprint,
            )

          call.resolve(toJSObject(result))
        } catch (error: TLSFingerprintError) {
          reject(call, error)
        } catch (error: IllegalArgumentException) {
          call.reject(
            error.message ?: "Invalid input",
            "INVALID_INPUT",
          )
        } catch (error: SSLException) {
          reject(call, TLSFingerprintError.SslError(error.message ?: "SSL/TLS error"))
        } catch (error: UnknownHostException) {
          call.reject(
            error.message ?: "Unknown host",
            "NETWORK_ERROR",
          )
        } catch (error: Exception) {
          call.reject(
            error.message ?: TLSFingerprintErrorMessages.INTERNAL_ERROR,
            "INIT_FAILED",
          )
        }
      }
    } catch (error: IllegalArgumentException) {
      call.reject(
        error.message ?: "Invalid input",
        "INVALID_INPUT",
      )
    } catch (error: Exception) {
      call.reject(
        error.message ?: TLSFingerprintErrorMessages.INTERNAL_ERROR,
        "INIT_FAILED",
      )
    }
  }

  // -----------------------------------------------------------------------------
  // SSL Pinning (multiple fingerprints)
  // -----------------------------------------------------------------------------

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
      jsArray
        ?.toList<Any>()
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?.takeIf { it.isNotEmpty() }

    if (url.isNullOrBlank()) {
      call.reject(TLSFingerprintErrorMessages.URL_REQUIRED, "INVALID_INPUT")
      return
    }

    val parsedUrl =
      try {
        java.net.URL(url)
      } catch (e: java.net.MalformedURLException) {
        call.reject(TLSFingerprintErrorMessages.INVALID_URL, "INVALID_INPUT")
        return
      }

    if (parsedUrl.host.isNullOrBlank()) {
      call.reject(TLSFingerprintErrorMessages.NO_HOST_FOUND_IN_URL, "INVALID_INPUT")
      return
    }

    fingerprints?.forEach { fp ->
      if (!TLSFingerprintUtils.isValidFingerprintFormat(fp)) {
        call.reject(TLSFingerprintErrorMessages.INVALID_FINGERPRINT_FORMAT, "INVALID_INPUT")
        return
      }
    }

    try {
      execute {
        try {
          val result: TLSFingerprintResultModel =
            implementation.checkCertificates(
              urlString = url,
              fingerprintsFromArgs = fingerprints,
            )

          call.resolve(toJSObject(result))
        } catch (error: TLSFingerprintError) {
          reject(call, error)
        } catch (error: IllegalArgumentException) {
          call.reject(
            error.message ?: "Invalid input",
            "INVALID_INPUT",
          )
        } catch (error: SSLException) {
          reject(call, TLSFingerprintError.SslError(error.message ?: "SSL/TLS error"))
        } catch (error: UnknownHostException) {
          call.reject(
            error.message ?: "Unknown host",
            "NETWORK_ERROR",
          )
        } catch (error: Exception) {
          call.reject(
            error.message ?: TLSFingerprintErrorMessages.INTERNAL_ERROR,
            "INIT_FAILED",
          )
        }
      }
    } catch (error: IllegalArgumentException) {
      call.reject(
        error.message ?: "Invalid input",
        "INVALID_INPUT",
      )
    } catch (error: Exception) {
      call.reject(
        error.message ?: TLSFingerprintErrorMessages.INTERNAL_ERROR,
        "INIT_FAILED",
      )
    }
  }

  // -----------------------------------------------------------------------------
  // Version
  // -----------------------------------------------------------------------------

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
