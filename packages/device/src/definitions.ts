/**
 * @file definitions.ts
 * This file defines the public TypeScript contract of the Device plugin:
 * the plugin configuration model, standardized error codes, the device
 * data model, and the platform-agnostic DevicePlugin API surface.
 *
 * Copyright (c) CapKit Team — MIT License.
 * Portions Copyright (c) Ionic / Capacitor team — MIT License.
 */

/// <reference types="@capacitor/cli" />

import { PluginListenerHandle } from '@capacitor/core';

/**
 * Extension of the Capacitor CLI configuration to include specific settings for Device.
 * This allows users to configure the plugin via capacitor.config.ts or capacitor.config.json.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the Device plugin.
     */
    Device?: DeviceConfig;
  }
}

/**
 * Static configuration options for the Device plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface DeviceConfig {
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
 * Standardized error codes used by the Device plugin.
 *
 * These codes are returned as part of structured error objects
 * and allow consumers to implement programmatic error handling.
 *
 * Defined as a readonly object literal rather than an enum to satisfy
 * CapKit strict TypeScript rules (no enums; use `as const` objects).
 *
 * @since 8.0.0
 */
export const DeviceErrorCode = {
  /** The device does not have the requested hardware or the feature is not available on this platform. */
  UNAVAILABLE: 'UNAVAILABLE',
  /** The user cancelled an interactive flow. */
  CANCELLED: 'CANCELLED',
  /** The user denied the permission or the feature is disabled by the OS. */
  PERMISSION_DENIED: 'PERMISSION_DENIED',
  /** The plugin failed to initialize or perform an operation. */
  INIT_FAILED: 'INIT_FAILED',
  /** The input provided to the plugin method is invalid, missing, or malformed. */
  INVALID_INPUT: 'INVALID_INPUT',
  /** The requested type is not valid or supported. */
  UNKNOWN_TYPE: 'UNKNOWN_TYPE',
  /** The requested resource does not exist. */
  NOT_FOUND: 'NOT_FOUND',
  /** The operation conflicts with the current state. */
  CONFLICT: 'CONFLICT',
  /** The operation did not complete within the expected time. */
  TIMEOUT: 'TIMEOUT',
} as const;

/**
 * Union type derived from the `DeviceErrorCode` object.
 *
 * Consumers can use this to obtain valid error code values at the type level
 * without depending on the runtime enum object.
 *
 * @since 8.0.0
 */
export type DeviceErrorCode = (typeof DeviceErrorCode)[keyof typeof DeviceErrorCode];

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
 * Structured error object returned by Device plugin operations.
 *
 * This object allows consumers to handle errors without relying
 * on exception-based control flow.
 */
export interface DeviceError {
  /**
   * Human-readable error description.
   */
  message: string;

  /**
   * Machine-readable error code.
   */
  code: DeviceErrorCode;
}

export type OperatingSystem = 'ios' | 'android' | 'windows' | 'mac' | 'unknown';

export interface DeviceId {
  /**
   * The identifier of the device as available to the app. This identifier may change
   * on modern mobile platforms that only allow per-app install ids.
   *
   * On iOS, the identifier is a UUID that uniquely identifies a device to the app's vendor
   * ([read more](https://developer.apple.com/documentation/uikit/uidevice/1620059-identifierforvendor)).
   *
   * On Android 8+, __the identifier is a 64-bit number (expressed as a hexadecimal string)__, unique to each combination
   * of app-signing key, user, and device
   * ([read more](https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID)).
   *
   * On web, a random identifier is generated and stored on localStorage for subsequent calls.
   * If localStorage is not available a new random identifier will be generated on every call.
   *
   * @since 8.0.0
   */
  identifier: string;
}

export interface DeviceInfo {
  /**
   * The name of the device. For example, "John's iPhone".
   *
   * This is only supported on iOS and Android 7.1 or above.
   *
   * On iOS 16+ this will return a generic device name without the appropriate
   * [entitlements](https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_developer_device-information_user-assigned-device-name).
   *
   * @since 8.0.0
   */
  name?: string;

