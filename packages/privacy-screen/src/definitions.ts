/// <reference types="@capacitor/cli" />

import { PluginListenerHandle } from '@capacitor/core';

/**
 * Static configuration options for the PrivacyScreen plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the PrivacyScreen plugin.
     */
    PrivacyScreen?: {
      /**
       * Enables verbose native logging.
       *
       * @default false
       * @since 8.0.0
       */
      verboseLogging?: boolean;
      /**
       * Enables privacy protection automatically when the native
       * plugin loads, without waiting for the first `enable()` call.
       *
       * @default false
       * @since 8.0.0
       */
      enabled?: boolean;
    };
  }
}

/**
 * Platform-specific display options for the privacy screen.
 *
 * This shape mirrors the official `@capacitor/privacy-screen` configuration
 * so this plugin stays a drop-in replacement.
 *
 * @since 8.0.0
 */
export interface PrivacyScreenConfig {
  android?: {
    /**
     * Shows a dim scrim instead of the splash drawable while protected.
     *
     * @default false
     * @since 8.0.0
     */
    dimBackground?: boolean;
    /**
     * @deprecated FLAG_SECURE is always applied while the privacy screen is
     * enabled. To allow screenshots for a screen or flow, call `disable()`
     * before it and `enable()` after it.
     * @since 8.0.0
     */
    preventScreenshots?: boolean;
    /**
     * Overlay shown when the activity is hidden (e.g. system biometric prompt).
     *
     * @default 'none'
     * @since 8.0.0
     */
    privacyModeOnActivityHidden?: 'none' | 'dim' | 'splash';
  };
  ios?: {
    /**
     * Blur style used to obscure the app-switcher snapshot.
     *
     * @default 'none'
     * @since 8.0.0
     */
    blurEffect?: 'light' | 'dark' | 'none';
  };
}

/**
 * Result returned when privacy protection is toggled.
 *
 * Mirrors the official `@capacitor/privacy-screen` API.
 *
 * @since 8.0.0
 */
export interface PrivacyScreenActionResult {
  /**
   * Whether the native operation completed.
   *
   * @since 8.0.0
   */
  success: boolean;
}

/**
 * Current privacy-screen state.
 *
 * Mirrors the official `@capacitor/privacy-screen` API.
 *
 * @since 8.0.0
 */
export interface PrivacyScreenStatus {
  /**
   * Whether privacy protection is currently enabled.
   *
   * @since 8.0.0
   */
  enabled: boolean;
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
 * Public JavaScript API for the PrivacyScreen Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API compatible with the
 * official `@capacitor/privacy-screen` contract, plus screenshot
 * observability. All methods behave consistently across Android, iOS and Web.
 */
export interface PrivacyScreenPlugin {
  /**
   * Enables privacy-screen protection.
   *
   * On Android this sets `FLAG_SECURE`, which hides app content in the app
   * switcher and blocks screenshots and screen recordings.
   *
   * On iOS this installs an overlay that hides app content in the app
   * switcher.
   *
   * Only available on Android and iOS.
   *
   * @param config - Optional platform-specific display behavior.
   * @returns A promise resolving to `{ success: true }` when applied.
   *
   * @example
   * ```ts
   * await PrivacyScreen.enable({ ios: { blurEffect: 'dark' } });
   * ```
   *
   * @since 8.0.0
   */
  enable(config?: PrivacyScreenConfig): Promise<PrivacyScreenActionResult>;

  /**
   * Disables privacy-screen protection.
   *
   * Use only when the current screen must stay visible in system previews.
   *
   * Only available on Android and iOS.
   *
   * @returns A promise resolving to `{ success: true }` when applied.
   *
   * @example
   * ```ts
   * await PrivacyScreen.disable();
   * ```
   *
   * @since 8.0.0
   */
  disable(): Promise<PrivacyScreenActionResult>;

  /**
   * Returns the current privacy-screen enabled state.
   *
   * Only available on Android and iOS.
   *
   * @returns A promise resolving to `{ enabled }`.
   *
   * @since 8.0.0
   */
  isEnabled(): Promise<PrivacyScreenStatus>;

  /**
   * Listens for when the user takes a screenshot of the app.
   *
   * The event fires after the screenshot has been taken and therefore
   * cannot prevent it. On Android it is only emitted on API 34+ while
   * privacy protection is active; on older versions the listener is
   * never called.
   *
   * On iOS, this event is emitted when the user takes a screenshot,
   * regardless of whether privacy protection is active.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  addListener(eventName: 'screenshotTaken', listenerFunc: () => void): Promise<PluginListenerHandle>;

  /**
   * Listens for when the user starts a screen recording of the app.
   *
   * On iOS 11+, this event fires when the user starts screen recording
   * (e.g., via Control Center or screenshot UI). The event can only be
   * observed after the recording has started and therefore cannot be
   * prevented.
   *
   * Only available on iOS.
   *
   * @since 8.0.0
   */
  addListener(eventName: 'screenRecordingStarted', listenerFunc: () => void): Promise<PluginListenerHandle>;

  /**
   * Listens for when the user stops a screen recording of the app.
   *
   * On iOS 11+, this event fires when the user stops screen recording.
   * The event can only be observed after the recording has stopped and
   * therefore cannot be prevented.
   *
   * Only available on iOS.
   *
   * @since 8.0.0
   */
  addListener(eventName: 'screenRecordingStopped', listenerFunc: () => void): Promise<PluginListenerHandle>;

  /**
   * Removes all listeners for this plugin.
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
   * const { version } = await PrivacyScreen.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
