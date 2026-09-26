package io.capkit.redsys

import android.app.Activity
import com.redsys.tpvvinapplibrary.IPaymentResult
import com.redsys.tpvvinapplibrary.TPVV
import com.redsys.tpvvinapplibrary.TPVVConfiguration
import com.redsys.tpvvinapplibrary.TPVVConstants
import com.redsys.tpvvinapplibrary.UIDirectPaymentConfig
import io.capkit.redsys.utils.RedsysLogger
import org.json.JSONObject

/**
 * Redsys Native Implementation (Android)
 *
 * This class represents the Implementation Layer of the plugin.
 * It contains pure Android logic and orchestrates the TPVV SDK.
 *
 * Architectural rules:
 * - MUST NOT reference PluginCall
 * - MUST NOT depend on Capacitor bridge APIs
 * - MUST execute UI operations on the main thread
 */
class RedsysImpl(
  private val activity: Activity,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached immutable plugin configuration.
   * Injected once during plugin load.
   */
  private lateinit var config: RedsysConfig

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies configuration to the native SDK.
   *
   * This method must be called once during plugin initialization.
   */
  fun updateConfig(newConfig: RedsysConfig) {
    this.config = newConfig
    RedsysLogger.verbose = newConfig.verboseLogging
    RedsysLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )

    // -----------------------------------------------------------------------
    // Global SDK Configuration
    // -----------------------------------------------------------------------

    TPVVConfiguration.setLicense(config.license)
    TPVVConfiguration.setFuc(config.fuc)
    TPVVConfiguration.setTerminal(config.terminal)
    TPVVConfiguration.setCurrency(config.currency)

    // Apply optional merchant configuration
    config.merchantName?.let { TPVVConfiguration.setMerchantName(it) }
    config.merchantUrl?.let { TPVVConfiguration.setMerchantUrl(it) }
    config.titular?.let { TPVVConfiguration.setTitular(it) }

    val env =
      when (config.environment.lowercase()) {
        "real" -> TPVVConstants.ENVIRONMENT_REAL
        "test" -> TPVVConstants.ENVIRONMENT_TEST
        else -> TPVVConstants.ENVIRONMENT_INTEGRATION
      }
    TPVVConfiguration.setEnvironment(env)

    // Apply WebView behavior flags
    TPVVConfiguration.setEnableRedirection(config.enableRedirection)
    TPVVConfiguration.setEnableResultAlert(config.enableResultAlert)

    // Set global language for WebView flows
    config.merchantConsumerLanguage?.let { langCode: String ->
      // Ensure the language code is passed as defined in RedsysLanguage enum
      TPVVConfiguration.setLanguage(langCode)
    }

    // Optional Result Alert Customization (Android SDK 2.4.5)
    if (config.enableResultAlert) {
      // Use configured strings from UI options or fallback to defaults
      TPVVConfiguration.setResultAlertTextOk(
        config.uiResultAlertTextOk ?: "The operation has been completed successfully.",
      )
      TPVVConfiguration.setResultAlertTextKo(
        config.uiResultAlertTextKo ?: "An error occurred while processing the operation.",
      )
      TPVVConfiguration.setResultAlertTextButtonOk(config.uiResultAlertButtonTextOk ?: "Continue")
      TPVVConfiguration.setResultAlertTextButtonKo(config.uiResultAlertButtonTextKo ?: "Continue")
    }

    // Reset Direct Payment UI static state before applying new values
    UIDirectPaymentConfig.setTopBarColor(null)
    UIDirectPaymentConfig.setBackgroundColor(null)
    UIDirectPaymentConfig.setProgressBarColor(null)
    UIDirectPaymentConfig.setBtnText(null)

    // Apply Direct Payment UI customization using static SDK configuration
    val uiConfig = UIDirectPaymentConfig()

    // Apply granular UI settings found in UIDirectPaymentConfig.class
    config.uiCardHeaderBgColor?.let { UIDirectPaymentConfig.setCardHeadTextBackgroundColor(it) }
    config.uiCardHeaderText?.let { UIDirectPaymentConfig.setCardHeadText(it) }
    config.uiLabelTextColor?.let {
      UIDirectPaymentConfig.setCardHeadTextColor(it)
      UIDirectPaymentConfig.setInfoTextColor(it)
    }

    // Global progress bar color
    config.uiProgressBarColor?.let { TPVVConfiguration.setProgressBarColor(it) }

    TPVVConfiguration.setUiDirectPaymentConfig(uiConfig)
  }

  // ---------------------------------------------------------------------------
  // Direct Payment
  // ---------------------------------------------------------------------------

  /**
   * Executes the direct payment flow.
   * Resets and applies UI static configuration for each transaction to emulate instance behavior.
   */
  fun doDirectPayment(
    order: String,
    amount: Double,
    type: String,
    id: String?,
    desc: String?,
    extra: HashMap<String, String>?,
    uiOptions: JSONObject?,
    callback: IPaymentResult,
  ) {
    activity.runOnUiThread {
      // 1. Reset static SDK UI state to defaults
      UIDirectPaymentConfig.setTopBarColor(null)
      UIDirectPaymentConfig.setBackgroundColor(null)
      UIDirectPaymentConfig.setProgressBarColor(null)
      UIDirectPaymentConfig.setBtnText(null)
      UIDirectPaymentConfig.setCardHeadTextBackgroundColor(null)

      // 2. Apply runtime UI overrides from JavaScript if present
      uiOptions?.let { ui ->
        ui.getString("backgroundColor")?.let { UIDirectPaymentConfig.setBackgroundColor(it) }
        ui.getString("confirmButtonText")?.let { UIDirectPaymentConfig.setBtnText(it) }
        ui.getString("androidTopBarColor")?.let { UIDirectPaymentConfig.setTopBarColor(it) }
        ui.getString("cardHeaderBackgroundColor")?.let { UIDirectPaymentConfig.setCardHeadTextBackgroundColor(it) }
      }

      TPVV.doDirectPayment(activity, order, amount, type, id, desc, extra, callback)
    }
  }

  // ---------------------------------------------------------------------------
  // Web Payment Initialization (Phase 1)
  // ---------------------------------------------------------------------------

  /**
   * Initializes the WebView payment flow.
   * Note: The SDK method name contains a typo 'inicializeWebViewPayment'.
   */
  fun initializeWebPayment(
    order: String,
    amount: Double,
    type: String,
    id: String?,
    desc: String?,
    extra: HashMap<String, String>?,
    onResult: (String) -> Unit,
    onError: (RedsysError) -> Unit,
  ) {
    activity.runOnUiThread {
      try {
        // We use the mandatory 'inicializeWebViewPayment' name from the SDK binary.
        TPVV.inicializeWebViewPayment(
          activity,
          order,
          amount,
          type,
          id ?: "", // Ensure non-null string for identifier
          desc ?: "",
          extra,
          object : com.redsys.tpvvinapplibrary.webviewPayment.IPaymentWVResult {
            override fun paymentResultOK(base64Data: String?) {
              if (base64Data != null) {
                onResult(base64Data)
              } else {
                onError(RedsysError.InitFailed("Base64 data is null"))
              }
            }

            override fun paymentResultKO(error: com.redsys.tpvvinapplibrary.ErrorResponse?) {
              onError(RedsysError.InitFailed(error?.desc ?: "Web payment initialization failed"))
            }
          },
        )
      } catch (e: Exception) {
        onError(RedsysError.InitFailed("Exception during WebView initialization: ${e.message}"))
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Web Payment Execution (Phase 2)
  // ---------------------------------------------------------------------------

  /**
   * Executes the WebView payment flow.
   *
   * The SDK handles Activity presentation internally.
   */
  fun processWebPayment(
    signature: String,
    callback: IPaymentResult,
  ) {
    activity.runOnUiThread {
      TPVV.doWebPayment(signature, activity, callback)
    }
  }
}
