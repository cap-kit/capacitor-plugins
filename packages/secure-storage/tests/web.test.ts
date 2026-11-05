import { afterEach, describe, expect, it, vi } from 'vitest';

import { GetResult, HasResult, KeysResult, RemoveResult } from '../src/definitions.js';
import { SecureStorageWeb } from '../src/web.js';

/**
 * Minimal shape of the resolved persistence backend, exposed here only to
 * let tests assert on raw stored bytes (ciphertext layout, stored names).
 */
interface KvBackend {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  keys(): Promise<string[]>;
}

const backendOf = async (plugin: SecureStorageWeb): Promise<KvBackend> => {
  // Raw-byte assertions must observe the serialized write chain, which the
  // public API awaits internally but a direct backend read does not.
  await (plugin as unknown as { storeWrites: Promise<void> }).storeWrites;
  return (plugin as unknown as { backend(): Promise<KvBackend> }).backend();
};

const SECURE_PREFIX = 'securestorage_encrypted_';
const INSECURE_PREFIX = 'insecure_CapacitorStorage_';

const newPlugin = (): SecureStorageWeb => new SecureStorageWeb();

/** Map-free LocalStorage double honoring the Storage surface the plugin uses. */
function makeLocalStorageStub(): Storage {
  const map = new Map<string, string>();
  return {
    get length(): number {
      return map.size;
    },
    key: (index: number) => [...map.keys()][index] ?? null,
    getItem: (key) => map.get(key) ?? null,
    setItem: (key, value) => {
      map.set(key, String(value));
    },
    removeItem: (key) => {
      map.delete(key);
    },
    clear: () => {
      map.clear();
    },
  };
}

describe('SecureStorageWeb — secure tier (encrypted)', () => {
  it('round-trips values, including unicode', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'user', value: 'secreto con ñ y 🔐' });
    await expect(plugin.get({ key: 'user' })).resolves.toEqual({ value: 'secreto con ñ y 🔐' } satisfies GetResult);
  });

  it('resolves null for a missing key', async () => {
    const plugin = newPlugin();
    await expect(plugin.get({ key: 'missing' })).resolves.toEqual({ value: null } satisfies GetResult);
  });

  it('last write wins for the same key', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'user', value: 'first' });
    await plugin.set({ key: 'user', value: 'second' });
    await expect(plugin.get({ key: 'user' })).resolves.toEqual({ value: 'second' } satisfies GetResult);
    await expect(plugin.keys()).resolves.toEqual({ keys: ['user'] } satisfies KeysResult);
  });

  it('has() reflects existence', async () => {
    const plugin = newPlugin();
    await expect(plugin.has({ key: 'user' })).resolves.toEqual({ exists: false } satisfies HasResult);
    await plugin.set({ key: 'user', value: 'v' });
    await expect(plugin.has({ key: 'user' })).resolves.toEqual({ exists: true } satisfies HasResult);
  });

  it('remove() reports existed and deletes the entry', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'user', value: 'v' });
    await expect(plugin.remove({ key: 'user' })).resolves.toEqual({ existed: true } satisfies RemoveResult);
    await expect(plugin.remove({ key: 'user' })).resolves.toEqual({ existed: false } satisfies RemoveResult);
    await expect(plugin.get({ key: 'user' })).resolves.toEqual({ value: null } satisfies GetResult);
  });

  it('getMany() returns mixed hits and nulls', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'a', value: '1' });
    await plugin.set({ key: 'b', value: '2' });
    await expect(plugin.getMany({ keys: ['a', 'b', 'c'] })).resolves.toEqual({
      values: { a: '1', b: '2', c: null },
    });
  });

  it('clear() removes only secure-tier entries and keeps insecure ones', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'a', value: '1' });
    await plugin.set({ key: 'b', value: '2' });
    await plugin.setInsecure({ key: 'plain', value: '3' });
    await plugin.clear();
    await expect(plugin.get({ key: 'a' })).resolves.toEqual({ value: null } satisfies GetResult);
    await expect(plugin.get({ key: 'b' })).resolves.toEqual({ value: null } satisfies GetResult);
    await expect(plugin.getInsecure({ key: 'plain' })).resolves.toEqual({ value: '3' } satisfies GetResult);
  });

  it('stores ciphertext JSON under the secure prefix, never plaintext', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'user', value: 'plaintext-secret' });
    const store = await backendOf(plugin);
    const raw = await store.getItem(`${SECURE_PREFIX}user`);
    expect(raw).not.toBeNull();
    expect(raw).not.toContain('plaintext-secret');
    const payload = JSON.parse(raw as string) as { v: number; iv: string; cipher: string };
    expect(payload.v).toBe(1);
    expect(payload.iv).toEqual(expect.any(String));
    expect(payload.cipher).toEqual(expect.any(String));
  });

  it('rejects with a clear error when the stored ciphertext cannot be decrypted', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'user', value: 'v' });
    const store = await backendOf(plugin);
    const raw = (await store.getItem(`${SECURE_PREFIX}user`)) as string;
    const payload = JSON.parse(raw) as { v: number; iv: string; cipher: string };
    payload.cipher = `${payload.cipher.slice(0, -4)}AAAA`;
    await store.setItem(`${SECURE_PREFIX}user`, JSON.stringify(payload));
    await expect(plugin.get({ key: 'user' })).rejects.toThrow('Stored value cannot be decrypted.');
  });

  it('rejects when WebCrypto is unavailable', async () => {
    vi.stubGlobal('crypto', undefined);
    const plugin = newPlugin();
    await expect(plugin.set({ key: 'user', value: 'v' })).rejects.toThrow('WebCrypto is unavailable.');
  });
});

