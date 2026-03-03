import Foundation
import Capacitor

/// Plugin configuration container.
///
/// Reads static configuration values defined under the `Fortress`
/// key in `capacitor.config.ts`.
public struct Config {

    // MARK: - Configuration Keys

    /// Centralized definition of configuration keys.
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let lockAfterMs = "lockAfterMs"
        static let enablePrivacyScreen = "enablePrivacyScreen"
        static let obfuscationPrefix = "obfuscationPrefix"
    }

    // MARK: - Public Configuration Values

    /// Enables verbose native logging.
    ///
    /// When enabled, the plugin prints additional debug information
    /// to the Xcode console.
    public let verboseLogging: Bool
    public let lockAfterMs: Int
    public let enablePrivacyScreen: Bool
    public let obfuscationPrefix: String

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultLockAfterMs: Int = 60_000
    private static let defaultEnablePrivacyScreen: Bool = true
    private static let defaultObfuscationPrefix: String = "ftrss_"

    // MARK: - Initialization

    /// Initializes configuration from the Capacitor plugin config.
    ///
    /// - Parameter plugin: CAPPlugin instance used to access typed config values.
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Verbose logging flag
        self.verboseLogging =
            config.getBoolean(
                Keys.verboseLogging,
                Self.defaultVerboseLogging
            )
        self.lockAfterMs =
            config.getInt(
                Keys.lockAfterMs,
                Self.defaultLockAfterMs
            )
        self.enablePrivacyScreen =
            config.getBoolean(
                Keys.enablePrivacyScreen,
                Self.defaultEnablePrivacyScreen
            )
        self.obfuscationPrefix =
            config.getString(
                Keys.obfuscationPrefix,
                Self.defaultObfuscationPrefix
            ) ?? Self.defaultObfuscationPrefix
    }
}
