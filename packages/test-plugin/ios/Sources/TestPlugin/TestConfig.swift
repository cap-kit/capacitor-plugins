import Foundation
import Capacitor

/**
 Plugin configuration container.

 This struct is responsible for reading and exposing static configuration
 values defined under the `Test` key in `capacitor.config.ts`.

 Configuration is:
 - read once during plugin initialization
 - treated as immutable runtime input
 - consumed only by native code (never by JavaScript)
 */
public struct TestConfig {

    // MARK: - Configuration Keys

    // Define configuration keys for consistency
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let customMessage = "customMessage"
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
     Custom message appended to echoed values.

     This property exists primarily for demonstration purposes and
     shows how to pass static configuration values from JavaScript
     to native code.

     Default: " (from config)"
     */
    public let customMessage: String?

    // MARK: - Private Defaults

    // Default values
    private let defaultVerboseLogging = false
    private let defaultCustomMessage = " (from config)"

    // MARK: - Init

    /**
     NOTE:
     We intentionally use `getConfigValue(_:)` instead of `getConfig()`
     due to Swift Package Manager toolchain limitations observed in
     Capacitor v8 (PluginConfig not reliably accessible in some setups).

     Initializes the configuration by reading values from the Capacitor bridge.

     - Parameter plugin: The CAPPlugin instance used to access typed configuration.
     */
    init(plugin: CAPPlugin) {
        let pluginId = plugin.pluginId

        // Bool
        verboseLogging = plugin.getConfigValue(Keys.verboseLogging) as? Bool ?? defaultVerboseLogging

        // String
        customMessage = plugin.getConfigValue(Keys.customMessage) as? String ?? defaultCustomMessage
    }
}
