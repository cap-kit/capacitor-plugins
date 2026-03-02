/// <reference types="@capacitor/cli" />

/**
 * Capacitor configuration extension for the Fortress plugin.
 *
 * Configuration values defined here can be provided under the `plugins.Fortress`
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
     * Configuration options for the Fortress plugin.
     */
    Fortress?: FortressConfig;
  }
}

/**
 * Static configuration options for the Fortress plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface FortressConfig {
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
 * Standardized error codes used by the Fortress plugin.
 *
 * These codes are returned as part of structured error objects
 * and allow consumers to implement programmatic error handling.
 *
 * Note:
 * On iOS (Swift Package Manager), errors are returned as data
 * objects rather than rejected Promises.
 *
 * @since 8.0.0
 */
export enum FortressErrorCode {
  /** The device does not have the requested hardware or the security policy restricts access. */
  UNAVAILABLE = 'UNAVAILABLE',
  /** The user explicitly cancelled the biometric prompt or the interactive authentication flow. */
  CANCELLED = 'CANCELLED',
  /** The user denied the permission or the feature is disabled in Fortress plugin. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The Fortress plugin failed to initialize (e.g., runtime error, Keychain/Keystore failure, or Looper failure). */
  INIT_FAILED = 'INIT_FAILED',
  /** The input provided to the plugin method is invalid, malformed, or exceeds constraints. */
  INVALID_INPUT = 'INVALID_INPUT',
  /** The requested Fortress plugin type is not valid or not supported by the plugin. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
  /** The requested resource or key does not exist in the secure or standard storage. */
  NOT_FOUND = 'NOT_FOUND',
  /** The operation conflicts with the current state of the plugin (e.g., re-initializing an active session). */
  CONFLICT = 'CONFLICT',
  /** The operation did not complete within the expected time frame. */
  TIMEOUT = 'TIMEOUT',
  /** A network connectivity or TLS handshake error occurred during a challenge-based flow. */
  NETWORK_ERROR = 'NETWORK_ERROR',
  /** The provided plugin configuration in capacitor.config.ts is invalid or malformed. */
  INVALID_CONFIG = 'INVALID_CONFIG',
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
 * Structured error object returned by Fortress plugin operations.
 *
 * This object allows consumers to handle errors without relying
 * on exception-based control flow.
 */
export interface FortressError {
  /**
   * Human-readable error description.
   */
  message: string;

  /**
   * Machine-readable error code.
   */
  code: FortressErrorCode;
}

/**
 * Public JavaScript API for the Fortress Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API.
 * All methods behave consistently across Android, iOS, and Web.
 */
export interface FortressPlugin {
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
   * const { version } = await Fortress.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
