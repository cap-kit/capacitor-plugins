/// <reference types="@capacitor/cli" />

/**
 * Static configuration options for the Biometrics plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the Biometrics plugin.
     */
    Biometrics?: {
      /**
       * Enables verbose native logging.
       *
       * @default false
       * @since 8.0.0
       */
      verboseLogging?: boolean;
    };
  }
}

/**
 * Biometry modality reported by the device.
 *
 * @since 8.0.0
 */
export type BiometryType = 'none' | 'touchId' | 'faceId' | 'fingerprint' | 'face' | 'iris';

/**
 * Strength class of the available biometry.
 *
 * - `strong`: Face ID / Touch ID / strong-class Android biometrics.
 * - `weak`: only weak modalities (e.g. some Android face unlocks).
 * - `none`: no usable biometry.
 *
 * @since 8.0.0
 */
export type BiometricStrength = 'strong' | 'weak' | 'none';

/**
 * Credential class that satisfied an authentication ceremony.
 *
 * `'unknown'` when nothing authenticated yet in this session or the
 * platform does not report it.
 *
 * @since 8.0.0
 */
export type AuthenticationType = 'biometric' | 'deviceCredential' | 'unknown';

/**
 * Prompt customization for `authenticate()`.
 *
 * Field names follow the Capawesome-style contract.
 *
 * @since 8.0.0
 */
