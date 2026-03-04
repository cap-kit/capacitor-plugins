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
   * @default true
   * @since 8.0.0
   */
  enablePrivacyScreen?: boolean;

  /**
   * Prefix used by key obfuscation utilities.
   *
   * @default 'ftrss_'
   * @since 8.0.0
   */
  obfuscationPrefix?: string;

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
}

/**
 * Prompt customization options for interactive authentication.
 *
 * Platform note:
 * - Android uses title/subtitle/description/negativeButtonText directly.
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
}

/**
 * Generic key/value payload for storage methods.
 */
export interface SecureValue {
  key: string;
  value: string;
  secure?: boolean;
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
