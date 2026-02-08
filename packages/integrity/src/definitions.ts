/// <reference types="@capacitor/cli" />

import { PluginListenerHandle } from '@capacitor/core';

/**
 * Capacitor configuration extension for the Integrity plugin.
 *
 * Configuration values defined here can be provided under the
 * `plugins.Integrity` key inside `capacitor.config.ts`.
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
     * Configuration options for the Integrity plugin.
     */
    Integrity?: IntegrityConfig;
  }
}

// -----------------------------------------------------------------------------
// Events
// -----------------------------------------------------------------------------

/**
 * Event payload emitted when a new integrity signal is detected.
 *
 * This event represents a *real-time observation* of a potential
 * integrity-relevant condition on the device.
 *
 * IMPORTANT:
 * - Signals are observational only.
 * - Emitting a signal does NOT imply that the environment is compromised.
 * - No blocking or enforcement is performed by the plugin.
 *
 * The host application is responsible for:
 * - interpreting signals
 * - correlating multiple signals
 * - applying any security or UX policy
 *
 * @since 8.0.0
 */
export type IntegritySignalEvent = IntegritySignal;

// -----------------------------------------------------------------------------
// Enums
// -----------------------------------------------------------------------------

/**
 * Standardized error codes used by the Integrity plugin.
 *
 * Errors are delivered via Promise rejection with a structured
 * `{ message, code }` object matching `IntegrityError`.
 *
 * @since 8.0.0
 */
export enum IntegrityErrorCode {
  /** Required data is missing or the feature is not available. */
  UNAVAILABLE = 'UNAVAILABLE',

  /** The user denied a required permission or the feature is disabled. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',

  /** The SSL pinning operation failed due to a runtime or initialization error. */
  INIT_FAILED = 'INIT_FAILED',

  /** Invalid or unsupported input was provided. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
}

/**
 * Standard reason codes that MAY be used when presenting
 * the integrity block page.
 *
 * These values are OPTIONAL and provided for convenience only.
 * Applications may define and use their own custom reason strings.
 *
 * @since 8.0.0
 */
export enum IntegrityBlockReason {
  COMPROMISED_ENVIRONMENT = 'compromised_environment',
  ROOT_DETECTED = 'root_detected',
  JAILBREAK_DETECTED = 'jailbreak_detected',
  EMULATOR_DETECTED = 'emulator_detected',
  DEBUG_ENVIRONMENT = 'debug_environment',
  INTEGRITY_FAILED = 'integrity_failed',
}

// -----------------------------------------------------------------------------
// Configuration
// -----------------------------------------------------------------------------

/**
 * Static configuration options for the Integrity plugin.
 *
 * These values can be defined in `capacitor.config.ts` and are used
 * natively as fallback values when runtime options are not provided.
 */
export interface IntegrityConfig {
  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information is printed
   * to the native console (Logcat on Android, Xcode on iOS).
   *
   * This option affects native logging behavior only and
   * has no impact on the JavaScript API or runtime behavior.
   *
   * @default false
   * @example true
   * @since 8.0.0
   */
  verboseLogging?: boolean;

  /**
   * Optional configuration for the integrity block page.
   *
   * This configuration controls the availability and source
   * of a developer-provided HTML page that may be presented
   * to the end user when the host application decides to do so.
   *
   * This configuration is:
   * - read only by native code
   * - immutable at runtime
   * - NOT accessible from JavaScript
   *
   * The Integrity plugin will NEVER automatically present
   * the block page. Presentation is always explicitly triggered
   * by the host application via the public API.
   *
   * @since 8.0.0
   */
  blockPage?: IntegrityBlockPageConfig;

  /**
   * Optional configuration for jailbreak URL scheme probing (iOS only).
   *
   * When enabled, the native iOS implementation may probe for
   * known jailbreak-related applications using URL schemes
   * such as `cydia://`.
   *
   * This configuration:
   * - is read natively at runtime
   * - is immutable
   * - is NOT accessible from JavaScript
   * - does NOT alter the public JavaScript API
   *
   * @since 8.0.0
   */
  jailbreakUrlSchemes?: JailbreakUrlSchemesConfig;
}

/**
 *
 * @since 8.0.0
 */
export interface IntegrityBlockPageConfig {
  /**
   * Enables the block page feature.
   *
   * When set to false or omitted, calls to `presentBlockPage()`
   * will be ignored or resolved as not presented.
   *
   * @default false
   * @example true
   * @since 8.0.0
   */
  enabled?: boolean;

