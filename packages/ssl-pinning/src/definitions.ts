/// <reference types="@capacitor/cli" />

/**
 * Extension of the Capacitor CLI configuration to include specific settings for People.
 * This allows users to configure the plugin via capacitor.config.ts or capacitor.config.json.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the SSLPinning plugin.
     */
    SSLPinning?: SSLPinningConfig;
  }
}

// -- Enums --

/**
 * Standardized error codes for programmatic handling of ssl pinning failures.
 * @since 0.0.15
 */
export enum SSLPinningErrorCode {
  /** The device does not have the requested hardware. */
  UNAVAILABLE = 'UNAVAILABLE',
  /** The user denied the permission or the feature is disabled in settings. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The ssl pinning failed to initialize (e.g., runtime error or Looper failure). */
  INIT_FAILED = 'INIT_FAILED',
  /** The requested ssl pinning type is not valid or not supported by the plugin. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
}

// -- Interfaces --

/**
 * Configuration options for initializing the People plugin.
 * These values can be set in capacitor.config.ts.
 */
export interface SSLPinningConfig {
  /**
   * Enables detailed logging in the native console (Logcat/Xcode).
   * Useful for debugging sensor data flow and lifecycle events.
   * @example true
   * @default false
   *
   * @since 0.0.15
   */
  verboseLogging?: boolean;

  /**
   * Default fingerprint used by checkCertificate()
   * if no arguments are provided.
   * @example "50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"
   * @default undefined
   *
   * @since 0.0.14
   */
  fingerprint?: string;

  /**
   * Default fingerprints used by checkCertificates()
   * if no arguments are provided.
   * @example ["50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"]
   * @default undefined
   *
   * @since 0.0.15
   */
  fingerprints?: string[];
}

/**
 * Options for checking a single SSL certificate.
 */
export interface SSLPinningOptions {
  /**
   * The URL of the server whose SSL certificate needs to be checked.
   * @example "https://example.com"
   */
  url: string;

  /**
   * The expected fingerprint of the SSL certificate to validate against.
   * This is typically a hash string such as SHA-256.
   */
  fingerprint: string;
}

/**
 * Options for checking multiple SSL certificates.
 */
export interface SSLPinningMultiOptions {
  /**
   * The URL of the server whose SSL certificate needs to be checked.
   * @example "https://example.com"
   */
  url: string;

  /**
   * The expected fingerprints of the SSL certificate to validate against.
   * This is typically an array of hash strings such as SHA-256.
   */
  fingerprints: string[];
}

/**
 * Result returned by the SSL certificate check.
 *
 * NOTE:
 * On iOS (Swift Package Manager), errors are returned
 * as part of the resolved result object rather than
 * Promise rejections.
 */
export interface SSLPinningResult {
  /**
   * The subject of the certificate, representing the entity the certificate is issued to.
   * @platform Android
   * @example "CN=example.com, O=Example Corp, C=US"
   */
  subject?: string;

  /**
   * The issuer of the certificate, indicating the certificate authority that issued it.
   * Results may vary slightly between iOS and Android platforms.
   * @example "CN=Example CA, O=Example Corp, C=US"
   */
  issuer?: string;

  /**
   * The start date from which the certificate is valid.
   * Format: ISO 8601 string or platform-specific date representation.
   * @platform Android
   * @example "2023-01-01T00:00:00Z"
   */
  validFrom?: string;

  /**
   * The end date until which the certificate is valid.
   * Format: ISO 8601 string or platform-specific date representation.
   * @platform Android
   * @example "2024-01-01T00:00:00Z"
   */
  validTo?: string;

  /**
   * The fingerprint that is expected to match the certificate's actual fingerprint.
   * This is typically provided in the SSLPinningOptions.
   */
  expectedFingerprint?: string;

  /**
   * The actual fingerprint of the SSL certificate retrieved from the server.
   * @example "50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"
   */
  actualFingerprint?: string;

