/// <reference types="@capacitor/cli" />

import { PluginListenerHandle } from '@capacitor/core';

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

  /**
   * Native/Web logging threshold.
   *
   * - `error`: errors only
   * - `warn`: warnings and errors
   * - `debug`: debug/info/warn/error
   * - `verbose`: maximum logging level
   *
   * @default 'info'
   * @since 8.0.0
   */
  logLevel?: 'error' | 'warn' | 'debug' | 'verbose';

  /**
   * Global auto-lock timeout in milliseconds.
   *
   * @default 60000
   * @since 8.0.0
   */
  lockAfterMs?: number;

  /**
   * Security level for biometric hardware access.
   *
   * @default 'biometryCurrentSet'
   * @since 8.0.0
   */
  accessControl?: BiometricAccessControl;

  /**
   * Enables or disables privacy protection for app snapshots.
   *
   * Platform behavior:
   * - Android relies on window snapshot protection in recents/task switcher.
   * - iOS uses a visual privacy overlay.
   *
   * Note: On Android recents previews, system-protected cards may not render
   * custom overlay text/image and can appear as a blank/protected preview.
   *
   * @default true
   * @since 8.0.0
   */
  enablePrivacyScreen?: boolean;

  /**
   * Optional text rendered on top of the privacy screen overlay.
   *
   * This is intended for lock-state messaging such as
   * "Session Locked" or "Tap to Unlock".
   *
   * Platform note:
   * - Android: text is shown on the in-app overlay.
   * - Android recents/task switcher: system snapshot protection may hide
   *   custom text in preview cards.
   *
   * @since 8.0.0
   */
  privacyOverlayText?: string;

  /**
   * Optional native asset name rendered on top of the privacy screen overlay.
   *
   * Asset lookup rules:
   * - iOS: Image from app asset catalog by name
   * - Android: Drawable resource by name
   *
   * Platform note:
   * - Android: image is shown on the in-app overlay.
   * - Android recents/task switcher: system snapshot protection may hide
   *   custom images in preview cards.
   *
   * @since 8.0.0
   */
  privacyOverlayImageName?: string;

  /**
   * Controls whether privacy overlay text is visible.
   *
   * @default true
   * @since 8.0.0
   */
  privacyOverlayShowText?: boolean;

  /**
   * Controls whether privacy overlay image is visible.
   *
   * @default true
   * @since 8.0.0
   */
  privacyOverlayShowImage?: boolean;

  /**
   * Optional text color (hex string) for the privacy overlay label.
   *
   * Example: `#FFFFFF`
   *
   * @since 8.0.0
   */
  privacyOverlayTextColor?: string;

  /**
   * Optional background opacity for the privacy overlay scrim.
   *
   * Allowed range: `0.0` to `1.0`.
   *
   * @since 8.0.0
   */
  privacyOverlayBackgroundOpacity?: number;

  /**
   * Controls the privacy overlay visual theme.
   *
   * - `system`: follow device appearance (light/dark)
   * - `light`: force light overlay appearance
   * - `dark`: force dark overlay appearance
   *
   * @default 'system'
   * @since 8.0.0
   */
  privacyOverlayTheme?: 'system' | 'light' | 'dark';

  /**
   * Prefix used by key obfuscation utilities.
   *
   * @default 'ftrss_'
   * @since 8.0.0
   */
  obfuscationPrefix?: string;

  /**
   * Base64-encodes stored key names (secure vault and insecure storage,
   * all platforms) on top of the obfuscation prefix.
   *
   * Obfuscation hides key names from casual inspection; it is not
   * encryption. Reads transparently accept both encoded and plain forms,
   * so toggling never orphans existing entries.
   *
   * @default false
   * @since 8.0.0
   */
  obfuscateKeys?: boolean;

  /**
   * WebAuthn configuration for Web platform unlock behavior.
   *
   * - `local` mode stores credential metadata only in browser storage.
   * - `server` mode uses backend challenge and assertion verification endpoints.
   *
   * @since 8.0.0
   */
  webAuthn?: WebAuthnConfig;

  /**
   * Enables in-memory cached authentication for unlock operations.
   *
   * When enabled, repeated `unlock()` calls within `cachedAuthenticationTimeoutMs`
   * can skip the interactive biometric prompt.
   *
   * @default false
   * @since 8.0.0
   */
  allowCachedAuthentication?: boolean;

  /**
   * Cached authentication validity window in milliseconds.
   *
   * This value is only used when `allowCachedAuthentication` is enabled.
   *
   * @default 30000
   * @since 8.0.0
   */
  cachedAuthenticationTimeoutMs?: number;

  /**
   * Asymmetric key-pair strategy for cryptographic operations.
   *
   * - `auto`: platform default strategy
   * - `ecc`: force elliptic-curve key generation where supported
   * - `rsa`: force RSA key generation where supported
   *
   * @default 'auto'
   * @since 8.0.0
   */
  cryptoStrategy?: 'auto' | 'ecc' | 'rsa';

  /**
   * RSA key size used when `cryptoStrategy` is set to `rsa`.
   *
   * @default 2048
   * @since 8.0.0
   */
  keySize?: 2048 | 4096;

  /**
   * Maximum failed biometric attempts before temporary lockout.
   *
   * @default 5
   * @since 8.0.0
   */
  maxBiometricAttempts?: number;

  /**
   * Temporary lockout duration in milliseconds after reaching the
   * biometric failure threshold.
   *
   * @default 30000
   * @since 8.0.0
   */
  lockoutDurationMs?: number;

  /**
   * Maximum allowed age in milliseconds for the last successful biometric
   * authentication before requiring a fresh authentication.
   *
   * @default 0 (disabled)
   * @since 8.0.0
   */
  requireFreshAuthenticationMs?: number;

  /**
   * Symmetric encryption algorithm used by the Web secure storage layer.
   * Native platforms keep hardware-backed secure defaults.
   *
   * @default 'AES-GCM'
   * @since 8.0.0
   */
  encryptionAlgorithm?: 'AES-GCM' | 'AES-CBC';

  /**
   * Enables iCloud Keychain synchronization for iOS secure-storage entries.
   *
   * Platform behavior:
   * - iOS: when enabled, generic-password vault items are created as synchronizable
   * - Android/Web: ignored (no-op)
   *
   * @default false
   * @since 8.0.0
   */
  enableICloudKeychainSync?: boolean;

  /**
   * Persists web session lock/auth state across page reloads.
   *
   * Platform behavior:
   * - Web: when enabled, vault/session state is restored from persisted storage
   * - iOS/Android: ignored (no-op)
   *
   * @default false
   * @since 8.0.0
   */
  persistSessionState?: boolean;

  /**
   * Controls fallback behavior when biometric authentication is unavailable
   * or fails during an interactive prompt.
   *
   * - `deviceCredential`: always allow device credential fallback when supported.
   * - `none`: disallow device credential fallback and require biometrics only.
   * - `systemDefault`: preserve legacy behavior (`allowDevicePasscode` on native).
   *
   * @default 'systemDefault'
   * @since 8.0.0
   */
  fallbackStrategy?: 'deviceCredential' | 'none' | 'systemDefault';
}

