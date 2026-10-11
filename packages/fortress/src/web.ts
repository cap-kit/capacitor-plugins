import { WebPlugin } from '@capacitor/core';

import {
  AuthenticationTypeResult,
  AuthenticateOptions,
  AuthenticateWithChallengeResult,
  BiometricKeysExistResult,
  BiometricStrengthResult,
  BiometricTypeResult,
  BiometricTypesResult,
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
  GetManyOptions,
  GetManyResult,
  KeychainAccess,
  KeysOptions,
  KeysResult,
  HasDeviceCredentialResult,
  IsAvailableResult,
  IsEnrolledResult,
  KeyAliasOptions,
  ObfuscatedKeyResult,
  PluginVersionResult,
  PrivacyScreenActionResult,
  PrivacyScreenConfig,
  PrivacyScreenStatus,
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
 * Minimal async key/value backend behind the web persistence layer.
 *
 * The primary implementation is Ionic Storage (IndexedDB first,
 * LocalStorage fallback); the fallback is a plain in-memory map used
 * only when no persistent engine exists at all.
 */
interface WebKvBackend {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
  keys(): Promise<string[]>;
}

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
  private static readonly RUNTIME_CONFIG_KEY = 'fortress_runtime_config_v1';
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
  private privacyScreenEnabled = true;
  private privacyScreenConfig: PrivacyScreenConfig = {};
  private readonly visibilityChangeHandler = (): void => {
    void this.handleVisibilityChange();
  };

  // ---------------------------------------------------------------------------
  // Persistent web store (localforage engine chain + memory fallback)
  // ---------------------------------------------------------------------------

  private storeBackend: Promise<WebKvBackend> | null = null;
  private storeWrites: Promise<void> = Promise.resolve();
  private sessionWrittenKeys = new Set<string>();

  private backend(): Promise<WebKvBackend> {
    if (this.storeBackend === null) {
      this.storeBackend = this.initBackend();
    }
    return this.storeBackend;
  }

  private initPersistentStore(): void {
    void this.backend();
  }

  private async initBackend(): Promise<WebKvBackend> {
    try {
      const backend = await this.indexedDbBackend('fortress');
      await this.importLegacyKeys(backend);
      return backend;
    } catch {
      // IndexedDB unavailable or failed its probe: try LocalStorage.
    }
    try {
      const backend = this.localStorageBackend();
      await this.importLegacyKeys(backend);
      return backend;
    } catch {
      // No persistent engine at all (e.g. storage fully blocked).
    }
    this.logDebug('No persistent web engine available; using in-memory store');
    const memory = new Map<string, string>();
    return {
      getItem: (key) => Promise.resolve(memory.get(key) ?? null),
      setItem: (key, value) => {
        memory.set(key, value);
        return Promise.resolve();
      },
      removeItem: (key) => {
        memory.delete(key);
        return Promise.resolve();
      },
      keys: () => Promise.resolve([...memory.keys()]),
    };
  }

  /**
   * IndexedDB backend probed before adoption with a full
   * write/read/delete round-trip. Connections are short-lived per
   * operation: no retained handles, no version-change deadlocks.
   */
  private async indexedDbBackend(dbName: string): Promise<WebKvBackend> {
    if (typeof indexedDB === 'undefined') {
      throw new Error('IndexedDB unavailable.');
    }

    const openDb = (): Promise<IDBDatabase> =>
      new Promise((resolve, reject) => {
        const request = indexedDB.open(dbName, 1);
        request.onupgradeneeded = () => {
          const db = request.result;
          if (!db.objectStoreNames.contains('keyvalue')) {
            db.createObjectStore('keyvalue');
          }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error ?? new Error('IndexedDB open failed.'));
      });

    const withStore = <T>(mode: IDBTransactionMode, task: (store: IDBObjectStore) => IDBRequest<T>): Promise<T> =>
      openDb().then(
        (db) =>
          new Promise<T>((resolve, reject) => {
            let failed: unknown = null;
            let result: T | undefined;
            try {
              const tx = db.transaction('keyvalue', mode);
              const request = task(tx.objectStore('keyvalue'));
              request.onsuccess = () => {
                result = request.result as T;
              };
              request.onerror = () => {
                failed = request.error;
              };
              tx.oncomplete = () => {
                db.close();
                if (failed !== null && failed !== undefined) {
                  reject(failed instanceof Error ? failed : new Error('IndexedDB request failed.'));
                } else {
                  resolve(result as T);
                }
              };
              tx.onerror = () => {
                db.close();
                reject(tx.error ?? new Error('IndexedDB transaction failed.'));
              };
              tx.onabort = () => {
                db.close();
                reject(tx.error ?? new Error('IndexedDB transaction aborted.'));
              };
            } catch (error) {
              db.close();
              reject(error instanceof Error ? error : new Error('IndexedDB request failed.'));
            }
          }),
      );

    const backend: WebKvBackend = {
      getItem: (key) =>
        withStore('readonly', (store) => store.get(key)).then((value) =>
          value === undefined || value === null ? null : String(value),
        ),
      setItem: (key, value) => withStore('readwrite', (store) => store.put(value, key)).then(() => undefined),
      removeItem: (key) => withStore('readwrite', (store) => store.delete(key)).then(() => undefined),
      keys: () => withStore('readonly', (store) => store.getAllKeys()).then((keys) => keys.map(String)),
    };

    const probeKey = `__probe__${Date.now()}`;
    try {
      await backend.setItem(probeKey, '1');
      if ((await backend.getItem(probeKey)) !== '1') {
        throw new Error('IndexedDB probe mismatch.');
      }
    } finally {
      await backend.removeItem(probeKey).catch(() => undefined);
    }
    return backend;
  }

  /**
   * LocalStorage backend. The probe catches Safari-private-mode-style
   * zero-quota stores at selection time instead of mid-session.
   */
  private localStorageBackend(): WebKvBackend {
    if (typeof localStorage === 'undefined') {
      throw new Error('LocalStorage unavailable.');
    }
    const probeKey = `__probe__${Date.now()}`;
    try {
      localStorage.setItem(probeKey, '1');
      if (localStorage.getItem(probeKey) !== '1') {
        throw new Error('LocalStorage probe mismatch.');
      }
    } finally {
      try {
        localStorage.removeItem(probeKey);
      } catch {
        // Best effort.
      }
    }
    return {
      getItem: (key) => Promise.resolve(this.legacyGet(key)),
      setItem: (key, value) => {
        try {
          localStorage.setItem(key, value);
        } catch {
          throw new Error('LocalStorage write failed.');
        }
        return Promise.resolve();
      },
      removeItem: (key) => {
        try {
          localStorage.removeItem(key);
        } catch {
          // Best effort.
        }
        return Promise.resolve();
      },
      keys: () => {
        const found: string[] = [];
        try {
          for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key !== null) {
              found.push(key);
            }
          }
        } catch {
          // Best effort.
        }
        return Promise.resolve(found);
      },
    };
  }

  /**
   * One-time migration of legacy raw-localStorage entries into the
   * engine store. Copies only Fortress-owned keys missing from the new
   * store (and never keys written this session, so fresh writes win).
   * Legacy copies stay in place as a downgrade backup.
   */
  private async importLegacyKeys(backend: WebKvBackend): Promise<void> {
    let existing: Set<string>;
    try {
      existing = new Set(await backend.keys());
    } catch {
      return;
    }

    const fixedKeys = [FortressWeb.SESSION_KEY, FortressWeb.RUNTIME_CONFIG_KEY, FortressWeb.WEBAUTHN_STATE_KEY];
    const prefixes = [FortressWeb.SECURE_STORAGE_PREFIX, this.insecurePrefix(), 'ftrss_', 'fortress_'];

    const legacyKeys: string[] = [];
    try {
      if (typeof localStorage === 'undefined') {
        return;
      }
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key !== null && (fixedKeys.includes(key) || prefixes.some((prefix) => key.startsWith(prefix)))) {
          legacyKeys.push(key);
        }
      }
    } catch {
      return;
    }

    for (const key of legacyKeys) {
      if (existing.has(key) || this.sessionWrittenKeys.has(key)) {
        continue;
      }
      const legacy = this.legacyGet(key);
      if (legacy !== null) {
        try {
          await backend.setItem(key, legacy);
        } catch {
          // Best effort; the downgrade read path still serves legacy data.
        }
      }
    }
  }

  private async storeGet(key: string): Promise<string | null> {
    try {
      await this.storeWrites;
      const value = await (await this.backend()).getItem(key);
      if (value !== null) {
        return value;
      }
    } catch {
      // Fall through to legacy below, then to a mapped failure.
    }
    // Downgrade path: serve legacy copies (e.g. pre-migration data).
    const legacy = this.legacyGet(key);
    if (legacy !== null) {
      this.storeSet(key, legacy);
      return legacy;
    }
    return null;
  }

  private storeSet(key: string, value: string): void {
    this.sessionWrittenKeys.add(key);
    this.storeWrites = this.storeWrites
      .then(() => this.backend())
      .then((backend) => backend.setItem(key, value))
      .catch(() => undefined);
  }

  private storeRemove(key: string): void {
    this.sessionWrittenKeys.add(key);
    this.storeWrites = this.storeWrites
      .then(() => this.backend())
      .then((backend) => backend.removeItem(key))
      .catch(() => undefined);
  }

  private async storeKeys(): Promise<string[]> {
    try {
      await this.storeWrites;
      return await (await this.backend()).keys();
    } catch {
      return [];
    }
  }

  private legacyGet(key: string): string | null {
    try {
      return typeof localStorage === 'undefined' ? null : localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  // -----------------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------------

  constructor() {
    super();
    this.config = this.loadPersistedRuntimeConfig();
    this.currentLogLevel = this.resolveLogLevel(this.config.logLevel, this.config.verboseLogging);
    this.loadSession();
    this.privacyScreenEnabled = this.config.enablePrivacyScreen ?? true;
    this.initPersistentStore();

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
      privacyScreenEnabled: this.privacyScreenEnabled,
      obfuscateKeys: this.config.obfuscateKeys ?? false,
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
    const sanitizedOverrides = this.sanitizeRuntimeConfigOverrides(config as Record<string, unknown>);
    const nextConfig: FortressConfig = {
      ...this.config,
      ...sanitizedOverrides,
    };

    const wasPersisting = this.config.persistSessionState === true;
    this.config = nextConfig;
    this.noteInsecurePrefix();
    this.saveRuntimeConfigOverrides(nextConfig);
    this.currentLogLevel = this.resolveLogLevel(nextConfig.logLevel, nextConfig.verboseLogging);
    if (sanitizedOverrides.enablePrivacyScreen !== undefined) {
      // A policy change re-attaches privacy to the lock policy and clears manual control.
      this.privacyScreenEnabled = sanitizedOverrides.enablePrivacyScreen;
    }
    this.logDebug('Configuration applied', `logLevel=${this.currentLogLevel}`);

    const isPersisting = this.config.persistSessionState === true;
    if (isPersisting && !wasPersisting) {
      this.restorePersistedSession();
    } else if (!isPersisting && wasPersisting) {
      this.storeRemove(FortressWeb.SESSION_KEY);
    }

    if (nextConfig.lockAfterMs !== undefined && nextConfig.lockAfterMs > 0) {
      this.startAutoLockTimer(nextConfig.lockAfterMs);
    }
  }

  async resetRuntimeConfig(): Promise<void> {
    const wasPersisting = this.config.persistSessionState === true;

    this.storeRemove(FortressWeb.RUNTIME_CONFIG_KEY);
    this.config = {};
    this.privacyScreenEnabled = true;
    this.currentLogLevel = this.resolveLogLevel(this.config.logLevel, this.config.verboseLogging);

    const isPersisting = this.config.persistSessionState === true;
    if (!isPersisting && wasPersisting) {
      this.storeRemove(FortressWeb.SESSION_KEY);
    }
  }

  // -----------------------------------------------------------------------------
  // Secure Storage (Encrypted localStorage)
  // -----------------------------------------------------------------------------

  async setValue(value: SecureValue): Promise<void> {
    await this.assertSecureVaultAccess();
    const encodedKey = this.encodeKey(value.key);
    const encryptedPayload = await this.encryptValue(value.value);
    this.storeSet(encodedKey, encryptedPayload);
    await this.touchSession();
  }

  async getValue(key: { key: string }): Promise<ValueResult> {
    await this.assertSecureVaultAccess();
    const decoded = await this.readSecureValue(key.key);
    if (decoded === null) {
      return { value: null };
    }
    await this.touchSession();
    return { value: decoded };
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
      const names = operation.secure
        ? this.secureNameCandidates(operation.key)
        : this.insecureNameCandidates(operation.key);
      for (const storageKey of names) {
        if (!snapshot.has(storageKey)) {
          snapshot.set(storageKey, await this.storeGet(storageKey));
        }
      }
    }

    try {
      for (const operation of operations) {
        if (operation.secure) {
          const storageKey = this.encodeKey(operation.key);
          const encryptedPayload = await this.encryptValue(operation.value);
          this.storeSet(storageKey, encryptedPayload);
        } else {
          this.storeSet(this.obfuscateKey(operation.key), operation.value);
        }
      }
      await this.touchSession();
    } catch (error) {
      for (const [storageKey, previousValue] of snapshot) {
        if (previousValue === null) {
          this.storeRemove(storageKey);
        } else {
          this.storeSet(storageKey, previousValue);
        }
      }
      throw error;
    }
  }

  async removeValue(key: { key: string }): Promise<void> {
    for (const name of this.secureNameCandidates(key.key)) {
      this.storeRemove(name);
    }
    this.touchSession();
  }

  async clearAll(): Promise<void> {
    const keysToRemove: string[] = [];

    for (const key of await this.storeKeys()) {
      if (key.startsWith(FortressWeb.SECURE_STORAGE_PREFIX)) {
        keysToRemove.push(key);
      }
    }

    keysToRemove.forEach((key) => this.storeRemove(key));
    // Logic: Also clear WebAuthn enrollment state during a full wipe
    this.storeRemove(FortressWeb.WEBAUTHN_STATE_KEY);
    this.touchSession();
  }

  // -----------------------------------------------------------------------------
  // Insecure Storage (Plain localStorage)
  // -----------------------------------------------------------------------------

  async setInsecureValue(value: SecureValue): Promise<void> {
    const obfuscatedKey = this.obfuscateKey(value.key);
    this.storeSet(obfuscatedKey, value.value);
    this.touchSession();
  }

  async getInsecureValue(key: { key: string }): Promise<ValueResult> {
    let value: string | null = null;
    for (const name of this.insecureNameCandidates(key.key)) {
      value = await this.storeGet(name);
      if (value !== null) {
        break;
      }
    }

    this.touchSession();
    return { value };
  }

  async removeInsecureValue(key: { key: string }): Promise<void> {
    for (const name of this.insecureNameCandidates(key.key)) {
      this.storeRemove(name);
    }
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
    if (options.secure !== false) {
      for (const name of this.secureNameCandidates(options.key)) {
        if ((await this.storeGet(name)) !== null) {
          return { exists: true };
        }
      }
      return { exists: false };
    }
    for (const name of this.insecureNameCandidates(options.key)) {
      if ((await this.storeGet(name)) !== null) {
        return { exists: true };
      }
    }
    return { exists: false };
  }

  async keys(options?: KeysOptions): Promise<KeysResult> {
    const secure = options?.secure !== false;
    const found: string[] = [];

    if (secure) {
      for (const stored of await this.storeKeys()) {
        if (stored.startsWith(FortressWeb.SECURE_STORAGE_PREFIX)) {
          found.push(this.decodeKey(stored.slice(FortressWeb.SECURE_STORAGE_PREFIX.length)));
        }
      }
    } else {
      const prefixes = this.insecurePrefixVariants();
      for (const stored of await this.storeKeys()) {
        if (
          stored === FortressWeb.SESSION_KEY ||
          stored === FortressWeb.RUNTIME_CONFIG_KEY ||
          stored === FortressWeb.WEBAUTHN_STATE_KEY
        ) {
          continue;
        }
        const prefix = prefixes.find((candidate) => stored.startsWith(candidate));
        if (prefix !== undefined) {
          found.push(this.decodeKey(stored.slice(prefix.length)));
        }
      }
    }

    return { keys: [...new Set(found)] };
  }

  async getMany(options: GetManyOptions): Promise<GetManyResult> {
    const secure = options.secure !== false;
    const values: Record<string, string | null> = {};

    for (const key of options.keys) {
      if (secure) {
        values[key] = (await this.getValue({ key })).value;
      } else {
        values[key] = (await this.getInsecureValue({ key })).value;
      }
    }

    return { values };
  }

  async setSynchronize(options: { synchronize: boolean }): Promise<void> {
    void options;
    // iCloud Keychain does not exist on Web; no-op for API parity.
  }

  async getSynchronize(): Promise<{ synchronize: boolean }> {
    return { synchronize: false };
  }

  async setDefaultKeychainAccess(options: { access: KeychainAccess }): Promise<void> {
    void options;
    // iOS Keychain accessibility has no Web equivalent; no-op for API parity.
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

  // ---------------------------------------------------------------------------
  // Privacy Screen (manual runtime control — Ionic API parity)
  // ---------------------------------------------------------------------------

  async enable(config?: PrivacyScreenConfig): Promise<PrivacyScreenActionResult> {
    if (config !== undefined) {
      this.privacyScreenConfig = config;
    }
    // Web has no OS-level snapshot protection; track the state for API parity.
    this.privacyScreenEnabled = true;
    this.logDebug('Privacy screen enabled (web: state-tracked only)', this.privacyScreenConfig);
    return { success: true };
  }

  async disable(): Promise<PrivacyScreenActionResult> {
    this.privacyScreenEnabled = false;
    this.logDebug('Privacy screen disabled (web: state-tracked only)');
    return { success: true };
  }

  async isEnabled(): Promise<PrivacyScreenStatus> {
    return { enabled: this.privacyScreenEnabled };
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
    this.storeRemove(FortressWeb.WEBAUTHN_STATE_KEY);

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
    const webAuthnApi = typeof window !== 'undefined' ? (window as any).PublicKeyCredential : undefined;
    const hasWebAuthnApi =
      typeof webAuthnApi !== 'undefined' &&
      typeof webAuthnApi.isUserVerifyingPlatformAuthenticatorAvailable === 'function';

    let isBiometricsAvailable = false;
    if (hasWebAuthnApi) {
      isBiometricsAvailable = await webAuthnApi.isUserVerifyingPlatformAuthenticatorAvailable();
    }

    const state = this.readWebAuthnState();
    const isBiometricsEnabled = isBiometricsAvailable && state.credentialIds.length > 0;

    const isDeviceSecure = globalThis.isSecureContext && isBiometricsAvailable;

    const status: DeviceSecurityStatus = {
      isBiometricsAvailable,
      isBiometricsEnabled,
      isDeviceSecure,
      biometryType: isBiometricsAvailable ? 'fingerprint' : 'none',
      biometryTypes: isBiometricsAvailable ? ['fingerprint'] : [],
      strongBiometryIsAvailable: isBiometricsAvailable,
    };

    return this.applySecurityOverrides(status);
  }

  // ---------------------------------------------------------------------------
  // Standalone Biometrics (no vault or session side effects)
  // ---------------------------------------------------------------------------

  async authenticate(options?: AuthenticateOptions): Promise<void> {
    void options;
    this.assertNotBiometricLockedOut();
    this.assertWebAuthnAvailable();

    try {
      const state = await this.ensureWebAuthnCredential();
      const allowCredentials = state.credentialIds.map((credentialId) => this.toAllowCredential(credentialId));

      const credential = await navigator.credentials.get({
        publicKey: {
          challenge: this.createRandomChallenge(),
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

      // Pure identity proof: vault and session state are intentionally untouched.
      this.clearBiometricFailureState();
    } catch (error) {
      this.recordBiometricFailure(error);
      this.logWarn('Authenticate failed', error);

      if (error instanceof FortressWebError) {
        throw error;
      }

      if (error instanceof DOMException) {
        throw this.mapWebAuthnError(error);
      }

      this.throwWebError(FortressErrorCode.INIT_FAILED);
    }
  }

  async cancelAuthentication(): Promise<void> {
    // Web has no cancellable system prompt; nothing to do.
  }

  async isAvailable(): Promise<IsAvailableResult> {
    const status = await this.checkStatus();
    return { isAvailable: status.isBiometricsAvailable };
  }

  async isEnrolled(): Promise<IsEnrolledResult> {
    const status = await this.checkStatus();
    return { isEnrolled: status.isBiometricsEnabled };
  }

  async getBiometricType(): Promise<BiometricTypeResult> {
    const status = await this.checkStatus();
    return { biometryType: status.biometryType };
  }

  async getBiometricTypes(): Promise<BiometricTypesResult> {
    const status = await this.checkStatus();
    return { biometryTypes: status.biometryTypes };
  }

  async hasDeviceCredential(): Promise<HasDeviceCredentialResult> {
    const status = await this.checkStatus();
    return { hasDeviceCredential: status.isDeviceSecure };
  }

  async getBiometricStrengthLevel(): Promise<BiometricStrengthResult> {
    const status = await this.checkStatus();
    return { strengthLevel: status.strongBiometryIsAvailable ? 'strong' : 'none' };
  }

  async getAuthenticationType(): Promise<AuthenticationTypeResult> {
    // Web never reports which credential satisfied the ceremony.
    return { authenticationType: 'unknown' };
  }

  async enroll(): Promise<void> {
    this.throwWebError(FortressErrorCode.UNAVAILABLE);
  }

  /**
   * Overrides the detected biometry type for development/testing flows.
   */
  async setBiometryType(options: SetBiometryTypeOptions): Promise<void> {
    this.securityOverrides.biometryType = options.biometryType;

    if (options.biometryType === 'none') {
      this.securityOverrides.isBiometricsAvailable = false;
      this.securityOverrides.isBiometricsEnabled = false;
      this.securityOverrides.biometryTypes = [];
      this.securityOverrides.strongBiometryIsAvailable = false;
    } else {
      this.securityOverrides.isBiometricsAvailable = true;
      this.securityOverrides.biometryTypes = [options.biometryType];
      this.securityOverrides.strongBiometryIsAvailable = true;
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
      biometryTypes: this.securityOverrides.biometryTypes ?? status.biometryTypes,
      strongBiometryIsAvailable: this.securityOverrides.strongBiometryIsAvailable ?? status.strongBiometryIsAvailable,
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
    const body = (this.config.obfuscateKeys ?? false) ? btoa(key) : key;
    return `${FortressWeb.SECURE_STORAGE_PREFIX}${body}`;
  }

  private secureNameCandidates(key: string): string[] {
    const plain = `${FortressWeb.SECURE_STORAGE_PREFIX}${key}`;
    const encoded = `${FortressWeb.SECURE_STORAGE_PREFIX}${btoa(key)}`;
    return (this.config.obfuscateKeys ?? false) ? [encoded, plain] : [plain, encoded];
  }

  private insecureNameCandidates(key: string): string[] {
    const bodies = (this.config.obfuscateKeys ?? false) ? [btoa(key), key] : [key, btoa(key)];
    const names: string[] = [];
    for (const prefix of this.insecurePrefixVariants()) {
      for (const body of bodies) {
        names.push(`${prefix}${body}`);
      }
    }
    return names;
  }

  private async readSecureValue(key: string): Promise<string | null> {
    for (const name of this.secureNameCandidates(key)) {
      const stored = await this.storeGet(name);
      if (stored === null) {
        continue;
      }
      try {
        return await this.decryptValue(stored);
      } catch {
        this.throwWebError(FortressErrorCode.SECURITY_VIOLATION);
      }
    }
    return null;
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
  private insecurePrefix(): string {
    // Force a fallback prefix if the one in config is empty or invalid
    const prefix = this.config.obfuscationPrefix;
    return prefix !== undefined && prefix.trim().length > 0 ? prefix : 'ftrss_';
  }

  private knownInsecurePrefixes = new Set<string>(['ftrss_', 'fortress_']);

  private insecurePrefixVariants(): string[] {
    const ordered = [this.insecurePrefix()];
    for (const seen of this.knownInsecurePrefixes) {
      if (seen !== this.insecurePrefix()) {
        ordered.push(seen);
      }
    }
    return ordered;
  }

  private noteInsecurePrefix(): void {
    const prefix = this.insecurePrefix();
    if (prefix.length > 0) {
      this.knownInsecurePrefixes.add(prefix);
    }
  }

  private obfuscateKey(key: string): string {
    const body = (this.config.obfuscateKeys ?? false) ? btoa(key) : key;
    return `${this.insecurePrefix()}${body}`;
  }

  private decodeKey(encoded: string): string {
    try {
      const binary = atob(encoded);
      const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
      const decoded = new TextDecoder().decode(bytes);
      const roundTrip = btoa(
        Array.from(new TextEncoder().encode(decoded), (byte) => String.fromCharCode(byte)).join(''),
      );
      return roundTrip === encoded ? decoded : encoded;
    } catch {
      return encoded;
    }
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

  private webCryptoScope(): { origin: string; hostname: string } {
    const location = (globalThis as { location?: { origin?: string; hostname?: string } }).location;
    return {
      origin: location?.origin ?? 'capacitor-local',
      hostname: location?.hostname ?? 'localhost',
    };
  }

  private async getOrCreateWebCryptoKey(): Promise<CryptoKey> {
    const now = Date.now();
    if (this.webCryptoKeyCache !== null && this.webCryptoKeyCache.expiresAt > now) {
      return this.webCryptoKeyCache.key;
    }

    const encryptionAlgorithm = this.getEncryptionAlgorithm();
    const scope = this.webCryptoScope();
    const passphrase = `${scope.origin}|${this.config.obfuscationPrefix ?? ''}|fortress-web-key`;
    const saltSource = `${scope.hostname}|fortress-salt-v1`;
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

  private saveRuntimeConfigOverrides(config: FortressConfig): void {
    const overrides = this.sanitizeRuntimeConfigOverrides(config as Record<string, unknown>);
    const payload = {
      version: 1,
      updatedAt: Date.now(),
      overrides,
    };

    this.storeSet(FortressWeb.RUNTIME_CONFIG_KEY, JSON.stringify(payload));
  }

  private loadPersistedRuntimeConfig(): FortressConfig {
    let rawPayload: string | null;
    try {
      rawPayload = typeof localStorage === 'undefined' ? null : localStorage.getItem(FortressWeb.RUNTIME_CONFIG_KEY);
    } catch {
      rawPayload = null;
    }
    if (rawPayload === null) {
      return {};
    }

    try {
      const payload = JSON.parse(rawPayload) as {
        version?: number;
        overrides?: Record<string, unknown>;
      };

      if (payload.version !== 1 || payload.overrides === undefined) {
        return {};
      }

      return this.sanitizeRuntimeConfigOverrides(payload.overrides);
    } catch {
      return {};
    }
  }

  private sanitizeRuntimeConfigOverrides(overrides: Record<string, unknown>): FortressConfig {
    const sanitized: FortressConfig = {};
    const allowedLogLevels = new Set(['error', 'warn', 'info', 'debug', 'verbose']);
    const allowedOverlayThemes = new Set(['system', 'light', 'dark']);
    const allowedFallbackStrategies = new Set(['deviceCredential', 'none', 'systemDefault']);
    const allowedEncryptionAlgorithms = new Set(['AES-GCM', 'AES-CBC']);

    if (typeof overrides.verboseLogging === 'boolean') sanitized.verboseLogging = overrides.verboseLogging;
    if (typeof overrides.logLevel === 'string' && allowedLogLevels.has(overrides.logLevel)) {
      sanitized.logLevel = overrides.logLevel as FortressConfig['logLevel'];
    }
    if (
      typeof overrides.lockAfterMs === 'number' &&
      Number.isInteger(overrides.lockAfterMs) &&
      overrides.lockAfterMs >= 0
    ) {
      sanitized.lockAfterMs = overrides.lockAfterMs;
    }
    if (typeof overrides.enablePrivacyScreen === 'boolean')
      sanitized.enablePrivacyScreen = overrides.enablePrivacyScreen;
    if (typeof overrides.obfuscateKeys === 'boolean') {
      sanitized.obfuscateKeys = overrides.obfuscateKeys;
    }
    if (typeof overrides.obfuscationPrefix === 'string' && overrides.obfuscationPrefix.length > 0) {
      sanitized.obfuscationPrefix = overrides.obfuscationPrefix;
    }
    if (typeof overrides.privacyOverlayText === 'string') sanitized.privacyOverlayText = overrides.privacyOverlayText;
    if (typeof overrides.privacyOverlayImageName === 'string') {
      sanitized.privacyOverlayImageName = overrides.privacyOverlayImageName;
    }
    if (typeof overrides.privacyOverlayShowText === 'boolean') {
      sanitized.privacyOverlayShowText = overrides.privacyOverlayShowText;
    }
    if (typeof overrides.privacyOverlayShowImage === 'boolean') {
      sanitized.privacyOverlayShowImage = overrides.privacyOverlayShowImage;
    }
    if (typeof overrides.privacyOverlayTextColor === 'string') {
      sanitized.privacyOverlayTextColor = overrides.privacyOverlayTextColor;
    }
    if (
      typeof overrides.privacyOverlayBackgroundOpacity === 'number' &&
      (overrides.privacyOverlayBackgroundOpacity === -1 ||
        (overrides.privacyOverlayBackgroundOpacity >= 0 && overrides.privacyOverlayBackgroundOpacity <= 1))
    ) {
      sanitized.privacyOverlayBackgroundOpacity = overrides.privacyOverlayBackgroundOpacity;
    }
    if (typeof overrides.privacyOverlayTheme === 'string' && allowedOverlayThemes.has(overrides.privacyOverlayTheme)) {
      sanitized.privacyOverlayTheme = overrides.privacyOverlayTheme as FortressConfig['privacyOverlayTheme'];
    }
    if (typeof overrides.fallbackStrategy === 'string' && allowedFallbackStrategies.has(overrides.fallbackStrategy)) {
      sanitized.fallbackStrategy = overrides.fallbackStrategy as FortressConfig['fallbackStrategy'];
    }
    if (typeof overrides.allowCachedAuthentication === 'boolean') {
      sanitized.allowCachedAuthentication = overrides.allowCachedAuthentication;
    }
    if (
      typeof overrides.cachedAuthenticationTimeoutMs === 'number' &&
      Number.isInteger(overrides.cachedAuthenticationTimeoutMs) &&
      overrides.cachedAuthenticationTimeoutMs >= 0
    ) {
      sanitized.cachedAuthenticationTimeoutMs = overrides.cachedAuthenticationTimeoutMs;
    }
    if (
      typeof overrides.maxBiometricAttempts === 'number' &&
      Number.isInteger(overrides.maxBiometricAttempts) &&
      overrides.maxBiometricAttempts >= 1
    ) {
      sanitized.maxBiometricAttempts = overrides.maxBiometricAttempts;
    }
    if (
      typeof overrides.lockoutDurationMs === 'number' &&
      Number.isInteger(overrides.lockoutDurationMs) &&
      overrides.lockoutDurationMs >= 0
    ) {
      sanitized.lockoutDurationMs = overrides.lockoutDurationMs;
    }
    if (
      typeof overrides.requireFreshAuthenticationMs === 'number' &&
      Number.isInteger(overrides.requireFreshAuthenticationMs) &&
      overrides.requireFreshAuthenticationMs >= 0
    ) {
      sanitized.requireFreshAuthenticationMs = overrides.requireFreshAuthenticationMs;
    }
    if (
      typeof overrides.encryptionAlgorithm === 'string' &&
      allowedEncryptionAlgorithms.has(overrides.encryptionAlgorithm)
    ) {
      sanitized.encryptionAlgorithm = overrides.encryptionAlgorithm as FortressConfig['encryptionAlgorithm'];
    }
    if (typeof overrides.persistSessionState === 'boolean') {
      sanitized.persistSessionState = overrides.persistSessionState;
    }

    return sanitized;
  }

  /**
   * Saves web session state to localStorage when persistence is enabled.
   * Removes persisted payload when persistence is disabled.
   */
  private saveSession(): void {
    if (this.config.persistSessionState !== true) {
      this.storeRemove(FortressWeb.SESSION_KEY);
      return;
    }

    const persistedState: PersistedSessionState = {
      persistSessionState: true,
      isLocked: this.session.isLocked,
      lastActiveAt: this.session.lastActiveAt,
      lastSuccessfulAuthAt: this.lastSuccessfulAuthAt,
      vaultState: this.vaultState,
    };

    this.storeSet(FortressWeb.SESSION_KEY, JSON.stringify(persistedState));
  }

  /**
   * Restores persisted web session state from localStorage.
   *
   * Returns `true` only when a valid, opt-in payload is restored.
   */
  private restorePersistedSession(): boolean {
    let stored: string | null;
    try {
      stored = typeof localStorage === 'undefined' ? null : localStorage.getItem(FortressWeb.SESSION_KEY);
    } catch {
      stored = null;
    }
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

    if (
      typeof PublicKeyCredential === 'undefined' ||
      typeof (globalThis as { navigator?: { credentials?: unknown } }).navigator?.credentials === 'undefined'
    ) {
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
    const baseIdentifier = `${this.webCryptoScope().origin}|${navigator.userAgent}`;
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
    let rawState: string | null;
    try {
      rawState = typeof localStorage === 'undefined' ? null : localStorage.getItem(FortressWeb.WEBAUTHN_STATE_KEY);
    } catch {
      rawState = null;
    }

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
    this.storeSet(FortressWeb.WEBAUTHN_STATE_KEY, JSON.stringify(state));
  }

  private createInitialWebAuthnState(): WebAuthnState {
    return {
      credentialIds: [],
      userId: this.arrayBufferToBase64Url(this.createRandomChallenge()),
      rpId: this.webCryptoScope().hostname,
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
