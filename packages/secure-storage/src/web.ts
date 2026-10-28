import { WebPlugin } from '@capacitor/core';

import type {
  DefaultAccessOptions,
  GetManyOptions,
  GetManyResult,
  GetResult,
  HasResult,
  KeyOptions,
  KeysResult,
  PluginVersionResult,
  RemoveResult,
  ConfigureOptions,
  SecureStoragePlugin,
  SetOptions,
  SynchronizeOptions,
  SynchronizeResult,
} from './definitions.js';
import { PLUGIN_VERSION } from './version.js';

/**
 * Minimal async key/value backend behind the web persistence layer.
 */
interface WebKvBackend {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
  keys(): Promise<string[]>;
}

/**
 * Web implementation of the SecureStorage plugin.
 *
 * Browsers offer no hardware-backed storage, so values are encrypted with
 * AES-GCM under an origin-bound software key (PBKDF2, 150k iterations) and
 * persisted through localforage (IndexedDB first, LocalStorage fallback,
 * in-memory last resort) — strictly better than the plaintext
 * `localStorage` the community plugins settle for, and good enough for
 * development. Do not treat it as hardware-grade: use native builds for
 * production secrets.
 */
export class SecureStorageWeb extends WebPlugin implements SecureStoragePlugin {
  private static readonly SCHEMA_VERSION = 1;
  private static readonly KEY_TTL_MS = 5 * 60_000;
  private static readonly PREFIX = 'securestorage_encrypted_';

  private keyCache: { key: CryptoKey; expiresAt: number } | null = null;
  private insecureGroup = 'CapacitorStorage';
  private obfuscateKeys = false;
  private obfuscationPrefix = '';
  private knownPrefixes = new Set<string>(['']);
  private storeBackend: Promise<WebKvBackend> | null = null;
  private storeWrites: Promise<void> = Promise.resolve();

  constructor() {
    super();
    void this.backend();
  }

  async set(options: SetOptions): Promise<void> {
    this.assertCryptoAvailable();
    this.storeSet(this.encodeName(options.key), await this.encryptValue(options.value));
  }

  async get(options: KeyOptions): Promise<GetResult> {
    const stored = await this.readFirst(this.nameCandidates(options.key));
    if (stored === null) {
      return { value: null };
    }
    try {
      return { value: await this.decryptValue(stored) };
    } catch {
      return Promise.reject(new Error('Stored value cannot be decrypted.'));
    }
  }

  async remove(options: KeyOptions): Promise<RemoveResult> {
    const names = this.nameCandidates(options.key);
    let existed = false;
    for (const name of names) {
      if ((await this.storeGet(name)) !== null) {
        existed = true;
      }
      this.storeRemove(name);
    }
    return { existed };
  }

  async has(options: KeyOptions): Promise<HasResult> {
    return { exists: (await this.readFirst(this.nameCandidates(options.key))) !== null };
  }

  async keys(): Promise<KeysResult> {
    const keys: string[] = [];
    for (const stored of await this.storeKeys()) {
      if (stored.startsWith(SecureStorageWeb.PREFIX)) {
        keys.push(this.decodeName(stored.slice(SecureStorageWeb.PREFIX.length)));
      }
    }
    return { keys: [...new Set(keys)] };
  }

  async getMany(options: GetManyOptions): Promise<GetManyResult> {
    const values: Record<string, string | null> = {};
    for (const key of options.keys) {
      values[key] = (await this.get({ key })).value;
    }
    return { values };
  }

  async clear(): Promise<void> {
    const doomed = (await this.storeKeys()).filter((key) =>
      key.startsWith(SecureStorageWeb.PREFIX),
    );
    for (const key of doomed) {
      this.storeRemove(key);
    }
    this.keyCache = null;
  }