export interface AuthenticateOptions {
  /**
   * Reason shown in the system dialog. Falls back to `description`.
   *
   * @since 8.0.0
   */
  reason?: string;
  /**
   * Dialog title (Android; iOS uses the system dialog).
   *
   * @since 8.0.0
   */
  title?: string;
  /**
   * Dialog subtitle (Android).
   *
   * @since 8.0.0
   */
  subtitle?: string;
  /**
   * Dialog description (Android and iOS localized reason).
   *
   * @since 8.0.0
   */
  description?: string;
  /**
   * Cancel button text.
   *
   * Android: negative button (not shown when device credential is in the
   * authenticator set). iOS: localized cancel title.
   *
   * @since 8.0.0
   */
  cancelButtonText?: string;
  /**
   * iOS only: fallback button title ("Use Passcode"). An empty string
   * hides the fallback button.
   *
   * @since 8.0.0
   */
  iosFallbackButtonText?: string;
  /**
   * Allows device credential (PIN/pattern/password/passcode) fallback.
   *
   * @default true
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
  biometryType: BiometryType;
}

/**
 * Result of `getBiometricTypes()`.
 *
 * @since 8.0.0
 */
export interface BiometricTypesResult {
  biometryTypes: BiometryType[];
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
 * Result of `isDeviceSecure()`.
 *
 * @since 8.0.0
 */
export interface IsDeviceSecureResult {
  /**
   * Whether the device is secured by any lock-screen credential:
   * PIN/pattern/password (Android `KeyguardManager.isDeviceSecure`) or
   * the passcode required by `.deviceOwnerAuthentication` (iOS).
   * Biometrics cannot be enrolled without one, so this doubles as the
   * broad "any secure lock" check.
   *
   * @since 8.0.0
   */
  isDeviceSecure: boolean;
}

/**
 * Result of `getBiometricStrengthLevel()`.
 *
 * @since 8.0.0
 */
export interface BiometricStrengthResult {
  strengthLevel: BiometricStrength;
}

/**
 * Result of `getAuthenticationType()`.
 *
 * @since 8.0.0
 */
export interface AuthenticationTypeResult {
  authenticationType: AuthenticationType;
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
 * Access control applied to a value stored with `setSecureData()`.
 *
 * The secure store is biometry-bound by design: there is no
 * unprotected mode. Every stored value requires biometric
 * authentication on read.
 *
 * - `biometryAny`: the value survives biometric enrollment changes
 *   (e.g. adding a new fingerprint). Recommended for most apps.
 * - `biometryCurrentSet`: the value is invalidated when the enrolled
 *   biometric set changes (more secure; data is lost on enrollment
 *   changes).
 *
 * @since 8.0.0
 */
export type AccessControl = 'biometryAny' | 'biometryCurrentSet';

/**
 * Options for `setSecureData()`.
 *
 * @since 8.0.0
 */
export interface SetSecureDataOptions {
  /**
   * Unique, stable, app-specific key for the stored value.
   *
   * @since 8.0.0
   */
  key: string;
  /**
   * String payload. Serialize objects with `JSON.stringify()` before
   * storing. Keep payloads small (a few KB).
   *
   * @since 8.0.0
   */
  value: string;
  /**
   * Biometric access control applied to the value. Required: the
   * store is biometry-bound by design.
   *
   * @since 8.0.0
   */
  accessControl: AccessControl;
  /**
   * Android only: title for the biometric prompt shown while
   * protecting the value at write time.
   *
   * @since 8.0.0
   */
  title?: string;
  /**
   * Android only: negative/cancel button text for the write-time
   * prompt.
   *
   * @since 8.0.0
   */
  negativeButtonText?: string;
  /**
   * Android only: maximum prompt attempts (device maximum is 5).
   *
   * @since 8.0.0
   */
  maxAttempts?: number;
}

/**
 * Options for `getSecureData()`.
 *
 * @since 8.0.0
 */
export interface GetSecureDataOptions {
  /**
   * Key the value was stored under.
   *
   * @since 8.0.0
   */
  key: string;
  /**
   * iOS only: reason shown as the prompt subtitle (Touch ID only;
   * Face ID does not display it).
   *
   * @since 8.0.0
   */
  reason?: string;
  /**
   * Android only: title for the biometric prompt.
   *
   * @since 8.0.0
   */
  title?: string;
  /**
   * Android only: subtitle for the biometric prompt.
   *
   * @since 8.0.0
   */
  subtitle?: string;
  /**
   * Android only: description for the biometric prompt.
   *
   * @since 8.0.0
   */
  description?: string;
  /**
   * Android only: negative/cancel button text for the prompt.
   *
   * @since 8.0.0
   */
  negativeButtonText?: string;
  /**
   * iOS only: fallback button title. An empty string hides the
   * fallback button.
   *
   * @since 8.0.0
   */
  fallbackTitle?: string;
  /**
   * Android only: maximum prompt attempts (device maximum is 5).
   *
   * @since 8.0.0
   */
  maxAttempts?: number;
}

/**
 * Result of `getSecureData()`.
 *
 * @since 8.0.0
 */
export interface SecureDataResult {
  /**
   * The decrypted stored value.
   *
   * @since 8.0.0
   */
  value: string;
}

/**
 * Options for `deleteSecureData()`.
 *
 * @since 8.0.0
 */
export interface DeleteSecureDataOptions {
  /**
   * Key of the value to delete.
   *
   * @since 8.0.0
   */
  key: string;
}

/**
 * Options for `isSecureDataSaved()`.
 *
 * @since 8.0.0
 */
export interface IsSecureDataSavedOptions {
  /**
   * Key to check.
   *
   * @since 8.0.0
   */
  key: string;
}

/**
 * Result of `isSecureDataSaved()`.
 *
 * @since 8.0.0
 */
export interface IsSecureDataSavedResult {
  /**
   * Whether a value exists for the key.
   *
   * @since 8.0.0
   */
  isSaved: boolean;
}

/**
 * Stable error codes the bridge delivers on the rejection object
 * (`error.code`). The codes are identical on Android and iOS; match
 * rejections against these constants instead of raw strings.
 *
 * @since 8.0.0
 */
export enum BiometricAuthError {
  CANCELLED = 'CANCELLED',
  UNAVAILABLE = 'UNAVAILABLE',
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  INIT_FAILED = 'INIT_FAILED',
  INVALID_INPUT = 'INVALID_INPUT',
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
  NOT_FOUND = 'NOT_FOUND',
  CONFLICT = 'CONFLICT',
  TIMEOUT = 'TIMEOUT',
}

/**
 * Public JavaScript API for the Biometrics Capacitor plugin.
 *
 * Standalone biometric authentication: Face ID, Touch ID, fingerprint,
 * face and iris recognition with device-credential fallback.
 */
export interface BiometricsPlugin {
  /**
   * Authenticates the user with biometrics or device credentials.
   *
   * Shows the system prompt. Resolves on success, rejects with
   * `CANCELLED` (user dismissed) or `UNAVAILABLE` (no usable
   * authenticator). Rejects if another authentication is in progress.
   *
   * Only available on Android and iOS.
   *
   * @param options - Prompt customization and credential fallback.
   *
   * @example
   * ```ts
   * await Biometrics.authenticate({
   *   title: 'Authentication Required',
   *   subtitle: 'Please authenticate to continue',
   *   cancelButtonText: 'Cancel',
   *   allowDeviceCredential: true,
   * });
   * ```
   *
   * @since 8.0.0
   */
  authenticate(options?: AuthenticateOptions): Promise<void>;

