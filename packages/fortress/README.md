<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Fortress</h3>
<p align="center">
  <strong>
    <code>@cap-kit/fortress</code>
  </strong>
</p>

<p align="center">
  <strong>@cap-kit/fortress</strong> is an enterprise-grade Capacitor v8 plugin that unifies<br>
  <strong>hardware-backed secure storage</strong>, <strong>biometric authentication</strong>, 
  <strong>session management</strong>, and <strong>privacy protection</strong> into a single, 
  platform-consistent API.
</p>

<p align="center">Designed for high-security mobile applications, Fortress leverages:</p>

<ul align="center" style="list-style: none; padding: 0;">
  <li>🔐 iOS Secure Enclave</li>
  <li>🔒 Android Keystore + StrongBox</li>
  <li>👆 BiometricPrompt (BIOMETRIC_STRONG)</li>
  <li>⏳ Time-based auto-lock session control</li>
  <li>🛡 Privacy screen protection for task switcher snapshots</li>
</ul>

<p align="center">
  Fortress follows a strict layered architecture (Bridge → Implementation → Config → Utils), ensuring clean separation
  of concerns, predictable behavior, and production-grade reliability.
</p>

---

## Why Fortress?

Fortress is built for applications that require:

- Hardware-backed cryptographic security
- Biometric-bound key pair generation and signing
- StrongBox enforcement (optional strict mode)
- Session-aware secure storage
- Deterministic cross-platform error handling

Unlike simple secure storage wrappers, Fortress provides a complete security container designed for fintech, enterprise, and privacy-sensitive applications.

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/fortress">
    <img src="https://img.shields.io/npm/v/@cap-kit/fortress?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/fortress">
    <img src="https://img.shields.io/npm/dm/@cap-kit/fortress?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/fortress?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Install

```bash
pnpm add @cap-kit/fortress
# or
npm install @cap-kit/fortress
# or
yarn add @cap-kit/fortress
# then run:
npx cap sync
```

## Configuration

Configuration options for the Fortress plugin.

| Prop                 | Type                 | Description                                                                                                                                    | Default            | Since |
| -------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). | <code>false</code> | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Fortress": {
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/fortress" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Fortress: {
      verboseLogging: true,
    },
  },
};

export default config;
```

## Native Requirements

### Android

### iOS

---

## Permissions

### Android

### iOS

---

## API

<docgen-index>

- [`configure(...)`](#configure)
- [`setValue(...)`](#setvalue)
- [`getValue(...)`](#getvalue)
- [`removeValue(...)`](#removevalue)
- [`clearAll()`](#clearall)
- [`unlock()`](#unlock)
- [`lock()`](#lock)
- [`isLocked()`](#islocked)
- [`getSession()`](#getsession)
- [`resetSession()`](#resetsession)
- [`touchSession()`](#touchsession)
- [`setInsecureValue(...)`](#setinsecurevalue)
- [`getInsecureValue(...)`](#getinsecurevalue)
- [`removeInsecureValue(...)`](#removeinsecurevalue)
- [`getObfuscatedKey(...)`](#getobfuscatedkey)
- [`hasKey(...)`](#haskey)
- [`addListener('sessionLocked' | 'sessionUnlocked', ...)`](#addlistenersessionlocked--sessionunlocked-)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Fortress Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

### configure(...)

```typescript
configure(config: FortressConfig) => Promise<void>
```

Applies runtime-safe Fortress configuration values.

Configuration values set here take precedence over
those defined in capacitor.config.ts.

| Param        | Type                                                      | Description                          |
| ------------ | --------------------------------------------------------- | ------------------------------------ |
| **`config`** | <code><a href="#fortressconfig">FortressConfig</a></code> | - The Fortress configuration object. |

**Since:** 8.0.0

#### Example

```ts
await Fortress.configure({
  lockAfterMs: 300000,
  enablePrivacyScreen: true,
});
```

---

### setValue(...)

```typescript
setValue(value: SecureValue) => Promise<void>
```

Stores a secure value in the encrypted vault.

Values are encrypted using hardware-backed security
(Secure Enclave on iOS, Keystore on Android).

| Param       | Type                                                | Description                             |
| ----------- | --------------------------------------------------- | --------------------------------------- |
| **`value`** | <code><a href="#securevalue">SecureValue</a></code> | - The key-value pair to store securely. |

**Since:** 8.0.0

#### Example

```ts
await Fortress.setValue({ key: 'auth_token', value: 'abc123' });
```

---

### getValue(...)

```typescript
getValue(key: { key: string; }) => Promise<ValueResult>
```

Reads a secure value from the encrypted vault.

| Param     | Type                          | Description            |
| --------- | ----------------------------- | ---------------------- |
| **`key`** | <code>{ key: string; }</code> | - The key to retrieve. |

**Returns:** <code>Promise&lt;<a href="#valueresult">ValueResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { value } = await Fortress.getValue({ key: 'auth_token' });
```

