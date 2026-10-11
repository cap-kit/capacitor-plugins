/// <reference types="@capacitor/cli" />

/**
 * Extension of the Capacitor CLI configuration to include specific settings for Rank.
 * This allows users to configure the plugin via capacitor.config.ts or capacitor.config.json.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the Rank plugin.
     */
    Rank?: RankConfig;
  }
}

/**
 * Static configuration options for the Rank plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface RankConfig {
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

  /**
   * The Apple App ID used for App Store redirection on iOS.
   * Example: '123456789'
   * * @since 8.0.0
   */
  appleAppId?: string;

  /**
   * The Android Package Name used for Play Store redirection.
   * Example: 'com.example.app'
   * * @since 8.0.0
   */
  androidPackageName?: string;

  /**
   * If true, the `requestReview` method will resolve immediately
   * without waiting for the native OS review flow to complete.
   * * @default false
   * @since 8.0.0
   */
  fireAndForget?: boolean;
}

/**
 * Standardized error codes used by the Rank plugin.
 *
 * These codes are returned as part of structured error objects
 * and allow consumers to implement programmatic error handling.
 *
 * @since 8.0.0
 */
export enum RankErrorCode {
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
 * Diagnostic result for Android In-App Review availability.
 *
 * This result describes whether the Google Play Review
 * flow can actually be displayed in the current environment.
 *
 * @since 8.0.0
 */
export interface ReviewEnvironmentResult {
  /**
   * True if the environment supports showing the review dialog.
   */
  canRequestReview: boolean;

  /**
   * Optional diagnostic reason when the review dialog cannot be shown.
   */
  reason?: 'PLAY_STORE_NOT_AVAILABLE' | 'NOT_INSTALLED_FROM_PLAY_STORE';
}

/**
 * Options for the `requestReview` method.
 */
export interface ReviewOptions {
  /**
   * Override the global configuration to determine if the promise
   * should resolve immediately or wait for the native flow.
   */
  fireAndForget?: boolean;
}

/**
 * Options for the `openStore` method.
 */
export interface StoreOptions {
  /**
   * Runtime override for the Apple App ID on iOS.
   */
  appId?: string;
  /**
   * Runtime override for the Android Package Name.
   */
  packageName?: string;
}

/**
 * Result object returned by the `isAvailable()` method.
 */
export interface AvailabilityResult {
  /**
   * Indicates whether the native In-App Review UI is available.
   */
  value: boolean;
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
 * Structured error object returned by Rank plugin operations.
 *
 * This object allows consumers to handle errors without relying
 * on exception-based control flow.
 */
export interface RankError {
  /**
   * Human-readable error description.
   */
  message: string;

  /**
   * Machine-readable error code.
   */
  code: RankErrorCode;
}

/**
 * Public JavaScript API for the Rank Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API.
 * All methods behave consistently across Android, iOS, and Web.
 */
export interface RankPlugin {
  /**
   * Checks if the native In-App Review UI can be displayed.
   * On Android, it verifies Google Play Services availability.
   * On iOS, it checks the OS version compatibility.
   *
   * @returns A promise resolving to an AvailabilityResult object.
   *
   * @example
   * ```ts
   * const { value } = await Rank.isAvailable();
   * if (value) {
   *   // Show review prompt or related UI
   * } else {
   *   // Fallback behavior for unsupported platforms
   * }
   * ```
   *
   * @since 8.0.0
   */
  isAvailable(): Promise<AvailabilityResult>;

  /**
   * Performs a diagnostic check to determine whether the
   * Google Play In-App Review dialog can be displayed.
   *
   * This does NOT trigger the review flow.
   * Android-only. On other platforms, it resolves as unavailable.
   *
   * @since 8.0.0
   */
  checkReviewEnvironment(): Promise<ReviewEnvironmentResult>;

  /**
   * Requests the display of the native review popup.
   * On Web, this operation calls unimplemented().
   * @param options Optional review configuration overrides.
   *
   * @returns A promise that resolves when the request is sent or completed.
   *
   * @example
   * ```ts
   * // Basic usage with default configuration
   * await Rank.requestReview();
   *
   * // Usage with fire-and-forget behavior
   * await Rank.requestReview({ fireAndForget: true });
   * ```
   *
   * @since 8.0.0
   */
  requestReview(options?: ReviewOptions): Promise<void>;

  /**
   * Opens the App Store product page internally (iOS) or redirects to the Store (Android/Web).
   * @param options Store identification.
   *
   * @example
   * ```ts
   * // On iOS, this will open an internal App Store overlay.
   * await Rank.presentProductPage({
   *   appId: '123456789' // iOS App ID for URL generation
   * });
   *
   * // On Android, this will redirect to the Play Store.
   * await Rank.presentProductPage({
   *   packageName: 'com.example.app' // Android Package Name for URL generation
   * });
   * ```
   *
   * @since 8.0.0
   */
  presentProductPage(options?: StoreOptions): Promise<void>;

  /**
   * Opens the app's page in the App Store (iOS) or Play Store (Android).
   * On Web, it performs a URL redirect if parameters are provided.
   * @param options Optional store identification overrides.
   *
   * @returns A promise that resolves when the store application is launched.
   *
   * @example
   * ```ts
   * // On Web, this will open the store page in a new tab if identifiers are provided.
   * await Rank.openStore({
   *   appId: '123456789', // iOS App ID for URL generation
   *   packageName: 'com.example.app' // Android Package Name for URL generation
   * });
   * ```
   *
   * @since 8.0.0
   */
  openStore(options?: StoreOptions): Promise<void>;

  /**
   * Opens the App Store listing page for a specific app.
   * If no appId is provided, it uses the one from the plugin configuration.
   *
   * @example
   * ```ts
   * // Opens the store listing page.
   * // Uses the provided appId or falls back to the one in capacitor.config.ts
   * await Rank.openStoreListing({
   *   appId: '123456789'
   * });
   * ```
   *
   * @since 8.0.0
   */
  openStoreListing(options?: { appId?: string }): Promise<void>;

  /**
   * Performs a search in the app store for the given terms.
   *
   * @example
   * ```ts
   * // Searches the store for specific terms.
   * // Android: market://search | iOS: itms-apps search
   * await Rank.search({
   *   terms: 'Capacitor Plugins'
   * });
   * ```
   *
   * @since 8.0.0
   */
  search(options: { terms: string }): Promise<void>;

  /**
   * Opens the developer's page in the app store.
   * @param options.devId On Android, this is the developer ID (numeric or string). On iOS, this is ignored.
   *
   * @example
   * ```ts
   * // Navigates to a developer or brand page.
   * await Rank.openDevPage({
   *   devId: '543216789'
   * });
   * ```
   *
   * @since 8.0.0
   */
  openDevPage(options: { devId: string }): Promise<void>;

  /**
   * Opens a specific app collection (Android Only).
   *
   * @example
   * ```ts
   * // Opens a curated collection (Android only).
   * await Rank.openCollection({
   *   name: 'editors_choice'
   * });
   * ```
   *
   * @since 8.0.0
   */
  openCollection(options: { name: string }): Promise<void>;

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
   * const { version } = await Rank.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