  /**
   * The device model. For example, "iPhone13,4".
   *
   * @since 8.0.0
   */
  model: string;

  /**
   * The device platform (lowercase).
   *
   * @since 8.0.0
   */
  platform: 'ios' | 'android' | 'web';

  /**
   * The operating system of the device.
   *
   * @since 8.0.0
   */
  operatingSystem: OperatingSystem;

  /**
   * The version of the device OS.
   *
   * @since 8.0.0
   */
  osVersion: string;

  /**
   * The iOS version number.
   *
   * Only available on iOS.
   *
   * Multi-part version numbers are crushed down into an integer padded to two-digits, ex: `"16.3.1"` -> `160301`
   *
   * @since 8.0.0
   */
  iOSVersion?: number;

  /**
   * The Android SDK version number.
   *
   * Only available on Android.
   *
   * @since 8.0.0
   */
  androidSDKVersion?: number;

  /**
   * The manufacturer of the device.
   *
   * @since 8.0.0
   */
  manufacturer: string;

  /**
   * Whether the app is running in a simulator/emulator.
   *
   * @since 8.0.0
   */
  isVirtual: boolean;

  /**
   * Approximate memory used by the current app, in bytes. Divide by
   * 1048576 to get the number of MBs used.
   *
   * @since 8.0.0
   */
  memUsed?: number;

  /**
   * How much free disk space is available on the normal data storage
   * path for the os, in bytes.
   *
   * On Android it returns the free disk space on the "system"
   * partition holding the core Android OS.
   * On iOS this value is not accurate.
   *
   * @deprecated Use `realDiskFree`.
   * @since 8.0.0
   */
  diskFree?: number;

  /**
   * The total size of the normal data storage path for the OS, in bytes.
   *
   * On Android it returns the disk space on the "system"
   * partition holding the core Android OS.
   *
   * @deprecated Use `realDiskTotal`.
   * @since 8.0.0
   */
  diskTotal?: number;

  /**
   * How much free disk space is available on the normal data storage, in bytes.
   *
   * @since 8.0.0
   */
  realDiskFree?: number;

  /**
   * The total size of the normal data storage path, in bytes.
   *
   * @since 8.0.0
   */
  realDiskTotal?: number;

  /**
   * The web view browser version
   *
   * @since 8.0.0
   */
  webViewVersion: string;
}

export interface BatteryInfo {
  /**
   * A percentage (0 to 1) indicating how much the battery is charged.
   *
   * @since 8.0.0
   */
  batteryLevel?: number;

  /**
   * Whether the device is charging.
   *
   * @since 8.0.0
   */
  isCharging?: boolean;
}

/**
 * Extended battery information including charge source and detailed state.
 *
 * @since 8.0.0
 */
export interface BatteryExtras {
  /**
   * The current charge source.
   *
   * - `"ac"` — AC adapter
   * - `"usb"` — USB port
   * - `"wireless"` — Wireless charging
   * - `"unknown"` — Charge source not determinable
   *
   * @since 8.0.0
   */
  chargeSource: 'ac' | 'usb' | 'wireless' | 'unknown';

  /**
   * The detailed battery state.
   *
   * - `"unknown"` — Battery state is unknown
   * - `"unplugged"` — Not connected to a power source
   * - `"charging"` — Connected and charging
   * - `"full"` — Connected and fully charged
   * - `"not-charging"` — Connected but not charging (e.g. battery temperature limit)
   *
   * @since 8.0.0
   */
  detailedState: 'unknown' | 'unplugged' | 'charging' | 'full' | 'not-charging';

  /**
   * Battery health status.
   *
   * Only available on Android.
   *
   * - `"good"` — Battery is in good condition
   * - `"overheat"` — Battery is overheating
   * - `"dead"` — Battery is dead
   * - `"unknown"` — Health status not available
   * - `"cold"` — Battery is too cold
   * - `"unspecified"` — Health status not reported
   *
   * @since 8.0.0
   */
  health?: 'good' | 'overheat' | 'dead' | 'unknown' | 'cold' | 'unspecified';

