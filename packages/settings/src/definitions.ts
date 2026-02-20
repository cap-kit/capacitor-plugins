/// <reference types="@capacitor/cli" />

/**
 * Capacitor configuration extension for the Settings plugin.
 *
 * Configuration values defined here can be provided under the `plugins.Settings`
 * key inside `capacitor.config.ts`.
 *
 * These values are:
 * - read natively at build/runtime
 * - NOT accessible from JavaScript at runtime
 * - treated as read-only static configuration
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the Settings plugin.
     */
    Settings?: SettingsConfig;
  }
}

/**
 * Static configuration options for the Settings plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface SettingsConfig {
  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information is printed
   * to the native console (Logcat on Android, Xcode on iOS).
   *
   * This option affects native logging behavior only and
   * has no impact on the JavaScript API.
   *
   * @default false
   * @example true
   * @since 8.0.0
   */
  verboseLogging?: boolean;
}

/**
 * Standardized error codes used by the Settings plugin.
 *
 * These codes are returned when a Promise is rejected and
 * allow consumers to implement programmatic error handling.
 *
 * @since 8.0.0
 */
export enum SettingsErrorCode {
  /** The device does not have the requested hardware or the feature is not available on this platform. */
  UNAVAILABLE = 'UNAVAILABLE',
  /** The user cancelled an interactive flow. */
  CANCELLED = 'CANCELLED',
  /** The user denied the permission or the feature is disabled by the OS. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The plugin failed to initialize or perform an operation. */
  INIT_FAILED = 'INIT_FAILED',
  /** The input provided to the plugin method is invalid, missing, or malformed. */
  INVALID_INPUT = 'INVALID_INPUT',
  /** The requested type is not valid or supported. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
  /** The requested resource does not exist. */
  NOT_FOUND = 'NOT_FOUND',
  /** The operation conflicts with the current state. */
  CONFLICT = 'CONFLICT',
  /** The operation did not complete within the expected time. */
  TIMEOUT = 'TIMEOUT',
}

/**
 * Result object returned by the `getPluginVersion()` method.
 */
export interface PluginVersionResult {
  /**
   * The native plugin version string.
   */
  version: string;
}

/**
 * Structured error object returned when a Settings plugin operation fails.
 *
 * When a method fails, the Promise is rejected with an object
 * conforming to this interface.
 */
export interface SettingsError {
  /**
   * Human-readable error description.
   */
  message: string;

  /**
   * Machine-readable error code.
   */
  code: SettingsErrorCode;
}

/**
 * Platform-specific options for opening system settings.
 *
 * This interface allows specifying settings options for both
 * Android and iOS in a single call.
 *
 * @remarks
 * The plugin will use the option corresponding to the
 * current runtime platform.
 *
 * Unsupported or unavailable options on the current platform
 * will result in a rejected Promise with code:
 * `SettingsErrorCode.UNAVAILABLE`
 *
 * - iOS: if `optionIOS` is missing or empty, rejects with `SettingsErrorCode.INVALID_INPUT`.
 * - Android: if `optionAndroid` is missing or empty, rejects with `SettingsErrorCode.INVALID_INPUT`.
 *
 * Availability and behavior depend on platform-specific
 * system capabilities and restrictions.
 */
export interface PlatformOptions {
  /**
   * Android settings option to open.
   *
   * Used only when running on Android.
   * Mapped internally to a system Intent.
   *
   * If running on Android and this option is missing or empty,
   * the promise will be rejected with `SettingsErrorCode.INVALID_INPUT`.
   */
  optionAndroid?: AndroidSettings;

  /**
   * iOS settings option to open.
   *
   * Used only when running on iOS.
   * Mapped internally to an iOS settings URL scheme.
   *
   * If running on iOS and this option is missing or empty,
   * the promise will be rejected with `SettingsErrorCode.INVALID_INPUT`.
   */
  optionIOS?: IOSSettings;
}

/**
 * Android-specific options for opening system settings.
 *
 * @platform Android
 *
 * @remarks
 * Android settings are opened via system Intents.
 * Availability and behavior depend on:
 *
 * - Android version
 * - device manufacturer (OEM)
 * - system configuration and user restrictions
 *
 * Some settings rely on intents that are not part of the public
 * Android SDK and may not be available on all devices.
 *
 * When a requested settings screen cannot be resolved, the plugin
 * rejects with code: `SettingsErrorCode.UNAVAILABLE`
 *
 * This interface does NOT guarantee that a given settings screen
 * will open successfully on all devices.
 */
export interface AndroidOptions {
  /**
   * The Android settings section to open.
   *
   * Each value maps to a specific Android system Intent.
   * Support varies depending on the device and OS version.
   *
   * @example AndroidSettings.Wifi
   * @example AndroidSettings.Bluetooth
   * @example AndroidSettings.ApplicationDetails
   */
  option: AndroidSettings;
}

