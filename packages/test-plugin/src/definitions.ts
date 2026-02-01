/// <reference types="@capacitor/cli" />

/**
 * Capacitor configuration extension for the Test plugin.
 *
 * Configuration values defined here can be provided under the `plugins.Test`
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
     * Configuration options for the Test plugin.
     */
    Test?: TestConfig;
  }
}

/**
 * Static configuration options for the Test plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface TestConfig {
  /**
   * Custom message appended to the echoed value.
   *
   * This option exists mainly as an example showing how to pass
   * static configuration data from JavaScript to native platforms.
   *
   * @default " (from config)"
   * @example " - Hello from Config!"
   * @since 0.0.1
   */
  customMessage?: string;

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
   * @since 1.0.0
   */
  verboseLogging?: boolean;
}

/**
 * Standardized error codes used by the Test plugin.
 *
 * These codes are returned as part of structured error objects
 * and allow consumers to implement programmatic error handling.
 *
 * Note:
 * On iOS (Swift Package Manager), errors are returned as data
 * objects rather than rejected Promises.
 *
 * @since 1.0.0
 */
export enum TestErrorCode {
  /** The device does not have the requested hardware. */
  UNAVAILABLE = 'UNAVAILABLE',
  /** The user denied the permission or the feature is disabled. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The TestPlugin failed to initialize (e.g., runtime error or Looper failure). */
  INIT_FAILED = 'INIT_FAILED',
  /** The requested TestPlugin type is not valid or not supported by the plugin. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
}

/**
 * Options object for the `echo` method.
 *
 * This object defines the input payload sent from JavaScript
 * to the native plugin implementation.
 */
export interface EchoOptions {
  /**
   * The string value to be echoed back by the plugin.
   *
   * This value is passed to the native layer and returned
   * unchanged, optionally with a configuration-based suffix.
   *
   * @example "Hello, World!"
   */
  value: string;
}

/**
 * Result object returned by the `echo` method.
 *
 * This object represents the resolved value of the echo operation
 * after native processing has completed.
 */
export interface EchoResult {
  /**
   * The echoed string value.
   *
   * If a `customMessage` is configured, it will be appended
   * to the original input value.
   */
  value: string;
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
 * Structured error object returned by Test plugin operations.
 *
 * This object allows consumers to handle errors without relying
 * on exception-based control flow.
 */
export interface TestError {
  /**
   * Human-readable error description.
   */
  message: string;

  /**
   * Machine-readable error code.
   */
  code: TestErrorCode;
}

/**
 * Public JavaScript API for the Test Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API.
 * All methods behave consistently across Android, iOS, and Web.
 */
export interface TestPlugin {
  /**
   * Echoes the provided value.
   *
   * If the plugin is configured with a `customMessage`, that value
   * will be appended to the returned string.
   *
   * This method is primarily intended as an example demonstrating
   * native ↔ JavaScript communication.
   *
   * @param options Object containing the value to echo.
   * @returns A promise resolving to the echoed value.
   *
   * @example
   * ```ts
   * const { value } = await Test.echo({ value: 'Hello' });
   * console.log(value);
   * ```
   *
   * @since 0.0.1
   */
  echo(options: EchoOptions): Promise<EchoResult>;

  /**
   * Opens the operating system's application settings page.
   *
   * @throws {TestError} Rejects if the settings page cannot be opened.
   *
   * @since 1.0.0
   */
  openAppSettings(): Promise<void>;

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
   * const { version } = await Test.getPluginVersion();
   * ```
   *
   * @since 0.0.1
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
