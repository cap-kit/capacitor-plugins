import Foundation
import CommonCrypto
import TPVVInLibrary

/**
 Redsys Utility Helpers (iOS)

 This type centralizes:
 - SDK → JS response mapping
 - Cryptographic helpers
 - Enum mapping utilities

 This layer MUST remain pure:
 - No access to CAPPluginCall
 - No dependency on Bridge logic
 - No UI operations
 */
struct RedsysUtils {

    // MARK: - Response Mapping (Direct Payment)

    /**
     * Maps a DirectPaymentResponseOK into a JS-compatible dictionary.
     * Card details are not available in DirectPayment flow, so we ensure empty strings for parity.
     */
    static func mapDirectPaymentResponse(
        _ response: DirectPaymentResponseOK
    ) -> [String: Any] {

        return [
            "code": Int(response.code),
            "desc": response.desc,

            "amount": response.Ds_Amount,
            "currency": response.Ds_Currency,
            "order": response.Ds_Order,
            "merchantCode": TPVVConfiguration.shared.appFuc,
            "terminal": response.Ds_Terminal,
            "responseCode": response.Ds_Response,
            "authorisationCode": response.Ds_AuthorisationCode,
            "transactionType": response.Ds_TransactionType,
            "securePayment": response.Ds_SecurePayment,
            "signature": "",

            // Card details are not available in DirectPayment flow
            "cardNumber": "",
            "cardBrand": "",
            "cardCountry": "",
            "cardType": "",
            "expiryDate": "",

            "merchantIdentifier": response.Ds_Merchant_Identifier,
            "consumerLanguage": "",
            "date": "",
            "hour": "",
            "merchantData": response.Ds_MerchantData,
            "extraParams": response.Ds_Extra_Params
        ]
    }

    // MARK: - Response Mapping (WebView)

    /**
     * Maps a WebViewPaymentResponseOK into a JS-compatible dictionary.
     * Full card and signature info is available in WebView flow.
     */
    static func mapWebViewResponse(
        _ response: WebViewPaymentResponseOK
    ) -> [String: Any] {

        return [
            "code": Int(response.code),
            "desc": response.desc,

            "amount": response.Ds_Amount,
            "currency": response.Ds_Currency,
            "order": response.Ds_Order,
            "merchantCode": TPVVConfiguration.shared.appFuc,
            "terminal": TPVVConfiguration.shared.appTerminal,
            "responseCode": response.Ds_Response,
            "authorisationCode": response.Ds_AuthorisationCode,
            "transactionType": response.Ds_TransactionType,
            "securePayment": response.Ds_SecurePayment,
            "signature": response.Ds_Signature,

            "cardNumber": maskCardNumber(response.Ds_Card_Number),
            "cardBrand": response.Ds_Card_Brand,
            "cardCountry": response.Ds_Card_Country,
            "cardType": response.Ds_Card_Type,
            "expiryDate": response.Ds_ExpiryDate,

            "merchantIdentifier": response.Ds_Merchant_Identifier,
            "consumerLanguage": response.Ds_ConsumerLanguage,
            "date": response.Ds_Date,
            "hour": response.Ds_Hour,
            "merchantData": response.Ds_MerchantData,
            "extraParams": response.Ds_Extra_Params
        ]
    }

    /**
     * Masks a card number using a specific pattern.
     * Replicates the reference implementation logic for cross-platform parity.
     */
    static func maskCardNumber(_ cardNumber: String, mask: String = "xxxx-xxxx-xxxx-####") -> String {
        var maskedNumber = ""
        let cardNumberChars = Array(cardNumber)
        let maskChars = Array(mask)
        var index = 0

        for i in 0..<maskChars.count {
            let c = maskChars[i]
            switch c {
            case "#":
                if index < cardNumberChars.count {
                    maskedNumber.append(cardNumberChars[index])
                    index += 1
                }
            case "x":
                maskedNumber.append("x")
                index += 1
            default:
                maskedNumber.append(c)
            }
        }
        return maskedNumber
    }

    // MARK: - Cryptographic Utilities

    /**
     * Computes HMAC (SHA256 or SHA512) using the provided Base64 key.
     * Aligned with Redsys merchant signature requirements for iOS.
     */
    static func calculateHMAC(data: String, keyBase64: String, algorithm: String) -> String? {
        // Redsys keys are Base64 encoded. Decoding is mandatory before HMAC.
        guard let keyData = Data(base64Encoded: keyBase64, options: .ignoreUnknownCharacters) else {
            RedsysLogger.error("Failed to decode Base64 key for HMAC")
            return nil
        }

        let is256 = algorithm.contains("256")
        let alg = is256 ? CCHmacAlgorithm(kCCHmacAlgSHA256) : CCHmacAlgorithm(kCCHmacAlgSHA512)
        let digestLen = is256 ? CC_SHA256_DIGEST_LENGTH : CC_SHA512_DIGEST_LENGTH

        var hmac = [UInt8](repeating: 0, count: Int(digestLen))

        // Ensure UTF-8 encoding for data parity between Android and iOS
        let dataToSign = data.data(using: .utf8) ?? Data()

        dataToSign.withUnsafeBytes { dataBytes in
            keyData.withUnsafeBytes { keyBytes in
                CCHmac(
                    alg,
                    keyBytes.baseAddress,
                    keyData.count,
                    dataBytes.baseAddress,
                    dataToSign.count,
                    &hmac
                )
            }
        }

        return Data(hmac).base64EncodedString()
    }

    // MARK: - Enum Mapping

    /**
     Maps JavaScript payment method identifiers to SDK constants.
     */
    static func mapPaymentMethods(_ methods: [String]?) -> [String] {
        guard let methods = methods else { return [] }
        return methods.compactMap { method in
            let lowercasedMethod = method.lowercased()
            switch lowercasedMethod {
            case "card":
                return PaymentMethod.card
            case "transfer":
                return PaymentMethod.transfer
            case "domiciliation":
                return PaymentMethod.domiciliation
            case "paypal":
                return PaymentMethod.paypal
            case "bizum":
                return PaymentMethod.immediatePayment
            default:
                return method
            }
        }
    }

    /**
     Maps JavaScript transaction type strings to SDK TransactionType enum.
     */
    static func mapTransactionType(_ type: String?) -> TransactionType {
        switch type?.lowercased() {
        case "preauthorization": return .preauthorization
        case "traditional": return .traditional
        case "authentication": return .paymentTypeAuthentication
        default: return .normal
        }
    }

    /**
     * Converts a Hex color string (e.g., "#FFFFFF") to a UIColor object.
     * Required for mapping Capacitor config colors to iOS SDK.
     */
    static func colorFromHex(_ hex: String) -> UIColor {
        var cString: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if cString.hasPrefix("#") {
            cString.remove(at: cString.startIndex)
        }

        if cString.count != 6 {
            return UIColor.gray
        }

        var rgbValue: UInt64 = 0
        Scanner(string: cString).scanHexInt64(&rgbValue)

        return UIColor(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: CGFloat(1.0)
        )
    }
}
