import { WebPlugin } from '@capacitor/core';

import type {
  AuthenticateOptions,
  AuthenticationTypeResult,
  BiometricStrengthResult,
  BiometricTypeResult,
  BiometricTypesResult,
  BiometricsPlugin,
  DeleteSecureDataOptions,
  GetSecureDataOptions,
  HasDeviceCredentialResult,
  IsAvailableResult,
  IsDeviceSecureResult,
  IsEnrolledResult,
  IsSecureDataSavedOptions,
  IsSecureDataSavedResult,
  PluginVersionResult,
  SecureDataResult,
  SetSecureDataOptions,
} from './definitions.js';
import { PLUGIN_VERSION } from './version.js';

/**
 * Web implementation of the Biometrics plugin.
 *
 * Browsers expose biometric ceremonies through WebAuthn: `authenticate()`
 * performs a platform-authenticator assertion against a locally created
 * credential. Availability reflects
 * `PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()`.
 * `enroll()` is unavailable on Web. The biometric-bound secure store
 * (`setSecureData()` and friends) is unavailable on Web too: it has
 * no WebAuthn equivalent.
 */
export class BiometricsWeb extends WebPlugin implements BiometricsPlugin {
  private static readonly WEBAUTHN_TIMEOUT_MS = 60_000;
  private static readonly WEBAUTHN_STATE_KEY = 'biometrics_webauthn_state';

  constructor() {
    super();
  }

  async authenticate(_options?: AuthenticateOptions): Promise<void> {
    if (!globalThis.isSecureContext) {
      return Promise.reject(new Error('Biometrics unavailable: insecure context.'));
    }
    if (typeof PublicKeyCredential === 'undefined' || typeof navigator.credentials === 'undefined') {
      return Promise.reject(new Error('Biometrics unavailable on this device.'));
    }

    try {
      const state = this.readWebAuthnState();
      const allowCredentials = state.credentialIds.map((id) => ({
        id: this.base64UrlToArrayBuffer(id),
        type: 'public-key' as const,
      }));
      const challenge = new Uint8Array(32);
      crypto.getRandomValues(challenge);

      // Ensure a local credential exists so the ceremony can complete.
      const credentialIds = allowCredentials.length > 0 ? allowCredentials : await this.createLocalCredential(state);

      const credential = await navigator.credentials.get({
        publicKey: {
          challenge: challenge.buffer as ArrayBuffer,
          timeout: BiometricsWeb.WEBAUTHN_TIMEOUT_MS,
          userVerification: 'preferred',
          allowCredentials: credentialIds,
        },
      });

      if (!(credential instanceof PublicKeyCredential)) {
        return Promise.reject(new Error('Biometric authentication failed.'));
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === 'NotAllowedError') {
        return Promise.reject(new Error('Biometric authentication was cancelled.'));
      }
      return Promise.reject(error instanceof Error ? error : new Error('Biometric authentication failed.'));
    }
  }

  async cancelAuthentication(): Promise<void> {
    // Web has no cancellable system prompt; nothing to do.
  }

  async isAvailable(): Promise<IsAvailableResult> {
    return { isAvailable: await this.platformAuthenticatorAvailable() };
  }

  async isEnrolled(): Promise<IsEnrolledResult> {
    const available = await this.platformAuthenticatorAvailable();
    return { isEnrolled: available && this.readWebAuthnState().credentialIds.length > 0 };
  }

  async getBiometricType(): Promise<BiometricTypeResult> {
    return { biometryType: (await this.platformAuthenticatorAvailable()) ? 'fingerprint' : 'none' };
  }

  async getBiometricTypes(): Promise<BiometricTypesResult> {
    return {
      biometryTypes: (await this.platformAuthenticatorAvailable()) ? ['fingerprint'] : [],
    };
  }

  async hasDeviceCredential(): Promise<HasDeviceCredentialResult> {
    return { hasDeviceCredential: globalThis.isSecureContext === true };
  }

  async isDeviceSecure(): Promise<IsDeviceSecureResult> {
    return { isDeviceSecure: globalThis.isSecureContext === true };
  }

  async getBiometricStrengthLevel(): Promise<BiometricStrengthResult> {
    return { strengthLevel: (await this.platformAuthenticatorAvailable()) ? 'strong' : 'none' };
  }

  async getAuthenticationType(): Promise<AuthenticationTypeResult> {
    return { authenticationType: 'unknown' };
  }

  async enroll(): Promise<void> {
    return Promise.reject(new Error('Enrollment is not available on Web.'));
  }

  async setSecureData(_options: SetSecureDataOptions): Promise<void> {
    return Promise.reject(new Error('Secure storage is not available on Web.'));
  }

  async getSecureData(_options: GetSecureDataOptions): Promise<SecureDataResult> {
    return Promise.reject(new Error('Secure storage is not available on Web.'));
  }

  async deleteSecureData(_options: DeleteSecureDataOptions): Promise<void> {
    return Promise.reject(new Error('Secure storage is not available on Web.'));
  }

  async isSecureDataSaved(_options: IsSecureDataSavedOptions): Promise<IsSecureDataSavedResult> {
    return Promise.reject(new Error('Secure storage is not available on Web.'));
  }

  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: PLUGIN_VERSION };
  }

  private async platformAuthenticatorAvailable(): Promise<boolean> {
    try {
      if (
        !globalThis.isSecureContext ||
        typeof window.PublicKeyCredential === 'undefined' ||
        typeof window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable !== 'function'
      ) {
        return false;
      }
      return await window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
    } catch {
      return false;
    }
  }

  private readWebAuthnState(): { credentialIds: string[] } {
    try {
      const raw = localStorage.getItem(BiometricsWeb.WEBAUTHN_STATE_KEY);
      if (raw === null) {
        return { credentialIds: [] };
      }
      const parsed = JSON.parse(raw) as Partial<{ credentialIds: unknown }>;
      const ids = Array.isArray(parsed.credentialIds)
        ? parsed.credentialIds.filter((id): id is string => typeof id === 'string')
        : [];
      return { credentialIds: ids };
    } catch {
      return { credentialIds: [] };
    }
  }

  private async createLocalCredential(state: {
    credentialIds: string[];
  }): Promise<{ id: ArrayBuffer; type: 'public-key' }[]> {
    const challenge = new Uint8Array(32);
    crypto.getRandomValues(challenge);
    const userId = new Uint8Array(32);
    crypto.getRandomValues(userId);

    const created = await navigator.credentials.create({
      publicKey: {
        challenge: challenge.buffer as ArrayBuffer,
        rp: { name: 'Biometrics', id: globalThis.location.hostname },
        user: {
          id: userId.buffer as ArrayBuffer,
          name: 'biometrics-user',
          displayName: 'Biometrics User',
        },
        pubKeyCredParams: [
          { type: 'public-key', alg: -7 },
          { type: 'public-key', alg: -257 },
        ],
        authenticatorSelection: { userVerification: 'preferred', residentKey: 'preferred' },
        timeout: BiometricsWeb.WEBAUTHN_TIMEOUT_MS,
        attestation: 'none',
      },
    });

    if (!(created instanceof PublicKeyCredential)) {
      return Promise.reject(new Error('Biometric authentication failed.'));
    }

    const credentialId = this.arrayBufferToBase64Url(created.rawId);
    const next = { credentialIds: [...state.credentialIds, credentialId] };
    localStorage.setItem(BiometricsWeb.WEBAUTHN_STATE_KEY, JSON.stringify(next));
    return [{ id: created.rawId as ArrayBuffer, type: 'public-key' }];
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
}