---

### removeValue(...)

```typescript
removeValue(key: { key: string; }) => Promise<void>
```

Removes a secure value from the encrypted vault.

| Param     | Type                          | Description          |
| --------- | ----------------------------- | -------------------- |
| **`key`** | <code>{ key: string; }</code> | - The key to remove. |

**Since:** 8.0.0

#### Example

```ts
await Fortress.removeValue({ key: 'auth_token' });
```

---

### clearAll()

```typescript
clearAll() => Promise<void>
```

Clears all secure values from the encrypted vault.

**Since:** 8.0.0

#### Example

```ts
await Fortress.clearAll();
```

---

### unlock()

```typescript
unlock() => Promise<void>
```

Triggers the secure unlock flow using biometrics or device credentials.

This method initiates authentication via Face ID, Touch ID,
or the device passcode as a fallback.

**Since:** 8.0.0

#### Example

```ts
try {
  await Fortress.unlock();
  console.log('Vault unlocked');
} catch (e) {
  console.error('Authentication failed:', e.message);
}
```

---

### lock()

```typescript
lock() => Promise<void>
```

Locks the secure vault immediately.

All stored secure values become inaccessible until
the user authenticates again via unlock().

**Since:** 8.0.0

#### Example

```ts
await Fortress.lock();
```

---

### isLocked()

```typescript
isLocked() => Promise<{ isLocked: boolean; }>
```

Reads the current lock state of the vault.

**Returns:** <code>Promise&lt;{ isLocked: boolean; }&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { isLocked } = await Fortress.isLocked();
```

---

### getSession()

```typescript
getSession() => Promise<FortressSession>
```

Returns the current session state including lock status and activity timestamp.

**Returns:** <code>Promise&lt;<a href="#fortresssession">FortressSession</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const session = await Fortress.getSession();
console.log('Locked:', session.isLocked);
console.log('Last active:', new Date(session.lastActiveAt));
```

---

### resetSession()

```typescript
resetSession() => Promise<void>
```

Resets the session activity state.

This clears the last active timestamp and locks the vault.

**Since:** 8.0.0

#### Example

```ts
await Fortress.resetSession();
```

---

### touchSession()

```typescript
touchSession() => Promise<void>
```

Updates the session activity timestamp to prevent auto-lock.

Use this method to keep the session alive during
active user interaction.

**Since:** 8.0.0

#### Example

```ts
document.addEventListener('click', () => {
  Fortress.touchSession();
});
```

---

### setInsecureValue(...)

```typescript
setInsecureValue(value: SecureValue) => Promise<void>
```

Stores a value in standard (insecure) storage.

This uses SharedPreferences (Android), UserDefaults (iOS),
or localStorage (Web). Use for non-sensitive data only.

| Param       | Type                                                | Description                    |
| ----------- | --------------------------------------------------- | ------------------------------ |
| **`value`** | <code><a href="#securevalue">SecureValue</a></code> | - The key-value pair to store. |

**Since:** 8.0.0

#### Example

```ts
await Fortress.setInsecureValue({ key: 'theme', value: 'dark' });
```

---

### getInsecureValue(...)

```typescript
getInsecureValue(key: { key: string; }) => Promise<ValueResult>
```

Reads a value from standard (insecure) storage.