/**
 * Runtime configuration snapshot currently used by the plugin.
 *
 * This reflects static startup configuration merged with
 * runtime overrides applied via `configure(...)`.
 *
 * @since 8.0.0
 */
export interface FortressRuntimeConfig {
  verboseLogging: boolean;
  logLevel: 'error' | 'warn' | 'debug' | 'verbose' | 'info';
  lockAfterMs: number;
  enablePrivacyScreen: boolean;
  privacyOverlayText: string;
  privacyOverlayImageName: string;
  privacyOverlayShowText: boolean;
  privacyOverlayShowImage: boolean;
  privacyOverlayTextColor: string;
  privacyOverlayBackgroundOpacity: number;
  privacyOverlayTheme: 'system' | 'light' | 'dark';
  privacyScreenEnabled: boolean;
  obfuscateKeys: boolean;
  fallbackStrategy: 'none' | 'deviceCredential' | 'systemDefault';
  allowCachedAuthentication: boolean;
  cachedAuthenticationTimeoutMs: number;
  maxBiometricAttempts: number;
  lockoutDurationMs: number;
  requireFreshAuthenticationMs: number;
  encryptionAlgorithm: 'AES-GCM' | 'AES-CBC';
  persistSessionState: boolean;
}

/**
 * Platform-specific display options for the privacy screen.
 *
 * This shape mirrors the official `@capacitor/privacy-screen` configuration
 * so Fortress stays a drop-in replacement. Visual style beyond these knobs
 * is governed by the richer Fortress overlay options
 * (`privacyOverlayText`, `privacyOverlayImageName`, `privacyOverlayTheme`, ...).
 *
 * @since 8.0.0
 */
