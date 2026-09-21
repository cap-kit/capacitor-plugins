import Foundation
import Capacitor

/**
 * Plugin configuration container for the Rank plugin.
 *
 * This struct is responsible for reading and exposing static configuration
 * values defined under the `Rank` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization (`load()`).
 * - Treated as immutable runtime input.
 * - Consumed only by native code (never by JavaScript).
 */
public struct RankConfig {

    // MARK: - Configuration Keys

    /**
     * Internal structure to maintain consistent configuration keys.
     */
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let appleAppId = "appleAppId"
        static let fireAndForget = "fireAndForget"
    }

    // MARK: - Public Properties

    /**
     * Enables verbose native logging via RankLogger.
     *
     * When enabled, additional debug information is printed to the Xcode console.
     * Default: false
     */
    public let verboseLogging: Bool

    /**
     * Optional Apple App ID for App Store redirection.
     *
     * If provided, the plugin uses this ID to direct users to the specific review page.
     * Default: nil
     */
    let appleAppId: String?

    /**
     * Global policy for review request resolution.
     *
     * If true, the plugin resolves promises immediately without awaiting OS feedback.
     * Note: On iOS, this is the default behavior as StoreKit provides no completion callback.
     * Default: false
     */
    let fireAndForget: Bool

    // MARK: - Private Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultAppleAppId: String? = nil
    private static let defaultFireAndForget: Bool = false

    // MARK: - Initialization

    /**
     * Initializes the configuration by reading values from the Capacitor bridge.
     *
     * - Parameter plugin: The CAPPlugin instance used to access typed configuration via `getConfig()`.
     */
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Extract verboseLogging with fallback to default
        self.verboseLogging = config.getBoolean(
            Keys.verboseLogging,
            Self.defaultVerboseLogging
        )

        // Extract optional appleAppId string
        self.appleAppId = config.getString(
            Keys.appleAppId,
            Self.defaultAppleAppId
        )

        // Extract fireAndForget with fallback to default
        self.fireAndForget = config.getBoolean(
            Keys.fireAndForget,
            Self.defaultFireAndForget
        )
    }
}
