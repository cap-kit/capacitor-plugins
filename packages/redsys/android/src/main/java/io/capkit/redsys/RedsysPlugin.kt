package io.capkit.redsys

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.redsys.tpvvinapplibrary.ErrorResponse
import com.redsys.tpvvinapplibrary.IPaymentResult
import com.redsys.tpvvinapplibrary.ResultResponse
import io.capkit.redsys.utils.RedsysLogger
import io.capkit.redsys.utils.RedsysUtils

/**
 * Redsys Capacitor Plugin (Android Bridge Layer)
 *
 * Responsibilities:
 * - Validate incoming JavaScript parameters
 * - Delegate business logic to RedsysImpl
 * - Convert native results into JS-compatible objects
 * - Map native errors into standardized JS error codes
 *
 * Business logic MUST NOT live in this class.
 */
@CapacitorPlugin(
  name = "Redsys",
  permissions = [
    Permission(
      alias = "network",
      strings = [android.Manifest.permission.INTERNET],
    ),
  ],
)
class RedsysPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable configuration container loaded from capacitor.config.ts.
   * Initialized once during plugin load.
   */
  private lateinit var config: RedsysConfig

  /**
   * Native implementation layer.
   * Contains SDK interaction and platform-specific logic.
   */
  private lateinit var implementation: RedsysImpl

  // ---------------------------------------------------------------------------
  // Companion Object
  // ---------------------------------------------------------------------------

  private companion object {
    const val ACCOUNT_TYPE = "io.capkit.redsys"
    const val ACCOUNT_NAME = "Redsys"
  }

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read static configuration
   * - initialize native resources
   * - inject configuration into the implementation
   */
  override fun load() {
    super.load()

    config = RedsysConfig(this)
    implementation = RedsysImpl(activity)
    implementation.updateConfig(config)

    RedsysLogger.verbose = config.verboseLogging
    RedsysLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  // ---------------------------------------------------------------------------
  // Error Handling
  // ---------------------------------------------------------------------------

  /**
   * Maps internal RedsysError types to standardized JS error codes.
   */
  private fun reject(
    call: PluginCall,
    error: RedsysError,
  ) {
    val code =
      when (error) {
        is RedsysError.Unavailable -> "UNAVAILABLE"
        is RedsysError.PermissionDenied -> "PERMISSION_DENIED"
        is RedsysError.InitFailed -> "INIT_FAILED"
        is RedsysError.UnknownType -> "UNKNOWN_TYPE"
        is RedsysError.CryptoError -> "CRYPTO_ERROR"
      }

    call.reject(error.message, code)
  }

  /**
   * Converts SDK-level errors into a standardized JS error.
   * Preserves the original Redsys error code.
   */
  private fun rejectWithSdkError(
    call: PluginCall,
    error: ErrorResponse,
  ) {
    val data = JSObject()
    data.put("sdkCode", error.code)

    call.reject(
      error.desc,
      "SDK_ERROR",
      data,
    )
  }

  // ---------------------------------------------------------------------------
  // Direct Payment
  // ---------------------------------------------------------------------------

  /**
   * Starts the direct payment flow.
   * The result is delivered asynchronously via IPaymentResult.
   */
  @PluginMethod
  fun doDirectPayment(call: PluginCall) {
    val order = call.getString("order") ?: return call.reject("order required")
    val amount = call.getDouble("amount") ?: return call.reject("amount required")

    implementation.doDirectPayment(
      order = order,
      amount = amount,
      type = RedsysUtils.mapTransactionType(call.getString("transactionType")),
      id = call.getString("identifier"),
      desc = call.getString("description"),
      extra = RedsysUtils.toHashMap(call.getObject("extraParams")),
      uiOptions = call.getObject("uiOptions"),
      callback =
        object : IPaymentResult {
          override fun paymentResultOK(res: ResultResponse) {
            val ret = RedsysUtils.resultToJSObject(res)
            call.resolve(ret)
          }

          override fun paymentResultKO(err: ErrorResponse) {
            rejectWithSdkError(call, err)
          }
        },
    )
  }

  // ---------------------------------------------------------------------------
  // Web Payment Initialization (Phase 1)
  // ---------------------------------------------------------------------------

  /**
   * Initializes WebView payment flow.
   * Returns Base64 merchant data for signature generation.
   */
  @PluginMethod
  fun initializeWebPayment(call: PluginCall) {
    val order =
      call.getString("order")
        ?: return call.reject("order required")

    val amount =
      call.getDouble("amount")
        ?: return call.reject("amount required")

    implementation.initializeWebPayment(
      order = order,
      amount = amount,
      type = RedsysUtils.mapTransactionType(call.getString("transactionType")),
      id = call.getString("identifier"),
      desc = call.getString("description"),
      extra = RedsysUtils.toHashMap(call.getObject("extraParams")),
      onResult = { base64 ->
        val ret = JSObject()
        ret.put("base64Data", base64)
        call.resolve(ret)
      },
      onError = { error ->
        reject(call, error)
      },
    )
  }

  // ---------------------------------------------------------------------------
  // Web Payment Execution (Phase 2)
  // ---------------------------------------------------------------------------

  /**
   * Executes the WebView payment flow.
   */
  @PluginMethod
  fun processWebPayment(call: PluginCall) {
    // Using the 'signature' property from our RedsysConfig instance
    val signature = call.getString("signature") ?: if (config.signature.isNullOrEmpty()) null else config.signature

    if (signature == null) {
      return call.reject("signature required in call or configuration")
    }

    implementation.processWebPayment(
      signature,
      object : IPaymentResult {
        override fun paymentResultOK(res: ResultResponse) {
          val ret = RedsysUtils.resultToJSObject(res)
          call.resolve(ret)
        }

        override fun paymentResultKO(err: ErrorResponse) {
          rejectWithSdkError(call, err)
        }
      },
    )
  }

  // ---------------------------------------------------------------------------
  // Cryptographic Utilities
  // ---------------------------------------------------------------------------

  /**
   * Computes HMAC signature for WebView payments.
   * WARNING: For production, signatures should be generated on the backend.
   */
  @PluginMethod
  fun computeHash(call: PluginCall) {
    val data = call.getString("data") ?: return call.reject("data required")
    val key = call.getString("keyBase64") ?: return call.reject("keyBase64 required")
    val alg = call.getString("algorithm") ?: "HMAC_SHA256_V1"

    // Redsys requires SHA256 or SHA512 depending on terminal config
    val signature = RedsysUtils.calculateHMAC(data, key, alg)

    if (signature != null) {
      val ret = JSObject()
      ret.put("signature", signature)
      call.resolve(ret)
    } else {
      val error = RedsysError.CryptoError("Hash computation failed. Verify Base64 key format.")
      reject(call, error)
    }
  }

  // ---------------------------------------------------------------------------
  // Version Information
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }
}