describe('SecureStorageWeb — insecure tier (plaintext)', () => {
  it('round-trips values stored in plaintext under the insecure namespace', async () => {
    const plugin = newPlugin();
    await plugin.setInsecure({ key: 'user', value: 'remembered-password' });
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: 'remembered-password' });
    const store = await backendOf(plugin);
    await expect(store.getItem(`${INSECURE_PREFIX}user`)).resolves.toBe('remembered-password');
  });

  it('never writes insecure entries under the secure prefix (8.1.1 write/read asymmetry regression)', async () => {
    const plugin = newPlugin();
    await plugin.setInsecure({ key: 'user', value: 'v' });
    const store = await backendOf(plugin);
    const names = await store.keys();
    const insecureNames = names.filter((name) => name.startsWith(INSECURE_PREFIX));
    expect(insecureNames).toEqual([`${INSECURE_PREFIX}user`]);
    expect(names.some((name) => name.startsWith(SECURE_PREFIX))).toBe(false);
  });

  it('keeps insecure entries readable with obfuscateKeys enabled (8.1.1 regression)', async () => {
    const plugin = newPlugin();
    await plugin.configure({ obfuscateKeys: true });
    await plugin.setInsecure({ key: 'user', value: 'v' });
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
    await expect(plugin.hasInsecure({ key: 'user' })).resolves.toEqual({ exists: true } satisfies HasResult);
    const store = await backendOf(plugin);
    const names = (await store.keys()).filter((name) => name.startsWith(INSECURE_PREFIX));
    expect(names).toHaveLength(1);
    expect(names[0]).not.toContain('user');
  });

  it('keeps insecure entries readable with an obfuscationPrefix set', async () => {
    const plugin = newPlugin();
    await plugin.configure({ obfuscateKeys: true, obfuscationPrefix: 'muface_' });
    await plugin.setInsecure({ key: 'user', value: 'v' });
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
    await expect(plugin.keysInsecure()).resolves.toEqual({ keys: ['user'] } satisfies KeysResult);
  });

  it('reads fall back across obfuscation candidates after a flag toggle', async () => {
    const plugin = newPlugin();
    await plugin.configure({ obfuscateKeys: true });
    await plugin.setInsecure({ key: 'user', value: 'v' });
    await plugin.configure({ obfuscateKeys: false });
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
  });

  it('reads fall back to previously used prefixes after a prefix change', async () => {
    const plugin = newPlugin();
    await plugin.configure({ obfuscationPrefix: 'p1_' });
    await plugin.setInsecure({ key: 'user', value: 'v' });
    await plugin.configure({ obfuscationPrefix: 'p2_' });
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
  });

  it('removeInsecure() reports existed and deletes across candidate names', async () => {
    const plugin = newPlugin();
    await plugin.setInsecure({ key: 'user', value: 'v' });
    await expect(plugin.removeInsecure({ key: 'user' })).resolves.toEqual({ existed: true } satisfies RemoveResult);
    await expect(plugin.removeInsecure({ key: 'user' })).resolves.toEqual({ existed: false } satisfies RemoveResult);
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: null } satisfies GetResult);
  });

  it('keysInsecure() and clearInsecure() are scoped to the insecure namespace', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'secure', value: 'v' });
    await plugin.setInsecure({ key: 'plain', value: 'v' });
    await expect(plugin.keysInsecure()).resolves.toEqual({ keys: ['plain'] } satisfies KeysResult);
    await plugin.clearInsecure();
    await expect(plugin.getInsecure({ key: 'plain' })).resolves.toEqual({ value: null } satisfies GetResult);
    await expect(plugin.get({ key: 'secure' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
  });

  it('keysInsecure() lists raw key names verbatim even when they look like base64 fixed points', async () => {
    const plugin = newPlugin();
    await plugin.setInsecure({ key: 'YWJj', value: 'v' });
    await expect(plugin.keysInsecure()).resolves.toEqual({ keys: ['YWJj'] } satisfies KeysResult);
  });
});

