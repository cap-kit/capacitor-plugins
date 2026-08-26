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
   * @since 8.0.0
   */
  getId(): Promise<DeviceId>;

  /**
   * Return information about the underlying device/os/platform.
   *
   * @since 8.0.0
   */
  getInfo(): Promise<DeviceInfo>;

  /**
   * Return information about the battery.
   *
   * @since 8.0.0
   */
  getBatteryInfo(): Promise<BatteryInfo>;

  /**
   * Get the device's current language locale code.
   *
   * @since 8.0.0
   */
  getLanguageCode(): Promise<GetLanguageCodeResult>;

  /**
   * Get the device's current language locale tag.
   *
   * @since 8.0.0
   */
  getLanguageTag(): Promise<LanguageTag>;

  /**
   * Listen for changes to whether the device is charging (including when the battery becomes full while plugged in).
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
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
