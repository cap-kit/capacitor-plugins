import Foundation
import Capacitor

/**
 Plugin configuration container.

 This struct is responsible for reading and exposing
 static configuration values defined under the
 `Integrity` key in capacitor.config.ts.

 Configuration rules:
 - Read once during plugin initialization
 - Treated as immutable runtime input
 - Accessible only from native code
 */
public struct IntegrityConfig {

    // MARK: - Configuration Keys

    /**
     Centralized definition of configuration keys.
     Avoids string duplication and typos.
     */
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let blockPage = "blockPage"
        static let blockPageEnabled = "enabled"
        static let blockPageUrl = "url"
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
     Optional configuration for the integrity block page.
     */
    public let blockPage: BlockPageConfig?

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false
    // private static let defaultBlockPage
    // private static let defaultBlockPageEnabled
    // private static let defaultBlockPageUrl

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

        // Block page configuration
        if let blockPageConfig = config.getObject(Keys.blockPage) {

            let enabled =
                blockPageConfig[Keys.blockPageEnabled] as? Bool ?? false

            let url =
                blockPageConfig[Keys.blockPageUrl] as? String

            self.blockPage = BlockPageConfig(
                enabled: enabled,
                url: url
            )
        } else {
            self.blockPage = nil
        }
    }
}

// MARK: - Block Page Config

/**
 Configuration for the optional integrity block page.
 */
public struct BlockPageConfig {
    public let enabled: Bool
    public let url: String?
}