  /**
   * Battery temperature in degrees Celsius.
   *
   * Only available on Android. iOS does not expose battery temperature
   * through public APIs.
   *
   * @since 8.0.0
   */
  temperature?: number;
}

/**
 * Display characteristics of the device screen.
 *
 * @since 8.0.0
 */
export interface DisplayInfo {
  /**
   * Screen width in physical pixels.
   *
   * @since 8.0.0
   */
  widthPx: number;

  /**
   * Screen height in physical pixels.
   *
   * @since 8.0.0
   */
  heightPx: number;

  /**
   * Screen density in DPI (dots per inch).
   *
   * Common values: 160 (mdpi), 240 (hdpi), 320 (xhdpi), 480 (xxhdpi), 640 (xxxhdpi).
   *
   * @since 8.0.0
   */
  densityDpi: number;

  /**
   * Display scale factor.
   *
   * On iOS this corresponds to `UIScreen.main.scale` (e.g. 2.0, 3.0).
   * On Android this is `DisplayMetrics.density` (e.g. 1.0, 1.5, 2.0, 3.0).
   *
   * @since 8.0.0
   */
  scale: number;

  /**
   * Maximum display refresh rate in Hz.
   *
   * On iOS this corresponds to `UIScreen.main.maximumFramesPerWindow`.
   * On Android this corresponds to `Display.getRefreshRate()`.
   *
   * @since 8.0.0
   */
  refreshRateHz: number;
}

/**
 * Device configuration and user preferences.
 *
 * @since 8.0.0
 */
export interface DeviceConfiguration {
  /**
   * Current interface orientation.
   *
   * - `"portrait"` — Device is in portrait orientation
   * - `"landscape"` — Device is in landscape orientation
   * - `"unknown"` — Orientation cannot be determined
   *
   * @since 8.0.0
   */
  orientation: 'portrait' | 'landscape' | 'unknown';

  /**
   * Whether the device is in dark mode.
   *
   * On iOS this corresponds to `UITraitCollection.userInterfaceStyle`.
   * On Android this corresponds to `Configuration.uiMode`.
   *
   * @since 8.0.0
   */
  isDarkMode: boolean;

  /**
   * User's preferred text size multiplier.
   *
   * A value of `1.0` means default text size. Values above 1.0 indicate
   * the user has increased text size for accessibility.
   *
   * On iOS this corresponds to `UIContentSizeCategory` converted to a numeric scale.
   * On Android this corresponds to `Configuration.fontScale`.
   *
   * @since 8.0.0
   */
  fontScale: number;

  /**
   * Device form factor / idiom.
   *
   * - `"phone"` — iPhone or small Android phone
   * - `"tablet"` — iPad or Android tablet
   * - `"desktop"` — Mac Catalyst or desktop mode
   * - `"unknown"` — Cannot determine form factor
   *
   * @since 8.0.0
   */
  idiom: 'phone' | 'tablet' | 'desktop' | 'unknown';

  /**
   * Screen size category relative to standard phone dimensions.
   *
   * - `"small"` — Smaller than normal phone
   * - `"normal"` — Standard phone
   * - `"large"` — Large phone / small tablet
   * - `"xlarge"` — Tablet or larger
   * - `"unknown"` — Cannot determine
   *
   * @since 8.0.0
   */
  screenSize: 'small' | 'normal' | 'large' | 'xlarge' | 'unknown';
}

/**
 * Device power and thermal state.
 *
 * @since 8.0.0
 */
export interface PowerState {
  /**
   * Whether the device is in low power / battery saver mode.
   *
   * On iOS this corresponds to `ProcessInfo.isLowPowerModeEnabled`.
   * On Android this corresponds to `PowerManager.isPowerSaveMode()`.
   *
   * @since 8.0.0
   */
  isLowPowerMode: boolean;

