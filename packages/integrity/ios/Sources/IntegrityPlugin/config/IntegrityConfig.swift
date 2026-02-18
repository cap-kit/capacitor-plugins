import Foundation
import Capacitor

/// Plugin configuration container.
///
/// Reads static configuration values defined under the `Integrity`
/// key in `capacitor.config.ts`.
public struct IntegrityConfig {

    // MARK: - Configuration Keys

    /// Centralized definition of configuration keys.
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let blockPage = "blockPage"
        static let blockPageEnabled = "enabled"
        static let blockPageUrl = "url"

        // Jailbreak URL scheme probing (opt-in)
        static let jailbreakUrlSchemes = "jailbreakUrlSchemes"
        static let jailbreakUrlSchemesEnabled = "enabled"
        static let jailbreakUrlSchemesList = "schemes"
    }

    // MARK: - Public Configuration Values

    /// Enables verbose native logging.
    ///
    /// When enabled, the plugin prints additional debug information
    /// to the Xcode console.
    public let verboseLogging: Bool

    /// Optional configuration for the integrity block page.
    public let blockPage: BlockPageConfig?

    // Optional jailbreak URL scheme probing configuration
    public let jailbreakUrlSchemes: JailbreakUrlSchemeConfig?

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false
    // private static let defaultBlockPage
    // private static let defaultBlockPageEnabled
    // private static let defaultBlockPageUrl

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

        // Jailbreak URL scheme probing configuration (opt-in)
        if let schemeConfig = config.getObject(Keys.jailbreakUrlSchemes) {
            let enabled =
                schemeConfig[Keys.jailbreakUrlSchemesEnabled] as? Bool ?? false

            let schemes =
                schemeConfig[Keys.jailbreakUrlSchemesList] as? [String] ?? []

            self.jailbreakUrlSchemes = JailbreakUrlSchemeConfig(
                enabled: enabled,
                schemes: schemes
            )
        } else {
            self.jailbreakUrlSchemes = nil
        }
    }
}

// MARK: - Block Page Config

/// Configuration for the optional integrity block page.
public struct BlockPageConfig {
    public let enabled: Bool
    public let url: String?
}

// MARK: - Jailbreak Url Scheme Config

/// Configuration for jailbreak URL scheme probing.
public struct JailbreakUrlSchemeConfig {
    public let enabled: Bool
    public let schemes: [String]
}