  /**
   * Indicates whether the actual fingerprint matches the expected fingerprint.
   * `true` if they match, `false` otherwise.
   */
  fingerprintMatched?: boolean;

  /**
   * A descriptive error message if an issue occurred during the SSL certificate check.
   * @example "Unable to retrieve certificate from the server."
   */
  error?: string;
}

/**
 * Result returned by the getPluginVersion method.
 */
export interface PluginVersionResult {
  /** The native version string of the plugin. */
  version: string;
}

/**
 * Error object returned by ssl pinning operations.
 * Includes the ssl pinning type and a descriptive message.
 */
export interface SSLPinningError {
  /** A descriptive error message */
  message: string;
  /** Standardized error code */
  code: SSLPinningErrorCode;
}

/**
 * Result returned by the getPluginVersion method.
 */
export interface PluginVersionResult {
  /** The native version string of the plugin. */
  version: string;
}

/**
 * Interface defining the structure of an SSL Certificate Checker Plugin.
 *
 * Implementations of this interface should provide the logic for checking
 * the status and details of an SSL certificate based on the provided options.
 */
export interface SSLPinningPlugin {
  /**
   * Check the SSL certificate of a server.
   *
   * @param options - Options for checking the SSL certificate.
   * @returns A promise resolving to the result of the SSL certificate check.
   * @throws SSLPinningError if the check fails.
   *
   * @example
   * ```typescript
   * import { SSLPinning } from '@cap-kit/ssl-pinning';
   *
   * const result = await SSLPinning.checkCertificate({
   *   url: 'https://example.com',
   *   fingerprint: '50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26'
   * });
   *
   * console.log('SSL Pinning Result:', result);
   * ```
   *
   * @since 0.0.14
   */
  checkCertificate(): Promise<SSLPinningResult>;

  /**
   * Check the SSL certificate of a server.
   * @param options - Options for checking the SSL certificate.
   * @returns A promise resolving to the result of the SSL certificate check.
   * @throws SSLPinningError if the check fails.
   *
   * @example
   * ```typescript
   * import { SSLPinning } from '@cap-kit/ssl-pinning';
   *
   * const result = await SSLPinning.checkCertificate({
   *   url: 'https://example.com',
   *   fingerprint: '50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26'
   * });
   *
   * console.log('SSL Pinning Result:', result);
   * ```
   *
   * @since 0.0.14
   */
  checkCertificate(options: SSLPinningOptions): Promise<SSLPinningResult>;

  /**
   * Check the SSL certificates of multiple servers.
   *
   * @returns A promise resolving to an array of results for each SSL certificate check.
   * @throws SSLPinningError if any of the checks fail.
   *
   * @example
   * ```typescript
   * import { SSLPinning } from '@cap-kit/ssl-pinning';
   *
   * const results = await SSLPinning.checkCertificates();
   *
   * results.forEach(result => {
   *   console.log('SSL Pinning Result:', result);
   * });
   * ```
   *
   * @since 0.0.15
   */
  checkCertificates(): Promise<SSLPinningResult[]>;

  /**
   * Check the SSL certificates of multiple servers.
   * @returns A promise resolving to an array of results for each SSL certificate check.
   * @throws SSLPinningError if any of the checks fail.
   * @param options - Options for checking the SSL certificates.
   *
   * @example
   * ```typescript
   * import { SSLPinning } from '@cap-kit/ssl-pinning';
   *
   * const results = await SSLPinning.checkCertificates([
   *   {
   *     url: 'https://example.com',
   *     fingerprints: ['50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26']
   *   },
   *   {
   *     url: 'https://another-example.com',
   *     fingerprints: ['AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90']
   *   }
   * ]);
   *
   * results.forEach(result => {
   *   console.log('SSL Pinning Result:', result);
   * });
   * ```
   *
   * @since 0.0.15
   */
  checkCertificates(options: SSLPinningMultiOptions[]): Promise<SSLPinningResult[]>;

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