export interface PrivacyScreenConfig {
  android?: {
    /**
     * Shows a dim scrim instead of the splash drawable while protected.
     *
     * @default false
     */
    dimBackground?: boolean;
    /**
     * @deprecated FLAG_SECURE is always applied while the privacy screen is
     * enabled. To allow screenshots for a screen or flow, call `disable()`
     * before it and `enable()` after it.
     */
    preventScreenshots?: boolean;
    /**
     * Overlay shown when the activity is hidden (e.g. system biometric prompt).
     *
     * @default 'none'
     */
    privacyModeOnActivityHidden?: 'none' | 'dim' | 'splash';
  };
  ios?: {
    /**
     * Blur style used to obscure the app-switcher snapshot.
     *
     * @default 'none'
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
   */
  enabled: boolean;
}

/**
 * Input payload for standalone identity verification.
 *
 * Unlike `unlock()`, `authenticate()` never changes vault or session state:
 * it only proves the user is present with biometrics or device credentials.
 *
 * @since 8.0.0
 */
export interface AuthenticateOptions {
  /**
   * Reason shown in the system prompt.
   */
  reason?: string;
  promptMessage?: string;
  promptOptions?: BiometricPromptOptions;
  /**
   * Allows device credential (PIN/pattern/password/passcode) fallback.
   *
   * When omitted, the configured `fallbackStrategy` applies.
   *
   * @since 8.0.0
   */
  allowDeviceCredential?: boolean;
}

/**
 * Result of `isAvailable()`.
 *
 * @since 8.0.0
 */
export interface IsAvailableResult {
  isAvailable: boolean;
}

/**
 * Result of `isEnrolled()`.
 *
 * @since 8.0.0
 */
export interface IsEnrolledResult {
  isEnrolled: boolean;
}

/**
 * Result of `getBiometricType()`.
 *
 * @since 8.0.0
 */
export interface BiometricTypeResult {
  biometryType: DeviceSecurityStatus['biometryType'];
}

/**
 * Result of `getBiometricTypes()`.
 *
 * @since 8.0.0
 */
export interface BiometricTypesResult {
  biometryTypes: DeviceSecurityStatus['biometryType'][];
}

/**
 * Result of `hasDeviceCredential()`.
 *
 * @since 8.0.0
 */
export interface HasDeviceCredentialResult {
  hasDeviceCredential: boolean;
}

/**
 * Result of `getBiometricStrengthLevel()`.
 *
 * - `strong`: Face ID / Touch ID / strong-class Android biometrics.
 * - `weak`: only weak modalities (e.g. some Android face unlocks).
 * - `none`: no usable biometry.
 *
 * @since 8.0.0
 */
export interface BiometricStrengthResult {
  strengthLevel: 'strong' | 'weak' | 'none';
}

/**
 * Result of `getAuthenticationType()`.
 *
 * Reports which credential satisfied the last successful `authenticate()`
 * (or `unlock()`) on native platforms. `'unknown'` when nothing has
 * authenticated yet in this session or the platform does not report it.
 *
 * @since 8.0.0
 */
export interface AuthenticationTypeResult {
  authenticationType: 'biometric' | 'deviceCredential' | 'unknown';
}

/**
 * Prompt customization options for interactive authentication.
 *
 * Platform note:
 * - Android uses title/subtitle/description/negativeButtonText directly.
 * - Android BiometricPrompt layout/iconography remains system-controlled.
 * - iOS uses localized reason + cancel title best-effort mapping.
 * - Web keeps this shape for API parity.
 */
export interface BiometricPromptOptions {
  title?: string;
  subtitle?: string;
  description?: string;
  negativeButtonText?: string;
  confirmationRequired?: boolean;
}

/**
 * Optional input payload for vault unlock.
 */
export interface UnlockOptions {
  promptMessage?: string;
  promptOptions?: BiometricPromptOptions;
}

/**
 * WebAuthn behavior and backend integration options (Web platform only).
 */
export interface WebAuthnConfig {
  /**
   * WebAuthn operating mode.
   *
   * @default 'local'
   * @since 8.0.0
   */
  mode?: 'local' | 'server';

  /**
   * HTTP endpoint that starts WebAuthn registration and returns challenge payload.
   *
   * @since 8.0.0
   */
  registrationStartUrl?: string;

  /**
   * HTTP endpoint that verifies registration attestation.
   *
   * @since 8.0.0
   */
  registrationFinishUrl?: string;

  /**
   * HTTP endpoint that starts WebAuthn authentication and returns challenge payload.
   *
   * @since 8.0.0
   */
  authenticationStartUrl?: string;

  /**
   * HTTP endpoint that verifies authentication assertion.
   *
   * @since 8.0.0
   */
  authenticationFinishUrl?: string;

