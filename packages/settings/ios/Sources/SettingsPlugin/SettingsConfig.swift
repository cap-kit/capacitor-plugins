import Foundation
import Capacitor

/**
 Plugin configuration container.

 This struct is responsible for reading and exposing static configuration
 values defined under the `Settings` key in `capacitor.config.ts`.

 Configuration is:
 - read once during plugin initialization
 - treated as immutable runtime input
 - consumed only by native code
 */
public struct SettingsConfig {

    // MARK: - Configuration Keys

    // Define configuration keys for consistency
    private struct Keys {
        static let verboseLogging = "verboseLogging"
    }

    // MARK: - Public Config Values

    /**
     Enables verbose native logging.

     When enabled, additional debug information is printed
     to the Xcode console via the plugin logger.

     Default: false
     */
    public let verboseLogging: Bool

    // MARK: - Private Defaults

    // Default values
    private let defaultVerboseLogging = false

    // MARK: - Init

    /**
     Initializes the configuration by reading values from the Capacitor bridge.

     - Parameter plugin: The CAPPlugin instance used to access typed configuration.
     */
    init(plugin: CAPPlugin) {
        // Use getConfigValue(key) to ensure compatibility across all v8 environments

        // Bool - Verbose Logging
        verboseLogging = plugin.getConfigValue(Keys.verboseLogging) as? Bool ?? defaultVerboseLogging
    }
}
