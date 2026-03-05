package io.capkit.fortress.config

import com.getcapacitor.Plugin

/**
 * Plugin configuration container.
 *
 * This class is responsible for reading and exposing
 * static configuration values defined under the
 * `Fortress` key in capacitor.config.ts.
 *
 * Configuration rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime input
 * - Accessible only from native code
 *
 * @property verboseLogging Enables verbose native logging.
 */
class Config(
  plugin: Plugin,
) {
  // -----------------------------------------------------------------------------
  // Configuration Keys
  // -----------------------------------------------------------------------------

  /**
   * Centralized definition of configuration keys.
   * Avoids string duplication and typos.
   */
  private object Keys {
    const val VERBOSE_LOGGING = "verboseLogging"
    const val LOG_LEVEL = "logLevel"
    const val LOCK_AFTER_MS = "lockAfterMs"
    const val ENABLE_PRIVACY_SCREEN = "enablePrivacyScreen"
    const val PRIVACY_OVERLAY_TEXT = "privacyOverlayText"
    const val PRIVACY_OVERLAY_IMAGE_NAME = "privacyOverlayImageName"
    const val PRIVACY_OVERLAY_SHOW_TEXT = "privacyOverlayShowText"
    const val PRIVACY_OVERLAY_SHOW_IMAGE = "privacyOverlayShowImage"
    const val PRIVACY_OVERLAY_TEXT_COLOR = "privacyOverlayTextColor"
    const val PRIVACY_OVERLAY_BACKGROUND_OPACITY = "privacyOverlayBackgroundOpacity"
    const val PRIVACY_OVERLAY_THEME = "privacyOverlayTheme"
    const val OBFUSCATION_PREFIX = "obfuscationPrefix"
    const val REQUIRE_STRONGBOX = "requireStrongBox"
    const val ALLOW_DEVICE_PASSCODE = "allowDevicePasscode"
    const val FALLBACK_STRATEGY = "fallbackStrategy"
    const val BIOMETRIC_PROMPT_TEXT = "biometricPromptText"
    const val PREFIX = "prefix"
    const val ALLOW_CACHED_AUTHENTICATION = "allowCachedAuthentication"
    const val CACHED_AUTHENTICATION_TIMEOUT_MS = "cachedAuthenticationTimeoutMs"
    const val CRYPTO_STRATEGY = "cryptoStrategy"
    const val KEY_SIZE = "keySize"
    const val MAX_BIOMETRIC_ATTEMPTS = "maxBiometricAttempts"
    const val LOCKOUT_DURATION_MS = "lockoutDurationMs"
    const val REQUIRE_FRESH_AUTHENTICATION_MS = "requireFreshAuthenticationMs"
    const val ENCRYPTION_ALGORITHM = "encryptionAlgorithm"
    const val PERSIST_SESSION_STATE = "persistSessionState"
  }

  // -----------------------------------------------------------------------------
  // Public Configuration Values
  // -----------------------------------------------------------------------------

  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information
   * is printed to Logcat.
   *
   * @default false
   */
  var verboseLogging: Boolean
  var logLevel: String
  var lockAfterMs: Int
  var enablePrivacyScreen: Boolean
  var privacyOverlayText: String
  var privacyOverlayImageName: String
  var privacyOverlayShowText: Boolean
  var privacyOverlayShowImage: Boolean
  var privacyOverlayTextColor: String
  var privacyOverlayBackgroundOpacity: Double
  var privacyOverlayTheme: String
  var obfuscationPrefix: String
  var requireStrongBox: Boolean
  var allowDevicePasscode: Boolean
  var fallbackStrategy: String
  var biometricPromptText: String
  var prefix: String
  var allowCachedAuthentication: Boolean
  var cachedAuthenticationTimeoutMs: Int
  var cryptoStrategy: String
  var keySize: Int
  var maxBiometricAttempts: Int
  var lockoutDurationMs: Int
  var requireFreshAuthenticationMs: Int
  var encryptionAlgorithm: String
  var persistSessionState: Boolean

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val config = plugin.config

    // Verbose logging flag
    verboseLogging =
      config.getBoolean(Keys.VERBOSE_LOGGING, false)

    logLevel =
      config.getString(Keys.LOG_LEVEL, if (verboseLogging) "debug" else "info")
        ?: if (verboseLogging) {
          "debug"
        } else {
          "info"
        }

    lockAfterMs =
      config.getInt(Keys.LOCK_AFTER_MS, 60000)

    enablePrivacyScreen =
      config.getBoolean(Keys.ENABLE_PRIVACY_SCREEN, true)

    privacyOverlayText =
      config.getString(Keys.PRIVACY_OVERLAY_TEXT, "") ?: ""

    privacyOverlayImageName =
      config.getString(Keys.PRIVACY_OVERLAY_IMAGE_NAME, "") ?: ""

    privacyOverlayShowText =
      config.getBoolean(Keys.PRIVACY_OVERLAY_SHOW_TEXT, true)

    privacyOverlayShowImage =
      config.getBoolean(Keys.PRIVACY_OVERLAY_SHOW_IMAGE, true)

    privacyOverlayTextColor =
      config.getString(Keys.PRIVACY_OVERLAY_TEXT_COLOR, "") ?: ""

    privacyOverlayBackgroundOpacity =
      config.getString(Keys.PRIVACY_OVERLAY_BACKGROUND_OPACITY, "")?.toDoubleOrNull() ?: -1.0

    privacyOverlayTheme =
      config.getString(Keys.PRIVACY_OVERLAY_THEME, "system") ?: "system"

    obfuscationPrefix =
      config.getString(Keys.OBFUSCATION_PREFIX, "ftrss_")

    // Read requireStrongBox from capacitor.config.ts
    requireStrongBox =
      config.getBoolean(Keys.REQUIRE_STRONGBOX, false)

    allowDevicePasscode =
      config.getBoolean(Keys.ALLOW_DEVICE_PASSCODE, true)

    fallbackStrategy =
      config.getString(Keys.FALLBACK_STRATEGY, "systemDefault") ?: "systemDefault"

    biometricPromptText =
      config.getString(Keys.BIOMETRIC_PROMPT_TEXT, "Cancel") ?: "Cancel"

    prefix =
      config.getString(Keys.PREFIX, "") ?: ""

    allowCachedAuthentication =
      config.getBoolean(Keys.ALLOW_CACHED_AUTHENTICATION, false)

    cachedAuthenticationTimeoutMs =
      config.getInt(Keys.CACHED_AUTHENTICATION_TIMEOUT_MS, 30000)

    cryptoStrategy =
      config.getString(Keys.CRYPTO_STRATEGY, "auto") ?: "auto"

    keySize =
      config.getInt(Keys.KEY_SIZE, 2048)

    maxBiometricAttempts =
      config.getInt(Keys.MAX_BIOMETRIC_ATTEMPTS, 5)

    lockoutDurationMs =
      config.getInt(Keys.LOCKOUT_DURATION_MS, 30000)

    requireFreshAuthenticationMs =
      config.getInt(Keys.REQUIRE_FRESH_AUTHENTICATION_MS, 0)

    encryptionAlgorithm =
      config.getString(Keys.ENCRYPTION_ALGORITHM, "AES-GCM") ?: "AES-GCM"

    persistSessionState =
      config.getBoolean(Keys.PERSIST_SESSION_STATE, false)
  }
}
