import { WebPlugin } from '@capacitor/core';

import {
  AuthenticateWithChallengeResult,
  BiometricKeysExistResult,
  ChallengeAuthOptions,
  CreateKeysResult,
  CreateSignatureOptions,
  CreateSignatureResult,
  UnlockOptions,
  FortressErrorCode,
  FortressConfig,
  FortressPlugin,
  FortressRuntimeConfig,
  FortressSession,
  GenerateChallengePayloadOptions,
  GenerateChallengePayloadResult,
  HasKeyOptions,
  HasKeyResult,
  KeyAliasOptions,
  ObfuscatedKeyResult,
  PluginVersionResult,
  RegisterWithChallengeResult,
  SecureValue,
  SetBiometryIsEnrolledOptions,
  SetBiometryTypeOptions,
  SetDeviceIsSecureOptions,
  ValueResult,
  DeviceSecurityStatus,
} from './definitions';
import { PLUGIN_VERSION } from './version';

type VaultState = 'LOCKED' | 'UNLOCKING' | 'UNLOCKED' | 'EXPIRED';

/**
 * Web implementation of the Fortress plugin.
 *
 * This implementation provides a best-effort secure storage on the web
 * using localStorage with Web Crypto encryption. Note that web storage
 * is NOT hardware-backed and should not be used for highly sensitive data.
 *
 * Security limitations on Web:
 * - No hardware-backed encryption (Secure Enclave/Keystore)
 * - Encryption key is software-derived and origin-bound (not hardware-protected)
 * - No biometric authentication
 * - Session state is in-memory only (resets on page reload)
 */
export class FortressWeb extends WebPlugin implements FortressPlugin {
  // -----------------------------------------------------------------------------
  // Constants
  // -----------------------------------------------------------------------------

  private static readonly SECURE_STORAGE_PREFIX = 'fortress_secure_';
  private static readonly SESSION_KEY = 'fortress_session';
  private static readonly WEBAUTHN_STATE_KEY = 'fortress_webauthn_state';
  private static readonly WEBAUTHN_RP_NAME = 'Fortress';
  private static readonly WEBAUTHN_USER_NAME = 'fortress-user';
  private static readonly WEBAUTHN_USER_DISPLAY_NAME = 'Fortress User';
  private static readonly WEBAUTHN_TIMEOUT_MS = 60_000;
  private static readonly WEB_CRYPTO_KEY_CACHE_TTL_MS = 5 * 60_000;
  private static readonly WEB_CRYPTO_SCHEMA_VERSION = 1;
  private static readonly VAULT_INVALIDATION_REASON_SECURITY_STATE_CHANGED = 'security_state_changed';
  private static readonly VAULT_INVALIDATION_REASON_KEYPAIR_INVALIDATED = 'keypair_invalidated';
  private static readonly VAULT_INVALIDATION_REASON_KEYS_DELETED = 'keys_deleted';

  private static readonly ERROR_MESSAGES: Record<FortressErrorCode, string> = {
    [FortressErrorCode.UNAVAILABLE]: 'Feature is unavailable on this device or configuration.',
    [FortressErrorCode.CANCELLED]: 'Operation was cancelled by the user.',
    [FortressErrorCode.PERMISSION_DENIED]: 'Required permission is denied.',
    [FortressErrorCode.INIT_FAILED]: 'Native initialization failed.',
    [FortressErrorCode.INVALID_INPUT]: 'Invalid input provided.',
    [FortressErrorCode.NOT_FOUND]: 'Requested resource not found.',
    [FortressErrorCode.CONFLICT]: 'Operation conflicts with current vault state.',
    [FortressErrorCode.TIMEOUT]: 'Operation timed out.',
    [FortressErrorCode.SECURITY_VIOLATION]: 'Security validation failed.',
    [FortressErrorCode.VAULT_LOCKED]: 'Vault is locked.',
  };

  private static readonly LOG_LEVEL_WEIGHT: Record<'error' | 'warn' | 'info' | 'debug' | 'verbose', number> = {
    error: 0,
    warn: 1,
    info: 2,
    debug: 3,
    verbose: 4,
  };

  // -----------------------------------------------------------------------------
  // State
  // -----------------------------------------------------------------------------

  private config: FortressConfig = {};
  private session: FortressSession = {
    isLocked: false,
    lastActiveAt: Date.now(),
  };
  private lastTouchAt = 0;
  private webCryptoKeyCache: {
    key: CryptoKey;
    expiresAt: number;
  } | null = null;
  private lastKnownSecurityStatus: DeviceSecurityStatus | null = null;
  private lastSuccessfulAuthAt = 0;
  private failedBiometricAttempts = 0;
  private lockoutUntilMs = 0;
  private securityOverrides: Partial<DeviceSecurityStatus> = {};
  private currentLogLevel: 'error' | 'warn' | 'info' | 'debug' | 'verbose' = 'info';
  private vaultState: VaultState = 'LOCKED';
  private readonly visibilityChangeHandler = (): void => {
    void this.handleVisibilityChange();
  };

  // -----------------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------------

  constructor() {
    super();
    this.loadSession();

    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', this.visibilityChangeHandler);
    }

