import Foundation
import TPVVInLibrary

/**
 * Redsys Native Implementation (iOS)
 *
 * This class represents the Implementation Layer of the plugin.
 * It contains platform-specific logic and orchestrates the TPVVInLibrary SDK.
 *
 * Architectural rules:
 * - MUST NOT access CAPPluginCall
 * - MUST NOT depend on Capacitor APIs
 * - MUST return platform-agnostic results to the Bridge layer
 */
@objc public final class RedsysImpl: NSObject {

    // MARK: - Properties

    /// Cached immutable configuration injected from the Bridge layer.
    private var config: RedsysConfig?

    // MARK: - Initialization

    /**
     * Initializes the implementation instance.
     */
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     Applies plugin configuration and initializes the TPVV SDK.

     This method must be called exactly once during plugin load.
     */
    func applyConfig(_ config: RedsysConfig) {

        // Ensure configuration is applied only once.
        precondition(
            self.config == nil,
            "RedsysImpl.applyConfig(_:) must be called exactly once"
        )
        self.config = config

        // Synchronize logger state
        RedsysLogger.verbose = config.verboseLogging
        RedsysLogger.debug("Configuration applied. Verbose logging:", config.verboseLogging)

        // MARK: - SDK Global Configuration

        TPVVConfiguration.shared.appLicense = config.license
        TPVVConfiguration.shared.appFuc = config.fuc
        TPVVConfiguration.shared.appTerminal = config.terminal
        TPVVConfiguration.shared.appCurrency = config.currency

        // Apply optional merchant configuration
        if let titular = config.titular {
            TPVVConfiguration.shared.appMerchantTitular = titular
        }

        if let merchantName = config.merchantName {
            TPVVConfiguration.shared.appMerchantName = merchantName
        }

        if let merchantUrl = config.merchantUrl {
            TPVVConfiguration.shared.appMerchantURL = merchantUrl
        }

        // Configure redirection URLs for 3DS flow
        // Note: If set, the user must manually tap 'Back' to close the WebView
        if let urlOK = config.merchantUrl { // Usually merchantUrl acts as base
            TPVVConfiguration.shared.appURLOK = urlOK
        }
        if let urlKO = config.merchantUrl {
            TPVVConfiguration.shared.appURLKO = urlKO
        }

        // These fields may not always be present but are supported by SDK
        if let merchantDescription = config.merchantDescription {
            TPVVConfiguration.shared.appMerchantDescription = merchantDescription
        }

        if let merchantData = config.merchantData {
            TPVVConfiguration.shared.appMerchantData = merchantData
        }

        if let merchantGroup = config.merchantGroup {
            TPVVConfiguration.shared.appMerchantGroup = merchantGroup
        }

        // MARK: - Environment Mapping

        switch config.environment.lowercased() {
        case "real": TPVVConfiguration.shared.appEnviroment = .Real
        case "test": TPVVConfiguration.shared.appEnviroment = .Test
        default: TPVVConfiguration.shared.appEnviroment = .Integration
        }

        RedsysLogger.debug("RedsysImpl: SDK initialized in \(TPVVConfiguration.shared.appEnviroment) mode.")

        // Apply WebView payment methods
        if let methods = config.paymentMethods {
            TPVVConfiguration.shared.appMerchantPayMethods =
                RedsysUtils.mapPaymentMethods(methods).first ?? PaymentMethod.card
        }

        // Apply WebView language
        // Note: This setting affects only the WebView/3DS flow.
        if let language = config.merchantConsumerLanguage {
            TPVVConfiguration.shared.appMerchantConsumerLanguage = language
        }
    }

    // MARK: - Web Payment (Phase 1: Initialization)

    /**
     * Initializes the WebView payment flow on iOS.
     */
    @objc func initializeWebPayment(
        order: String,
        amount: Double, // Using Double as per swiftinterface inspection
        type: TransactionType,
        description: String?,
        identifier: String?,
        extraParams: [String: String]?,
        completion: @escaping (String?, Error?) -> Void
    ) {
        // SDK_INAPP.inicializeWebPayment is the official method in the framework.
        SDK_INAPP.inicializeWebPayment(
            orderNumber: order,
            amount: amount,
            productDescription: description ?? "",
            transactionType: type,
            identifier: identifier ?? "",
            extraParams: extraParams ?? [:],
            success: { base64String in
                completion(base64String, nil)
            },
            failure: { error in
                completion(nil, RedsysError.initFailed(error.desc))
            }
        )
    }

