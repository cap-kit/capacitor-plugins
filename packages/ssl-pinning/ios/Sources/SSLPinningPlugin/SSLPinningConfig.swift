import Foundation
import Capacitor

/**
 Plugin configuration container.

 This struct is responsible for reading and exposing
 static configuration values defined under the
 `SSLPinning` key in capacitor.config.ts.

 Configuration rules:
 - Read once during plugin initialization
 - Treated as immutable runtime input
 - Accessible only from native code
 */
public struct SSLPinningConfig {

    // MARK: - Configuration Keys

    /**
     Centralized definition of configuration keys.
     Avoids string duplication and typos.
     */
    private enum Keys {
        static let verboseLogging = "verboseLogging"
        static let fingerprint = "fingerprint"
        static let fingerprints = "fingerprints"
    }

    // MARK: - Public Configuration Values

    /**
     Enables verbose native logging.

     When enabled, the plugin prints additional
     debug information to the Xcode console.

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

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultFingerprint: String? = nil
    private static let defaultFingerprints: [String] = []

    // MARK: - Initialization

    /**
     Initializes the configuration by reading values
     from the Capacitor PluginConfig.

     - Parameter plugin: The CAPPlugin instance used
     to access typed configuration values.
     */
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Verbose logging flag
        self.verboseLogging =
            config.getBoolean(
                Keys.verboseLogging,
                Self.defaultVerboseLogging
            )

        // Single fingerprint (optional)
        if let value = config.getString(Keys.fingerprint),
           !value.isEmpty {
            self.fingerprint = value
        } else {
            self.fingerprint = Self.defaultFingerprint
        }

        // Multiple fingerprints (optional)
        self.fingerprints =
            config.getArray(Keys.fingerprints)?
            .compactMap { $0 as? String }
            .filter { !$0.isEmpty }
            ?? Self.defaultFingerprints
    }
}
