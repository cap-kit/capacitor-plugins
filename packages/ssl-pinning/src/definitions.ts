/// <reference types="@capacitor/cli" />

/**
 * Capacitor configuration extension for the SSLPinning plugin.
 *
 * Configuration values defined here can be provided under the
 * `plugins.SSLPinning` key inside `capacitor.config.ts`.
 *
 * These values are:
 * - read natively at build/runtime
 * - NOT accessible from JavaScript at runtime
 * - treated as read-only static configuration
 *
 * @see https://capacitorjs.com/docs/plugins/configuration-values
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the SSLPinning plugin.
     */
    SSLPinning?: SSLPinningConfig;
  }
}

// -----------------------------------------------------------------------------
// Enums
// -----------------------------------------------------------------------------

/**
 * Standardized error codes for programmatic handling of SSL pinning failures.
 *
 * Errors are delivered via Promise rejection as `CapacitorException`
 * with one of the following codes.
 *
 * @since 0.0.15
 */
export enum SSLPinningErrorCode {
  /** Required data is missing or the feature is not available. */
  UNAVAILABLE = 'UNAVAILABLE',

  /** The user denied a required permission or the feature is disabled. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',

  /** The SSL pinning operation failed due to a runtime or initialization error. */
  INIT_FAILED = 'INIT_FAILED',

  /** Invalid or unsupported input was provided. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
}

// -----------------------------------------------------------------------------
// Configuration
// -----------------------------------------------------------------------------

/**
 * Static configuration options for the SSLPinning plugin.
 *
 * These values can be defined in `capacitor.config.ts` and are used
 * natively as fallback values when runtime options are not provided.
 */
export interface SSLPinningConfig {
  /**
   * Enables verbose native logging (Logcat / Xcode console).
   *
   * @default false
   * @since 0.0.15
   */
  verboseLogging?: boolean;

  /**
   * Default fingerprint used by `checkCertificate()` when
   * `options.fingerprint` is not provided at runtime.
   *
   * @example "50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"
   * @since 0.0.14
   */
  fingerprint?: string;

  /**
   * Default fingerprints used by `checkCertificates()` when
   * `options.fingerprints` is not provided at runtime.
   *
   * @example ["50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"]
   * @since 0.0.15
   */
  fingerprints?: string[];
}

// -----------------------------------------------------------------------------
// Method Options
// -----------------------------------------------------------------------------

/**
 * Options for checking a single SSL certificate.
 */
export interface SSLPinningOptions {
  /**
   * HTTPS URL of the server whose SSL certificate must be checked.
   *
   * This value is REQUIRED and cannot be provided via configuration.
   *
   * @example "https://example.com"
   */
  url: string;

  /**
   * Expected SHA-256 fingerprint of the certificate.
   *
   * Resolution order:
   * 1. `options.fingerprint` (runtime)
   * 2. `plugins.SSLPinning.fingerprint` (config)
   *
   * If neither is provided, the Promise is rejected with
   * `SSLPinningErrorCode.UNAVAILABLE`.
   */
  fingerprint?: string;
}

/**
 * Options for checking an SSL certificate using multiple allowed fingerprints.
 */
export interface SSLPinningMultiOptions {
  /**
   * HTTPS URL of the server whose SSL certificate must be checked.
   *
   * This value is REQUIRED and cannot be provided via configuration.
   *
   * @example "https://example.com"
   */
  url: string;

  /**
   * Expected SHA-256 fingerprints of the certificate.
   *
   * Resolution order:
   * 1. `options.fingerprints` (runtime)
   * 2. `plugins.SSLPinning.fingerprints` (config)
   *
   * If neither is provided, the Promise is rejected with
   * `SSLPinningErrorCode.UNAVAILABLE`.
   */
  fingerprints?: string[];
}

// -----------------------------------------------------------------------------
// Results
// -----------------------------------------------------------------------------

/**
 * Result returned by a successful SSL certificate check.
 *
 * This object is returned ONLY on success.
 * Failures are delivered via Promise rejection.
 */
export interface SSLPinningResult {
  /**
   * Actual SHA-256 fingerprint of the server certificate.
   */
  actualFingerprint: string;

  /**
   * Indicates whether the certificate fingerprint matched.
   */
  fingerprintMatched: boolean;

  /**
   * The fingerprint that successfully matched, if any.
   */
  matchedFingerprint?: string;
}

/**
 * Result returned by the getPluginVersion method.
 */
export interface PluginVersionResult {
  /** The native version string of the plugin. */
  version: string;
}

// -----------------------------------------------------------------------------
// Plugin Interface
// -----------------------------------------------------------------------------

/**
 * SSL Pinning Capacitor Plugin interface.
 */
export interface SSLPinningPlugin {
  /**
   * Checks the SSL certificate of a server using a single fingerprint.
   *
   * @throws CapacitorException with code `SSLPinningErrorCode`
   *
   * @since 0.0.14
   */
  checkCertificate(options: SSLPinningOptions): Promise<SSLPinningResult>;

  /**
   * Checks the SSL certificate of a server using multiple allowed fingerprints.
   *
   * @throws CapacitorException with code `SSLPinningErrorCode`
   *
   * @since 0.0.15
   */

  checkCertificates(options: SSLPinningMultiOptions): Promise<SSLPinningResult>;

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
   * const { version } = await SSLPinning.getPluginVersion();
   * ```
   *
   * @since 0.0.15
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