  /**
   * URL or local path of the HTML page to present.
   *
   * This value may reference:
   * - a local file bundled with the application
   * - a remote HTTPS URL
   *
   * Interpretation and loading are platform-specific
   * and handled entirely by native code.
   *
   * @example 'integrity-block.html'
   * @example 'https://example.com/integrity.html'
   * @since 8.0.0
   */
  url: string;
}

/**
 * Configuration for jailbreak URL scheme probing (iOS only).
 *
 * This configuration enables the Integrity plugin to probe for
 * known jailbreak-related applications by checking whether
 * specific URL schemes can be opened by the system.
 *
 * IMPORTANT:
 * - This feature is **disabled by default**.
 * - It is read natively and is NOT accessible from JavaScript at runtime.
 * - Enabling this feature requires declaring the corresponding
 *   schemes in `LSApplicationQueriesSchemes` inside `Info.plist`.
 * - This detection emits a LOW confidence signal and MUST NOT
 *   be treated as a standalone jailbreak decision.
 *
 * @since 8.0.0
 */
export interface JailbreakUrlSchemesConfig {
  /**
   * Enables jailbreak URL scheme probing.
   *
   * When set to false or omitted, no URL scheme probing
   * will be performed by the native iOS implementation.
   *
   * @default false
   * @example true
   */
  enabled?: boolean;

  /**
   * List of URL schemes to probe.
   *
   * Each scheme should be provided WITHOUT the `://` suffix.
   *
   * @example ['cydia', 'sileo', 'zbra']
   */
  schemes: string[];
}

/**
 * Category of a detected integrity signal.
 *
 * Categories are intentionally broad and stable.
 * New detection techniques MUST reuse existing categories
 * whenever possible to avoid breaking consumers.
 *
 * @since 8.0.0
 */
export type IntegritySignalCategory = 'root' | 'jailbreak' | 'emulator' | 'debug' | 'hook' | 'tamper' | 'environment';

/**
 * Internal confidence levels used by native implementations.
 *
 * IMPORTANT:
 * This enum is INTERNAL and MUST NOT be considered a public API.
 * It exists to freeze semantic meaning and avoid string drift
 * across platforms and future refactors.
 */
enum IntegrityConfidenceLevel {
  LOW = 'low',
  MEDIUM = 'medium',
  HIGH = 'high',
}

/**
 * A single integrity signal detected on the current device.
 *
 * Signals represent *observations*, not decisions.
 * Multiple signals MAY be combined by the host application
 * to derive a security policy.
 *
 * Signals:
 * - are emitted asynchronously
 * - may occur at any time during the app lifecycle
 * - may be emitted before or after the first call to `check()`
 *
 * @since 8.0.0
 */
export interface IntegritySignal {
  /**
   * Stable identifier for the signal.
   *
   * This value:
   * - is stable across releases
   * - MUST NOT be parsed or pattern-matched
   * - is intended for analytics, logging, and policy evaluation
   */
  id: string;

  /**
   * High-level category of the signal.
   *
   * Categories allow grouping related signals
   * without relying on specific identifiers.
   */
  category: IntegritySignalCategory;

  /**
   * Confidence level of the detection.
   *
   * This value expresses how strongly the signal correlates
   * with a potentially compromised or risky environment.
   *
   * NOTE:
   * Although typed as a string union in the public API,
   * native implementations MUST only emit values defined
   * by the internal IntegrityConfidenceLevel enum.
   */
  confidence: IntegrityConfidenceLevel;

  /**
   * Optional human-readable description.
   *
   * This field:
   * - is intended for diagnostics and debugging only
   * - MAY be omitted or redacted in production builds
   * - MUST NOT be relied upon programmatically
   */
  description?: string;

  /**
   * Additional diagnostic metadata associated with the signal.
   *
   * Metadata provides granular details about the detection
   * (e.g. matched filesystem paths, runtime artifacts,
   * or environment properties) without altering the
   * stable signal identifier.
   *
   * IMPORTANT:
   * - Metadata is informational only.
   * - Keys and values are NOT guaranteed to be stable.
   * - Applications MUST NOT rely on specific metadata fields
   *   for security decisions.
   */
  metadata?: Record<string, string | number | boolean>;
}

/**
 * Summary of the execution environment in which
 * the integrity check was performed.
 *
 * @since 8.0.0
 */
export interface IntegrityEnvironment {
  /**
   * Current platform.
   */
  platform: 'ios' | 'android' | 'web';

  /**
   * Indicates whether the app is running
   * in an emulator or simulator environment.
   */
  isEmulator: boolean;

  /**
   * Indicates whether the app was built
   * in debug/development mode.
   */
  isDebugBuild: boolean;
}

/**
 * Result object returned by `Integrity.check()`.
 *
 * This object aggregates all detected signals
   and provides a provisional integrity score.
 *
 * @since 8.0.0
 */
export interface IntegrityReport {
  /**
   * Indicates whether the environment is considered compromised
   * according to the current scoring model.
   */
  compromised: boolean;

  /**
   * Provisional integrity score.
   *
   * The score ranges from 0 to 100 and is derived
   * from the detected signals.
   */
  score: number;

  /**
   * List of detected integrity signals.
   */
  signals: IntegritySignal[];

  /**
   * Execution environment summary.
   */
  environment: IntegrityEnvironment;

  /**
   * Unix timestamp (milliseconds) when the check was performed.
   */
  timestamp: number;