describe('SecureStorageWeb — configuration', () => {
  it('keys() lists raw key names verbatim even when they look like base64 fixed points', async () => {
    const plugin = newPlugin();
    // 'YWJj' decodes to 'abc' and re-encodes to itself: a base64 fixed point
    // the old decodeName() mistakenly treated as an obfuscated name.
    await plugin.set({ key: 'YWJj', value: 'v' });
    await expect(plugin.keys()).resolves.toEqual({ keys: ['YWJj'] } satisfies KeysResult);
    await expect(plugin.get({ key: 'YWJj' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
  });

  it('secure tier: obfuscateKeys encodes stored names while keys() lists logical names', async () => {
    const plugin = newPlugin();
    await plugin.configure({ obfuscateKeys: true });
    await plugin.set({ key: 'user', value: 'v' });
    const store = await backendOf(plugin);
    const names = (await store.keys()).filter((name) => name.startsWith(SECURE_PREFIX));
    // btoa('user') = 'dXNlcg==' -> URL-safe without padding.
    expect(names).toEqual([`${SECURE_PREFIX}dXNlcg`]);
    await expect(plugin.keys()).resolves.toEqual({ keys: ['user'] } satisfies KeysResult);
    await expect(plugin.get({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
  });

  it('secure tier reads fall back across obfuscation candidates after a flag toggle', async () => {
    const plugin = newPlugin();
    await plugin.configure({ obfuscateKeys: true });
    await plugin.set({ key: 'user', value: 'v' });
    await plugin.configure({ obfuscateKeys: false });
    await expect(plugin.get({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
    await expect(plugin.has({ key: 'user' })).resolves.toEqual({ exists: true } satisfies HasResult);
  });

  it('a group change namespaces new insecure writes away from the default group', async () => {
    const plugin = newPlugin();
    await plugin.setInsecure({ key: 'user', value: 'v1' });
    await plugin.configure({ group: 'Other' });
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: null } satisfies GetResult);
    await plugin.setInsecure({ key: 'user', value: 'v2' });
    await expect(plugin.keysInsecure()).resolves.toEqual({ keys: ['user'] } satisfies KeysResult);
  });
});

describe('SecureStorageWeb — crypto envelope', () => {
  it('reuses the derived key within the TTL and re-derives after it expires', async () => {
    vi.useFakeTimers();
    const deriveKey = vi.spyOn(crypto.subtle, 'deriveKey');
    const plugin = newPlugin();
    await plugin.set({ key: 'a', value: '1' });
    await plugin.set({ key: 'b', value: '2' });
    expect(deriveKey).toHaveBeenCalledTimes(1);
    vi.setSystemTime(Date.now() + 5 * 60_000 + 1);
    await plugin.set({ key: 'c', value: '3' });
    expect(deriveKey).toHaveBeenCalledTimes(2);
    await expect(plugin.get({ key: 'a' })).resolves.toEqual({ value: '1' } satisfies GetResult);
    await expect(plugin.get({ key: 'c' })).resolves.toEqual({ value: '3' } satisfies GetResult);
  });
});

describe('SecureStorageWeb — backend fallback chain', () => {
  it('uses LocalStorage when IndexedDB is unavailable and leaves no probe key behind', async () => {
    const storage = makeLocalStorageStub();
    vi.stubGlobal('localStorage', storage);
    const plugin = newPlugin();
    await plugin.setInsecure({ key: 'user', value: 'v' });
    await expect(plugin.getInsecure({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
    expect(storage.getItem(`${INSECURE_PREFIX}user`)).toBe('v');
    const probeKeys = [...Array(storage.length)].map((_, i) => storage.key(i) as string);
    expect(probeKeys.some((key) => key.startsWith('__probe__'))).toBe(false);
  });

  it('falls back to an in-memory backend when no storage engine exists', async () => {
    const plugin = newPlugin();
    await plugin.set({ key: 'user', value: 'v' });
    await expect(plugin.get({ key: 'user' })).resolves.toEqual({ value: 'v' } satisfies GetResult);
    await expect(plugin.keys()).resolves.toEqual({ keys: ['user'] } satisfies KeysResult);
  });
});

describe('SecureStorageWeb — API parity no-ops', () => {
  it('Keychain-oriented APIs resolve without effect on web', async () => {
    const plugin = newPlugin();
    await expect(plugin.setSynchronize({ synchronize: true })).resolves.toBeUndefined();
    await expect(plugin.getSynchronize()).resolves.toEqual({ synchronize: false });
    await expect(plugin.setDefaultKeychainAccess({ access: 'whenUnlockedThisDeviceOnly' })).resolves.toBeUndefined();
    await expect(plugin.getPluginVersion()).resolves.toEqual({ version: expect.any(String) });
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  vi.useRealTimers();
});
