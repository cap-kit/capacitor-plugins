import Foundation

/**
 Canonical error messages shared across platforms.

 These strings must remain byte-identical on iOS and Android.
 */
enum SSLPinningErrorMessages {
    static let urlRequired = "url is required"
    static let invalidUrlMustBeHttps = "Invalid URL. Must be https."
    static let invalidUrl = "Invalid URL."
    static let noFingerprintsProvided = "No fingerprints provided"
    static let noCertsProvided = "No certs provided"
    static let noHostFoundInUrl = "No host found in URL"
    static let invalidFingerprintFormat = "Invalid fingerprint format"
    static let unsupportedHost = "Unsupported host: %@"
    static let pinningFailed = "Pinning failed"
    static let excludedDomain = "Excluded domain"
    static let timeout = "Timeout"
    static let networkError = "Network error"
    static let internalError = "Internal error"
    static let invalidConfig = "Invalid configuration: %@"
    static let certNotFound = "Certificate not found: %@"

    static func unsupportedHost(_ value: String) -> String {
        return String(format: unsupportedHost, value)
    }

    static func invalidConfig(_ value: String) -> String {
        return String(format: invalidConfig, value)
    }

    static func certNotFound(_ value: String) -> String {
        return String(format: certNotFound, value)
    }
}
