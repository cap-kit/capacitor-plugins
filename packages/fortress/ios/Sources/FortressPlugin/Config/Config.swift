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
        static let privacyOverlayText = "privacyOverlayText"
        static let privacyOverlayImageName = "privacyOverlayImageName"
        static let privacyOverlayShowText = "privacyOverlayShowText"
        static let privacyOverlayShowImage = "privacyOverlayShowImage"
        static let privacyOverlayTextColor = "privacyOverlayTextColor"
        static let privacyOverlayBackgroundOpacity = "privacyOverlayBackgroundOpacity"
        static let privacyOverlayTheme = "privacyOverlayTheme"
        static let obfuscationPrefix = "obfuscationPrefix"
        static let allowDevicePasscode = "allowDevicePasscode"
        static let fallbackStrategy = "fallbackStrategy"
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
        static let persistSessionState = "persistSessionState"
    }

    // MARK: - Public Configuration Values

    /// Enables verbose native logging.
    ///
    /// When enabled, the plugin prints additional debug information
    /// to the Xcode console.
    public var verboseLogging: Bool
    public var logLevel: String
    public var lockAfterMs: Int
    public var enablePrivacyScreen: Bool
    public var privacyOverlayText: String
    public var privacyOverlayImageName: String
    public var privacyOverlayShowText: Bool
    public var privacyOverlayShowImage: Bool
    public var privacyOverlayTextColor: String
    public var privacyOverlayBackgroundOpacity: Double
    public var privacyOverlayTheme: String
    public var obfuscationPrefix: String
    public var allowDevicePasscode: Bool
    public var fallbackStrategy: String
    public var biometricPromptText: String
    public var prefix: String
    public var allowCachedAuthentication: Bool
    public var cachedAuthenticationTimeoutMs: Int
    public var cryptoStrategy: String
    public var keySize: Int
    public var maxBiometricAttempts: Int
    public var lockoutDurationMs: Int
    public var requireFreshAuthenticationMs: Int
    public var encryptionAlgorithm: String
    public var enableICloudKeychainSync: Bool
    public var persistSessionState: Bool

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultLockAfterMs: Int = 60_000
    private static let defaultEnablePrivacyScreen: Bool = true
    private static let defaultPrivacyOverlayText: String = ""
    private static let defaultPrivacyOverlayImageName: String = ""
    private static let defaultPrivacyOverlayShowText: Bool = true
    private static let defaultPrivacyOverlayShowImage: Bool = true
    private static let defaultPrivacyOverlayTextColor: String = ""
    private static let defaultPrivacyOverlayBackgroundOpacity: Double = -1
    private static let defaultPrivacyOverlayTheme: String = "system"
    private static let defaultObfuscationPrefix: String = "ftrss_"
    private static let defaultAllowDevicePasscode: Bool = true
    private static let defaultFallbackStrategy: String = "systemDefault"
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
    private static let defaultPersistSessionState: Bool = false

    // MARK: - Initialization

    // Initializes configuration from the Capacitor plugin config.
    // - Parameter plugin: CAPPlugin instance used to access typed config values.
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()
        func bool(_ key: String, _ defaultValue: Bool) -> Bool { config.getBoolean(key, defaultValue) }
        func int(_ key: String, _ defaultValue: Int) -> Int { config.getInt(key, defaultValue) }
        func string(_ key: String, _ defaultValue: String) -> String {
            config.getString(key, defaultValue) ?? defaultValue
        }

        self.verboseLogging = bool(Keys.verboseLogging, Self.defaultVerboseLogging)
        let defaultLogLevel = self.verboseLogging ? "debug" : "info"
        self.logLevel = string(Keys.logLevel, defaultLogLevel)
        self.lockAfterMs = int(Keys.lockAfterMs, Self.defaultLockAfterMs)
        self.enablePrivacyScreen = bool(Keys.enablePrivacyScreen, Self.defaultEnablePrivacyScreen)
        self.privacyOverlayText = string(Keys.privacyOverlayText, Self.defaultPrivacyOverlayText)
        self.privacyOverlayImageName = string(Keys.privacyOverlayImageName, Self.defaultPrivacyOverlayImageName)
        self.privacyOverlayShowText = bool(Keys.privacyOverlayShowText, Self.defaultPrivacyOverlayShowText)
        self.privacyOverlayShowImage = bool(Keys.privacyOverlayShowImage, Self.defaultPrivacyOverlayShowImage)
        self.privacyOverlayTextColor = string(Keys.privacyOverlayTextColor, Self.defaultPrivacyOverlayTextColor)
        let opacityRaw = string(Keys.privacyOverlayBackgroundOpacity, "")
        self.privacyOverlayBackgroundOpacity = Double(opacityRaw) ?? Self.defaultPrivacyOverlayBackgroundOpacity
        self.privacyOverlayTheme = string(Keys.privacyOverlayTheme, Self.defaultPrivacyOverlayTheme)
        self.obfuscationPrefix = string(Keys.obfuscationPrefix, Self.defaultObfuscationPrefix)
        self.allowDevicePasscode = bool(Keys.allowDevicePasscode, Self.defaultAllowDevicePasscode)
        self.fallbackStrategy = string(Keys.fallbackStrategy, Self.defaultFallbackStrategy)
        self.biometricPromptText = string(Keys.biometricPromptText, Self.defaultBiometricPromptText)
        self.prefix = string(Keys.prefix, Self.defaultPrefix)
        self.allowCachedAuthentication = bool(Keys.allowCachedAuthentication, Self.defaultAllowCachedAuthentication)
        self.cachedAuthenticationTimeoutMs = int(
            Keys.cachedAuthenticationTimeoutMs,
            Self.defaultCachedAuthenticationTimeoutMs
        )
        self.cryptoStrategy = string(Keys.cryptoStrategy, Self.defaultCryptoStrategy)
        self.keySize = int(Keys.keySize, Self.defaultKeySize)
        self.maxBiometricAttempts = int(Keys.maxBiometricAttempts, Self.defaultMaxBiometricAttempts)
        self.lockoutDurationMs = int(Keys.lockoutDurationMs, Self.defaultLockoutDurationMs)
        self.requireFreshAuthenticationMs = int(
            Keys.requireFreshAuthenticationMs,
            Self.defaultRequireFreshAuthenticationMs
        )
        self.encryptionAlgorithm = string(Keys.encryptionAlgorithm, Self.defaultEncryptionAlgorithm)
        self.enableICloudKeychainSync = bool(Keys.enableICloudKeychainSync, Self.defaultEnableICloudKeychainSync)
        self.persistSessionState = bool(Keys.persistSessionState, Self.defaultPersistSessionState)
    }
}