  /**
   * Current thermal state of the device.
   *
   * - `"nominal"` — No thermal issues
   * - `"fair"` — Thermal level is elevated but manageable
   * - `"serious"` — Device is actively throttling performance
   * - `"critical"` — Device is severely throttling; reduce workload immediately
   *
   * On iOS this corresponds to `ProcessInfo.thermalState`.
   * On Android this is derived from battery temperature and system thermal APIs.
   *
   * @since 8.0.0
   */
  thermalState: 'nominal' | 'fair' | 'serious' | 'critical';
}

/**
 * Device memory information beyond the basic `memUsed` field.
 *
 * @since 8.0.0
 */
export interface MemoryInfo {
  /**
   * Total physical RAM available on the device, in bytes.
   *
   * @since 8.0.0
   */
  physicalRam: number;

  /**
   * Number of active CPU cores available for the app.
   *
   * This may be less than the total core count on devices with
   * heterogeneous CPU architectures (big.LITTLE, etc.).
   *
   * @since 8.0.0
   */
  cpuCores: number;

  /**
   * Standard app memory budget in MB.
   *
   * On iOS this corresponds to `ProcessInfo.physicalMemory` (full RAM available to the app).
   * On Android this corresponds to `ActivityManager.getMemoryClass()`.
   *
   * @since 8.0.0
   */
  memoryClassMb: number;

  /**
   * Whether the device is classified as a low-RAM device.
   *
   * On Android this corresponds to `ActivityManager.isLowRamDevice()`.
   * On iOS this is always `false` (Apple does not expose this classification).
   *
   * @since 8.0.0
   */
  isLowRamDevice: boolean;

  /**
   * Whether the returned values are estimated or measured.
   *
   * When `true`, the data comes from a fallback path or partial reading
   * and should not be treated as authoritative.
   *
   * @since 8.0.0
   */
  isEstimated?: boolean;

  /**
   * Current CPU usage as a percentage (0–100), measured as a delta between two samples.
   *
   * The first call always returns `null`. Subsequent calls return the average CPU
   * usage across all cores since the previous call.
   *
   * On iOS this uses `host_statistics(HOST_CPU_LOAD_INFO)` Mach API.
   * On Android this uses `/proc/stat` delta parsing.
   * On web this is always `null`.
   *
   * @since 8.0.0
   */
  cpuUsagePercent?: number | null;

  /**
   * System memory pressure level, derived from available memory and OS signals.
   *
   * - `"normal"` — Sufficient free memory
   * - `"warning"` — Available memory is below a safe threshold
   * - `"critical"` — Device is under severe memory pressure; reduce allocation immediately
   * - `"unknown"` — Pressure level cannot be determined
   *
   * On iOS this maps from `ProcessInfo.physicalMemory` vs. free memory ratio.
   * On Android this maps from `ActivityManager.MemoryInfo.lowMemory` and `availMem` thresholds.
   * On web this is always `"unknown"`.
   *
   * @since 8.0.0
   */
  memoryPressure?: 'normal' | 'warning' | 'critical' | 'unknown';
}

/**
 * Disk storage information for the primary data volume.
 *
 * @since 8.0.0
 */
export interface StorageInfo {
  /**
   * Whether the returned values are estimated or measured.
   *
   * When `true`, the underlying OS call failed and zero-filled defaults
   * were returned. Consumers should treat these values as placeholders.
   *
   * @since 8.0.0
   */
  isEstimated: boolean;

  /**
   * Total storage capacity of the volume, in bytes.
   *
   * @since 8.0.0
   */
  totalBytes: number;

  /**
   * Free (available) storage on the volume, in bytes.
   *
   * On iOS this uses `URLResourceKey.volumeAvailableCapacityForImportantUsageKey`.
   * On Android this uses `StatFs` on the data directory.
   *
   * @since 8.0.0
   */
  freeBytes: number;

  /**
   * Used storage on the volume, in bytes (`totalBytes - freeBytes`).
   *
   * @since 8.0.0
   */
  usedBytes: number;

  /**
   * Percentage of storage used (0–100).
   *
   * @since 8.0.0
   */
  usedPercent: number;
}

