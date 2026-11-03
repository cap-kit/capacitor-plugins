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
        static let color = "color"
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

    /**
     Hex cover colors from config (`color`). Accepts a single hex string
     (`#RRGGBB` / `#AARRGGBB`, applied to both appearances) or an object
     `{ light: hex, dark: hex }` for per-appearance values. When a valid
     color is available for the current appearance, the cover is a solid
     color view instead of the blur; otherwise the blur cover is used.
     Default: nil (blur cover)
     */
    public let colorLight: String?
    public let colorDark: String?

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
        // `color` accepts a hex string or `{ light: hex, dark: hex }`.
        if let raw = plugin.getConfigValue(Keys.color) {
            if let hex = raw as? String {
                self.colorLight = hex
                self.colorDark = hex
            } else if let object = raw as? [String: Any] {
                self.colorLight = object["light"] as? String
                self.colorDark = object["dark"] as? String
            } else {
                self.colorLight = nil
                self.colorDark = nil
            }
        } else {
            self.colorLight = nil
            self.colorDark = nil
        }
    }
}
