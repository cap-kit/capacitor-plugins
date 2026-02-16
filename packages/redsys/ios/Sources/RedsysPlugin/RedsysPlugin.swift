import Foundation
import Capacitor
import TPVVInLibrary

/**
 * Redsys Capacitor Plugin (iOS Bridge Layer)
 *
 * This class acts as the Bridge Layer between the JavaScript API
 * and the native Redsys iOS SDK.
 *
 * Responsibilities:
 * - Validate incoming JS parameters
 * - Forward calls to the Implementation layer
 * - Correlate SDK delegate callbacks with saved CAPPluginCall instances
 * - Map native errors to standardized JS-facing error codes
 *
 * Business logic MUST NOT live in this layer.
 */
@objc(RedsysPlugin)
public final class RedsysPlugin: CAPPlugin, CAPBridgedPlugin, DirectPaymentResponseDelegate, WebViewPaymentResponseDelegate {

    // MARK: - Properties

    /// Native implementation layer (contains SDK interaction logic).
    private let implementation = RedsysImpl()

    /// Read-only configuration loaded from capacitor.config.ts.
    private var config: RedsysConfig?

    /// Tracks the active direct payment call identifier
    private var activeDirectPaymentCallId: String?

    /// Internal Capacitor identifier.
    public let identifier = "RedsysPlugin"

    /// JavaScript-facing plugin name.
    public let jsName = "Redsys"

    /// Methods exposed to the JavaScript layer.
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "doDirectPayment", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "initializeWebPayment", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "processWebPayment", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "computeHash", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Lifecycle

    /**
     Called once when the plugin is loaded by Capacitor.

     Initializes configuration and injects it into the implementation layer.
     */
    override public func load() {
        // Initialize RedsysConfig with the correct type
        let cfg = RedsysConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        RedsysLogger.debug("Plugin loaded.")
    }

    // MARK: - Error Handling

    /**
     Maps internal RedsysError cases to standardized JS error codes.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: RedsysError
    ) {
        let code: String

        switch error {
        case .unavailable:
            code = "UNAVAILABLE"
        case .permissionDenied:
            code = "PERMISSION_DENIED"
        case .initFailed:
            code = "INIT_FAILED"
        case .unknownType:
            code = "UNKNOWN_TYPE"
        case .cryptoError:
            code = "CRYPTO_ERROR"
        }

        call.reject(error.message, code)
    }

    // MARK: - Direct Payment

    /**
     Starts the direct payment flow.

     Saves the call for asynchronous SDK callback correlation.
     */
    @objc func doDirectPayment(_ call: CAPPluginCall) {
        guard let order = call.getString("order") else {
            return call.reject("order required")
        }
        guard let amount = call.getDouble("amount") else {
            return call.reject("amount required")
        }

        // Save the call to correlate with delegate response via Ds_Order
        bridge?.saveCall(call)

        // Store callback identifier for KO correlation
        activeDirectPaymentCallId = call.callbackId

        implementation.executeDirectPayment(
            order: order,
            amount: amount,
            type: RedsysUtils.mapTransactionType(call.getString("transactionType")),
            description: call.getString("description"),
            identifier: call.getString("identifier"),
            extraParams: call.getObject("extraParams") as? [String: String],
            uiOptions: call.getObject("uiOptions"),
            delegate: self
        )
    }

    // MARK: - Web Payment Initialization

    /**
     Initializes WebView payment flow (Step 1 of 2-step 3DS process).
     */
    @objc func initializeWebPayment(_ call: CAPPluginCall) {
        guard let order = call.getString("order") else { return call.reject("order required") }
        // Changed getFloat to getDouble for parity with SDK and Android
        guard let amount = call.getDouble("amount") else { return call.reject("amount required") }

        implementation.initializeWebPayment(
            order: order,
            amount: amount,
            type: RedsysUtils.mapTransactionType(call.getString("transactionType")),
            description: call.getString("description"),
            identifier: call.getString("identifier"),
            extraParams: call.getObject("extraParams") as? [String: String]
        ) { base64, error in
            if let error = error as? RedsysError {
                self.reject(call, error: error)
            } else if let base64 = base64 {
                call.resolve(["base64Data": base64])
            }
        }
    }