  async configure(options: ConfigureOptions): Promise<void> {
    if (options.group !== undefined && options.group.length > 0) {
      this.insecureGroup = options.group;
    }
    if (options.obfuscateKeys !== undefined) {
      this.obfuscateKeys = options.obfuscateKeys;
    }
    if (options.obfuscationPrefix !== undefined) {
      this.obfuscationPrefix = options.obfuscationPrefix;
      if (options.obfuscationPrefix.length > 0) {
        this.knownPrefixes.add(options.obfuscationPrefix);
      }
    }
  }

  async setInsecure(options: SetOptions): Promise<void> {
    this.storeSet(this.insecureKey(options.key), options.value);
  }

  async getInsecure(options: KeyOptions): Promise<GetResult> {
    return { value: await this.readFirst(this.insecureCandidates(options.key)) };
  }

  async removeInsecure(options: KeyOptions): Promise<RemoveResult> {
    const names = this.insecureCandidates(options.key);
    let existed = false;
    for (const name of names) {
      if ((await this.storeGet(name)) !== null) {
        existed = true;
      }
      this.storeRemove(name);
    }
    return { existed };
  }

  async hasInsecure(options: KeyOptions): Promise<HasResult> {
    return { exists: (await this.readFirst(this.insecureCandidates(options.key))) !== null };
  }

  async keysInsecure(): Promise<KeysResult> {
    const prefix = this.insecurePrefix();
    const keys: string[] = [];
    for (const stored of await this.storeKeys()) {
      if (stored.startsWith(prefix)) {
        keys.push(this.decodeName(stored.slice(prefix.length)));
      }
    }
    return { keys: [...new Set(keys)] };
  }

  async clearInsecure(): Promise<void> {
    const prefix = this.insecurePrefix();
    for (const stored of await this.storeKeys()) {
      if (stored.startsWith(prefix)) {
        this.storeRemove(stored);
      }
    }
  }

  private insecurePrefix(): string {
    return `insecure_${this.insecureGroup}_`;
  }

  private insecureKey(key: string): string {
    return `${this.insecurePrefix()}${this.encodeName(key)}`;
  }

  private insecureCandidates(key: string): string[] {
    const bodies = this.obfuscateKeys ? [this.encodeB64(key), key] : [key, this.encodeB64(key)];
    const names: string[] = [];
    for (const body of bodies) {
      names.push(`${this.insecurePrefix()}${this.obfuscationPrefix}${body}`);
    }
    for (const prefix of this.prefixVariants()) {
      if (prefix === this.obfuscationPrefix) {
        continue;
      }
      for (const body of bodies) {
        names.push(`${this.insecurePrefix()}${prefix}${body}`);
      }
    }
    return names;
  }

  /**
   * Encodes a key name for storage: engine scope + optional prefix plus
   * optional base64. Obfuscation hides names from casual inspection; it
   * is not encryption.
   */
  private encodeName(key: string): string {
    const body = this.obfuscateKeys ? this.encodeB64(key) : key;
    return `${SecureStorageWeb.PREFIX}${this.obfuscationPrefix}${body}`;
  }

  private prefixVariants(): string[] {
    const ordered = [this.obfuscationPrefix];
    for (const seen of this.knownPrefixes) {
      if (seen !== this.obfuscationPrefix) {
        ordered.push(seen);
      }
    }
    return ordered;
  }

  /**
   * Candidate stored names, primary first, across known prefix variants.
   * Reads try each in order so toggling the flag or changing the prefix
   * never orphans existing entries.
   */
  private nameCandidates(key: string): string[] {
    const bodies = this.obfuscateKeys
      ? [this.encodeB64(key), key]
      : [key, this.encodeB64(key)];
    const names: string[] = [];
    for (const body of bodies) {
      names.push(`${SecureStorageWeb.PREFIX}${this.obfuscationPrefix}${body}`);
    }
    for (const prefix of this.prefixVariants()) {
      if (prefix === this.obfuscationPrefix) {
        continue;
      }
      for (const body of bodies) {
        names.push(`${SecureStorageWeb.PREFIX}${prefix}${body}`);
      }
    }
    return names;
  }