| Param     | Type                          | Description            |
| --------- | ----------------------------- | ---------------------- |
| **`key`** | <code>{ key: string; }</code> | - The key to retrieve. |

**Returns:** <code>Promise&lt;<a href="#valueresult">ValueResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { value } = await Fortress.getInsecureValue({ key: 'theme' });
```

---

### removeInsecureValue(...)

```typescript
removeInsecureValue(key: { key: string; }) => Promise<void>
```

Removes a value from standard (insecure) storage.

| Param     | Type                          | Description          |
| --------- | ----------------------------- | -------------------- |
| **`key`** | <code>{ key: string; }</code> | - The key to remove. |

**Since:** 8.0.0

#### Example

```ts
await Fortress.removeInsecureValue({ key: 'theme' });
```

---

### getObfuscatedKey(...)

```typescript
getObfuscatedKey(key: { key: string; }) => Promise<ObfuscatedKeyResult>
```

Returns the obfuscated key representation.

Internal utility to mask keys in standard storage.
Useful for consistent key naming across storage tiers.

| Param     | Type                          | Description                      |
| --------- | ----------------------------- | -------------------------------- |
| **`key`** | <code>{ key: string; }</code> | - The original key to obfuscate. |

**Returns:** <code>Promise&lt;<a href="#obfuscatedkeyresult">ObfuscatedKeyResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { obfuscated } = await Fortress.getObfuscatedKey({ key: 'session_token' });
```

---

### hasKey(...)

```typescript
hasKey(options: HasKeyOptions) => Promise<HasKeyResult>
```

Checks whether a key exists in secure or insecure storage.

This is an optimized check that does not retrieve the value,
making it useful for checking session tokens without
triggering decryption.

| Param         | Type                                                    | Description                                        |
| ------------- | ------------------------------------------------------- | -------------------------------------------------- |
| **`options`** | <code><a href="#haskeyoptions">HasKeyOptions</a></code> | - The options containing the key and storage type. |

