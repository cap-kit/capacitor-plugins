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
  /** The user cancelled an interactive flow. */
  CANCELLED = 'CANCELLED',
  /** The user denied a required permission or the feature is disabled. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The SSL pinning operation failed due to a runtime or initialization error. */
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
  /** No runtime fingerprints, no config fingerprints, and no certificates were configured. */
  NO_PINNING_CONFIG = 'NO_PINNING_CONFIG',
  /** Certificate-based pinning was selected, but no valid certificate files were found. */
  CERT_NOT_FOUND = 'CERT_NOT_FOUND',
  /** Certificate-based trust evaluation failed at the handshake level. */
  TRUST_EVALUATION_FAILED = 'TRUST_EVALUATION_FAILED',
  /** The server certificate fingerprint did not match any expected fingerprint. */
  PINNING_FAILED = 'PINNING_FAILED',
  /** The request host matched an excluded domain. */
  EXCLUDED_DOMAIN = 'EXCLUDED_DOMAIN',
  /** Network connectivity or TLS handshake error. */
  NETWORK_ERROR = 'NETWORK_ERROR',
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

  /**
   * Local certificate filenames (e.g., ["mycert.cer"]).
   * Files must be in 'assets/certs' (Android) or main bundle 'certs' (iOS).
   *
   * This is the global fallback used when no domain-specific
   * certificates are configured via `certsByDomain`.
   *
   * @since 8.0.3
   */
  certs?: string[];

  /**
   * Per-domain certificate configuration.
   *
   * Maps a domain (or subdomain pattern) to a list of
   * certificate filenames to use for that domain.
   *
   * Matching rules:
   * - First, try exact domain match (e.g., "api.example.com")
   * - Then, try subdomain match (e.g., "example.com" matches "api.example.com")
   * - If multiple subdomain keys match, the MOST SPECIFIC (longest) wins
   * - If no match found, fallback to global `certs`
   *
   * @example
   * ```ts
   * {
   *   "api.example.com": ["api-cert.cer"],
   *   "example.com": ["wildcard.cer"],
   *   "other.com": ["other-cert.cer"]
   * }
   * ```
   *
   * @since 8.0.4
   */
  certsByDomain?: Record<string, string[]>;

  /**
   * Optional manifest file for certificate auto-discovery.
   *
   * The manifest is a JSON file containing either:
   * - `{ "certs": ["a.cer", "b.cer"] }`
   * - `{ "certsByDomain": { "example.com": ["cert.cer"] } }`
   * - Both, which extend/override the explicit config values
   *
   * Location:
   * - Android: assets/certs/<path>
   * - iOS: main bundle <path>
   *
   * Precedence (later overrides earlier):
   * 1. Explicit config values (certs, certsByDomain)
   * 2. Manifest values
   *
   * @example "certs/index.json"
   *
   * @since 8.0.4
   */
  certsManifest?: string;

  /**
   * Domains to bypass. Matches exact domain or subdomains.
   * Do not include schemes or paths.
   *
   * @since 8.0.3
   */
  excludedDomains?: string[];
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
 * Result returned by an SSL pinning operation.
 *
 * This object is returned for ALL outcomes:
 * - Success: `fingerprintMatched: true`
 * - Mismatch: `fingerprintMatched: false` with error info (RESOLVED, not rejected)
 *
 * Only operation failures (invalid input, config missing, network errors,
 * timeout, internal errors) reject the Promise.
 */
export interface SSLPinningResult {
  /**
   * The actual SHA-256 fingerprint of the server certificate.
   *
   * Present in fingerprint mode.
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
   * Indicates that SSL pinning was skipped because
   * the request host matched an excluded domain.
   */
  excludedDomain?: boolean;

  /**
   * Indicates which pinning mode was used.
   *
   * - "fingerprint"
   * - "cert"
   * - "excluded"
   */
  mode?: 'fingerprint' | 'cert' | 'excluded';

  /**
   * Human-readable error message when pinning fails.
   * Present when `fingerprintMatched: false`.
   */
  error?: string;

  /**
   * Standardized error code aligned with SSLPinningErrorCode.
   */
  errorCode?: SSLPinningErrorCode;
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