  private async readFirst(names: string[]): Promise<string | null> {
    for (const name of names) {
      const value = await this.storeGet(name);
      if (value !== null) {
        return value;
      }
    }
    return null;
  }

  /**
   * Best-effort inverse of the name encoding: strips the prefix, then
   * base64-decodes only when the value round-trips cleanly.
   */
  private decodeName(stored: string): string {
    const remainders = [stored];
    for (const prefix of this.prefixVariants()) {
      if (prefix.length > 0 && stored.startsWith(prefix)) {
        remainders.push(stored.slice(prefix.length));
      }
    }
    for (const remainder of remainders) {
      const decoded = this.decodeB64(remainder);
      if (decoded !== null) {
        return decoded;
      }
    }
    const current = this.obfuscationPrefix;
    return current.length > 0 && stored.startsWith(current)
      ? stored.slice(current.length)
      : stored;
  }

  private encodeB64(value: string): string {
    const bytes = new TextEncoder().encode(value);
    let binary = '';
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }

  private decodeB64(value: string): string | null {
    try {
      const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      if (!/^[A-Za-z0-9+/=]*$/.test(padded)) {
        return null;
      }
      const binary = atob(padded);
      const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
      const decoded = new TextDecoder().decode(bytes);
      return this.encodeB64(decoded) === value ? decoded : null;
    } catch {
      return null;
    }
  }

  async setSynchronize(_options: SynchronizeOptions): Promise<void> {
    // No iCloud Keychain on Web; no-op for API parity.
  }

  async getSynchronize(): Promise<SynchronizeResult> {
    return { synchronize: false };
  }