/**
 * System uptime information.
 *
 * @since 8.0.0
 */
export interface SystemUptime {
  /**
   * Time in seconds since the device was last booted.
   *
   * On iOS this corresponds to `ProcessInfo.systemUptime`.
   * On Android this corresponds to `SystemClock.elapsedRealtime()`.
   *
   * @since 8.0.0
   */
  uptimeSeconds: number;
}

/**
 * Application version information.
 *
 * @since 8.0.0
 */
export interface AppVersion {
  /**
   * Human-readable version string (e.g. "1.2.3").
   *
   * On iOS this corresponds to `CFBundleShortVersionString`.
   * On Android this corresponds to `PackageInfo.versionName`.
   *
   * @since 8.0.0
   */
  version: string;

  /**
   * Numeric build number used for update detection.
   *
   * On iOS this corresponds to `CFBundleVersion`.
   * On Android this corresponds to `PackageInfo.versionCode`.
   *
   * @since 8.0.0
   */
  buildNumber: number;
}

/**
 * Callback for battery charging state changes.
 *
 * The listener fires only when the charging state **changes** (e.g. the
 * charger is connected or disconnected). It is not invoked at subscription
 * time and emits nothing while the state stays the same; read
 * `getBatteryInfo()` for the current snapshot instead.
 *
 * @since 8.0.0
 */
export type BatteryChargingStateChangeListener = (info: BatteryInfo) => void;

export interface GetLanguageCodeResult {
  /**
   * Two character language code.
   *
   * @since 8.0.0
   */
  value: string;
}

export interface LanguageTag {
  /**
   * Returns a well-formed IETF BCP 47 language tag.
   *
   * @since 8.0.0
   */
  value: string;
}

/**
 * Public JavaScript API for the Device Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API.
 * All methods behave consistently across Android, iOS, and Web.
 */
export interface DevicePlugin {
  /**
   * Return an unique identifier for the device.
   *
   * @returns A promise resolving to the device identifier.
   *
   * @example
   * ```ts
   * const { identifier } = await Device.getId();
   * console.log('Device ID:', identifier);
   * ```
   *
   * @since 8.0.0
   */
  getId(): Promise<DeviceId>;

  /**
   * Return information about the underlying device/os/platform.
   *
   * @returns A promise resolving to comprehensive device information.
   *
   * @example
   * ```ts
   * const info = await Device.getInfo();
   * console.log(`${info.manufacturer} ${info.model}`);
   * console.log(`${info.operatingSystem} ${info.osVersion}`);
   * console.log(`Virtual: ${info.isVirtual}`);
   * ```
   *
   * @since 8.0.0
   */
  getInfo(): Promise<DeviceInfo>;

  /**
   * Return information about the battery.
   *
   * @returns A promise resolving to the current battery level and charging state.
   *
   * @example
   * ```ts
   * const battery = await Device.getBatteryInfo();
   * const percent = Math.round((battery.batteryLevel ?? 0) * 100);
   * console.log(`Battery: ${percent}% — ${battery.isCharging ? 'charging' : 'on battery'}`);
   * ```
   *
   * @since 8.0.0
   */
  getBatteryInfo(): Promise<BatteryInfo>;

  /**
   * Get the device's current language locale code.
   *
   * @returns A promise resolving to the two-character language code.
   *
   * @example
   * ```ts
   * const { value } = await Device.getLanguageCode();
   * console.log('Language:', value); // "en", "es", "it"
   * ```
   *
   * @since 8.0.0
   */
  getLanguageCode(): Promise<GetLanguageCodeResult>;

  /**
   * Get the device's current language locale tag.
   *
   * @returns A promise resolving to the BCP 47 language tag.
   *
   * @example
   * ```ts
   * const { value } = await Device.getLanguageTag();
   * console.log('Language tag:', value); // "en-US", "es-ES", "it-IT"
   * ```
   *
   * @since 8.0.0
   */
  getLanguageTag(): Promise<LanguageTag>;