  /**
   * Optional extra headers attached to server WebAuthn requests.
   *
   * @since 8.0.0
   */
  headers?: Record<string, string>;
}

/**
 * Native biometric access control options.
 */
export type BiometricAccessControl = 'biometryAny' | 'biometryCurrentSet' | 'passcodeAny' | 'devicePasscode';

export interface DeviceSecurityStatus {
  isBiometricsAvailable: boolean;
  isBiometricsEnabled: boolean;
  isDeviceSecure: boolean;
  biometryType: 'none' | 'touchId' | 'faceId' | 'fingerprint' | 'iris';
  /**
   * All enrolled biometry modalities known to the device.
   *
   * @since 8.0.0
   */
  biometryTypes: DeviceSecurityStatus['biometryType'][];
  /**
   * Whether strong biometry specifically is available (all iOS biometry
   * is strong; on Android weak modalities such as some face unlocks
   * may make this false while `isBiometricsAvailable` is true).
   *
   * @since 8.0.0
   */
  strongBiometryIsAvailable: boolean;
}

/**
 * Input payload for overriding detected biometry type in development/testing.
 */
export interface SetBiometryTypeOptions {
  biometryType: DeviceSecurityStatus['biometryType'];
}

/**
 * Input payload for overriding biometrics enrollment state in development/testing.
 */
export interface SetBiometryIsEnrolledOptions {
  isBiometricsEnabled: boolean;
}

/**
 * Input payload for overriding device secure-state in development/testing.
 */
export interface SetDeviceIsSecureOptions {
  isDeviceSecure: boolean;
}

/**
 * iOS Keychain accessibility level for secure storage items.
 *
 * Mirrors the platform `kSecAttrAccessible` constants. Applies to iOS
 * only; ignored on Android and Web.
 *
 * - `whenUnlocked`: foreground-only, migrates with encrypted backups.
 * - `whenUnlockedThisDeviceOnly`: foreground-only, never migrates.
 * - `afterFirstUnlock`: background-capable after first unlock, migrates.
 * - `afterFirstUnlockThisDeviceOnly`: background-capable, never migrates.
 * - `whenPasscodeSetThisDeviceOnly`: requires device passcode, never migrates.
 *
 * @since 8.0.0
 */
export type KeychainAccess =
  | 'whenUnlocked'
  | 'whenUnlockedThisDeviceOnly'
  | 'afterFirstUnlock'
  | 'afterFirstUnlockThisDeviceOnly'
  | 'whenPasscodeSetThisDeviceOnly';

/**
 * Generic key/value payload for storage methods.
 */
export interface SecureValue {
  key: string;
  value: string;
  secure?: boolean;
  /**
   * iOS Keychain accessibility for this item, overriding the default set
   * via `setDefaultKeychainAccess()`. Ignored on Android and Web.
   *
   * @since 8.0.0
   */
  access?: KeychainAccess;
}

/**
 * Result returned by secure and insecure read operations.
 */
export interface ValueResult {
  value: string | null;
}

/**
 * Current session status exposed to JavaScript.
 */
export interface FortressSession {
  isLocked: boolean;
  lastActiveAt: number;
}

/**
 * Result object used by key existence checks.
 */
export interface HasKeyResult {
  exists: boolean;
}

/**
 * Input payload for key existence checks.
 */
export interface HasKeyOptions {
  key: string;
  secure?: boolean;
}

/**
 * Input payload for key enumeration.
 *
 * @since 8.0.0
 */
export interface KeysOptions {
  /**
   * Which tier to list. Defaults to the secure vault.
   *
   * @default true
   */
  secure?: boolean;
}

/**
 * Result of key enumeration.
 *
 * Keys are returned in their original (de-obfuscated) form.
 *
 * @since 8.0.0
 */
export interface KeysResult {
  keys: string[];
}

/**
 * Input payload for batch reads.
 *
 * @since 8.0.0
 */
export interface GetManyOptions {
  keys: string[];
  /**
   * Which tier to read from. Defaults to the secure vault.
   *
   * @default true
   */
  secure?: boolean;
}

/**
 * Result of batch reads.
 *
 * Missing keys map to `null`. A locked vault (secure tier) rejects
 * the whole call with `VAULT_LOCKED` instead of returning partial data.
 *
 * @since 8.0.0
 */
export interface GetManyResult {
  values: Record<string, string | null>;
}

/**
 * Result object used by key obfuscation utility.
 */
export interface ObfuscatedKeyResult {
  obfuscated: string;
}

/**
 * Input payload for biometric signature creation.
 */
export interface CreateSignatureOptions {
  payload: string;
  keyAlias?: string;
  promptMessage?: string;
  promptOptions?: BiometricPromptOptions;
}

/**
 * Result payload returned by biometric signature creation.
 */
export interface CreateSignatureResult {
  success: true;
  signature: string;
}

/**
 * Result payload returned by key-pair existence checks.
 */
export interface BiometricKeysExistResult {
  keysExist: boolean;
}

/**
 * Result payload returned after creating a biometric key pair.
 */
export interface CreateKeysResult {
  publicKey: string;
}

/**
 * Input payload for key-pair operations.
 */
export interface KeyAliasOptions {
  keyAlias?: string;
}

/**
 * Input payload for challenge-based registration/authentication.
 */
export interface ChallengeAuthOptions extends KeyAliasOptions {
  challenge: string;
  promptMessage?: string;
  promptOptions?: BiometricPromptOptions;
}

/**
 * Result payload returned by challenge registration.
 */
export interface RegisterWithChallengeResult {
  publicKey: string;
  signature: string;
}

/**
 * Result payload returned by challenge authentication.
 */
export interface AuthenticateWithChallengeResult {
  signature: string;
}

/**
 * Payload emitted when the vault is invalidated by security posture changes.
 */
export interface VaultInvalidatedEvent {
  reason: 'security_state_changed' | 'keypair_invalidated' | 'keys_deleted';
}

/**
 * Input payload for canonical backend challenge payload generation.
 */
export interface GenerateChallengePayloadOptions {
  nonce: string;
}

/**
 * Result payload returned by backend challenge payload generation.
 */
export interface GenerateChallengePayloadResult {
  payload: string;
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
  /** The requested resource or key does not exist in the secure or standard storage. */
  NOT_FOUND = 'NOT_FOUND',
  /** The operation conflicts with the current state of the plugin (e.g., re-initializing an active session). */
  CONFLICT = 'CONFLICT',
  /** The operation did not complete within the expected time frame. */
  TIMEOUT = 'TIMEOUT',
  /** A cryptographic or integrity validation failed in native code. */
  SECURITY_VIOLATION = 'SECURITY_VIOLATION',
  /** The operation requires the secure vault to be unlocked first. */
  VAULT_LOCKED = 'VAULT_LOCKED',
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
   * Returns the runtime configuration currently used by Fortress.
   *
   * @returns A snapshot of effective runtime configuration values.
   * @since 8.0.0
   */
  getRuntimeConfig(): Promise<FortressRuntimeConfig>;