/**
 * iOS-specific options for opening system settings.
 *
 * @platform iOS
 *
 * @remarks
 * Apple officially supports opening only the app-specific settings screen.
 * All other settings destinations rely on undocumented URL schemes and may:
 *
 * - behave differently across iOS versions
 * - stop working in future iOS releases
 * - be restricted or rejected during App Store review
 *
 * For unsupported or unavailable settings, the plugin rejects with
 * code: `SettingsErrorCode.UNAVAILABLE`
 *
 * This interface does NOT guarantee that a given settings screen
 * will open successfully on all devices.
 */
export interface IOSOptions {
  /**
   * The iOS settings section to open.
   *
   * Most values correspond to internal iOS URL schemes.
   * Availability depends on the iOS version and system configuration.
   *
   * @example IOSSettings.App
   * @example IOSSettings.AppNotification
   * @example IOSSettings.Bluetooth
   */
  option: IOSSettings;
}

/**
 * Enumeration of supported Android system settings.
 *
 * @platform Android
 *
 * @remarks
 * Android settings are opened via system Intents.
 * Availability depends on:
 *
 * - Android version
 * - device manufacturer (OEM)
 * - system configuration and user restrictions
 *
 * Some intents are not part of the public Android SDK and may not be
 * available on all devices.
 *
 * When a requested settings screen cannot be resolved, the plugin
 * rejects with code: `SettingsErrorCode.UNAVAILABLE`
 *
 * @since 8.0.0
 */
export enum AndroidSettings {
  /**
   * Opens Accessibility settings.
   */
  Accessibility = 'accessibility',

  /**
   * Opens Airplane Mode settings.
   */
  AirplaneMode = 'airplane_mode',

  /**
   * Opens Access Point Name (APN) settings.
   */
  Apn = 'apn',

  /**
   * Opens the Application Details screen for the current app.
   */
  ApplicationDetails = 'application_details',

  /**
   * Opens Application settings.
   */
  Application = 'application',

  /**
   * Opens app-specific notification settings.
   */
  AppNotification = 'app_notification',

  /**
   * Opens Battery Optimization settings.
   *
   * Allows managing apps excluded from battery optimizations.
   */
  BatteryOptimization = 'battery_optimization',

  /**
   * Opens Bluetooth settings.
   */
  Bluetooth = 'bluetooth',

  /**
   * Opens Cast device settings.
   */
  Cast = 'cast',

  /**
   * Opens Data Roaming settings.
   */
  DataRoaming = 'data_roaming',

  /**
   * Opens Date & Time settings.
   */
  Date = 'date',

  /**
   * Opens Display settings.
   */
  Display = 'display',

  /**
   * Opens Home app selection settings.
   */
  Home = 'home',

  /**
   * Opens Location Services settings.
   */
  Location = 'location',

  /**
   * Opens NFC settings.
   */
  Nfc = 'nfc',

  /**
   * Opens NFC Sharing settings.
   */
  NfcSharing = 'nfcsharing',

  /**
   * Opens NFC Payment settings.
   */
  NfcPayment = 'nfc_payment',

  /**
   * Opens Print settings.
   */
  Print = 'print',

  /**
   * Opens Security settings.
   */
  Security = 'security',

  /**
   * Opens the main System Settings screen.
   */
  Settings = 'settings',

  /**
   * Opens Sound & Volume settings.
   */
  Sound = 'sound',

  /**
   * Opens Internal Storage settings.
   */
  Storage = 'storage',

  /**
   * Opens Text-to-Speech (TTS) settings.
   *
   * Uses a non-public intent on some devices.
   */
  TextToSpeech = 'text_to_speech',

  /**
   * Opens Usage Access settings.
   *
   * Allows managing apps with access to usage data.
   */
  Usage = 'usage',

  /**
   * Opens VPN settings.
   */
  VPN = 'vpn',

  /**
   * Opens Wi-Fi settings.
   */
  Wifi = 'wifi',

  /**
   * Opens Zen Mode (Do Not Disturb) settings.
   *
   * This uses a non-public intent and may not work on all devices.
   */
  ZenMode = 'zen_mode',

  /**
   * Opens Zen Mode Priority settings.
   */
  ZenModePriority = 'zen_mode_priority',

  /**
   * Opens Zen Mode Blocked Effects settings.
   */
  ZenModeBlockedEffects = 'zen_mode_blocked_effects',
}

/**
 * Enumeration of supported iOS system settings.
 *
 * @platform iOS
 *
 * @remarks
 * Apple officially supports opening only the app-specific settings screen.
 * All other values rely on undocumented URL schemes and may:
 *
 * - behave differently across iOS versions
 * - stop working in future releases
 *
 * - be restricted or rejected during App Store review
 *
 * Availability is best-effort and not guaranteed.
 *
 * @since 8.0.0
 */
export enum IOSSettings {
  /**
   * Opens the app-specific settings screen.
   *
   * This is the ONLY settings destination officially supported by Apple
   * and is considered App Store safe.
   */
  App = 'app',

  /**
   * Opens the app-specific notification settings.
   *
   * - iOS 16+: opens the dedicated notification settings screen
   * - iOS <16: falls back to the app settings screen
   */
  AppNotification = 'appNotification',

  /**
   * Opens iOS "About" settings.
   */
  About = 'about',

