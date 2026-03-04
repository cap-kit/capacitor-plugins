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
        static let logLevel = "logLevel"
        static let lockAfterMs = "lockAfterMs"
        static let enablePrivacyScreen = "enablePrivacyScreen"
        static let obfuscationPrefix = "obfuscationPrefix"
        static let allowDevicePasscode = "allowDevicePasscode"
        static let biometricPromptText = "biometricPromptText"
        static let prefix = "prefix"
        static let allowCachedAuthentication = "allowCachedAuthentication"
        static let cachedAuthenticationTimeoutMs = "cachedAuthenticationTimeoutMs"
        static let cryptoStrategy = "cryptoStrategy"
        static let keySize = "keySize"
        static let maxBiometricAttempts = "maxBiometricAttempts"
        static let lockoutDurationMs = "lockoutDurationMs"
        static let requireFreshAuthenticationMs = "requireFreshAuthenticationMs"
        static let encryptionAlgorithm = "encryptionAlgorithm"
        static let enableICloudKeychainSync = "enableICloudKeychainSync"
    }

    // MARK: - Public Configuration Values

    /// Enables verbose native logging.
    ///
    /// When enabled, the plugin prints additional debug information
    /// to the Xcode console.
    public let verboseLogging: Bool
    public let logLevel: String
    public let lockAfterMs: Int
    public let enablePrivacyScreen: Bool
    public let obfuscationPrefix: String
    public let allowDevicePasscode: Bool
    public let biometricPromptText: String
    public let prefix: String
    public let allowCachedAuthentication: Bool
    public let cachedAuthenticationTimeoutMs: Int
    public let cryptoStrategy: String
    public let keySize: Int
    public let maxBiometricAttempts: Int
    public let lockoutDurationMs: Int
    public let requireFreshAuthenticationMs: Int
    public let encryptionAlgorithm: String
    public let enableICloudKeychainSync: Bool

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultLockAfterMs: Int = 60_000
    private static let defaultEnablePrivacyScreen: Bool = true
    private static let defaultObfuscationPrefix: String = "ftrss_"
    private static let defaultAllowDevicePasscode: Bool = true
    private static let defaultBiometricPromptText: String = "Cancel"
    private static let defaultPrefix: String = ""
    private static let defaultAllowCachedAuthentication: Bool = false
    private static let defaultCachedAuthenticationTimeoutMs: Int = 30_000
    private static let defaultCryptoStrategy: String = "auto"
    private static let defaultKeySize: Int = 2048
    private static let defaultMaxBiometricAttempts: Int = 5
    private static let defaultLockoutDurationMs: Int = 30_000
    private static let defaultRequireFreshAuthenticationMs: Int = 0
    private static let defaultEncryptionAlgorithm: String = "AES-GCM"
    private static let defaultEnableICloudKeychainSync: Bool = false

    // MARK: - Initialization

    // Initializes configuration from the Capacitor plugin config.
    // - Parameter plugin: CAPPlugin instance used to access typed config values.
    // swiftlint:disable function_body_length
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Verbose logging flag
        self.verboseLogging =
            config.getBoolean(
                Keys.verboseLogging,
                Self.defaultVerboseLogging
            )

        self.logLevel = config.getString(
            Keys.logLevel,
            self.verboseLogging ? "debug" : "info"
        ) ?? (self.verboseLogging ? "debug" : "info")

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

        self.allowDevicePasscode = config.getBoolean(
            Keys.allowDevicePasscode,
            Self.defaultAllowDevicePasscode
        )

        self.biometricPromptText = config.getString(
            Keys.biometricPromptText,
            Self.defaultBiometricPromptText
        ) ?? Self.defaultBiometricPromptText

        self.prefix = config.getString(
            Keys.prefix,
            Self.defaultPrefix
        ) ?? Self.defaultPrefix

        self.allowCachedAuthentication = config.getBoolean(
            Keys.allowCachedAuthentication,
            Self.defaultAllowCachedAuthentication
        )

        self.cachedAuthenticationTimeoutMs = config.getInt(
            Keys.cachedAuthenticationTimeoutMs,
            Self.defaultCachedAuthenticationTimeoutMs
        )

        self.cryptoStrategy = config.getString(
            Keys.cryptoStrategy,
            Self.defaultCryptoStrategy
        ) ?? Self.defaultCryptoStrategy

        self.keySize = config.getInt(
            Keys.keySize,
            Self.defaultKeySize
        )

        self.maxBiometricAttempts = config.getInt(
            Keys.maxBiometricAttempts,
            Self.defaultMaxBiometricAttempts
        )

        self.lockoutDurationMs = config.getInt(
            Keys.lockoutDurationMs,
            Self.defaultLockoutDurationMs
        )

        self.requireFreshAuthenticationMs = config.getInt(
            Keys.requireFreshAuthenticationMs,
            Self.defaultRequireFreshAuthenticationMs
        )

        self.encryptionAlgorithm = config.getString(
            Keys.encryptionAlgorithm,
            Self.defaultEncryptionAlgorithm
        ) ?? Self.defaultEncryptionAlgorithm

        self.enableICloudKeychainSync = config.getBoolean(
            Keys.enableICloudKeychainSync,
            Self.defaultEnableICloudKeychainSync
        )
    }
    // swiftlint:enable function_body_length
}