  /**
   * Optional explanation metadata describing how the integrity score
   * was derived from the detected signals.
   *
   * This field is informational only and MUST NOT be treated
   * as a security decision or enforcement mechanism.
   *
   * @since 8.0.0
   */
  scoreExplanation?: IntegrityScoreExplanation;
}

/**
 * Describes how the integrity score was derived.
 *
 * This structure provides transparency and auditability
 * without exposing internal scoring algorithms.
 *
 * @since 8.0.0
 */
export interface IntegrityScoreExplanation {
  /**
   * Total number of detected signals.
   */
  totalSignals: number;

  /**
   * Breakdown of signals by confidence level.
   */
  byConfidence: {
    high: number;
    medium: number;
    low: number;
  };

  /**
   * List of signal identifiers that contributed to the score.
   */
  contributors: string[];
}

// -----------------------------------------------------------------------------
// Method Options
// -----------------------------------------------------------------------------

/**
 * Options controlling the behavior of `Integrity.check()`.
 *
 * These options influence *how* checks are performed,
   not *what* the public API returns.
 *
 * @since 8.0.0
 */
export interface IntegrityCheckOptions {
  /**
   * Desired strictness level.
   *
   * Higher levels may enable additional heuristics
   * at the cost of performance.
   */
  level?: 'basic' | 'standard' | 'strict';

  /**
   * Includes additional debug information
   * in the returned signals when enabled.
   */
  includeDebugInfo?: boolean;
}

/**
 * Options for presenting the integrity block page.
 *
 * @since 8.0.0
 */
export interface PresentBlockPageOptions {
  /**
   * Optional reason code passed to the block page.
   *
   * This value may be used for analytics,
   * localization, or user messaging.
   *
   * @since 8.0.0
   */
  reason?: string;

  /**
   * Whether the block page can be dismissed by the user.
   *
   * Defaults to false.
   * In production environments, this should typically remain disabled.
   *
   * @default false
   * @since 8.0.0
   */
  dismissible?: boolean;
}

// -----------------------------------------------------------------------------
// Results
// -----------------------------------------------------------------------------

/**
 * Result object returned by `presentBlockPage()`.
 *
 * @since 8.0.0
 */
export interface PresentBlockPageResult {
  /**
   * Indicates whether the block page was actually presented.
   */
  presented: boolean;
}

/**
 * Result returned by the getPluginVersion method.
 */
export interface PluginVersionResult {
  /** The native version string of the plugin. */
  version: string;
}

/**
 * Structured error object returned by Integrity plugin operations.
 *
 * This object allows consumers to handle errors without relying
 * on exception-based control flow.
 *
 * @since 8.0.0
 */
export interface IntegrityError {
  /**
   * Human-readable error description.
   */
  message: string;

  /**
   * Machine-readable error code.
   */
  code: IntegrityErrorCode;
}

// -----------------------------------------------------------------------------
// Plugin Interface
// -----------------------------------------------------------------------------

/**
 * Public JavaScript API for the Integrity Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API.
 * All methods behave consistently across Android, iOS, and Web.
 */
export interface IntegrityPlugin {
  /**
   * Executes a runtime integrity check.
   *
   * @example
   * ```ts
   * const report = await Integrity.check();
   * ```
   *
   * @since 8.0.0
   */
  check(options?: IntegrityCheckOptions): Promise<IntegrityReport>;

  /**
   * Presents the configured integrity block page, if enabled.
   *
   * The plugin never decides *when* this method should be called.
   * Invocation is entirely controlled by the host application.
   *
   * @example
   * ```ts
   * await Integrity.presentBlockPage({ reason: 'integrity_failed' });
   * ```
   *
   * @since 8.0.0
   */
  presentBlockPage(options?: PresentBlockPageOptions): Promise<PresentBlockPageResult>;

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
   * const { version } = await Integrity.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;

  /**
   * Registers a listener for real-time integrity signals.
   *
   * The provided callback is invoked every time a new integrity
   * signal is detected by the native layer.
   *
   * BEHAVIOR:
   * - Signals may be emitted at any time after plugin initialization.
   * - Signals detected before listener registration MAY be delivered
   *   immediately after registration.
   * - No guarantees are made about signal frequency or ordering
   *   across platforms.
   *
   * IMPORTANT:
   * - This listener is non-blocking.
   * - The plugin does NOT enforce any policy based on signals.
   *
   * @param eventName The event to listen for ('integritySignal').
   * @param listenerFunc Callback invoked with the detected signal.
   * @returns A Promise resolving to a `PluginListenerHandle`.
   *
   * @since 8.0.0
   */
  addListener(
    eventName: 'integritySignal',
    listenerFunc: (signal: IntegritySignalEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Removes all registered listeners for this plugin.
   *
   * NOTE:
   * - Removing listeners does NOT stop signal detection natively.
   * - Signals may continue to be detected and buffered
   *   until a listener is registered again.
   *
   * @since 8.0.0
   */
  removeAllListeners(): Promise<void>;
}