  /**
   * Opens Auto-Lock settings.
   */
  AutoLock = 'autoLock',

  /**
   * Opens Bluetooth settings.
   */
  Bluetooth = 'bluetooth',

  /**
   * Opens Date & Time settings.
   */
  DateTime = 'dateTime',

  /**
   * Opens FaceTime settings.
   */
  FaceTime = 'facetime',

  /**
   * Opens General settings.
   */
  General = 'general',

  /**
   * Opens Keyboard settings.
   */
  Keyboard = 'keyboard',

  /**
   * Opens iCloud settings.
   */
  ICloud = 'iCloud',

  /**
   * Opens iCloud Storage & Backup settings.
   */
  ICloudStorageBackup = 'iCloudStorageBackup',

  /**
   * Opens Language & Region (International) settings.
   */
  International = 'international',

  /**
   * Opens Location Services settings.
   */
  LocationServices = 'locationServices',

  /**
   * Opens Music settings.
   */
  Music = 'music',

  /**
   * Opens Notes settings.
   */
  Notes = 'notes',

  /**
   * Opens Notifications settings.
   *
   * Note: this is the global notifications screen,
   * not app-specific notifications.
   */
  Notifications = 'notifications',

  /**
   * Opens Phone settings.
   */
  Phone = 'phone',

  /**
   * Opens Photos settings.
   */
  Photos = 'photos',

  /**
   * Opens Managed Configuration profiles list.
   */
  ManagedConfigurationList = 'managedConfigurationList',

  /**
   * Opens Reset settings.
   */
  Reset = 'reset',

  /**
   * Opens Ringtone settings.
   */
  Ringtone = 'ringtone',

  /**
   * Opens Sounds settings.
   */
  Sounds = 'sounds',

  /**
   * Opens Software Update settings.
   */
  SoftwareUpdate = 'softwareUpdate',

  /**
   * Opens App Store settings.
   */
  Store = 'store',

  /**
   * Opens App Tracking Transparency settings.
   *
   * Available on iOS 14+.
   */
  Tracking = 'tracking',

  /**
   * Opens Wallpaper settings.
   */
  Wallpaper = 'wallpaper',

  /**
   * Opens Wi-Fi settings.
   */
  WiFi = 'wifi',

  /**
   * Opens Personal Hotspot (Tethering) settings.
   */
  Tethering = 'tethering',

  /**
   * Opens Do Not Disturb settings.
   */
  DoNotDisturb = 'doNotDisturb',

  /**
   * Opens Touch ID / Passcode settings.
   */
  TouchIdPasscode = 'touchIdPasscode',

  /**
   * Opens Screen Time settings.
   */
  ScreenTime = 'screenTime',

  /**
   * Opens Accessibility settings.
   */
  Accessibility = 'accessibility',

  /**
   * Opens VPN settings.
   */
  VPN = 'vpn',
}

/**
 * Public JavaScript API for the Settings Capacitor plugin.
 *
 * This plugin uses a standard Promise rejection model for errors.
 *
 * All methods return a Promise that:
 * - resolves when the operation is successful
 * - rejects when the operation fails or is not supported
 *
 * When rejected, the error object contains a machine-readable `code`
 * from `SettingsErrorCode`.
 */
export interface SettingsPlugin {
  /**
   * Opens the specified settings option on the current platform.
   * On Web, this method is not supported and will reject.
   *
   * @param options Platform-specific settings options.
   * @returns A promise resolving when the settings screen is opened.
   *
   * @since 8.0.0
   */
  open(options: PlatformOptions): Promise<void>;

  /**
   * Opens a specific system settings section. (iOS Only)
   *
   * This method is a platform-specific helper. For cross-platform usage,
   * prefer the generic `open()` method with `PlatformOptions`.
   *
   * @platforms iOS
   *
   * @remarks
   * Apple officially supports opening only the app-specific settings screen.
   * Other settings destinations rely on undocumented URL schemes and may:
   * - behave differently across iOS versions
   * - stop working in future releases
   * - be restricted or rejected during App Store review
   *
   * When unavailable or unsupported, the method rejects with
   * code: `SettingsErrorCode.UNAVAILABLE`.
   *
   * @param options iOS settings options.
   * @returns A promise resolving when the settings screen is opened.
   *
   * @since 8.0.0
   */
  openIOS(options: IOSOptions): Promise<void>;

  /**
   * Opens a specific Android Intent. (Android Only)
   * On Web, this method is not supported and will reject.
   *
   * This method is a platform-specific helper. For cross-platform usage,
   * prefer the generic `open()` method with `PlatformOptions`.
   *
   * @platforms Android
   *
   * @param options Android settings options.
   * @returns A promise resolving when the settings screen is opened.
   *
   * @since 8.0.0
   */
  openAndroid(options: AndroidOptions): Promise<void>;

  /**
   * Returns the native plugin version.
   *
   * The returned version corresponds to the native implementation
   * bundled with the application.
   *
   * @returns A promise resolving to the plugin version.
   *
   * @example
   * ```ts
   * const { version } = await Settings.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
