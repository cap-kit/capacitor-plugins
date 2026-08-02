package io.capkit.redsys

import android.content.Context
import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 *
 * The keys mirror the TypeScript `RedsysConfig` interface
 * (packages/redsys/src/definitions.ts).
 */
@Serializable
private data class RawConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("signature")
  val signature: String? = null,
  @SerialName("license")
  val license: String = "",
  @SerialName("environment")
  val environment: String = "Integration",
  @SerialName("fuc")
  val fuc: String = "",
  @SerialName("terminal")
  val terminal: String = "1",
  @SerialName("currency")
  val currency: String = "978",
  @SerialName("merchantName")
  val merchantName: String? = null,
  @SerialName("merchantUrl")
  val merchantUrl: String? = null,
  @SerialName("titular")
  val titular: String? = null,
  @SerialName("merchantConsumerLanguage")
  val merchantConsumerLanguage: String? = null,
  @SerialName("enableRedirection")
  val enableRedirection: Boolean = true,
  @SerialName("enableResultAlert")
  val enableResultAlert: Boolean = false,
  @SerialName("ui")
  val ui: RawUIOptions? = null,
)

@Serializable
private data class RawUIOptions(
  @SerialName("logo")
  val logo: String? = null,
  @SerialName("backgroundColor")
  val backgroundColor: String? = null,
  @SerialName("androidProgressBarColor")
  val androidProgressBarColor: String? = null,
  @SerialName("androidTopBarColor")
  val androidTopBarColor: String? = null,
  @SerialName("confirmButtonText")
  val confirmButtonText: String? = null,
  @SerialName("labelTextColor")
  val labelTextColor: String? = null,
  @SerialName("cardHeaderBackgroundColor")
  val cardHeaderBackgroundColor: String? = null,
  @SerialName("androidCardHeaderText")
  val androidCardHeaderText: String? = null,
  @SerialName("androidResultAlertTextOk")
  val androidResultAlertTextOk: String? = null,
  @SerialName("androidResultAlertTextKo")
  val androidResultAlertTextKo: String? = null,
  @SerialName("androidResultAlertButtonTextOk")
  val androidResultAlertButtonTextOk: String? = null,
  @SerialName("androidResultAlertButtonTextKo")
  val androidResultAlertButtonTextKo: String? = null,
)

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
    val configObject = plugin.config.getConfigJSON()
    val parsedConfig = parseConfig(configObject?.toString())

    verboseLogging = parsedConfig.verboseLogging
    license = parsedConfig.license
    environment = parsedConfig.environment
    fuc = parsedConfig.fuc
    terminal = parsedConfig.terminal
    currency = parsedConfig.currency

    // Sanitize optional strings: treat blank strings as null
    merchantName = parsedConfig.merchantName?.takeIf { it.isNotBlank() }
    merchantUrl = parsedConfig.merchantUrl?.takeIf { it.isNotBlank() }
    titular = parsedConfig.titular?.takeIf { it.isNotBlank() }
    merchantConsumerLanguage = parsedConfig.merchantConsumerLanguage?.takeIf { it.isNotBlank() }
    signature = parsedConfig.signature?.takeIf { it.isNotBlank() }

    enableRedirection = parsedConfig.enableRedirection
    enableResultAlert = parsedConfig.enableResultAlert

    // Map nested UI configuration with sanitization
    val ui = parsedConfig.ui
    uiLogo = ui?.logo?.takeIf { it.isNotBlank() }
    uiBackgroundColor = ui?.backgroundColor?.takeIf { it.isNotBlank() }
    uiProgressBarColor = ui?.androidProgressBarColor?.takeIf { it.isNotBlank() }
    uiTopBarColor = ui?.androidTopBarColor?.takeIf { it.isNotBlank() }
    uiConfirmButtonText = ui?.confirmButtonText?.takeIf { it.isNotBlank() }
    uiLabelTextColor = ui?.labelTextColor?.takeIf { it.isNotBlank() }
    uiCardHeaderBgColor = ui?.cardHeaderBackgroundColor?.takeIf { it.isNotBlank() }
    uiCardHeaderText = ui?.androidCardHeaderText?.takeIf { it.isNotBlank() }
    uiResultAlertTextOk = ui?.androidResultAlertTextOk?.takeIf { it.isNotBlank() }
    uiResultAlertTextKo = ui?.androidResultAlertTextKo?.takeIf { it.isNotBlank() }
    uiResultAlertButtonTextOk = ui?.androidResultAlertButtonTextOk?.takeIf { it.isNotBlank() }
    uiResultAlertButtonTextKo = ui?.androidResultAlertButtonTextKo?.takeIf { it.isNotBlank() }
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(jsonString: String?): RawConfigData {
      if (jsonString.isNullOrBlank()) return RawConfigData()
      return try {
        json.decodeFromString<RawConfigData>(jsonString)
      } catch (_: Exception) {
        RawConfigData()
      }
    }
  }
}