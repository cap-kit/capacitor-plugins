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
    }

    // MARK: - Public Configuration Values

    /// Enables verbose native logging.
    ///
    /// When enabled, the plugin prints additional debug information
    /// to the Xcode console.
    public let verboseLogging: Bool

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false

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
    }
}