  /**
   * Applies runtime-safe Fortress configuration values.
   *
   * Configuration values set here take precedence over
   * those defined in capacitor.config.ts.
   *
   * @param config - The Fortress configuration object.
   * @returns A promise that resolves when configuration is applied.
   *
   * @example
   * ```ts
   * await Fortress.configure({
   *   lockAfterMs: 300000,
   *   enablePrivacyScreen: true,
   * });
   * ```
   *
   * @since 8.0.0
   */
  configure(config: FortressConfig): Promise<void>;

  /**
   * Resets runtime overrides to the startup static baseline.
   *
   * This clears persisted runtime overrides and reapplies the
   * baseline values originally loaded at plugin startup.
   *
   * @since 8.0.0
   */
  resetRuntimeConfig(): Promise<void>;

  /**
   * Stores a secure value in the encrypted vault.
   *
   * Values are encrypted using hardware-backed security
   * (Secure Enclave on iOS, Keystore on Android).
   *
   * The method rejects with `VAULT_LOCKED` when the vault is locked.
   *
   * @param value - The key-value pair to store securely.
   * @returns A promise that resolves when the value is stored.
   *
   * @example
   * ```ts
   * await Fortress.setValue({ key: 'auth_token', value: 'abc123' });
   * ```
   *
   * @since 8.0.0
   */
  setValue(value: SecureValue): Promise<void>;

  /**
   * Reads a secure value from the encrypted vault.
   *
   * The method rejects with:
   * - `VAULT_LOCKED` when the vault is locked
   * - `SECURITY_VIOLATION` when stored data cannot be decrypted/validated
   *
   * @param key - The key to retrieve.
   * @returns A promise resolving to the stored value or null if not found.
   *
   * @example
   * ```ts
   * const { value } = await Fortress.getValue({ key: 'auth_token' });
   * ```
   *
   * @since 8.0.0
   */
  getValue(key: { key: string }): Promise<ValueResult>;

  /**
   *
   * @since 8.0.0
   */
  setMany(options: { values: SecureValue[] }): Promise<void>;

  /**
   *
   * @since 8.0.0
   */
  checkStatus(): Promise<DeviceSecurityStatus>;

  /**
   * Reports whether biometric authentication can currently be used.
   *
   * This is the `isAvailable` half of the official-style availability
   * contract: hardware present and usable, regardless of enrollment.
   *
   * @returns A promise resolving to `{ isAvailable }`.
   *
   * @since 8.0.0
   */
  isAvailable(): Promise<IsAvailableResult>;

  /**
   * Reports whether the user enrolled biometrics.
   *
   * This is the `isEnrolled` half of the official-style availability
   * contract. Check both `isAvailable()` and `isEnrolled()` before
   * calling `authenticate()`.
   *
   * @returns A promise resolving to `{ isEnrolled }`.
   *
   * @since 8.0.0
   */
  isEnrolled(): Promise<IsEnrolledResult>;

  /**
   * Returns the primary biometry modality of the device.
   *
   * Display helper — always decide with `isAvailable()` instead.
   *
   * @returns A promise resolving to `{ biometryType }`.
   *
   * @since 8.0.0
   */
  getBiometricType(): Promise<BiometricTypeResult>;

  /**
   * Returns every biometry modality known to the device hardware.
   *
   * Only Android devices report more than one entry; empty when none
   * is supported.
   *
   * @returns A promise resolving to `{ biometryTypes }`.
   *
   * @since 8.0.0
   */
  getBiometricTypes(): Promise<BiometricTypesResult>;

  /**
   * Reports whether the user set a device credential (PIN, pattern,
   * password, passcode) usable as authentication fallback.
   *
   * @returns A promise resolving to `{ hasDeviceCredential }`.
   *
   * @since 8.0.0
   */
  hasDeviceCredential(): Promise<HasDeviceCredentialResult>;

  /**
   * Reports the strength class of the available biometry.
   *
   * iOS biometry is always `strong` when available; on Android weak
   * modalities (e.g. some face unlocks) yield `weak`.
   *
   * @returns A promise resolving to `{ strengthLevel }`.
   *
   * @since 8.0.0
   */
  getBiometricStrengthLevel(): Promise<BiometricStrengthResult>;

  /**
   * Reports which credential satisfied the last successful native
   * `authenticate()` (or `unlock()`).
   *
   * @returns A promise resolving to `{ authenticationType }`.
   *
   * @since 8.0.0
   */
  getAuthenticationType(): Promise<AuthenticationTypeResult>;

  /**
   * Opens the system biometric enrollment screen.
   *
   * Only available on Android (API 30+ launches the system enroll flow).
   * iOS offers no public enrollment API and rejects with `UNAVAILABLE`,
   * as does Web.
   *
   * @since 8.0.0
   */
  enroll(): Promise<void>;

  /**
   * Overrides detected biometry type for development/testing scenarios.
   *
   * This method is intended for QA and simulator/device mocking flows.
   *
   * @since 8.0.0
   */
  setBiometryType(options: SetBiometryTypeOptions): Promise<void>;

  /**
   * Overrides biometrics enrollment state for development/testing scenarios.
   *
   * @since 8.0.0
   */
  setBiometryIsEnrolled(options: SetBiometryIsEnrolledOptions): Promise<void>;