    // MARK: - Web Payment (Phase 2: Execution)

    /**
     Executes the WebView payment flow (Step 2).

     Presents the WebView controller and assigns the delegate
     for asynchronous result handling.
     */
    @objc func processWebPayment(
        signature: String,
        signatureVersion: String?,
        delegate: WebViewPaymentResponseDelegate
    ) {
        DispatchQueue.main.async {
            let webVC = SDK_INAPP.doWebViewPayment(
                signature: signature,
                signatureVersion: signatureVersion ?? "HMAC_SHA256_V1"
            )
            webVC.delegate = delegate

            // Retrieve active scene rootViewController in a scene-safe way
            if let windowScene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first,
               let rootVC = windowScene.windows
                .first(where: { $0.isKeyWindow })?
                .rootViewController {

                rootVC.present(webVC, animated: true)
            }
        }
    }

    // MARK: - Direct Payment

    /**
     Executes the direct payment flow.

     Presents the native DirectPaymentViewController and
     assigns the delegate for asynchronous result handling.
     */
    @objc func executeDirectPayment(
        order: String,
        amount: Double,
        type: TransactionType,
        description: String?,
        identifier: String?,
        extraParams: [String: String]?,
        uiOptions: [String: Any]?,
        delegate: DirectPaymentResponseDelegate
    ) {
        DispatchQueue.main.async {
            let uiConfig = TPVInAppUIConfig()

            // Retrieve label color from config or default to black
            let labelColor = RedsysUtils.colorFromHex(self.config?.labelTextColor ?? "#000000") //
            let labelFont = UIFont.systemFont(ofSize: 16)

            // Background & Buttons using _ to ignore the returned value from fluent API
            if let bgColorHex = self.config?.uiBackgroundColor {
                _ = uiConfig.setBackgorundViewColor(color: RedsysUtils.colorFromHex(bgColorHex))
            }

            // Apply Runtime Overrides from uiOptions (JS Call)
            if let ui = uiOptions {
                if let bgColor = ui["backgroundColor"] as? String {
                    _ = uiConfig.setBackgorundViewColor(color: RedsysUtils.colorFromHex(bgColor))
                }
                if let btnText = ui["confirmButtonText"] as? String {
                    _ = uiConfig.setContinueButtonText(btnText)
                }
            }

            if let btnText = self.config?.uiConfirmButtonText { _ = uiConfig.setContinueButtonText(btnText) }
            if let cancelText = self.config?.uiCancelButtonText { _ = uiConfig.setCancelButtonText(cancelText) }

            // Labels customization
            if let cardLabelText = self.config?.cardNumberLabel {
                let labelCfg = PaymentUILabel.create(text: cardLabelText, textColor: labelColor, textFont: labelFont)
                _ = uiConfig.setNumberCardLabel(labelCfg)
            }
            if let expiryLabelText = self.config?.expirationLabel {
                let labelCfg = PaymentUILabel.create(text: expiryLabelText, textColor: labelColor, textFont: labelFont)
                _ = uiConfig.setExpireDateLabel(labelCfg)
            }
            if let cvvLabelText = self.config?.cvvLabel {
                let labelCfg = PaymentUILabel.create(text: cvvLabelText, textColor: labelColor, textFont: labelFont)
                _ = uiConfig.setCVVLabel(labelCfg)
            }

            // Logo from assets
            if let logoName = self.config?.uiLogo, let logoImage = UIImage(named: logoName) {
                _ = uiConfig.setLogo(logo: logoImage)
            }

            let paymentVC = DirectPaymentViewController(
                orderNumber: order,
                amount: amount,
                productDescription: description ?? "",
                transactionType: type,
                identifier: identifier ?? "",
                extraParams: extraParams ?? [:],
                uiViewConfig: uiConfig
            )

            paymentVC.delegate = delegate

            // Retrieve active scene rootViewController in a scene-safe way
            if let windowScene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first,
               let rootVC = windowScene.windows
                .first(where: { $0.isKeyWindow })?
                .rootViewController {

                rootVC.present(paymentVC, animated: true)
            }
        }
    }
}
