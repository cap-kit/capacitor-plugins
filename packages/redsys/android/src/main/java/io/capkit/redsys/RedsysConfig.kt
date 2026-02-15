package io.capkit.redsys

import android.content.Context
import com.getcapacitor.Plugin

/**
 * Redsys Plugin Configuration (Android)
 *
 * Immutable configuration container populated from `capacitor.config.ts`
 * under the `plugins.Redsys` namespace.
 *
 * Architectural rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime configuration
 * - Consumed exclusively by native layers
 * - Never accessed directly from JavaScript
 */
class RedsysConfig(
  plugin: Plugin,
) {
  // ---------------------------------------------------------------------------
  // Core Context
  // ---------------------------------------------------------------------------

  /**
   * Android application context.
   * Exposed for internal components that require system services.
   */
  val context: Context = plugin.context

  // ---------------------------------------------------------------------------
  // Core Configuration
  // ---------------------------------------------------------------------------

  /**
   * Enables verbose logging for native diagnostics.
   */
  val verboseLogging: Boolean

  /**
   * Redsys SDK license key.
   */
  val license: String

  /**
   * Target environment ("Integration", "Test", "Real").
   */
  val environment: String

  /**
   * Merchant FUC identifier.
   */
  val fuc: String

  /**
   * Terminal identifier.
   */
  val terminal: String

  /**
   * Currency numeric code (e.g., "978" for EUR).
   */
  val currency: String

  // ---------------------------------------------------------------------------
  // Merchant Metadata
  // ---------------------------------------------------------------------------

  val merchantName: String?
  val merchantUrl: String?
  val titular: String?
  val merchantConsumerLanguage: String?

  /** Optional global merchant signature. */
  val signature: String?

  // ---------------------------------------------------------------------------
  // Flow Flags
  // ---------------------------------------------------------------------------

  /**
   * Enables automatic redirection after WebView payment.
   */
  val enableRedirection: Boolean

  /**
   * Enables SDK result alert dialog.
   */
  val enableResultAlert: Boolean

  // ---------------------------------------------------------------------------
  // UI Customization (Optional)
  // ---------------------------------------------------------------------------

  val uiLogo: String?
  val uiBackgroundColor: String?
  val uiProgressBarColor: String?
  val uiTopBarColor: String?
  val uiConfirmButtonText: String?
  val uiLabelTextColor: String?
  val uiCardHeaderBgColor: String?
  val uiCardHeaderText: String?
  val uiResultAlertTextOk: String?
  val uiResultAlertTextKo: String?
  val uiResultAlertButtonTextOk: String?
  val uiResultAlertButtonTextKo: String?

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {

    val config = plugin.getConfig()

    // Core flags
    verboseLogging = config.getBoolean("verboseLogging", false)

    // Base configuration
    license = config.getString("license", "")
    environment = config.getString("environment", "Integration")
    fuc = config.getString("fuc", "")
    terminal = config.getString("terminal", "1")
    currency = config.getString("currency", "978")

    // Merchant metadata
    merchantName = config.getString("merchantName")
    merchantUrl = config.getString("merchantUrl")
    titular = config.getString("titular")
    merchantConsumerLanguage = config.getString("merchantConsumerLanguage")
    signature = config.getString("signature")

    // Flow behavior
    enableRedirection = config.getBoolean("enableRedirection", true)
    enableResultAlert = config.getBoolean("enableResultAlert", false)

    // Optional nested UI configuration
    val ui = config.getObject("ui")

    uiLogo = ui?.getString("logo")
    uiBackgroundColor = ui?.getString("backgroundColor")
    uiProgressBarColor = ui?.getString("androidProgressBarColor")
    uiTopBarColor = ui?.getString("androidTopBarColor")
    uiConfirmButtonText = ui?.getString("confirmButtonText")
    uiLabelTextColor = ui?.getString("labelTextColor")
    uiCardHeaderBgColor = ui?.getString("cardHeaderBackgroundColor")
    uiCardHeaderText = ui?.getString("androidCardHeaderText")
    uiResultAlertTextOk = ui?.getString("androidResultAlertTextOk")
    uiResultAlertTextKo = ui?.getString("androidResultAlertTextKo")
    uiResultAlertButtonTextOk = ui?.getString("androidResultAlertButtonTextOk")
    uiResultAlertButtonTextKo = ui?.getString("androidResultAlertButtonTextKo")
  }
}