  /**
   * Overrides device secure-state for development/testing scenarios.
   *
   * @since 8.0.0
   */
  setDeviceIsSecure(options: SetDeviceIsSecureOptions): Promise<void>;

  /**
   * Removes a secure value from the encrypted vault.
   *
   * @param key - The key to remove.
   * @returns A promise that resolves when the value is removed.
   *
   * @example
   * ```ts
   * await Fortress.removeValue({ key: 'auth_token' });
   * ```
   *
   * @since 8.0.0
   */
  removeValue(key: { key: string }): Promise<void>;

  /**
   * Clears all secure values from the encrypted vault.
   *
   * @returns A promise that resolves when all values are cleared.
   *
   * @example
   * ```ts
   * await Fortress.clearAll();
   * ```
   *
   * @since 8.0.0
   */
  clearAll(): Promise<void>;

  /**
   * Triggers the secure unlock flow using biometrics or device credentials.
   *
   * This method initiates authentication via Face ID, Touch ID,
   * or the device passcode as a fallback.
   *
   * @returns A promise that resolves when authentication succeeds.
   * @throws {FortressError} When authentication fails or is cancelled.
   *
   * @example
   * ```ts
   * try {
   *   await Fortress.unlock();
   *   console.log('Vault unlocked');
   * } catch (e) {
   *   console.error('Authentication failed:', e.message);
   * }
   * ```
   *
   * @since 8.0.0
   */
  unlock(options?: UnlockOptions): Promise<void>;

  /**
   * Verifies the user identity without touching vault or session state.
   *
   * This is the standalone biometric check from the official-style
   * biometrics contract (`authenticate` / `verifyIdentity`): the promise
   * resolves when the user authenticates and rejects with `CANCELLED`
   * (user dismissed) or `UNAVAILABLE` (no usable authenticator).
   *
   * Unlike `unlock()`, a success here does NOT unlock the vault, does NOT
   * update the session timestamp, and does NOT hide the privacy overlay.
   *
   * @param options - Prompt customization and credential fallback.
   * @returns A promise that resolves when authentication succeeds.
   *
   * @example
   * ```ts
   * await Fortress.authenticate({ reason: 'Confirm payment' });
   * ```
   *
   * @since 8.0.0
   */
  authenticate(options?: AuthenticateOptions): Promise<void>;

  /**
   * Cancels an ongoing interactive authentication prompt, if any.
   *
   * Resolves immediately when no prompt is active. On iOS the in-flight
   * system dialog is invalidated; on Android (SDK 29+) the
   * `BiometricPrompt` is cancelled; on Web it is a no-op.
   *
   * @since 8.0.0
   */
  cancelAuthentication(): Promise<void>;

  /**
   * Locks the secure vault immediately.
   *
   * All stored secure values become inaccessible until
   * the user authenticates again via unlock().
   *
   * @returns A promise that resolves when the vault is locked.
   *
   * @example
   * ```ts
   * await Fortress.lock();
   * ```
   *
   * @since 8.0.0
   */
  lock(): Promise<void>;

  /**
   * Reads the current lock state of the vault.
   *
   * @returns A promise resolving to the current lock state.
   *
   * @example
   * ```ts
   * const { isLocked } = await Fortress.isLocked();
   * ```
   *
   * @since 8.0.0
   */
  isLocked(): Promise<{ isLocked: boolean }>;

  /**
   * Returns the current session state including lock status and activity timestamp.
   *
   * @returns A promise resolving to the current session state.
   *
   * @example
   * ```ts
   * const session = await Fortress.getSession();
   * console.log('Locked:', session.isLocked);
   * console.log('Last active:', new Date(session.lastActiveAt));
   * ```
   *
   * @since 8.0.0
   */
  getSession(): Promise<FortressSession>;

  /**
   * Resets the session activity state.
   *
   * This clears the last active timestamp and locks the vault.
   *
   * @returns A promise that resolves when the session is reset.
   *
   * @example
   * ```ts
   * await Fortress.resetSession();
   * ```
   *
   * @since 8.0.0
   */
  resetSession(): Promise<void>;

  /**
   * Updates the session activity timestamp to prevent auto-lock.
   *
   * Use this method to keep the session alive during
   * active user interaction.
   *
   * @returns A promise that resolves when the timestamp is updated.
   *
   * @example
   * ```ts
   * document.addEventListener('click', () => {
   *   Fortress.touchSession();
   * });
   * ```
   *
   * @since 8.0.0
   */
  touchSession(): Promise<void>;

  /**
   * Checks whether a biometric key pair already exists.
   *
   * @param options - Optional key alias override.
   * @returns A promise resolving to registration state.
   *
   * @since 8.0.0
   */
  biometricKeysExist(options?: KeyAliasOptions): Promise<BiometricKeysExistResult>;

  /**
   * Creates (or replaces) a biometric key pair and returns the public key.
   *
   * @param options - Optional key alias override.
   * @returns A promise resolving to the generated public key.
   *
   * @since 8.0.0
   */
  createKeys(options?: KeyAliasOptions): Promise<CreateKeysResult>;

