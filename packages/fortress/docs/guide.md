# Fortress — Usage Guide

Practical usage guidance for the Fortress Capacitor plugin: the secure vault,
session and auto-lock control, biometric key pairs and challenge flows,
storage tiers, and native platform requirements.

## Quick start

Install the plugin and sync:

```bash
pnpm add @cap-kit/fortress
# then run:
npx cap sync
```

All Fortress methods return Promises that resolve on success and reject on
failure. Always handle errors with `try / catch`; the error object contains a
machine-readable `code` from `FortressErrorCode` (see the
[API notes and error handling](api-notes.md) guide).

## Secure vault

The vault stores key-value pairs encrypted using hardware-backed security
(Secure Enclave on iOS, Keystore on Android).

Storing a value:

```ts
await Fortress.setValue({ key: 'auth_token', value: 'abc123' });
```

Reading it back:

```ts
const { value } = await Fortress.getValue({ key: 'auth_token' });
```

`setMany()` stores multiple values in a single call:

```ts
await Fortress.setMany({
  values: [
    { key: 'a', value: '1' },
    { key: 'b', value: '2' },
  ],
});
```

Removing data:

```ts
await Fortress.removeValue({ key: 'auth_token' });
await Fortress.clearAll(); // clears every secure value
```

When the vault is locked, vault methods reject with `VAULT_LOCKED`. When
stored data cannot be decrypted or validated, reads reject with
`SECURITY_VIOLATION`.

## Sessions and auto-lock

Fortress tracks a session with a `lastActiveAt` timestamp and locks the vault
after the `lockAfterMs` timeout.

- `unlock()` triggers the interactive unlock flow using Face ID, Touch ID, or
  the device passcode as a fallback.
- `lock()` locks the vault immediately. All stored secure values become
  inaccessible until the user authenticates again via `unlock()`.
- `isLocked()` reads the current lock state.
- `getSession()` returns the session state including lock status and activity
  timestamp.
- `resetSession()` clears the last active timestamp and locks the vault.
- `touchSession()` updates the activity timestamp to prevent auto-lock. Call it
  during active user interaction, for example on click events:

```ts
const { isLocked } = await Fortress.isLocked();
console.log('Locked:', isLocked);

document.addEventListener('click', () => {
  Fortress.touchSession();
});
```

## Biometric keys and signatures

Fortress can generate biometric-bound key pairs in native secure hardware:

- `biometricKeysExist()` — checks whether a biometric key pair already exists.
- `createKeys()` — creates (or replaces) a biometric key pair and returns the
  public key.
- `deleteKeys()` — deletes the biometric key pair if it exists.
- `createSignature()` — creates a biometric-protected cryptographic signature.
  The method requires the vault to be unlocked and an existing biometric key
  pair in native secure hardware.

Signature encoding note:

- iOS/Android return Base64 (standard).
- Web (WebAuthn) returns Base64URL (no padding, `-` and `_`).
  Backend verification must normalize Base64URL to Base64 when verifying
  WebAuthn assertions.

## Challenge-based flows

`registerWithChallenge()` creates or replaces keys and signs a backend
challenge. `authenticateWithChallenge()` signs a backend challenge with
existing biometric keys. Both accept a `challenge` plus an optional prompt
message and prompt options.

`generateChallengePayload()` generates a canonical payload for backend
verification workflows. The payload includes a nonce, a timestamp, and a
non-PII device identifier hash to reduce replay attack risk and keep the
verification format deterministic.

## Storage tiers

- Secure storage: `setValue` / `getValue` / `removeValue` / `setMany` /
  `clearAll`.
- Insecure storage: `setInsecureValue` / `getInsecureValue` /
  `removeInsecureValue` use SharedPreferences (Android), UserDefaults (iOS),
  or localStorage (Web). Use for non-sensitive data only, such as UI
  preferences.
- `getObfuscatedKey()` returns the obfuscated key representation, an internal
  utility to mask keys in standard storage. Useful for consistent key naming
  across storage tiers. The obfuscation prefix is configurable via
  `obfuscationPrefix`.
- `hasKey()` checks whether a key exists in secure or insecure storage without
  retrieving the value, making it useful for checking session tokens without
  triggering decryption.

## Native requirements

### Android

- Android API 24+ (for BiometricPrompt)
- AndroidX Biometric library (included via Gradle)
- StrongBox hardware (optional, for enhanced security)
- Recommended: `compileSdkVersion` 34, `targetSdkVersion` 34

### iOS

- iOS 13.0+
- Xcode 16.0+
- Swift 5.9+

## Web platform

On Web, the unlock flow uses WebAuthn. In `local` mode, credential metadata is
stored only in browser storage. In `server` mode, Fortress uses backend
challenge and assertion verification endpoints. See the
[configuration guide](configuration.md) for the `webAuthn` options and the
[security model](security.md) for platform caveats.

The web secure storage layer encrypts values with the configured
`encryptionAlgorithm` (`AES-GCM` by default). `persistSessionState` persists
web session lock/auth state across page reloads.
