/// <reference types="@capacitor/cli" />

/**
 * Capacitor configuration extension for the TLSFingerprint plugin.
 *
 * Configuration values defined here can be provided under the
 * `plugins.TLSFingerprint` key inside `capacitor.config.ts`.
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
     * Configuration options for the TLSFingerprint plugin.
     */
    TLSFingerprint?: TLSFingerprintConfig;
  }
}

// -----------------------------------------------------------------------------
// Enums
// -----------------------------------------------------------------------------

/**
 * Standardized error codes for programmatic handling of TLS fingerprint failures.
 *
 * Errors are delivered via Promise rejection as `CapacitorException`
 * with one of the following codes.
 *
 * @since 8.0.0
 */
export enum TLSFingerprintErrorCode {
  /** Required data is missing or the feature is not available. */
  UNAVAILABLE = 'UNAVAILABLE',
  /** The user cancelled an interactive flow. */
  CANCELLED = 'CANCELLED',
  /** The user denied a required permission or the feature is disabled. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The TLS fingerprint operation failed due to a runtime or initialization error. */
  INIT_FAILED = 'INIT_FAILED',
  /** The input provided to the plugin method is invalid, missing, or malformed. */
  INVALID_INPUT = 'INVALID_INPUT',
  /** Invalid or unsupported input was provided. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
  /** The requested resource does not exist. */
  NOT_FOUND = 'NOT_FOUND',
  /** The operation conflicts with the current state. */
  CONFLICT = 'CONFLICT',
  /** The operation did not complete within the expected time. */
  TIMEOUT = 'TIMEOUT',
  /** The server certificate fingerprint did not match any expected fingerprint. */
  PINNING_FAILED = 'PINNING_FAILED',
  /** The request host matched an excluded domain. */
  EXCLUDED_DOMAIN = 'EXCLUDED_DOMAIN',
  /** Network connectivity or TLS handshake error. */
  NETWORK_ERROR = 'NETWORK_ERROR',
  /** SSL/TLS specific error (certificate expired, handshake failure, etc.). */
  SSL_ERROR = 'SSL_ERROR',
}

// -----------------------------------------------------------------------------
// Configuration
// -----------------------------------------------------------------------------

/**
 * Static configuration options for the TLSFingerprint plugin.
 *
 * These values can be defined in `capacitor.config.ts` and are used
 * natively as fallback values when runtime options are not provided.
 */
export interface TLSFingerprintConfig {
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
   * Default fingerprint used by `checkCertificate()` when
   * `options.fingerprint` is not provided at runtime.
   *
   * @example "50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"
   * @since 8.0.0
   */
  fingerprint?: string;

  /**
   * Default fingerprints used by `checkCertificates()` when
   * `options.fingerprints` is not provided at runtime.
   *
   * @example ["50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"]
   * @since 8.0.0
   */
  fingerprints?: string[];

  /**
   * Domains to bypass. Matches exact domain or subdomains.
   * Do not include schemes or paths.
   *
   * @since 8.0.0
   */
  excludedDomains?: string[];
}

// -----------------------------------------------------------------------------
// Method Options
// -----------------------------------------------------------------------------

/**
 * Options for checking a single SSL certificate.
 */
export interface TLSFingerprintOptions {
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
   * 2. `plugins.TLSFingerprint.fingerprint` (config)
   *
   * If neither is provided, the Promise is rejected with
   * `TLSFingerprintErrorCode.UNAVAILABLE`.
   */
  fingerprint?: string;
}

/**
 * Options for checking an SSL certificate using multiple allowed fingerprints.
 */
export interface TLSFingerprintMultiOptions {
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
   * 2. `plugins.TLSFingerprint.fingerprints` (config)
   *
   * If neither is provided, the Promise is rejected with
   * `TLSFingerprintErrorCode.UNAVAILABLE`.
   */
  fingerprints?: string[];
}

// -----------------------------------------------------------------------------
// Results
// -----------------------------------------------------------------------------

/**
 * Result returned by an TLS fingerprint operation.
 *
 * This object is returned for ALL outcomes:
 * - Success: `fingerprintMatched: true`
 * - Mismatch: `fingerprintMatched: false` with error info (RESOLVED, not rejected)
 *
 * Only operation failures (invalid input, config missing, network errors,
 * timeout, internal errors) reject the Promise.
 */
export interface TLSFingerprintResult {
  /**
   * The actual SHA-256 fingerprint of the server certificate.
   *
   * Present in fingerprint and excluded modes.
   */
  actualFingerprint?: string;

  /**
   * Indicates whether the certificate validation succeeded.
   *
   * - true  → Pinning passed
   * - false → Pinning failed
   */
  fingerprintMatched: boolean;

  /**
   * The fingerprint that successfully matched, if any.
   */
  matchedFingerprint?: string;

  /**
   * Indicates that TLS fingerprint was skipped because
   * the request host matched an excluded domain.
   */
  excludedDomain?: boolean;

  /**
   * Indicates which pinning mode was used.
   *
   * - "fingerprint"
   * - "excluded"
   */
  mode?: 'fingerprint' | 'excluded';

  /**
   * Human-readable error message when pinning fails.
   * Present when `fingerprintMatched: false`.
   */
  error?: string;

  /**
   * Standardized error code aligned with TLSFingerprintErrorCode.
   */
  errorCode?: TLSFingerprintErrorCode;
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
 * TLS Fingerprint Capacitor Plugin interface.
 */
export interface TLSFingerprintPlugin {
  /**
   * Checks the SSL certificate of a server using a single fingerprint.
   *
   * @throws CapacitorException with code `TLSFingerprintErrorCode`
   *
   * @since 8.0.0
   */
  checkCertificate(options: TLSFingerprintOptions): Promise<TLSFingerprintResult>;

  /**
   * Checks the SSL certificate of a server using multiple allowed fingerprints.
   *
   * @throws CapacitorException with code `TLSFingerprintErrorCode`
   *
   * @since 8.0.0
   */

  checkCertificates(options: TLSFingerprintMultiOptions): Promise<TLSFingerprintResult>;

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
   * const { version } = await TLSFingerprint.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
