import Foundation
import Capacitor

/**
 Helper struct to manage the SSLPinning plugin configuration.

 This struct reads static configuration values from `capacitor.config.ts`
 using Capacitor's `PluginConfig` API.

 IMPORTANT:
 - These values are READ-ONLY at runtime
 - JavaScript MUST NOT access them directly
 - Actual behavior is implemented in native code only
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
     Enables verbose native logging (Xcode / Logcat).

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

    // MARK: - Init

    /**
     Initializes the configuration by reading values from
     `capacitor.config.ts` via Capacitor's PluginConfig system.
     */
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Bool
        verboseLogging =
            (config.value(forKey: Keys.verboseLogging) as? Bool) ?? false

        // Optional string
        if let fprt = config.value(forKey: Keys.fingerprint) as? String,
           !fprt.isEmpty {
            fingerprint = fprt
        } else {
            fingerprint = nil
        }

        // Arrays (default empty)
        fingerprints =
            config.value(forKey: Keys.fingerprints) as? [String] ?? []
    }
}