  /**
   * Cancels an ongoing authentication prompt, if any.
   *
   * Resolves immediately when idle. Only available on Android
   * (SDK 29+) and iOS.
   *
   * @since 8.0.0
   */
  cancelAuthentication(): Promise<void>;

  /**
   * Reports whether biometric authentication can be used
   * (hardware present and usable, regardless of enrollment).
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  isAvailable(): Promise<IsAvailableResult>;

  /**
   * Reports whether the user enrolled biometrics.
   *
   * Check both `isAvailable()` and `isEnrolled()` before
   * `authenticate()`. Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  isEnrolled(): Promise<IsEnrolledResult>;

  /**
   * Returns the primary biometry modality (display helper;
   * decide with `isAvailable()` instead).
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  getBiometricType(): Promise<BiometricTypeResult>;

  /**
   * Returns every biometry modality known to the hardware.
   * Only Android reports more than one entry.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  getBiometricTypes(): Promise<BiometricTypesResult>;

  /**
   * Reports whether a device credential (PIN/pattern/password)
   * is set and usable as fallback.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  hasDeviceCredential(): Promise<HasDeviceCredentialResult>;

  /**
   * Reports whether the device is secured by any lock-screen credential
   * (PIN/pattern/password/passcode; biometrics always require one).
   * Reports the same value as `hasDeviceCredential()`.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  isDeviceSecure(): Promise<IsDeviceSecureResult>;

  /**
   * Reports the strength class of the available biometry.
   * Android only reports beyond `none`; iOS is always `strong`
   * when available.
   *
   * @since 8.0.0
   */
  getBiometricStrengthLevel(): Promise<BiometricStrengthResult>;

  /**
   * Reports which credential satisfied the last successful ceremony
   * in this session.
   *
   * @since 8.0.0
   */
  getAuthenticationType(): Promise<AuthenticationTypeResult>;

  /**
   * Opens the system biometric enrollment screen.
   *
   * Android only (API 30+). iOS and Web reject with `UNAVAILABLE`.
   *
   * @since 8.0.0
   */
  enroll(): Promise<void>;

  /**
   * Stores a string value encrypted under a biometry-bound key.
   *
   * The store is biometry-bound by design: there is no unprotected
   * mode. Android shows a biometric prompt bound to the encryption
   * key at write time; iOS applies SecAccessControl to the Keychain
   * item (no prompt at write time). Requires enrolled biometrics.
   *
   * Only available on Android and iOS.
   *
   * @param options - Key, payload and biometric access control.
   *
   * @example
   * ```ts
   * await Biometrics.setSecureData({
   *   key: 'session.token',
   *   value: JSON.stringify(token),
   *   accessControl: 'biometryAny',
   * });
   * ```
   *
   * @since 8.0.0
   */
  setSecureData(options: SetSecureDataOptions): Promise<void>;

  /**
   * Reads a biometry-protected value, prompting for biometric
   * authentication (system prompt on both platforms).
   *
   * Rejects with `NOT_FOUND` when no value exists for the key — never
   * stored, deleted, or invalidated by biometric enrollment changes
   * (with `biometryCurrentSet`). This is stable, expected behavior:
   * handle it in your catch block or check `isSecureDataSaved()`
   * first.
   *
   * Only available on Android and iOS.
   *
   * @param options - Key and prompt customization.
   *
   * @example
   * ```ts
   * const { value } = await Biometrics.getSecureData({
   *   key: 'session.token',
   *   reason: 'Unlock your session',
   * });
   * ```
   *
   * @since 8.0.0
   */
  getSecureData(options: GetSecureDataOptions): Promise<SecureDataResult>;

  /**
   * Deletes the value stored for the key.
   *
   * Only available on Android and iOS.
   *
   * @param options - Key of the value to delete.
   *
   * @since 8.0.0
   */
  deleteSecureData(options: DeleteSecureDataOptions): Promise<void>;

  /**
   * Reports whether a value exists for the key, without prompting
   * for authentication.
   *
   * Only available on Android and iOS.
   *
   * @param options - Key to check.
   *
   * @since 8.0.0
   */
  isSecureDataSaved(options: IsSecureDataSavedOptions): Promise<IsSecureDataSavedResult>;

  /**
   * Returns the native plugin version.
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
