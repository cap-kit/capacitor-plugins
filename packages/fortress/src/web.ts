import { WebPlugin } from '@capacitor/core';

import {
  FortressErrorCode,
  FortressConfig,
  FortressPlugin,
  FortressSession,
  HasKeyOptions,
  HasKeyResult,
  ObfuscatedKeyResult,
  PluginVersionResult,
  SecureValue,
  ValueResult,
} from './definitions';
import { PLUGIN_VERSION } from './version';

/**
 * Web implementation of the Fortress plugin.
 *
 * This implementation provides a best-effort secure storage on the web
 * using localStorage with base64 encoding. Note that web storage
 * is NOT hardware-backed and should not be used for highly sensitive data.
 *
 * Security limitations on Web:
 * - No hardware-backed encryption (Secure Enclave/Keystore)
 * - Data is stored in plain localStorage (encoded, not encrypted)
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

  // -----------------------------------------------------------------------------
  // State
  // -----------------------------------------------------------------------------

  private config: FortressConfig = {};
  private session: FortressSession = {
    isLocked: false,
    lastActiveAt: Date.now(),
  };

  // -----------------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------------

  constructor() {
    super();
    this.loadSession();
  }

  // -----------------------------------------------------------------------------
  // Configuration
  // -----------------------------------------------------------------------------

  async configure(config: FortressConfig): Promise<void> {
    this.config = config;

    if (config.lockAfterMs !== undefined && config.lockAfterMs > 0) {
      this.startAutoLockTimer(config.lockAfterMs);
    }
  }

  // -----------------------------------------------------------------------------
  // Secure Storage (Base64 encoded localStorage)
  // -----------------------------------------------------------------------------

  async setValue(value: SecureValue): Promise<void> {
    const encodedKey = this.encodeKey(value.key);
    const encodedValue = this.encodeValue(value.value);
    localStorage.setItem(encodedKey, encodedValue);
    this.touchSession();
  }

  async getValue(key: { key: string }): Promise<ValueResult> {
    const encodedKey = this.encodeKey(key.key);
    const stored = localStorage.getItem(encodedKey);

    if (stored === null) {
      return { value: null };
    }

    try {
      const decoded = this.decodeValue(stored);
      this.touchSession();
      return { value: decoded };
    } catch {
      return { value: null };
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

  async unlock(): Promise<void> {
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

      this.session.isLocked = false;
      this.session.lastActiveAt = Date.now();
      this.saveSession();
      this.notifyListeners('sessionUnlocked', {});
    } catch (error) {
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
    this.session.isLocked = true;
    this.saveSession();

    // Emit lock event
    this.notifyListeners('sessionLocked', {});
  }

  async isLocked(): Promise<{ isLocked: boolean }> {
    return { isLocked: this.session.isLocked };
  }

  async getSession(): Promise<FortressSession> {
    return { ...this.session };
  }

  async resetSession(): Promise<void> {
    this.session.isLocked = true;
    this.session.lastActiveAt = 0;
    this.saveSession();

    this.notifyListeners('sessionLocked', {});
  }

  async touchSession(): Promise<void> {
    if (!this.session.isLocked) {
      this.session.lastActiveAt = Date.now();
      this.saveSession();
    }
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

  /**
   * Encodes a value for secure storage.
   * Uses base64 encoding.
   */
  private encodeValue(value: string): string {
    return btoa(unescape(encodeURIComponent(value)));
  }

  /**
   * Decodes a value from secure storage.
   */
  private decodeValue(encoded: string): string {
    return decodeURIComponent(escape(atob(encoded)));
  }

  /**
   * Obfuscates a key for insecure storage.
   * Simple XOR-like transformation with prefix.
   */
  private obfuscateKey(key: string): string {
    const prefix = this.config.obfuscationPrefix ?? 'ftrss_';
    return `${prefix}${btoa(key)}`;
  }

  // -----------------------------------------------------------------------------
  // Session Persistence
  // -----------------------------------------------------------------------------

  /**
   * Loads session state from localStorage.
   * Note: Lock state resets on page reload for security.
   */
  private loadSession(): void {
    const stored = localStorage.getItem(FortressWeb.SESSION_KEY);

    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        this.session.lastActiveAt = parsed.lastActiveAt ?? Date.now();

        // Always start locked on web for security
        this.session.isLocked = true;
      } catch {
        this.session = { isLocked: true, lastActiveAt: Date.now() };
      }
    } else {
      this.session = { isLocked: true, lastActiveAt: Date.now() };
    }
  }

  /**
   * Saves session state to localStorage.
   */
  private saveSession(): void {
    localStorage.setItem(
      FortressWeb.SESSION_KEY,
      JSON.stringify({
        lastActiveAt: this.session.lastActiveAt,
      }),
    );
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
        pubKeyCredParams: [
          { type: 'public-key', alg: -7 },
          { type: 'public-key', alg: -257 },
        ],
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
        pubKeyCredParams: [
          { type: 'public-key', alg: -7 },
          { type: 'public-key', alg: -257 },
        ],
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

class FortressWebError extends Error {
  constructor(
    readonly code: FortressErrorCode,
    message: string,
  ) {
    super(message);
    this.name = 'FortressWebError';
  }
}
