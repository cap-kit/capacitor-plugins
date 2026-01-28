import Foundation
import Capacitor

/**
 Helper struct to manage the SSLPinning plugin configuration.

 This struct reads static configuration values from `capacitor.config.ts`
 using the Capacitor plugin instance's built-in config access.

 IMPORTANT:
 - These values are READ-ONLY at runtime.
 - JavaScript MUST NOT access them directly.
 - Actual behavior is implemented in native code only.
 */
public struct SSLPinningConfig {

    // MARK: - Configuration Keys

    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let fingerprint = "fingerprint"
        static let fingerprints = "fingerprints"
    }

    // MARK: - Public Config Values

    /**
     Enables verbose native logging.

     When enabled, additional debug information is printed
     to the Xcode console via the plugin logger.

     Default: false
     */
    public let verboseLogging: Bool

    /**
     Default SHA-256 fingerprint used by `checkCertificate()`
     when no fingerprint is provided at runtime.
     */
    public let fingerprint: String?

    /**
     Default SHA-256 fingerprints used by `checkCertificates()`
     when no fingerprints are provided at runtime.
     */
    public let fingerprints: [String]

    // MARK: - Private Defaults

    private let defaultVerboseLogging = false
    private let defaultFingerprint: String? = nil
    private let defaultFingerprints: [String] = []

    // MARK: - Init

    /**
     Initializes the configuration by reading values from the Capacitor bridge.

     - Parameter plugin: The CAPPlugin instance used to access typed configuration.
     */
    init(plugin: CAPPlugin) {
        // Use getConfigValue(key) to bypass SPM visibility issues and ensure stability.

        // Bool - Verbose Logging
        verboseLogging = plugin.getConfigValue(Keys.verboseLogging) as? Bool ?? defaultVerboseLogging

        // Optional String - Single Fingerprint
        // We validate that it is not empty after casting to avoid using empty strings as valid fingerprints.
        if let fprt = plugin.getConfigValue(Keys.fingerprint) as? String, !fprt.isEmpty {
            fingerprint = fprt
        } else {
            fingerprint = defaultFingerprint
        }

        // Array of Strings - Multiple Fingerprints
        fingerprints = plugin.getConfigValue(Keys.fingerprints) as? [String] ?? defaultFingerprints
    }
}