    // MARK: - DirectPaymentResponseDelegate

    /**
     Called by the SDK when direct payment succeeds.
     */
    @objc public func responseDirectPaymentOK(response: DirectPaymentResponseOK) {
        if let call = bridge?.savedCall(withID: response.Ds_Order) {
            let result = RedsysUtils.mapDirectPaymentResponse(response)
            call.resolve(result)
            bridge?.releaseCall(call)
        }
    }

    /**
     * Called by the SDK when direct payment fails.
     * Fixed for Capacitor v8 iOS dictionary syntax.
     */
    @objc public func responseDirectPaymentKO(response: DirectPaymentResponseKO) {
        if let callbackId = activeDirectPaymentCallId,
           let call = bridge?.savedCall(withID: callbackId) {

            // In iOS, JSObject is a Dictionary, use standard subscripting
            var errorMetadata = [String: Any]()
            errorMetadata["sdkCode"] = response.code

            // Correct reject signature: message, code, error, data
            call.reject(
                "\(response.desc) (SDK Code: \(response.code))",
                "SDK_ERROR",
                nil, // No underlying Swift Error object
                errorMetadata
            )

            bridge?.releaseCall(call)
            activeDirectPaymentCallId = nil
        }
    }

    // MARK: - Web Payment Execution

    /**
     Executes WebView payment (Step 2 of 2-step flow).
     */
    @objc func processWebPayment(_ call: CAPPluginCall) {
        // Check call arguments first, then fallback to global config (RedsysConfig)
        let signature = call.getString("signature") ?? config?.signature

        guard let signature = signature, !signature.isEmpty else {
            return call.reject("Signature is required for web payment in call or configuration")
        }

        bridge?.saveCall(call)

        implementation.processWebPayment(
            signature: signature,
            signatureVersion: call.getString("signatureVersion"),
            delegate: self
        )
    }

    // MARK: - WebViewPaymentResponseDelegate

    /**
     Called by the SDK when WebView payment succeeds.
     */
    @objc public func responsePaymentOK(response: WebViewPaymentResponseOK) {
        if let call = bridge?.savedCall(withID: response.Ds_Order) {
            let result = RedsysUtils.mapWebViewResponse(response)
            call.resolve(result)
            bridge?.releaseCall(call)
        }
    }

    /**
     Called by the SDK when WebView payment fails.
     */
    @objc public func responsePaymentKO(response: WebViewPaymentResponseKO) {
        // WebView KO often uses code as identifier or generic
        if let call = bridge?.savedCall(withID: "\(response.code)") {
            var errorMetadata = [String: Any]()
            errorMetadata["sdkCode"] = response.code

            call.reject(
                "\(response.desc) (SDK Code: \(response.code))",
                "SDK_ERROR",
                nil,
                errorMetadata
            )
            bridge?.releaseCall(call)
        }
    }

    // MARK: - Cryptographic Utilities

    /**
     Computes HMAC signature using the provided data and key.
     */
    @objc func computeHash(_ call: CAPPluginCall) {
        guard let data = call.getString("data"),
              let key = call.getString("keyBase64") else {
            return call.reject("Missing data or key")
        }
        let alg = call.getString("algorithm") ?? "HMAC_SHA256_V1"

        if let signature = RedsysUtils.calculateHMAC(data: data, keyBase64: key, algorithm: alg) {
            call.resolve(["signature": signature])
        } else {
            // Precise error for debugging Base64 issues common in Redsys integration
            self.reject(call, error: .cryptoError("Hash computation failed. Check Base64 key integrity."))
        }
    }

    // MARK: - Version Information

    /**
     Returns the native plugin version.
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve([
            "version": PluginVersion.number
        ])
    }
}
