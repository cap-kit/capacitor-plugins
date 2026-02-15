import Foundation
import Capacitor

/**
 * Redsys Plugin Configuration (iOS)
 *
 * Immutable configuration container populated from `capacitor.config.ts`
 * under the `Redsys` namespace.
 *
 * Architectural rules:
 * - Read once during plugin initialization (`load()`)
 * - Treated as immutable runtime configuration
 * - Consumed exclusively by native layers
 * - Never accessed directly from JavaScript
 */
public struct RedsysConfig {

    // MARK: - Configuration Keys

    /// Centralized configuration key definitions.
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let license = "license"
        static let environment = "environment"
        static let fuc = "fuc"
        static let terminal = "terminal"
        static let currency = "currency"
        static let paymentMethods = "paymentMethods"

        static let merchantName = "merchantName"
        static let merchantUrl = "merchantUrl"
        static let titular = "titular"
        static let merchantDescription = "merchantDescription"
        static let merchantData = "merchantData"
        static let merchantGroup = "merchantGroup"
        static let merchantConsumerLanguage = "merchantConsumerLanguage"
        static let signature = "signature"

        static let ui = "ui"
    }

    // MARK: - Core Configuration

    /// Enables verbose native logging.
    public let verboseLogging: Bool

    /// Redsys SDK license key.
    public let license: String

    /// Target environment ("Integration", "Test", "Real").
    public let environment: String

    /// Merchant FUC (merchant identifier).
    public let fuc: String

    /// Terminal identifier.
    public let terminal: String

    /// Currency numeric code (e.g., "978" for EUR).
    public let currency: String

    /// Allowed payment methods for WebView flow.
    public let paymentMethods: [String]?

    // MARK: - Merchant Metadata

    /// Merchant display name.
    public let merchantName: String?

    /// Merchant return URL.
    public let merchantUrl: String?

    /// Merchant titular name.
    public let titular: String?

    ///
    public let merchantDescription: String?

    ///
    public let merchantData: String?

    ///
    public let merchantGroup: String?

    ///
    public let merchantConsumerLanguage: String?

    /// Optional global merchant signature.
    public let signature: String?

    // MARK: - UI Customization (Optional)

    /// Custom logo asset name.
    let uiLogo: String?

    /// Background color (hex string).
    let uiBackgroundColor: String?

    /// iOS-specific background image asset name.
    let uiBackgroundImage: String?

    /// Custom confirm button text.
    let uiConfirmButtonText: String?

    /// iOS-specific cancel button text.
    let uiCancelButtonText: String?

    // MARK: - UI Label Customization

    public let cardNumberLabel: String?
    public let expirationLabel: String?
    public let cvvLabel: String?
    public let infoLabel: String?
    public let labelTextColor: String?

    // MARK: - Defaults

    private static let defaultVerboseLogging = false
    private static let defaultLicense = ""
    private static let defaultEnvironment = "Integration"
    private static let defaultFuc = ""
    private static let defaultTerminal = "1"
    private static let defaultCurrency = "978"

    // MARK: - Initialization

    /**
     Initializes configuration by reading values from Capacitor.

     - Parameter plugin: CAPPlugin used to access typed configuration via `getConfig()`.
     */
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Core flags
        self.verboseLogging = config.getBoolean(
            Keys.verboseLogging,
            Self.defaultVerboseLogging
        )

        // Basic configuration
        self.license = config.getString(Keys.license) ?? Self.defaultLicense
        self.environment = config.getString(Keys.environment) ?? Self.defaultEnvironment
        self.fuc = config.getString(Keys.fuc) ?? Self.defaultFuc
        self.terminal = config.getString(Keys.terminal) ?? Self.defaultTerminal
        self.currency = config.getString(Keys.currency) ?? Self.defaultCurrency

        self.paymentMethods = config.getArray("paymentMethods") as? [String]

        // Merchant metadata
        self.merchantName = config.getString("merchantName")
        self.merchantUrl = config.getString("merchantUrl")
        self.titular = config.getString("titular")
        self.merchantDescription = config.getString(Keys.merchantDescription)
        self.merchantData = config.getString(Keys.merchantData)
        self.merchantGroup = config.getString(Keys.merchantGroup)
        self.merchantConsumerLanguage = config.getString(Keys.merchantConsumerLanguage)
        self.signature = config.getString(Keys.signature)

        // UI configuration
        let ui = config.getObject(Keys.ui) ?? [:]

        self.uiLogo = ui["logo"] as? String
        self.uiBackgroundColor = ui["backgroundColor"] as? String
        self.uiBackgroundImage = ui["iosBackgroundImage"] as? String
        self.uiConfirmButtonText = ui["confirmButtonText"] as? String
        self.uiCancelButtonText = ui["iosCancelButtonText"] as? String

        // UI Label
        self.cardNumberLabel = ui["cardNumberLabel"] as? String
        self.expirationLabel = ui["expirationLabel"] as? String
        self.cvvLabel = ui["cvvLabel"] as? String
        self.infoLabel = ui["infoLabel"] as? String
        self.labelTextColor = ui["labelTextColor"] as? String
    }
}
