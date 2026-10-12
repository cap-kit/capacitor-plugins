import Foundation
import Capacitor

/**
 * Plugin configuration container for the PrivacyScreen plugin.
 *
 * This struct is responsible for reading and exposing static configuration
 * values defined under the `PrivacyScreen` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization (`load()`).
 * - Treated as immutable runtime input.
 * - Consumed only by native code (never by JavaScript).
 */
public struct PrivacyScreenConfig {

    // MARK: - Configuration Keys

    /**
     * Internal structure to maintain consistent configuration keys.
     */
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let enabled = "enabled"
    }

    // MARK: - Public Properties

    /**
     * Enables verbose native logging via PrivacyScreenLogger.
     *
     * When enabled, additional debug information is printed to the Xcode console.
     * Default: false
     */
    public let verboseLogging: Bool

    /**
     Enables privacy protection automatically when the native plugin
     loads, without waiting for the first `enable()` call.

     Default: false
     */
    public let enabled: Bool

    // MARK: - Private Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultEnabled: Bool = false

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
        self.enabled = config.getBoolean(
            Keys.enabled,
            Self.defaultEnabled
        )
    }
}