    void this.refreshSecuritySignals(false);
  }

  // -----------------------------------------------------------------------------
  // Configuration
  // -----------------------------------------------------------------------------

  async getRuntimeConfig(): Promise<FortressRuntimeConfig> {
    return {
      verboseLogging: this.config.verboseLogging ?? false,
      logLevel: this.resolveLogLevel(this.config.logLevel, this.config.verboseLogging),
      lockAfterMs: this.config.lockAfterMs ?? 60000,
      enablePrivacyScreen: this.config.enablePrivacyScreen ?? true,
      privacyOverlayText: this.config.privacyOverlayText ?? '',
      privacyOverlayImageName: this.config.privacyOverlayImageName ?? '',
      privacyOverlayShowText: this.config.privacyOverlayShowText ?? true,
      privacyOverlayShowImage: this.config.privacyOverlayShowImage ?? true,
      privacyOverlayTextColor: this.config.privacyOverlayTextColor ?? '',
      privacyOverlayBackgroundOpacity: this.config.privacyOverlayBackgroundOpacity ?? -1,
      privacyOverlayTheme: this.config.privacyOverlayTheme ?? 'system',
      fallbackStrategy: this.config.fallbackStrategy ?? 'systemDefault',
      allowCachedAuthentication: this.config.allowCachedAuthentication ?? false,
      cachedAuthenticationTimeoutMs: this.config.cachedAuthenticationTimeoutMs ?? 30000,
      maxBiometricAttempts: this.config.maxBiometricAttempts ?? 5,
      lockoutDurationMs: this.config.lockoutDurationMs ?? 30000,
      requireFreshAuthenticationMs: this.config.requireFreshAuthenticationMs ?? 0,
      encryptionAlgorithm: this.config.encryptionAlgorithm ?? 'AES-GCM',
      persistSessionState: this.config.persistSessionState ?? false,
    };
  }

  async configure(config: FortressConfig): Promise<void> {
    const wasPersisting = this.config.persistSessionState === true;
    this.config = config;
    this.currentLogLevel = this.resolveLogLevel(config.logLevel, config.verboseLogging);
    this.logDebug('Configuration applied', `logLevel=${this.currentLogLevel}`);

    const isPersisting = this.config.persistSessionState === true;
    if (isPersisting && !wasPersisting) {
      this.restorePersistedSession();
    } else if (!isPersisting && wasPersisting) {
      localStorage.removeItem(FortressWeb.SESSION_KEY);
    }

    if (config.lockAfterMs !== undefined && config.lockAfterMs > 0) {
      this.startAutoLockTimer(config.lockAfterMs);
    }
  }

  // -----------------------------------------------------------------------------
  // Secure Storage (Encrypted localStorage)
  // -----------------------------------------------------------------------------

  async setValue(value: SecureValue): Promise<void> {
    await this.assertSecureVaultAccess();
    const encodedKey = this.encodeKey(value.key);
    const encryptedPayload = await this.encryptValue(value.value);
    localStorage.setItem(encodedKey, encryptedPayload);
    await this.touchSession();
  }

  async getValue(key: { key: string }): Promise<ValueResult> {
    await this.assertSecureVaultAccess();
    const encodedKey = this.encodeKey(key.key);
    const stored = localStorage.getItem(encodedKey);

    if (stored === null) {
      return { value: null };
    }

    try {
      const decoded = await this.decryptValue(stored);
      await this.touchSession();
      return { value: decoded };
    } catch {
      this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
    }
  }

  async setMany(options: { values: SecureValue[] }): Promise<void> {
    const operations = options.values.map((item) => ({
      key: item.key,
      value: item.value,
      secure: item.secure !== false,
    }));

    if (operations.some((operation) => operation.secure)) {
      await this.assertSecureVaultAccess();
    }

    const snapshot = new Map<string, string | null>();
    for (const operation of operations) {
      const storageKey = operation.secure ? this.encodeKey(operation.key) : this.obfuscateKey(operation.key);
      if (!snapshot.has(storageKey)) {
        snapshot.set(storageKey, localStorage.getItem(storageKey));
      }
    }

    try {
      for (const operation of operations) {
        if (operation.secure) {
          const storageKey = this.encodeKey(operation.key);
          const encryptedPayload = await this.encryptValue(operation.value);
          localStorage.setItem(storageKey, encryptedPayload);
        } else {
          localStorage.setItem(this.obfuscateKey(operation.key), operation.value);
        }
      }
      await this.touchSession();
    } catch (error) {
      for (const [storageKey, previousValue] of snapshot) {
        if (previousValue === null) {
          localStorage.removeItem(storageKey);
        } else {
          localStorage.setItem(storageKey, previousValue);
        }
      }
      throw error;
    }
  }

  async removeValue(key: { key: string }): Promise<void> {
    const encodedKey = this.encodeKey(key.key);
    localStorage.removeItem(encodedKey);
    this.touchSession();
  }

  async clearAll(): Promise<void> {
    const keysToRemove: string[] = [];

    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key?.startsWith(FortressWeb.SECURE_STORAGE_PREFIX)) {
        keysToRemove.push(key);
      }
    }

    keysToRemove.forEach((key) => localStorage.removeItem(key));
    // Logic: Also clear WebAuthn enrollment state during a full wipe
    localStorage.removeItem(FortressWeb.WEBAUTHN_STATE_KEY);
    this.touchSession();
  }

  // -----------------------------------------------------------------------------
  // Insecure Storage (Plain localStorage)
  // -----------------------------------------------------------------------------

  async setInsecureValue(value: SecureValue): Promise<void> {
    const obfuscatedKey = this.obfuscateKey(value.key);
    localStorage.setItem(obfuscatedKey, value.value);
    this.touchSession();
  }

  async getInsecureValue(key: { key: string }): Promise<ValueResult> {
    const obfuscatedKey = this.obfuscateKey(key.key);
    const value = localStorage.getItem(obfuscatedKey);

    this.touchSession();
    return { value };
  }

  async removeInsecureValue(key: { key: string }): Promise<void> {
    const obfuscatedKey = this.obfuscateKey(key.key);
    localStorage.removeItem(obfuscatedKey);
    this.touchSession();
  }

  // -----------------------------------------------------------------------------
  // Key Utilities
  // -----------------------------------------------------------------------------

  async getObfuscatedKey(key: { key: string }): Promise<ObfuscatedKeyResult> {
    const obfuscated = this.obfuscateKey(key.key);
    return { obfuscated };
  }

  async hasKey(options: HasKeyOptions): Promise<HasKeyResult> {
    const storageKey = options.secure ? this.encodeKey(options.key) : this.obfuscateKey(options.key);

    const exists = localStorage.getItem(storageKey) !== null;
    return { exists };
  }

  // -----------------------------------------------------------------------------
  // Session Management (In-memory only - resets on reload)
  // -----------------------------------------------------------------------------

  async unlock(options?: UnlockOptions): Promise<void> {
    void options;
    this.assertNotBiometricLockedOut();

    if (this.shouldUseCachedAuthentication()) {
      const wasLocked = this.session.isLocked;
      this.transitionToVaultState('UNLOCKING');
      this.transitionToVaultState('UNLOCKED');
      this.session.lastActiveAt = Date.now();
      this.lastTouchAt = this.session.lastActiveAt;
      this.saveSession();

      if (wasLocked) {
        this.notifyListeners('sessionUnlocked', {});
        this.notifyListeners('onLockStatusChanged', { isLocked: false });
      }

      return;
    }

    this.assertWebAuthnAvailable();

    try {
      const state = await this.ensureWebAuthnCredential();
      const allowCredentials = state.credentialIds.map((credentialId) => this.toAllowCredential(credentialId));
      const challenge = this.isServerWebAuthnMode()
        ? await this.getServerAuthenticationChallenge(state)
        : this.createRandomChallenge();

      const credential = await navigator.credentials.get({
        publicKey: {
          challenge,
          timeout: FortressWeb.WEBAUTHN_TIMEOUT_MS,
          userVerification: 'preferred',
          ...(allowCredentials.length > 0 ? { allowCredentials } : {}),
        },
      });

      if (credential === null) {
        this.throwWebError(FortressErrorCode.CANCELLED);
      }

      if (!(credential instanceof PublicKeyCredential)) {
        this.throwWebError(FortressErrorCode.INIT_FAILED);
      }

      if (this.isServerWebAuthnMode()) {
        await this.verifyServerAuthentication(credential, state);
      }

      const wasLocked = this.session.isLocked;
      this.transitionToVaultState('UNLOCKING');
      this.transitionToVaultState('UNLOCKED');
      this.session.lastActiveAt = Date.now();
      this.lastTouchAt = this.session.lastActiveAt;
      this.lastSuccessfulAuthAt = this.session.lastActiveAt;
      this.clearBiometricFailureState();
      this.saveSession();
      if (wasLocked) {
        this.notifyListeners('sessionUnlocked', {});
        this.notifyListeners('onLockStatusChanged', { isLocked: false });
      }
    } catch (error) {
      this.recordBiometricFailure(error);
      this.logWarn('Unlock failed', error);

      if (error instanceof FortressWebError) {
        throw error;
      }

      if (error instanceof DOMException) {
        throw this.mapWebAuthnError(error);
      }

      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }
  }

  async lock(): Promise<void> {
    const wasUnlocked = !this.session.isLocked;
    this.transitionToVaultState('LOCKED');
    this.session.lastActiveAt = 0;
    this.lastTouchAt = 0;
    this.lastSuccessfulAuthAt = 0;
    this.saveSession();

    if (wasUnlocked) {
      this.notifyListeners('sessionLocked', {});
      this.notifyListeners('onLockStatusChanged', { isLocked: true });
    }
  }

  async isLocked(): Promise<{ isLocked: boolean }> {
    return { isLocked: this.session.isLocked };
  }

  async getSession(): Promise<FortressSession> {
    return { ...this.session };
  }

  async resetSession(): Promise<void> {
    const wasUnlocked = !this.session.isLocked;
    this.transitionToVaultState('LOCKED');
    this.session.lastActiveAt = 0;
    this.lastTouchAt = 0;
    this.lastSuccessfulAuthAt = 0;
    this.saveSession();

    if (wasUnlocked) {
      this.notifyListeners('sessionLocked', {});
      this.notifyListeners('onLockStatusChanged', { isLocked: true });
    }
  }

  async touchSession(): Promise<void> {
    if (!this.session.isLocked) {
      const now = Date.now();
      if (now - this.lastTouchAt < 1000) {
        return;
      }

      this.lastTouchAt = now;
      this.session.lastActiveAt = now;
      this.saveSession();

      // Debounce implementation: reset the auto-lock timer on each activity
      if (this.config.lockAfterMs && this.config.lockAfterMs > 0) {
        this.startAutoLockTimer(this.config.lockAfterMs);
      }
    }
  }

  async biometricKeysExist(options?: KeyAliasOptions): Promise<BiometricKeysExistResult> {
    void options;
    const state = this.readWebAuthnState();
    return { keysExist: state.credentialIds.length > 0 };
  }

  async createKeys(options?: KeyAliasOptions): Promise<CreateKeysResult> {
    void options;
    this.assertWebAuthnAvailable();

    const state = await this.ensureWebAuthnCredential();
    const publicKey = state.credentialIds[0];

    if (!publicKey) {
      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }

    return { publicKey };
  }

  async deleteKeys(options?: KeyAliasOptions): Promise<void> {
    void options;
    const hadKeys = this.readWebAuthnState().credentialIds.length > 0;
    localStorage.removeItem(FortressWeb.WEBAUTHN_STATE_KEY);

    if (hadKeys) {
      this.notifyListeners('onVaultInvalidated', {
        reason: FortressWeb.VAULT_INVALIDATION_REASON_KEYS_DELETED,
      });
      await this.refreshSecuritySignals(true);
    }
  }

  async createSignature(options: CreateSignatureOptions): Promise<CreateSignatureResult> {
    if (options.payload.trim().length === 0) {
      this.throwWebError(FortressErrorCode.INVALID_INPUT);
    }

    const { isLocked } = await this.isLocked();
    if (isLocked) {
      this.throwWebError(FortressErrorCode.VAULT_LOCKED);
    }

    this.assertWebAuthnAvailable();
    this.assertNotBiometricLockedOut();

    try {
      const state = await this.ensureWebAuthnCredential();
      const allowCredentials = state.credentialIds.map((credentialId) => this.toAllowCredential(credentialId));

      const credential = await navigator.credentials.get({
        publicKey: {
          challenge: this.utf8ToArrayBuffer(options.payload),
          timeout: FortressWeb.WEBAUTHN_TIMEOUT_MS,
          userVerification: 'preferred',
          ...(allowCredentials.length > 0 ? { allowCredentials } : {}),
        },
      });

      if (credential === null) {
        this.throwWebError(FortressErrorCode.CANCELLED);
      }

      if (!(credential instanceof PublicKeyCredential)) {
        this.throwWebError(FortressErrorCode.INIT_FAILED);
      }

      const assertionResponse = credential.response;
      if (!(assertionResponse instanceof AuthenticatorAssertionResponse)) {
        this.throwWebError(FortressErrorCode.INIT_FAILED);
      }

      this.lastSuccessfulAuthAt = Date.now();
      this.clearBiometricFailureState();

      return {
        success: true,
        signature: this.arrayBufferToBase64Url(assertionResponse.signature),
      };
    } catch (error) {
      this.recordBiometricFailure(error);
      this.logWarn('Create signature failed', error);

      if (error instanceof FortressWebError) {
        throw error;
      }

      if (error instanceof DOMException) {
        throw this.mapWebAuthnError(error);
      }

      this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
    }
  }

  async registerWithChallenge(options: ChallengeAuthOptions): Promise<RegisterWithChallengeResult> {
    if (options.challenge.trim().length === 0) {
      this.throwWebError(FortressErrorCode.INVALID_INPUT);
    }

    const { publicKey } = await this.createKeys({ keyAlias: options.keyAlias });
    const state = this.readWebAuthnState();
    const signature = await this.signChallengeWithWebAuthn(options.challenge, state);

    return {
      publicKey,
      signature,
    };
  }

  async authenticateWithChallenge(options: ChallengeAuthOptions): Promise<AuthenticateWithChallengeResult> {
    if (options.challenge.trim().length === 0) {
      this.throwWebError(FortressErrorCode.INVALID_INPUT);
    }

    const state = this.readWebAuthnState();
    if (state.credentialIds.length === 0) {
      this.notifyListeners('onVaultInvalidated', {
        reason: FortressWeb.VAULT_INVALIDATION_REASON_KEYPAIR_INVALIDATED,
      });
      this.throwWebError(FortressErrorCode.NOT_FOUND);
    }

    const signature = await this.signChallengeWithWebAuthn(options.challenge, state);
    return { signature };
  }

  async generateChallengePayload(options: GenerateChallengePayloadOptions): Promise<GenerateChallengePayloadResult> {
    if (options.nonce.trim().length === 0) {
      this.throwWebError(FortressErrorCode.INVALID_INPUT);
    }

    // Manual string building to guarantee key order and avoid JSON.stringify variations
    const timestamp = Date.now();
    const deviceHash = await this.getWebDeviceIdentifierHash();
    const controlChars =
      String.fromCharCode(0) +
      '-' +
      String.fromCharCode(31) +
      String.fromCharCode(127) +
      '-' +
      String.fromCharCode(159);
    const sanitizeRegex = new RegExp('[' + controlChars + ']', 'g');
    const sanitize = (str: string) => str.replace(sanitizeRegex, '');

    const payload =
      `{` +
      `"deviceIdentifierHash":"${sanitize(deviceHash)}",` +
      `"nonce":"${sanitize(options.nonce)}",` +
      `"timestamp":${timestamp}` +
      `}`;

    return {
      payload,
    };
  }

  async checkStatus(): Promise<DeviceSecurityStatus> {
    const hasWebAuthnApi =
      typeof window.PublicKeyCredential !== 'undefined' &&
      typeof window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable === 'function';

    let isBiometricsAvailable = false;
    if (hasWebAuthnApi) {
      isBiometricsAvailable = await window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
    }

    const state = this.readWebAuthnState();
    const isBiometricsEnabled = isBiometricsAvailable && state.credentialIds.length > 0;

    const isDeviceSecure = globalThis.isSecureContext && isBiometricsAvailable;

    const status: DeviceSecurityStatus = {
      isBiometricsAvailable,
      isBiometricsEnabled,
      isDeviceSecure,
      biometryType: isBiometricsAvailable ? 'fingerprint' : 'none',
    };

    return this.applySecurityOverrides(status);
  }

  /**
   * Overrides the detected biometry type for development/testing flows.
   */
  async setBiometryType(options: SetBiometryTypeOptions): Promise<void> {
    this.securityOverrides.biometryType = options.biometryType;

    if (options.biometryType === 'none') {
      this.securityOverrides.isBiometricsAvailable = false;
      this.securityOverrides.isBiometricsEnabled = false;
    } else {
      this.securityOverrides.isBiometricsAvailable = true;
    }

    await this.refreshSecuritySignals(true);
  }

  /**
   * Overrides biometric enrollment state for development/testing flows.
   */
  async setBiometryIsEnrolled(options: SetBiometryIsEnrolledOptions): Promise<void> {
    this.securityOverrides.isBiometricsEnabled = options.isBiometricsEnabled;

    if (options.isBiometricsEnabled) {
      this.securityOverrides.isBiometricsAvailable = true;
      if (this.securityOverrides.biometryType === 'none') {
        this.securityOverrides.biometryType = 'fingerprint';
      }
    }

    await this.refreshSecuritySignals(true);
  }

  /**
   * Overrides device secure-state for development/testing flows.
   */
  async setDeviceIsSecure(options: SetDeviceIsSecureOptions): Promise<void> {
    this.securityOverrides.isDeviceSecure = options.isDeviceSecure;
    await this.refreshSecuritySignals(true);
  }

  private async handleVisibilityChange(): Promise<void> {
    if (document.visibilityState !== 'visible') {
      return;
    }

    this.notifyListeners('onAppResume', {});
    await this.refreshSecuritySignals(true);
  }

  private shouldUseCachedAuthentication(): boolean {
    if (this.config.allowCachedAuthentication !== true) {
      return false;
    }

    const timeout = this.config.cachedAuthenticationTimeoutMs ?? 30_000;
    if (timeout <= 0 || this.lastSuccessfulAuthAt <= 0) {
      return false;
    }

    if (typeof this.config.requireFreshAuthenticationMs === 'number' && this.config.requireFreshAuthenticationMs > 0) {
      if (Date.now() - this.lastSuccessfulAuthAt > this.config.requireFreshAuthenticationMs) {
        return false;
      }
    }

    return Date.now() - this.lastSuccessfulAuthAt <= timeout;
  }

  private async refreshSecuritySignals(emitEvents: boolean): Promise<void> {
    const lockAfterMs = this.config.lockAfterMs;
    if (typeof lockAfterMs === 'number' && lockAfterMs > 0) {
      const idleTimeMs = Date.now() - this.session.lastActiveAt;
      if (!this.session.isLocked && idleTimeMs >= lockAfterMs) {
        await this.lock();
      }
    }

    const currentStatus = await this.checkStatus();
    const previousStatus = this.lastKnownSecurityStatus;
    const changed =
      previousStatus?.isBiometricsAvailable !== currentStatus.isBiometricsAvailable ||
      previousStatus.isBiometricsEnabled !== currentStatus.isBiometricsEnabled ||
      previousStatus.isDeviceSecure !== currentStatus.isDeviceSecure ||
      previousStatus.biometryType !== currentStatus.biometryType;

    if (emitEvents && changed) {
      this.notifyListeners('onSecurityStateChanged', currentStatus);
      if (
        previousStatus !== null &&
        (previousStatus.isDeviceSecure !== currentStatus.isDeviceSecure ||
          previousStatus.isBiometricsEnabled !== currentStatus.isBiometricsEnabled)
      ) {
        this.notifyListeners('onVaultInvalidated', {
          reason: FortressWeb.VAULT_INVALIDATION_REASON_SECURITY_STATE_CHANGED,
        });
      }
    }

    this.lastKnownSecurityStatus = currentStatus;
  }

  private applySecurityOverrides(status: DeviceSecurityStatus): DeviceSecurityStatus {
    return {
      isBiometricsAvailable: this.securityOverrides.isBiometricsAvailable ?? status.isBiometricsAvailable,
      isBiometricsEnabled: this.securityOverrides.isBiometricsEnabled ?? status.isBiometricsEnabled,
      isDeviceSecure: this.securityOverrides.isDeviceSecure ?? status.isDeviceSecure,
      biometryType: this.securityOverrides.biometryType ?? status.biometryType,
    };
  }

  // -----------------------------------------------------------------------------
  // Private Helpers - Encoding/Decoding
  // -----------------------------------------------------------------------------

  /**
   * Encodes a key for secure storage.
   * Uses base64 encoding with prefix.
   */
  private encodeKey(key: string): string {
    return `${FortressWeb.SECURE_STORAGE_PREFIX}${btoa(key)}`;
  }

  private async encryptValue(value: string): Promise<string> {
    this.assertWebCryptoAvailable();

    const algorithm = this.getEncryptionAlgorithm();
    const key = await this.getOrCreateWebCryptoKey();
    const iv = this.createRandomIv(algorithm);
    const plaintext = new TextEncoder().encode(value);
    const ciphertext = await crypto.subtle.encrypt(
      {
        name: algorithm,
        iv: iv as BufferSource,
      },
      key,
      plaintext,
    );

    const payload: EncryptedWebPayload = {
      v: FortressWeb.WEB_CRYPTO_SCHEMA_VERSION,
      alg: algorithm,
      iv: this.arrayBufferToBase64(iv.buffer),
      cipher: this.arrayBufferToBase64(ciphertext),
    };

    return JSON.stringify(payload);
  }

  private async decryptValue(payloadJson: string): Promise<string> {
    this.assertWebCryptoAvailable();

    const payload = this.parseEncryptedPayload(payloadJson);
    const algorithm = payload.alg ?? 'AES-GCM';
    const key = await this.getOrCreateWebCryptoKey();
    const iv = new Uint8Array(this.base64ToArrayBuffer(payload.iv));
    const ciphertext = this.base64ToArrayBuffer(payload.cipher);

    const plaintext = await crypto.subtle.decrypt(
      {
        name: algorithm,
        iv: iv as BufferSource,
      },
      key,
      ciphertext,
    );

    return new TextDecoder().decode(plaintext);
  }

  /**
   * Obfuscates a key for insecure storage.
   * Simple XOR-like transformation with prefix.
   */
  private obfuscateKey(key: string): string {
    // Force a fallback prefix if the one in config is empty or invalid
    const prefix =
      this.config.obfuscationPrefix && this.config.obfuscationPrefix.trim().length > 0
        ? this.config.obfuscationPrefix
        : 'ftrss_';
    return `${prefix}${btoa(key)}`;
  }

  private parseEncryptedPayload(payloadJson: string): EncryptedWebPayload {
    const parsed: unknown = JSON.parse(payloadJson);
    if (
      typeof parsed !== 'object' ||
      parsed === null ||
      !('v' in parsed) ||
      !('iv' in parsed) ||
      !('cipher' in parsed)
    ) {
      this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
    }

    const payload = parsed as Partial<EncryptedWebPayload>;
    if (
      payload.v !== FortressWeb.WEB_CRYPTO_SCHEMA_VERSION ||
      (payload.alg !== undefined && payload.alg !== 'AES-GCM' && payload.alg !== 'AES-CBC') ||
      typeof payload.iv !== 'string' ||
      payload.iv.length === 0 ||
      typeof payload.cipher !== 'string' ||
      payload.cipher.length === 0
    ) {
      this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
    }

    return payload as EncryptedWebPayload;
  }

  private async getOrCreateWebCryptoKey(): Promise<CryptoKey> {
    const now = Date.now();
    if (this.webCryptoKeyCache !== null && this.webCryptoKeyCache.expiresAt > now) {
      return this.webCryptoKeyCache.key;
    }

    const encryptionAlgorithm = this.getEncryptionAlgorithm();
    const passphrase = `${globalThis.location.origin}|${this.config.obfuscationPrefix ?? ''}|fortress-web-key`;
    const saltSource = `${globalThis.location.hostname}|fortress-salt-v1`;
    const baseKey = await crypto.subtle.importKey('raw', new TextEncoder().encode(passphrase), 'PBKDF2', false, [
      'deriveKey',
    ]);

    const key = await crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt: new TextEncoder().encode(saltSource),
        iterations: 150_000,
        hash: 'SHA-256',
      },
      baseKey,
      {
        name: encryptionAlgorithm,
        length: 256,
      },
      false,
      ['encrypt', 'decrypt'],
    );

    this.webCryptoKeyCache = {
      key,
      expiresAt: now + FortressWeb.WEB_CRYPTO_KEY_CACHE_TTL_MS,
    };

    return key;
  }

  private createRandomIv(algorithm: 'AES-GCM' | 'AES-CBC'): Uint8Array {
    const iv = new Uint8Array(algorithm === 'AES-CBC' ? 16 : 12);
    crypto.getRandomValues(iv);
    return iv;
  }

  private getEncryptionAlgorithm(): 'AES-GCM' | 'AES-CBC' {
    return this.config.encryptionAlgorithm === 'AES-CBC' ? 'AES-CBC' : 'AES-GCM';
  }

  private getCryptoStrategy(): 'auto' | 'ecc' | 'rsa' {
    return this.config.cryptoStrategy ?? 'auto';
  }

  private getPubKeyCredParams(): { type: 'public-key'; alg: -7 | -257 }[] {
    const strategy = this.getCryptoStrategy();

    if (strategy === 'ecc') {
      return [{ type: 'public-key', alg: -7 }];
    }

    if (strategy === 'rsa') {
      return [{ type: 'public-key', alg: -257 }];
    }

    return [
      { type: 'public-key', alg: -7 },
      { type: 'public-key', alg: -257 },
    ];
  }

  private assertNotBiometricLockedOut(): void {
    if (Date.now() < this.lockoutUntilMs) {
      this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
    }
  }

  private recordBiometricFailure(error: unknown): void {
    const maxAttempts = this.config.maxBiometricAttempts ?? 5;
    const lockoutDurationMs = this.config.lockoutDurationMs ?? 30_000;

    if (maxAttempts <= 0 || lockoutDurationMs <= 0) {
      return;
    }

    const fortressError = error instanceof FortressWebError ? error : null;
    if (fortressError?.code === FortressErrorCode.CANCELLED) {
      return;
    }

    this.failedBiometricAttempts += 1;
    if (this.failedBiometricAttempts >= maxAttempts) {
      this.lockoutUntilMs = Date.now() + lockoutDurationMs;
      this.failedBiometricAttempts = 0;
    }
  }

  private clearBiometricFailureState(): void {
    this.failedBiometricAttempts = 0;
    this.lockoutUntilMs = 0;
  }

  private isLockedState(state: VaultState): boolean {
    return state !== 'UNLOCKED';
  }

  private transitionToVaultState(nextState: VaultState): void {
    this.vaultState = nextState;
    this.session.isLocked = this.isLockedState(nextState);
  }

  private resolveLogLevel(
    level: FortressConfig['logLevel'],
    verboseLogging: FortressConfig['verboseLogging'],
  ): 'error' | 'warn' | 'info' | 'debug' | 'verbose' {
    if (level === 'error' || level === 'warn' || level === 'debug' || level === 'verbose') {
      return level;
    }

    if (verboseLogging === true) {
      return 'debug';
    }

    return 'info';
  }

  private canLog(level: 'error' | 'warn' | 'info' | 'debug' | 'verbose'): boolean {
    return FortressWeb.LOG_LEVEL_WEIGHT[this.currentLogLevel] >= FortressWeb.LOG_LEVEL_WEIGHT[level];
  }

  private logDebug(message: string, ...args: unknown[]): void {
    if (this.canLog('debug')) {
      console.debug('[FortressWeb]', message, ...args);
    }
  }

  private logWarn(message: string, ...args: unknown[]): void {
    if (this.canLog('warn')) {
      console.warn('[FortressWeb]', message, ...args);
    }
  }

  private arrayBufferToBase64(buffer: ArrayBufferLike): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  }

  private base64ToArrayBuffer(base64Value: string): ArrayBuffer {
    const binary = atob(base64Value);
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index);
    }
    return bytes.buffer;
  }

  private assertWebCryptoAvailable(): void {
    if (typeof crypto === 'undefined' || typeof crypto.subtle === 'undefined') {
      this.throwWebError(FortressErrorCode.UNAVAILABLE);
    }
  }

  private async assertSecureVaultAccess(): Promise<void> {
    const isLocked = await this.isVaultLockedByPolicy();
    if (isLocked) {
      this.throwWebError(FortressErrorCode.VAULT_LOCKED);
    }
  }

  private async isVaultLockedByPolicy(): Promise<boolean> {
    if (this.isLockedState(this.vaultState)) {
      return true;
    }

    if (typeof this.config.requireFreshAuthenticationMs === 'number' && this.config.requireFreshAuthenticationMs > 0) {
      const freshnessAge = Date.now() - this.lastSuccessfulAuthAt;
      if (this.lastSuccessfulAuthAt <= 0 || freshnessAge > this.config.requireFreshAuthenticationMs) {
        this.transitionToVaultState('EXPIRED');
        this.session.lastActiveAt = 0;
        this.lastTouchAt = 0;
        this.lastSuccessfulAuthAt = 0;
        this.saveSession();
        return true;
      }
    }

    if (typeof this.config.lockAfterMs === 'number' && this.config.lockAfterMs > 0) {
      const idleTimeMs = Date.now() - this.session.lastActiveAt;
      if (idleTimeMs >= this.config.lockAfterMs) {
        this.transitionToVaultState('EXPIRED');
        this.session.lastActiveAt = 0;
        this.lastTouchAt = 0;
        this.lastSuccessfulAuthAt = 0;
        this.saveSession();
        return true;
      }
    }

    return false;
  }

  // -----------------------------------------------------------------------------
  // Session Persistence
  // -----------------------------------------------------------------------------

  /**
   * Loads web session state from persisted storage when enabled.
   * Falls back to a deterministic locked baseline if persistence is disabled
   * or no valid persisted payload is available.
   */
  private loadSession(): void {
    if (!this.restorePersistedSession()) {
      const now = Date.now();
      this.session = { isLocked: true, lastActiveAt: now };
      this.transitionToVaultState('LOCKED');
      this.lastTouchAt = now;
      this.lastSuccessfulAuthAt = 0;
    }
  }

  /**
   * Saves web session state to localStorage when persistence is enabled.
   * Removes persisted payload when persistence is disabled.
   */
  private saveSession(): void {
    if (this.config.persistSessionState !== true) {
      localStorage.removeItem(FortressWeb.SESSION_KEY);
      return;
    }

    const persistedState: PersistedSessionState = {
      persistSessionState: true,
      isLocked: this.session.isLocked,
      lastActiveAt: this.session.lastActiveAt,
      lastSuccessfulAuthAt: this.lastSuccessfulAuthAt,
      vaultState: this.vaultState,
    };

    localStorage.setItem(FortressWeb.SESSION_KEY, JSON.stringify(persistedState));
  }

  /**
   * Restores persisted web session state from localStorage.
   *
   * Returns `true` only when a valid, opt-in payload is restored.
   */
  private restorePersistedSession(): boolean {
    const stored = localStorage.getItem(FortressWeb.SESSION_KEY);
    if (stored === null) {
      return false;
    }

    try {
      const parsed = JSON.parse(stored) as Partial<PersistedSessionState>;
      if (parsed.persistSessionState !== true) {
        return false;
      }

      const lastActiveAt = typeof parsed.lastActiveAt === 'number' ? parsed.lastActiveAt : Date.now();
      const lastSuccessfulAuthAt = typeof parsed.lastSuccessfulAuthAt === 'number' ? parsed.lastSuccessfulAuthAt : 0;
      const vaultState =
        parsed.vaultState === 'LOCKED' ||
        parsed.vaultState === 'UNLOCKING' ||
        parsed.vaultState === 'UNLOCKED' ||
        parsed.vaultState === 'EXPIRED'
          ? parsed.vaultState
          : parsed.isLocked === false
            ? 'UNLOCKED'
            : 'LOCKED';

      this.vaultState = vaultState;
      this.session = {
        isLocked: this.isLockedState(vaultState),
        lastActiveAt,
      };
      this.lastTouchAt = lastActiveAt;
      this.lastSuccessfulAuthAt = lastSuccessfulAuthAt;
      return true;
    } catch {
      return false;
    }
  }

  // -----------------------------------------------------------------------------
  // Auto-lock Timer
  // -----------------------------------------------------------------------------

  private autoLockTimerId: ReturnType<typeof setTimeout> | null = null;

  private startAutoLockTimer(lockAfterMs: number): void {
    if (this.autoLockTimerId) {
      clearTimeout(this.autoLockTimerId);
    }

    this.autoLockTimerId = setTimeout(() => {
      if (!this.session.isLocked && Date.now() - this.session.lastActiveAt >= lockAfterMs) {
        this.lock();
      }
    }, lockAfterMs);
  }

  private assertWebAuthnAvailable(): void {
    if (!globalThis.isSecureContext) {
      this.throwWebError(FortressErrorCode.UNAVAILABLE);
    }

    if (typeof PublicKeyCredential === 'undefined' || typeof navigator.credentials === 'undefined') {
      this.throwWebError(FortressErrorCode.UNAVAILABLE);
    }

    if (typeof crypto === 'undefined' || typeof crypto.getRandomValues === 'undefined') {
      this.throwWebError(FortressErrorCode.UNAVAILABLE);
    }
  }

  private createRandomChallenge(): ArrayBuffer {
    const challenge = new Uint8Array(32);
    crypto.getRandomValues(challenge);
    return challenge.buffer.slice(challenge.byteOffset, challenge.byteOffset + challenge.byteLength) as ArrayBuffer;
  }

  private utf8ToArrayBuffer(value: string): ArrayBuffer {
    const bytes = new TextEncoder().encode(value);
    return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
  }

  private async getWebDeviceIdentifierHash(): Promise<string> {
    const baseIdentifier = `${globalThis.location.origin}|${navigator.userAgent}`;
    return this.sha256Hex(baseIdentifier);
  }

  private async sha256Hex(value: string): Promise<string> {
    if (typeof crypto.subtle === 'undefined') {
      this.throwWebError(FortressErrorCode.UNAVAILABLE);
    }

    const bytes = new TextEncoder().encode(value);
    const digest = await crypto.subtle.digest('SHA-256', bytes);
    return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
  }

  private async ensureWebAuthnCredential(): Promise<WebAuthnState> {
    const state = this.readWebAuthnState();
    if (state.credentialIds.length > 0) {
      return state;
    }

    if (this.isServerWebAuthnMode()) {
      return this.registerServerCredential(state);
    }

    const createdCredential = await navigator.credentials.create({
      publicKey: {
        challenge: this.createRandomChallenge(),
        rp: {
          name: FortressWeb.WEBAUTHN_RP_NAME,
          id: state.rpId,
        },
        user: {
          id: this.base64UrlToArrayBuffer(state.userId),
          name: FortressWeb.WEBAUTHN_USER_NAME,
          displayName: FortressWeb.WEBAUTHN_USER_DISPLAY_NAME,
        },
        pubKeyCredParams: this.getPubKeyCredParams(),
        authenticatorSelection: {
          userVerification: 'preferred',
          residentKey: 'preferred',
        },
        timeout: FortressWeb.WEBAUTHN_TIMEOUT_MS,
        attestation: 'none',
      },
    });

    if (!(createdCredential instanceof PublicKeyCredential)) {
      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }

    const credentialId = this.arrayBufferToBase64Url(createdCredential.rawId);
    const persistedState: WebAuthnState = {
      ...state,
      credentialIds: [credentialId],
    };

    this.writeWebAuthnState(persistedState);
    return persistedState;
  }

  private async signChallengeWithWebAuthn(challenge: string, state: WebAuthnState): Promise<string> {
    this.assertWebAuthnAvailable();
    this.assertNotBiometricLockedOut();

    try {
      const allowCredentials = state.credentialIds.map((credentialId) => this.toAllowCredential(credentialId));

      const credential = await navigator.credentials.get({
        publicKey: {
          challenge: this.utf8ToArrayBuffer(challenge),
          timeout: FortressWeb.WEBAUTHN_TIMEOUT_MS,
          userVerification: 'preferred',
          ...(allowCredentials.length > 0 ? { allowCredentials } : {}),
        },
      });

      if (credential === null) {
        this.throwWebError(FortressErrorCode.CANCELLED);
      }

      if (!(credential instanceof PublicKeyCredential)) {
        this.throwWebError(FortressErrorCode.INIT_FAILED);
      }

      const assertionResponse = credential.response;
      if (!(assertionResponse instanceof AuthenticatorAssertionResponse)) {
        this.throwWebError(FortressErrorCode.INIT_FAILED);
      }

      this.lastSuccessfulAuthAt = Date.now();
      this.clearBiometricFailureState();

      return this.arrayBufferToBase64Url(assertionResponse.signature);
    } catch (error) {
      this.recordBiometricFailure(error);
      this.logWarn('Challenge signature failed', error);

      if (error instanceof FortressWebError) {
        throw error;
      }

      if (error instanceof DOMException) {
        throw this.mapWebAuthnError(error);
      }

      this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
    }
  }

  private readWebAuthnState(): WebAuthnState {
    const fallback = this.createInitialWebAuthnState();
    const rawState = localStorage.getItem(FortressWeb.WEBAUTHN_STATE_KEY);

    if (rawState === null) {
      return fallback;
    }

    try {
      const parsed = JSON.parse(rawState) as Partial<WebAuthnState>;
      return {
        credentialIds: Array.isArray(parsed.credentialIds)
          ? parsed.credentialIds.filter((id): id is string => typeof id === 'string')
          : [],
        userId: typeof parsed.userId === 'string' ? parsed.userId : fallback.userId,
        rpId: typeof parsed.rpId === 'string' ? parsed.rpId : fallback.rpId,
      };
    } catch {
      return fallback;
    }
  }

  private writeWebAuthnState(state: WebAuthnState): void {
    localStorage.setItem(FortressWeb.WEBAUTHN_STATE_KEY, JSON.stringify(state));
  }

  private createInitialWebAuthnState(): WebAuthnState {
    return {
      credentialIds: [],
      userId: this.arrayBufferToBase64Url(this.createRandomChallenge()),
      rpId: globalThis.location.hostname,
    };
  }

  private arrayBufferToBase64Url(value: ArrayBuffer): string {
    const bytes = new Uint8Array(value);
    const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join('');
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }

  private base64UrlToArrayBuffer(value: string): ArrayBuffer {
    const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    const binary = atob(padded);
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
    return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
  }

  private isServerWebAuthnMode(): boolean {
    return this.config.webAuthn?.mode === 'server';
  }

  private getRequiredServerConfig(): RequiredServerWebAuthnConfig {
    const webAuthnConfig = this.config.webAuthn;

    if (
      webAuthnConfig?.registrationStartUrl === undefined ||
      webAuthnConfig.registrationFinishUrl === undefined ||
      webAuthnConfig.authenticationStartUrl === undefined ||
      webAuthnConfig.authenticationFinishUrl === undefined
    ) {
      this.throwWebError(FortressErrorCode.UNAVAILABLE);
    }

    return {
      registrationStartUrl: webAuthnConfig.registrationStartUrl,
      registrationFinishUrl: webAuthnConfig.registrationFinishUrl,
      authenticationStartUrl: webAuthnConfig.authenticationStartUrl,
      authenticationFinishUrl: webAuthnConfig.authenticationFinishUrl,
      headers: webAuthnConfig.headers ?? {},
    };
  }

  private async registerServerCredential(state: WebAuthnState): Promise<WebAuthnState> {
    const serverConfig = this.getRequiredServerConfig();
    const registrationStart = await this.postJson<ServerRegistrationStartPayload>(serverConfig.registrationStartUrl, {
      userId: state.userId,
      rpId: state.rpId,
    });

    const registrationCredential = await navigator.credentials.create({
      publicKey: {
        challenge: this.base64UrlToArrayBuffer(registrationStart.challenge),
        rp: {
          name: registrationStart.rpName ?? FortressWeb.WEBAUTHN_RP_NAME,
          id: registrationStart.rpId ?? state.rpId,
        },
        user: {
          id: this.base64UrlToArrayBuffer(registrationStart.userId ?? state.userId),
          name: registrationStart.userName ?? FortressWeb.WEBAUTHN_USER_NAME,
          displayName: registrationStart.userDisplayName ?? FortressWeb.WEBAUTHN_USER_DISPLAY_NAME,
        },
        pubKeyCredParams: this.getPubKeyCredParams(),
        authenticatorSelection: {
          userVerification: 'preferred',
          residentKey: 'preferred',
        },
        timeout: FortressWeb.WEBAUTHN_TIMEOUT_MS,
        attestation: 'none',
      },
    });

    if (!(registrationCredential instanceof PublicKeyCredential)) {
      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }

    const attestationResponse = registrationCredential.response;
    if (!(attestationResponse instanceof AuthenticatorAttestationResponse)) {
      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }

    const credentialId = this.arrayBufferToBase64Url(registrationCredential.rawId);
    await this.postJson(serverConfig.registrationFinishUrl, {
      id: registrationCredential.id,
      credentialId,
      rawId: credentialId,
      type: registrationCredential.type,
      response: {
        clientDataJSON: this.arrayBufferToBase64Url(attestationResponse.clientDataJSON),
        attestationObject: this.arrayBufferToBase64Url(attestationResponse.attestationObject),
      },
    });

    const persistedState: WebAuthnState = {
      credentialIds: [credentialId],
      userId: registrationStart.userId ?? state.userId,
      rpId: registrationStart.rpId ?? state.rpId,
    };

    this.writeWebAuthnState(persistedState);
    return persistedState;
  }

  private async getServerAuthenticationChallenge(state: WebAuthnState): Promise<ArrayBuffer> {
    const serverConfig = this.getRequiredServerConfig();
    const authenticationStart = await this.postJson<ServerAuthenticationStartPayload>(
      serverConfig.authenticationStartUrl,
      {
        credentialIds: state.credentialIds,
        userId: state.userId,
        rpId: state.rpId,
      },
    );

    return this.base64UrlToArrayBuffer(authenticationStart.challenge);
  }

  private async verifyServerAuthentication(credential: PublicKeyCredential, state: WebAuthnState): Promise<void> {
    const serverConfig = this.getRequiredServerConfig();
    const assertionResponse = credential.response;

    if (!(assertionResponse instanceof AuthenticatorAssertionResponse)) {
      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }

    const verificationResponse = await this.postJson<ServerAuthenticationFinishResponse>(
      serverConfig.authenticationFinishUrl,
      {
        id: credential.id,
        credentialId: this.arrayBufferToBase64Url(credential.rawId),
        rawId: this.arrayBufferToBase64Url(credential.rawId),
        type: credential.type,
        response: {
          clientDataJSON: this.arrayBufferToBase64Url(assertionResponse.clientDataJSON),
          authenticatorData: this.arrayBufferToBase64Url(assertionResponse.authenticatorData),
          signature: this.arrayBufferToBase64Url(assertionResponse.signature),
          userHandle:
            assertionResponse.userHandle === null ? null : this.arrayBufferToBase64Url(assertionResponse.userHandle),
        },
        context: {
          credentialIds: state.credentialIds,
          userId: state.userId,
          rpId: state.rpId,
        },
      },
    );

    if (verificationResponse.verified !== true) {
      this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
    }
  }

  private async postJson<TResponse = unknown>(url: string, payload: unknown): Promise<TResponse> {
    const serverConfig = this.getRequiredServerConfig();
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        ...serverConfig.headers,
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }

    return (await response.json()) as TResponse;
  }

  private toAllowCredential(credentialId: string): PublicKeyCredentialDescriptor {
    return {
      id: this.base64UrlToArrayBuffer(credentialId),
      type: 'public-key',
    };
  }

  private mapWebAuthnError(error: DOMException): FortressWebError {
    if (error.name === 'NotAllowedError' || error.name === 'AbortError') {
      return this.createWebError(FortressErrorCode.CANCELLED);
    }

    if (error.name === 'NotSupportedError' || error.name === 'SecurityError') {
      return this.createWebError(FortressErrorCode.UNAVAILABLE);
    }

    if (error.name === 'InvalidStateError') {
      return this.createWebError(FortressErrorCode.CONFLICT);
    }

    return this.createWebError(FortressErrorCode.INIT_FAILED);
  }

  private throwWebError(code: FortressErrorCode): never {
    throw this.createWebError(code);
  }

  private createWebError(code: FortressErrorCode): FortressWebError {
    return new FortressWebError(code, FortressWeb.ERROR_MESSAGES[code]);
  }

  // -----------------------------------------------------------------------------
  // Plugin Info
  // -----------------------------------------------------------------------------

  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: PLUGIN_VERSION };
  }
}

interface WebAuthnState {
  credentialIds: string[];
  userId: string;
  rpId: string;
}

interface RequiredServerWebAuthnConfig {
  registrationStartUrl: string;
  registrationFinishUrl: string;
  authenticationStartUrl: string;
  authenticationFinishUrl: string;
  headers: Record<string, string>;
}

interface ServerRegistrationStartPayload {
  challenge: string;
  rpId?: string;
  rpName?: string;
  userId?: string;
  userName?: string;
  userDisplayName?: string;
}

interface ServerAuthenticationStartPayload {
  challenge: string;
}

interface ServerAuthenticationFinishResponse {
  verified: boolean;
}

interface EncryptedWebPayload {
  v: number;
  alg?: 'AES-GCM' | 'AES-CBC';
  iv: string;
  cipher: string;
}

interface PersistedSessionState {
  persistSessionState: boolean;
  isLocked: boolean;
  lastActiveAt: number;
  lastSuccessfulAuthAt: number;
  vaultState: VaultState;
}

class FortressWebError extends Error {
  constructor(
    readonly code: FortressErrorCode,
    message: string,
  ) {
    super(message);
    this.name = 'FortressWebError';
  }
}