  /**
   * Deletes the biometric key pair if it exists.
   *
   * @param options - Optional key alias override.
   * @returns A promise that resolves when deletion completes.
   *
   * @since 8.0.0
   */
  deleteKeys(options?: KeyAliasOptions): Promise<void>;

  /**
   * Creates a biometric-protected cryptographic signature.
   *
   * The method requires the vault to be unlocked and an existing
   * biometric key pair in native secure hardware.
   *
   * Signature encoding note:
   * - iOS/Android return Base64 (standard).
   * - Web (WebAuthn) returns Base64URL (no padding, '-' and '_').
   * Backend verification must normalize Base64URL → Base64 when verifying WebAuthn assertions.
   *
   * @param options - Signature request payload.
   * @returns A promise resolving to signature metadata.
   *
   * @since 8.0.0
   */
  createSignature(options: CreateSignatureOptions): Promise<CreateSignatureResult>;

  /**
   * Creates or replaces keys and signs a backend challenge.
   *
   * @param options - Challenge and optional prompt/alias.
   * @returns A promise resolving to `{ publicKey, signature }`.
   *
   * @since 8.0.0
   */
  registerWithChallenge(options: ChallengeAuthOptions): Promise<RegisterWithChallengeResult>;

  /**
   * Signs a backend challenge with existing biometric keys.
   *
   * @param options - Challenge and optional prompt/alias.
   * @returns A promise resolving to `{ signature }`.
   *
   * @since 8.0.0
   */
  authenticateWithChallenge(options: ChallengeAuthOptions): Promise<AuthenticateWithChallengeResult>;

  /**
   * Generates a canonical payload for backend verification workflows.
   *
   * The payload includes nonce, timestamp, and a non-PII device identifier hash
   * to reduce replay attack risk and keep verification format deterministic.
   *
   * @param options - Payload generation options.
   * @returns A promise resolving to the generated payload string.
   *
   * @since 8.0.0
   */
  generateChallengePayload(options: GenerateChallengePayloadOptions): Promise<GenerateChallengePayloadResult>;

  /**
   * Stores a value in standard (insecure) storage.
   *
   * This uses SharedPreferences (Android), UserDefaults (iOS),
   * or localStorage (Web). Use for non-sensitive data only.
   *
   * @param value - The key-value pair to store.
   * @returns A promise that resolves when the value is stored.
   *
   * @example
   * ```ts
   * await Fortress.setInsecureValue({ key: 'theme', value: 'dark' });
   * ```
   *
   * @since 8.0.0
   */
  setInsecureValue(value: SecureValue): Promise<void>;

  /**
   * Reads a value from standard (insecure) storage.
   *
   * @param key - The key to retrieve.
   * @returns A promise resolving to the stored value or null if not found.
   *
   * @example
   * ```ts
   * const { value } = await Fortress.getInsecureValue({ key: 'theme' });
   * ```
   *
   * @since 8.0.0
   */
  getInsecureValue(key: { key: string }): Promise<ValueResult>;

  /**
   * Removes a value from standard (insecure) storage.
   *
   * @param key - The key to remove.
   * @returns A promise that resolves when the value is removed.
   *
   * @example
   * ```ts
   * await Fortress.removeInsecureValue({ key: 'theme' });
   * ```
   *
   * @since 8.0.0
   */
  removeInsecureValue(key: { key: string }): Promise<void>;

  /**
   * Returns the obfuscated key representation.
   *
   * Internal utility to mask keys in standard storage.
   * Useful for consistent key naming across storage tiers.
   *
   * @param key - The original key to obfuscate.
   * @returns A promise resolving to the obfuscated key.
   *
   * @example
   * ```ts
   * const { obfuscated } = await Fortress.getObfuscatedKey({ key: 'session_token' });
   * ```
   *
   * @since 8.0.0
   */
  getObfuscatedKey(key: { key: string }): Promise<ObfuscatedKeyResult>;

  /**
   * Enables or disables iCloud Keychain synchronization at runtime.
   *
   * iOS only; a no-op on Android and Web. This overrides the static
   * `enableICloudKeychainSync` value for the running session.
   *
   * @param options - Desired synchronization state.
   *
   * @since 8.0.0
   */
  setSynchronize(options: { synchronize: boolean }): Promise<void>;

  /**
   * Reports whether iCloud Keychain synchronization is active.
   *
   * iOS only; always `false` elsewhere.
   *
   * @since 8.0.0
   */
  getSynchronize(): Promise<{ synchronize: boolean }>;

  /**
   * Sets the default iOS Keychain accessibility for subsequently
   * stored secure items.
   *
   * iOS only; a no-op on Android and Web. Per-item `access` in
   * `setValue()` overrides this default. Session-scoped: resets on
   * restart unless also set via static configuration.
   *
   * @param options - Default accessibility level.
   *
   * @since 8.0.0
   */
  setDefaultKeychainAccess(options: { access: KeychainAccess }): Promise<void>;

  /**
   * Checks whether a key exists in secure or insecure storage.
   *
   * This is an optimized check that does not retrieve the value,
   * making it useful for checking session tokens without
   * triggering decryption.
   *
   * @param options - The options containing the key and storage type.
   * @returns A promise resolving to whether the key exists.
   *
   * @example
   * ```ts
   * const { exists } = await Fortress.hasKey({ key: 'auth_token', secure: true });
   * ```
   *
   * @since 8.0.0
   */
  hasKey(options: HasKeyOptions): Promise<HasKeyResult>;

