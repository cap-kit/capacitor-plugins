# SecureStorage — Usage Guide

## Quick start

```bash
pnpm add @cap-kit/secure-storage
npx cap sync
```

```ts
import { SecureStorage } from '@cap-kit/secure-storage';

await SecureStorage.set({ key: 'theme', value: 'dark' });
const { value } = await SecureStorage.get({ key: 'theme' });
```

## Basic operations

```ts
// Write (overwrites), read (null when absent)
await SecureStorage.set({ key: 'token', value: 'abc123' });
const { value } = await SecureStorage.get({ key: 'token' });

// Existence without decryption cost
const { exists } = await SecureStorage.has({ key: 'token' });

// Removal reports whether anything was there
const { existed } = await SecureStorage.remove({ key: 'token' });

// Wipe everything (rotates the Android master key)
await SecureStorage.clear();
```

## Enumeration and batch reads

```ts
const { keys } = await SecureStorage.keys();
const { values } = await SecureStorage.getMany({ keys });
// values: Record<string, string | null> — missing keys map to null
```

## iCloud sync and Keychain access (iOS)

```ts
await SecureStorage.setSynchronize({ synchronize: true });
const { synchronize } = await SecureStorage.getSynchronize();

await SecureStorage.setDefaultKeychainAccess({ access: 'afterFirstUnlock' });
await SecureStorage.set({ key: 'pin', value: '1234', access: 'whenUnlocked' });
```

Both are no-ops on Android and Web by design.

## Insecure tier (Preferences-style)

Plaintext storage for non-sensitive data, namespaced by group:

```ts
await SecureStorage.configure({ group: 'ui' });
await SecureStorage.setInsecure({ key: 'theme', value: 'dark' });
const { value } = await SecureStorage.getInsecure({ key: 'theme' });
const { keys } = await SecureStorage.keysInsecure();
await SecureStorage.clearInsecure(); // current group only
```

## Key-name obfuscation

`obfuscateKeys` (default `false`, static or via `configure()`) base64-encodes
stored key names on top of `obfuscationPrefix`, in both tiers and on every
platform. Reads accept both forms and listings decode tolerantly.
Obfuscation is not encryption.

## Web platform

Values are AES-GCM encrypted under an origin-bound software key — better
than the plaintext `localStorage` community plugins settle for, but not
hardware-grade. Fine for development; ship native builds for production
secrets.

## Demo app

A working demo lives in `example-app/static/src/app/secure-storage/`:
key/value CRUD with badges, stored-keys chips with batch read, iCloud
toggle, Keychain access selector, and an event log. Apply it to the
shared demo via `pnpm run app:demo:prepare` from the package root.
