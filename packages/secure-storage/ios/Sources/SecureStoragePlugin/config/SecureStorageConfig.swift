import Foundation
import Capacitor

/**
 * Plugin configuration container for the SecureStorage plugin.
 *
 * This struct is responsible for reading and exposing static configuration
 * values defined under the `SecureStorage` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization (`load()`).
 * - Treated as immutable runtime input.
 * - Consumed only by native code (never by JavaScript).
 */
public struct SecureStorageConfig {

    // MARK: - Configuration Keys

    /**
     * Internal structure to maintain consistent configuration keys.
     */
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let enableICloudKeychainSync = "enableICloudKeychainSync"
        static let obfuscateKeys = "obfuscateKeys"
        static let obfuscationPrefix = "obfuscationPrefix"
    }

    // MARK: - Public Properties

    /**
     * Enables verbose native logging via SecureStorageLogger.
     *
     * When enabled, additional debug information is printed to the Xcode console.
     * Default: false
     */
    public let verboseLogging: Bool

    /**
     Enables iCloud Keychain synchronization for stored entries.

     Default: false
     */
    public let enableICloudKeychainSync: Bool

    /**
     Base64-encodes stored key names (both tiers) on top of any prefix.
     Obfuscation hides names from casual inspection; it is not encryption.

     Default: false
     */
    public let obfuscateKeys: Bool

    /**
     Prefix prepended to stored key names before optional encoding.

     Default: '' (no namespacing beyond the tier/service scope)
     */
    public let obfuscationPrefix: String

    // MARK: - Private Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultEnableICloudKeychainSync: Bool = false
    private static let defaultObfuscateKeys: Bool = false
    private static let defaultObfuscationPrefix: String = ""

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
        self.enableICloudKeychainSync = config.getBoolean(
            Keys.enableICloudKeychainSync,
            Self.defaultEnableICloudKeychainSync
        )
        self.obfuscateKeys = config.getBoolean(
            Keys.obfuscateKeys,
            Self.defaultObfuscateKeys
        )
        self.obfuscationPrefix = config.getString(
            Keys.obfuscationPrefix,
            Self.defaultObfuscationPrefix
        ) ?? Self.defaultObfuscationPrefix
    }
}