  /**
   * Lists keys in secure or insecure storage.
   *
   * Returns original key names (insecure keys are de-obfuscated).
   * Secure enumeration requires an unlocked vault.
   *
   * @param options - Tier selection, defaulting to secure storage.
   * @returns A promise resolving to the key list.
   *
   * @example
   * ```ts
   * const { keys } = await Fortress.keys({ secure: true });
   * ```
   *
   * @since 8.0.0
   */
  keys(options?: KeysOptions): Promise<KeysResult>;

  /**
   * Reads several keys in one call.
   *
   * Missing keys map to `null` in the result record. Secure reads
   * require an unlocked vault and reject with `VAULT_LOCKED` otherwise.
   *
   * @param options - Keys plus tier selection.
   * @returns A promise resolving to the key/value record.
   *
   * @example
   * ```ts
   * const { values } = await Fortress.getMany({ keys: ['a', 'b'] });
   * ```
   *
   * @since 8.0.0
   */
  getMany(options: GetManyOptions): Promise<GetManyResult>;

  /**
   * Adds listeners for lock state change events.
   *
   * @param eventName - The event to listen for ('sessionLocked' or 'sessionUnlocked').
   * @param listenerFunc - The callback function to execute when the event fires.
   * @returns A promise resolving to a listener handle for cleanup.
   *
   * @example
   * ```ts
   * const handle = await Fortress.addListener('sessionLocked', () => {
   *   console.log('Vault has been locked');
   * });
   *
   * // To remove the listener:
   * await handle.remove();
   * ```
   *
   * @since 8.0.0
   */
  addListener(eventName: 'sessionLocked' | 'sessionUnlocked', listenerFunc: () => void): Promise<PluginListenerHandle>;

  /**
   * Adds a listener for security posture changes.
   *
   * @since 8.0.0
   */
  addListener(
    eventName: 'onSecurityStateChanged',
    listenerFunc: (status: DeviceSecurityStatus) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Adds a listener for lock-state changes with payload.
   *
   * @since 8.0.0
   */
  addListener(
    eventName: 'onLockStatusChanged',
    listenerFunc: (state: { isLocked: boolean }) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Adds a listener for vault invalidation events.
   *
   * @since 8.0.0
   */
  addListener(
    eventName: 'onVaultInvalidated',
    listenerFunc: (event: VaultInvalidatedEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Adds a listener for app resume events.
   *
   * This event is emitted when the app/tab becomes active in foreground.
   *
   * @since 8.0.0
   */
  addListener(eventName: 'onAppResume', listenerFunc: () => void): Promise<PluginListenerHandle>;

  /**
   * Adds a listener for screenshot events.
   *
   * The event fires after the screenshot has been taken and therefore
   * cannot prevent it. On Android it is only emitted on API 34+ while
   * privacy protection is active; on older versions the listener is
   * never called.
   *
   * @since 8.0.0
   */
  addListener(eventName: 'screenshotTaken', listenerFunc: () => void): Promise<PluginListenerHandle>;

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

  /**
   * Enables privacy-screen protection independently of the vault lock state.
   *
   * This is the manual runtime control from the official
   * `@capacitor/privacy-screen` API: protection applies immediately and
   * stays in effect regardless of later `lock()` / `unlock()` transitions.
   *
   * Relationship with the lock policy:
   * - While `enablePrivacyScreen` is set, lock/unlock transitions drive the
   *   overlay automatically (follow-lock policy, the historical behavior).
   * - Calling `enable()` / `disable()` detaches privacy from that policy:
   *   explicit manual control wins from that moment on.
   * - `configure({ enablePrivacyScreen })` or `resetRuntimeConfig()`
   *   re-attaches the policy and clears the manual override.
   *
   * @param config - Optional platform-specific display behavior.
   * @returns A promise resolving to `{ success: true }` when applied.
   *
   * @example
   * ```ts
   * await Fortress.enable({ ios: { blurEffect: 'dark' } });
   * ```
   *
   * @since 8.0.0
   */
  enable(config?: PrivacyScreenConfig): Promise<PrivacyScreenActionResult>;

  /**
   * Disables privacy-screen protection independently of the vault lock state.
   *
   * Use this only when the current screen must stay visible in system
   * previews (screenshots, screen recording, app switcher).
   *
   * @returns A promise resolving to `{ success: true }` when applied.
   *
   * @since 8.0.0
   */
  disable(): Promise<PrivacyScreenActionResult>;

  /**
   * Returns the current privacy-screen enabled state.
   *
   * This state is independent from `isLocked()`: the vault can be unlocked
   * while privacy protection stays on (e.g. hiding balances in the
   * app switcher).
   *
   * @returns A promise resolving to `{ enabled }`.
   *
   * @since 8.0.0
   */
  isEnabled(): Promise<PrivacyScreenStatus>;

  /**
   * Removes all listeners for this plugin.
   *
   * @since 8.0.0
   */
  removeAllListeners(): Promise<void>;
}