  async setDefaultKeychainAccess(_options: DefaultAccessOptions): Promise<void> {
    // No Keychain on Web; no-op for API parity.
  }

  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: PLUGIN_VERSION };
  }

  // ---------------------------------------------------------------------------
  // Persistent store (localforage engine chain + memory fallback)
  // ---------------------------------------------------------------------------

  private backend(): Promise<WebKvBackend> {
    if (this.storeBackend === null) {
      this.storeBackend = this.initBackend();
    }
    return this.storeBackend;
  }

  private async initBackend(): Promise<WebKvBackend> {
    try {
      const backend = await this.indexedDbBackend();
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
   * One-time migration of entries written by previous versions straight
   * to LocalStorage. Copies only missing keys; legacy copies stay as a
   * downgrade backup.
   */
  private async importLegacyKeys(backend: WebKvBackend): Promise<void> {
    let existing: Set<string>;
    try {
      existing = new Set(await backend.keys());
    } catch {
      return;
    }
    let legacyKeys: string[] = [];
    try {
      if (typeof localStorage === 'undefined') {
        return;
      }
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key !== null && key.startsWith(SecureStorageWeb.PREFIX)) {
          legacyKeys.push(key);
        }
      }
    } catch {
      return;
    }
    for (const key of legacyKeys) {
      if (existing.has(key)) {
        continue;
      }
      let legacy: string | null = null;
      try {
        legacy = localStorage.getItem(key);
      } catch {
        legacy = null;
      }
      if (legacy !== null) {
        try {
          await backend.setItem(key, legacy);
        } catch {
          // Best effort.
        }
      }
    }
  }

  /**
   * IndexedDB backend probed before adoption with a full
   * write/read/delete round-trip. Connections are short-lived per
   * operation: no retained handles, no version-change deadlocks.
   */
  private async indexedDbBackend(): Promise<WebKvBackend> {
    if (typeof indexedDB === 'undefined') {
      throw new Error('IndexedDB unavailable.');
    }

    const openDb = (): Promise<IDBDatabase> =>
      new Promise((resolve, reject) => {
        const request = indexedDB.open('securestorage', 1);
        request.onupgradeneeded = () => {
          const db = request.result;
          if (!db.objectStoreNames.contains('keyvalue')) {
            db.createObjectStore('keyvalue');
          }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error ?? new Error('IndexedDB open failed.'));
      });

    const withStore = <T>(
      mode: IDBTransactionMode,
      task: (store: IDBObjectStore) => IDBRequest<T>,
    ): Promise<T> =>
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
      setItem: (key, value) =>
        withStore('readwrite', (store) => store.put(value, key)).then(() => undefined),
      removeItem: (key) =>
        withStore('readwrite', (store) => store.delete(key)).then(() => undefined),
      keys: () =>
        withStore('readonly', (store) => store.getAllKeys()).then((keys) => keys.map(String)),
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
      getItem: (key) => {
        try {
          return Promise.resolve(localStorage.getItem(key));
        } catch {
          return Promise.resolve(null);
        }
      },
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

  private async storeGet(key: string): Promise<string | null> {
    try {
      await this.storeWrites;
      return await (await this.backend()).getItem(key);
    } catch {
      return null;
    }
  }

  private storeSet(key: string, value: string): void {
    this.storeWrites = this.storeWrites
      .then(() => this.backend())
      .then((backend) => backend.setItem(key, value))
      .catch(() => undefined);
  }

  private storeRemove(key: string): void {
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

  // ---------------------------------------------------------------------------
  // WebCrypto envelope (AES-GCM, origin-bound software key)
  // ---------------------------------------------------------------------------

  private assertCryptoAvailable(): void {
    if (typeof crypto === 'undefined' || typeof crypto.subtle === 'undefined') {
      throw new Error('WebCrypto is unavailable.');
    }
  }

  private webCryptoScope(): { origin: string; hostname: string } {
    const location = (globalThis as { location?: { origin?: string; hostname?: string } }).location;
    return {
      origin: location?.origin ?? 'capacitor-local',
      hostname: location?.hostname ?? 'localhost',
    };
  }

  private async getOrCreateKey(): Promise<CryptoKey> {
    const now = Date.now();
    if (this.keyCache !== null && this.keyCache.expiresAt > now) {
      return this.keyCache.key;
    }

    const scope = this.webCryptoScope();
    const passphrase = `${scope.origin}|securestorage-web-key`;
    const salt = `${scope.hostname}|securestorage-salt-v1`;
    const baseKey = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(passphrase),
      'PBKDF2',
      false,
      ['deriveKey'],
    );
    const key = await crypto.subtle.deriveKey(
      { name: 'PBKDF2', salt: new TextEncoder().encode(salt), iterations: 150_000, hash: 'SHA-256' },
      baseKey,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt'],
    );

    this.keyCache = { key, expiresAt: now + SecureStorageWeb.KEY_TTL_MS };
    return key;
  }

  private async encryptValue(value: string): Promise<string> {
    const key = await this.getOrCreateKey();
    const iv = new Uint8Array(12);
    crypto.getRandomValues(iv);
    const ciphertext = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv: iv as BufferSource },
      key,
      new TextEncoder().encode(value),
    );
    return JSON.stringify({
      v: SecureStorageWeb.SCHEMA_VERSION,
      iv: this.bufferToBase64(iv.buffer),
      cipher: this.bufferToBase64(ciphertext),
    });
  }

  private async decryptValue(payloadJson: string): Promise<string> {
    const payload = JSON.parse(payloadJson) as { v?: number; iv?: string; cipher?: string };
    if (payload.v !== SecureStorageWeb.SCHEMA_VERSION || !payload.iv || !payload.cipher) {
      throw new Error('Unsupported payload.');
    }
    const key = await this.getOrCreateKey();
    const plaintext = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: this.base64ToBuffer(payload.iv) as BufferSource },
      key,
      this.base64ToBuffer(payload.cipher),
    );
    return new TextDecoder().decode(plaintext);
  }

  private bufferToBase64(buffer: ArrayBufferLike): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  }

  private base64ToBuffer(base64Value: string): ArrayBuffer {
    const binary = atob(base64Value);
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index);
    }
    return bytes.buffer;
  }
}