**Returns:** <code>Promise&lt;<a href="#haskeyresult">HasKeyResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { exists } = await Fortress.hasKey({ key: 'auth_token', secure: true });
```

---

### addListener('sessionLocked' | 'sessionUnlocked', ...)

```typescript
addListener(eventName: 'sessionLocked' | 'sessionUnlocked', listenerFunc: () => void) => Promise<PluginListenerHandle>
```

Adds listeners for lock state change events.

| Param              | Type                                              | Description                                                       |
| ------------------ | ------------------------------------------------- | ----------------------------------------------------------------- |
| **`eventName`**    | <code>'sessionLocked' \| 'sessionUnlocked'</code> | - The event to listen for ('sessionLocked' or 'sessionUnlocked'). |
| **`listenerFunc`** | <code>() =&gt; void</code>                        | - The callback function to execute when the event fires.          |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const handle = await Fortress.addListener('sessionLocked', () => {
  console.log('Vault has been locked');
});

// To remove the listener:
await handle.remove();
```

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { version } = await Fortress.getPluginVersion();
```

---

### Interfaces

#### FortressConfig

Static configuration options for the Fortress plugin.

These values are defined in `capacitor.config.ts` and consumed
exclusively by native code during plugin initialization.

Configuration values:

- do NOT change the JavaScript API shape
- do NOT enable/disable methods
- are applied once during plugin load

| Prop                      | Type                                                                      | Description                                                                                                                                                                                                                              | Default                           | Since |
| ------------------------- | ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- | ----- |
| **`verboseLogging`**      | <code>boolean</code>                                                      | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API. | <code>false</code>                | 8.0.0 |
| **`lockAfterMs`**         | <code>number</code>                                                       | Global auto-lock timeout in milliseconds.                                                                                                                                                                                                | <code>60000</code>                | 8.0.0 |
| **`accessControl`**       | <code><a href="#biometricaccesscontrol">BiometricAccessControl</a></code> | Security level for biometric hardware access.                                                                                                                                                                                            | <code>'biometryCurrentSet'</code> | 8.0.0 |
| **`enablePrivacyScreen`** | <code>boolean</code>                                                      | Enables or disables privacy protection for app snapshots.                                                                                                                                                                                | <code>true</code>                 | 8.0.0 |
| **`obfuscationPrefix`**   | <code>string</code>                                                       | Prefix used by key obfuscation utilities.                                                                                                                                                                                                | <code>'ftrss\_'</code>            | 8.0.0 |
| **`webAuthn`**            | <code><a href="#webauthnconfig">WebAuthnConfig</a></code>                 | WebAuthn configuration for Web platform unlock behavior. - `local` mode stores credential metadata only in browser storage. - `server` mode uses backend challenge and assertion verification endpoints.                                 |                                   | 8.0.0 |

#### WebAuthnConfig

WebAuthn behavior and backend integration options (Web platform only).

| Prop                          | Type                                      | Description                                                                      | Default              | Since |
| ----------------------------- | ----------------------------------------- | -------------------------------------------------------------------------------- | -------------------- | ----- |
| **`mode`**                    | <code>'local' \| 'server'</code>          | WebAuthn operating mode.                                                         | <code>'local'</code> | 8.0.0 |
| **`registrationStartUrl`**    | <code>string</code>                       | HTTP endpoint that starts WebAuthn registration and returns challenge payload.   |                      | 8.0.0 |
| **`registrationFinishUrl`**   | <code>string</code>                       | HTTP endpoint that verifies registration attestation.                            |                      | 8.0.0 |
| **`authenticationStartUrl`**  | <code>string</code>                       | HTTP endpoint that starts WebAuthn authentication and returns challenge payload. |                      | 8.0.0 |
| **`authenticationFinishUrl`** | <code>string</code>                       | HTTP endpoint that verifies authentication assertion.                            |                      | 8.0.0 |
| **`headers`**                 | <code>Record&lt;string, string&gt;</code> | Optional extra headers attached to server WebAuthn requests.                     |                      | 8.0.0 |

#### SecureValue

Generic key/value payload for storage methods.

| Prop        | Type                |
| ----------- | ------------------- |
| **`key`**   | <code>string</code> |
| **`value`** | <code>string</code> |

#### ValueResult

Result returned by secure and insecure read operations.

| Prop        | Type                        |
| ----------- | --------------------------- |
| **`value`** | <code>string \| null</code> |

#### FortressSession

Current session status exposed to JavaScript.

| Prop               | Type                 |
| ------------------ | -------------------- |
| **`isLocked`**     | <code>boolean</code> |
| **`lastActiveAt`** | <code>number</code>  |

#### ObfuscatedKeyResult

Result object used by key obfuscation utility.

| Prop             | Type                |
| ---------------- | ------------------- |
| **`obfuscated`** | <code>string</code> |

#### HasKeyResult

Result object used by key existence checks.

| Prop         | Type                 |
| ------------ | -------------------- |
| **`exists`** | <code>boolean</code> |

#### HasKeyOptions

Input payload for key existence checks.

| Prop         | Type                 |
| ------------ | -------------------- |
| **`key`**    | <code>string</code>  |
| **`secure`** | <code>boolean</code> |

#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

### Type Aliases

#### BiometricAccessControl

Native biometric access control options.

<code>'biometryAny' | 'biometryCurrentSet' | 'passcodeAny' | 'devicePasscode'</code>

</docgen-api>

---

## Error handling

All Fortress plugin methods return Promises and may reject in case of failure.
Consumers should always handle errors using `try / catch`.

### Example

```ts
import { Fortress, FortressErrorCode } from '@cap-kit/fortress';

try {
  await Fortress.xxx();
} catch (err: any) {
  switch (err.code) {
    case FortressErrorCode.UNAVAILABLE:
      // Feature not supported on this device or platform
      break;

    case FortressErrorCode.INIT_FAILED:
      // Native initialization or runtime failure
      break;

    default:
      // Unknown or unexpected error
      console.error(err.message);
  }
}
```

### Error codes

The following error codes may be returned by the plugin:

- `UNAVAILABLE` — The feature is not supported on the current device or platform
- `PERMISSION_DENIED` — A required permission was denied (platform-dependent)
- `INIT_FAILED` — Native initialization or runtime failure
- `UNKNOWN_TYPE` — Invalid or unsupported input

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## License

MIT