  /**
   * Return extended battery information including charge source and detailed state.
   *
   * The `health` and `temperature` fields are only available on Android.
   * On web, throws `unavailable`.
   *
   * @returns A promise resolving to battery charge source, detailed state, and platform-specific extras.
   *
   * @example
   * ```ts
   * const extras = await Device.getBatteryExtras();
   * console.log(`Charging via: ${extras.chargeSource}`); // "ac", "usb", "wireless"
   * console.log(`State: ${extras.detailedState}`);       // "charging", "full", "unplugged"
   *
   * // Android-only fields
   * if (extras.health) {
   *   console.log(`Battery health: ${extras.health}`);   // "good", "overheat", "dead"
   * }
   * if (extras.temperature) {
   *   console.log(`Temperature: ${extras.temperature}°C`);
   * }
   * ```
   *
   * @since 8.0.0
   */
  getBatteryExtras(): Promise<BatteryExtras>;

  /**
   * Return display characteristics (resolution, density, refresh rate).
   *
   * On web, returns partial data from `window.screen`. Refresh rate defaults to 60 Hz
   * as web does not reliably expose this value.
   *
   * @returns A promise resolving to screen dimensions and display properties.
   *
   * @example
   * ```ts
   * const display = await Device.getDisplayInfo();
   * console.log(`Screen: ${display.widthPx}×${display.heightPx}`);
   * console.log(`Density: ${display.densityDpi} DPI (${display.scale}x)`);
   * console.log(`Refresh rate: ${display.refreshRateHz} Hz`);
   * ```
   *
   * @since 8.0.0
   */
  getDisplayInfo(): Promise<DisplayInfo>;

  /**
   * Return device configuration and user preferences (orientation, dark mode, font scale).
   *
   * On web, returns partial data from `matchMedia` and `screen.orientation`.
   * `idiom` defaults to `"phone"` and `screenSize` defaults to `"normal"` on web.
   *
   * @returns A promise resolving to the current device configuration and accessibility settings.
   *
   * @example
   * ```ts
   * const config = await Device.getConfiguration();
   * console.log(`Orientation: ${config.orientation}`);
   * console.log(`Dark mode: ${config.isDarkMode}`);
   * console.log(`Font scale: ${config.fontScale}x`);
   * console.log(`Device type: ${config.idiom}`);       // "phone", "tablet", "desktop"
   * console.log(`Screen size: ${config.screenSize}`);  // "small", "normal", "large", "xlarge"
   * ```
   *
   * @since 8.0.0
   */
  getConfiguration(): Promise<DeviceConfiguration>;

  /**
   * Return power and thermal state information.
   *
   * Use this to adapt app behavior when the device is in power-saving mode
   * or experiencing thermal throttling.
   *
   * Only available on iOS and Android. On web, throws `unavailable`.
   *
   * @returns A promise resolving to the current power and thermal state.
   *
   * @example
   * ```ts
   * const power = await Device.getPowerState();
   * if (power.isLowPowerMode) {
   *   // Reduce animations, defer background sync, lower frame rate
   *   console.log('Low power mode active — reducing workload');
   * }
   * if (power.thermalState === 'serious' || power.thermalState === 'critical') {
   *   // Pause heavy computation, skip video processing
   *   console.log(`Thermal throttling detected: ${power.thermalState}`);
   * }
   * ```
   *
   * @since 8.0.0
   */
  getPowerState(): Promise<PowerState>;

  /**
   * Return memory information (physical RAM, CPU cores, memory class) and
   * optional CPU usage and memory pressure readings.
   *
   * Use this to adapt cache sizes, parallelism, and feature gating based on
   * device capabilities. `cpuUsagePercent` requires two calls to produce a
   * value (delta-based); the first call returns `null`.
   *
   * On web, returns partial data from `navigator.deviceMemory` and
   * `performance.memory` (Chrome only). `cpuUsagePercent` and `memoryPressure`
   * are always `null`/`"unknown"` on web.
   *
   * @returns A promise resolving to device memory and CPU information.
   *
   * @example
   * ```ts
   * const memory = await Device.getMemoryInfo();
   * const ramMB = Math.round(memory.physicalRam / (1024 * 1024));
   * console.log(`RAM: ${ramMB} MB — ${memory.cpuCores} cores`);
   * console.log(`Memory budget: ${memory.memoryClassMb} MB`);
   *
   * if (memory.isLowRamDevice) {
   *   // Reduce image cache, skip animations, use smaller thumbnails
   *   console.log('Low RAM device — using reduced features');
   * }
   *
   * // CPU usage (delta-based, null on first call)
   * if (memory.cpuUsagePercent != null) {
   *   console.log(`CPU usage: ${memory.cpuUsagePercent.toFixed(1)}%`);
   * }
   *
   * // Memory pressure
   * if (memory.memoryPressure === 'critical') {
   *   console.log('Critical memory pressure — release cached resources');
   * }
   * ```
   *
   * @since 8.0.0
   */
  getMemoryInfo(): Promise<MemoryInfo>;

  /**
   * Return disk storage information for the primary data volume.
   *
   * On web, uses `navigator.storage.estimate()` which is available in
   * most modern browsers. Returns zeros if unavailable.
   *
   * @returns A promise resolving to total, free, and used storage in bytes.
   *
   * @example
   * ```ts
   * const storage = await Device.getStorageInfo();
   * const freeGB = (storage.freeBytes / (1024 * 1024 * 1024)).toFixed(1);
   * console.log(`Free storage: ${freeGB} GB (${storage.usedPercent.toFixed(0)}% used)`);
   *
   * if (storage.usedPercent > 90) {
   *   console.log('Storage almost full — suggest cleanup to user');
   * }
   * ```
   *
   * @since 8.0.0
   */
  getStorageInfo(): Promise<StorageInfo>;

  /**
   * Return system uptime (time since last boot).
   *
   * Useful for analytics, performance heuristics, and detecting long uptimes
   * that may correlate with degraded performance.
   *
   * Only available on iOS and Android. On web, throws `unavailable`.
   *
   * @returns A promise resolving to the system uptime in seconds.
   *
   * @example
   * ```ts
   * const { uptimeSeconds } = await Device.getSystemUptime();
   * const days = Math.floor(uptimeSeconds / 86400);
   * console.log(`Device uptime: ${days} day(s)`);
   *
   * if (uptimeSeconds > 7 * 86400) {
   *   // Suggest restarting the device after 7+ days
   *   console.log('Device has been running for over a week');
   * }
   * ```
   *
   * @since 8.0.0
   */
  getSystemUptime(): Promise<SystemUptime>;

  /**
   * Return application version information (version string + build number).
   *
   * @returns A promise resolving to the app version and build number.
   *
   * @example
   * ```ts
   * const app = await Device.getAppVersion();
   * console.log(`App version: ${app.version} (build ${app.buildNumber})`);
   *
   * // Use for update checks or analytics
   * analytics.setAppVersion(app.version);
   * ```
   *
   * @since 8.0.0
   */
  getAppVersion(): Promise<AppVersion>;

  /**
   * Listen for changes to whether the device is charging (including when the battery becomes full while plugged in).
   *
   * The listener fires only when the charging state **changes**. It is not invoked at subscription time.
   *
   * @example
   * ```ts
   * const handle = await Device.addListener('batteryChargingStateChange', (info) => {
   *   console.log(`Charging: ${info.isCharging ? 'yes' : 'no'}`);
   * });
   *
   * // Later, remove the listener
   * await handle.remove();
   * ```
   *
   * @since 8.0.0
   */
  addListener(
    eventName: 'batteryChargingStateChange',
    listenerFunc: BatteryChargingStateChangeListener,
  ): Promise<PluginListenerHandle>;

  /**
   * Remove all listeners for this plugin.
   *
   * @example
   * ```ts
   * await Device.removeAllListeners();
   * ```
   *
   * @since 8.0.0
   */
  removeAllListeners(): Promise<void>;

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
   * const { version } = await Device.getPluginVersion();
   * console.log('Plugin version:', version);
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
