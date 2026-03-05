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

### Runtime Configuration

You can override configuration at runtime using `configure()`:

```ts
import { Fortress } from '@cap-kit/fortress';

// Update session timeout
await Fortress.configure({
  lockAfterMs: 300000, // 5 minutes
});

// Update privacy overlay text
await Fortress.configure({
  privacyOverlayText: 'Session Expired',
});
```

**Note**: Some configuration changes take effect immediately (e.g., privacy overlay), while others apply on the next lifecycle transition (e.g., session timeout).

## Native Requirements

### Android

- Android API 23+ (for BiometricPrompt)
- AndroidX Biometric library (included via Gradle)
- StrongBox hardware (optional, for enhanced security)
- Recommended: `compileSdkVersion` 34, `targetSdkVersion` 34

### iOS

- iOS 13.0+
- Xcode 16.0+
- Swift 5.9+

---

## Permissions

### Android

The plugin automatically adds the following permissions to your app's manifest:

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

### iOS

You must add the following to your `Info.plist`:

```xml
<key>NSFaceIDUsageDescription</key>
<string>Fortress uses Face ID to secure your data.</string>
```

#### Optional Capabilities

For iCloud Keychain sync support, enable the **Keychain Sharing** capability in your Xcode project:

1. Select your app target in Xcode
2. Go to **Signing & Capabilities**
3. Click **+ Capability** → **Keychain Sharing**
4. Add an appropriate Keychain Access Group (optional)

This enables the `enableICloudKeychainSync` configuration option.

---

## API

<docgen-index>

- [`getRuntimeConfig()`](#getruntimeconfig)
- [`configure(...)`](#configure)
- [`setValue(...)`](#setvalue)
- [`getValue(...)`](#getvalue)
- [`setMany(...)`](#setmany)
- [`checkStatus()`](#checkstatus)
- [`setBiometryType(...)`](#setbiometrytype)
- [`setBiometryIsEnrolled(...)`](#setbiometryisenrolled)
- [`setDeviceIsSecure(...)`](#setdeviceissecure)
- [`removeValue(...)`](#removevalue)
- [`clearAll()`](#clearall)
- [`unlock(...)`](#unlock)
- [`lock()`](#lock)
- [`isLocked()`](#islocked)
- [`getSession()`](#getsession)
- [`resetSession()`](#resetsession)
- [`touchSession()`](#touchsession)
- [`biometricKeysExist(...)`](#biometrickeysexist)
- [`createKeys(...)`](#createkeys)
- [`deleteKeys(...)`](#deletekeys)
- [`createSignature(...)`](#createsignature)
- [`registerWithChallenge(...)`](#registerwithchallenge)
- [`authenticateWithChallenge(...)`](#authenticatewithchallenge)
- [`generateChallengePayload(...)`](#generatechallengepayload)
- [`setInsecureValue(...)`](#setinsecurevalue)
- [`getInsecureValue(...)`](#getinsecurevalue)
- [`removeInsecureValue(...)`](#removeinsecurevalue)
- [`getObfuscatedKey(...)`](#getobfuscatedkey)
- [`hasKey(...)`](#haskey)
- [`addListener('sessionLocked' | 'sessionUnlocked', ...)`](#addlistenersessionlocked--sessionunlocked-)
- [`addListener('onSecurityStateChanged', ...)`](#addlisteneronsecuritystatechanged-)
- [`addListener('onLockStatusChanged', ...)`](#addlisteneronlockstatuschanged-)
- [`addListener('onVaultInvalidated', ...)`](#addlisteneronvaultinvalidated-)
- [`addListener('onAppResume', ...)`](#addlisteneronappresume-)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Fortress Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

### getRuntimeConfig()

```typescript
getRuntimeConfig() => Promise<FortressRuntimeConfig>
```

Returns the runtime configuration currently used by Fortress.

**Returns:** <code>Promise&lt;<a href="#fortressruntimeconfig">FortressRuntimeConfig</a>&gt;</code>

**Since:** 8.0.0

---

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

The method rejects with `VAULT_LOCKED` when the vault is locked.

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

The method rejects with:

- `VAULT_LOCKED` when the vault is locked
- `SECURITY_VIOLATION` when stored data cannot be decrypted/validated

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

### setMany(...)

```typescript
setMany(options: { values: SecureValue[]; }) => Promise<void>
```

| Param         | Type                                    |
| ------------- | --------------------------------------- |
| **`options`** | <code>{ values: SecureValue[]; }</code> |

**Since:** 8.0.0

---

### checkStatus()

```typescript
checkStatus() => Promise<DeviceSecurityStatus>
```

**Returns:** <code>Promise&lt;<a href="#devicesecuritystatus">DeviceSecurityStatus</a>&gt;</code>

**Since:** 8.0.0

---

### setBiometryType(...)

```typescript
setBiometryType(options: SetBiometryTypeOptions) => Promise<void>
```

Overrides detected biometry type for development/testing scenarios.

This method is intended for QA and simulator/device mocking flows.

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code><a href="#setbiometrytypeoptions">SetBiometryTypeOptions</a></code> |

**Since:** 8.0.0

---

### setBiometryIsEnrolled(...)

```typescript
setBiometryIsEnrolled(options: SetBiometryIsEnrolledOptions) => Promise<void>
```

Overrides biometrics enrollment state for development/testing scenarios.

| Param         | Type                                                                                  |
| ------------- | ------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#setbiometryisenrolledoptions">SetBiometryIsEnrolledOptions</a></code> |

**Since:** 8.0.0

---

### setDeviceIsSecure(...)

```typescript
setDeviceIsSecure(options: SetDeviceIsSecureOptions) => Promise<void>
```

Overrides device secure-state for development/testing scenarios.

| Param         | Type                                                                          |
| ------------- | ----------------------------------------------------------------------------- |
| **`options`** | <code><a href="#setdeviceissecureoptions">SetDeviceIsSecureOptions</a></code> |

**Since:** 8.0.0

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

### unlock(...)

```typescript
unlock(options?: UnlockOptions | undefined) => Promise<void>
```

Triggers the secure unlock flow using biometrics or device credentials.

This method initiates authentication via Face ID, Touch ID,
or the device passcode as a fallback.

| Param         | Type                                                    |
| ------------- | ------------------------------------------------------- |
| **`options`** | <code><a href="#unlockoptions">UnlockOptions</a></code> |

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

### biometricKeysExist(...)

```typescript
biometricKeysExist(options?: KeyAliasOptions | undefined) => Promise<BiometricKeysExistResult>
```

Checks whether a biometric key pair already exists.

| Param         | Type                                                        | Description                    |
| ------------- | ----------------------------------------------------------- | ------------------------------ |
| **`options`** | <code><a href="#keyaliasoptions">KeyAliasOptions</a></code> | - Optional key alias override. |

**Returns:** <code>Promise&lt;<a href="#biometrickeysexistresult">BiometricKeysExistResult</a>&gt;</code>

**Since:** 8.0.0

---

### createKeys(...)

```typescript
createKeys(options?: KeyAliasOptions | undefined) => Promise<CreateKeysResult>
```

Creates (or replaces) a biometric key pair and returns the public key.

| Param         | Type                                                        | Description                    |
| ------------- | ----------------------------------------------------------- | ------------------------------ |
| **`options`** | <code><a href="#keyaliasoptions">KeyAliasOptions</a></code> | - Optional key alias override. |

**Returns:** <code>Promise&lt;<a href="#createkeysresult">CreateKeysResult</a>&gt;</code>

**Since:** 8.0.0

---

### deleteKeys(...)

```typescript
deleteKeys(options?: KeyAliasOptions | undefined) => Promise<void>
```

Deletes the biometric key pair if it exists.

| Param         | Type                                                        | Description                    |
| ------------- | ----------------------------------------------------------- | ------------------------------ |
| **`options`** | <code><a href="#keyaliasoptions">KeyAliasOptions</a></code> | - Optional key alias override. |

**Since:** 8.0.0

---

### createSignature(...)

```typescript
createSignature(options: CreateSignatureOptions) => Promise<CreateSignatureResult>
```

Creates a biometric-protected cryptographic signature.

The method requires the vault to be unlocked and an existing
biometric key pair in native secure hardware.

Signature encoding note:

- iOS/Android return Base64 (standard).
- Web (WebAuthn) returns Base64URL (no padding, '-' and '\_').
  Backend verification must normalize Base64URL → Base64 when verifying WebAuthn assertions.

| Param         | Type                                                                      | Description                  |
| ------------- | ------------------------------------------------------------------------- | ---------------------------- |
| **`options`** | <code><a href="#createsignatureoptions">CreateSignatureOptions</a></code> | - Signature request payload. |

**Returns:** <code>Promise&lt;<a href="#createsignatureresult">CreateSignatureResult</a>&gt;</code>

**Since:** 8.0.0

---

### registerWithChallenge(...)

```typescript
registerWithChallenge(options: ChallengeAuthOptions) => Promise<RegisterWithChallengeResult>
```

Creates or replaces keys and signs a backend challenge.

| Param         | Type                                                                  | Description                            |
| ------------- | --------------------------------------------------------------------- | -------------------------------------- |
| **`options`** | <code><a href="#challengeauthoptions">ChallengeAuthOptions</a></code> | - Challenge and optional prompt/alias. |

**Returns:** <code>Promise&lt;<a href="#registerwithchallengeresult">RegisterWithChallengeResult</a>&gt;</code>

**Since:** 8.0.0

---

### authenticateWithChallenge(...)

```typescript
authenticateWithChallenge(options: ChallengeAuthOptions) => Promise<AuthenticateWithChallengeResult>
```

Signs a backend challenge with existing biometric keys.

| Param         | Type                                                                  | Description                            |
| ------------- | --------------------------------------------------------------------- | -------------------------------------- |
| **`options`** | <code><a href="#challengeauthoptions">ChallengeAuthOptions</a></code> | - Challenge and optional prompt/alias. |

**Returns:** <code>Promise&lt;<a href="#authenticatewithchallengeresult">AuthenticateWithChallengeResult</a>&gt;</code>

**Since:** 8.0.0

---

### generateChallengePayload(...)

```typescript
generateChallengePayload(options: GenerateChallengePayloadOptions) => Promise<GenerateChallengePayloadResult>
```

Generates a canonical payload for backend verification workflows.

The payload includes nonce, timestamp, and a non-PII device identifier hash
to reduce replay attack risk and keep verification format deterministic.

| Param         | Type                                                                                        | Description                   |
| ------------- | ------------------------------------------------------------------------------------------- | ----------------------------- |
| **`options`** | <code><a href="#generatechallengepayloadoptions">GenerateChallengePayloadOptions</a></code> | - Payload generation options. |

**Returns:** <code>Promise&lt;<a href="#generatechallengepayloadresult">GenerateChallengePayloadResult</a>&gt;</code>

**Since:** 8.0.0

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

### addListener('onSecurityStateChanged', ...)

```typescript
addListener(eventName: 'onSecurityStateChanged', listenerFunc: (status: DeviceSecurityStatus) => void) => Promise<PluginListenerHandle>
```

Adds a listener for security posture changes.

| Param              | Type                                                                                       |
| ------------------ | ------------------------------------------------------------------------------------------ |
| **`eventName`**    | <code>'onSecurityStateChanged'</code>                                                      |
| **`listenerFunc`** | <code>(status: <a href="#devicesecuritystatus">DeviceSecurityStatus</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

---

### addListener('onLockStatusChanged', ...)

```typescript
addListener(eventName: 'onLockStatusChanged', listenerFunc: (state: { isLocked: boolean; }) => void) => Promise<PluginListenerHandle>
```

Adds a listener for lock-state changes with payload.

| Param              | Type                                                    |
| ------------------ | ------------------------------------------------------- |
| **`eventName`**    | <code>'onLockStatusChanged'</code>                      |
| **`listenerFunc`** | <code>(state: { isLocked: boolean; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

---

### addListener('onVaultInvalidated', ...)

```typescript
addListener(eventName: 'onVaultInvalidated', listenerFunc: (event: VaultInvalidatedEvent) => void) => Promise<PluginListenerHandle>
```

Adds a listener for vault invalidation events.

| Param              | Type                                                                                        |
| ------------------ | ------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'onVaultInvalidated'</code>                                                           |
| **`listenerFunc`** | <code>(event: <a href="#vaultinvalidatedevent">VaultInvalidatedEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

---

### addListener('onAppResume', ...)

```typescript
addListener(eventName: 'onAppResume', listenerFunc: () => void) => Promise<PluginListenerHandle>
```

Adds a listener for app resume events.

This event is emitted when the app/tab becomes active in foreground.

| Param              | Type                       |
| ------------------ | -------------------------- |
| **`eventName`**    | <code>'onAppResume'</code> |
| **`listenerFunc`** | <code>() =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

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

#### FortressRuntimeConfig

Runtime configuration snapshot currently used by the plugin.

This reflects static startup configuration merged with
runtime overrides applied via `configure(...)`.

| Prop                                  | Type                                                             |
| ------------------------------------- | ---------------------------------------------------------------- |
| **`verboseLogging`**                  | <code>boolean</code>                                             |
| **`logLevel`**                        | <code>'debug' \| 'error' \| 'warn' \| 'info' \| 'verbose'</code> |
| **`lockAfterMs`**                     | <code>number</code>                                              |
| **`enablePrivacyScreen`**             | <code>boolean</code>                                             |
| **`privacyOverlayText`**              | <code>string</code>                                              |
| **`privacyOverlayImageName`**         | <code>string</code>                                              |
| **`privacyOverlayShowText`**          | <code>boolean</code>                                             |
| **`privacyOverlayShowImage`**         | <code>boolean</code>                                             |
| **`privacyOverlayTextColor`**         | <code>string</code>                                              |
| **`privacyOverlayBackgroundOpacity`** | <code>number</code>                                              |
| **`privacyOverlayTheme`**             | <code>'system' \| 'light' \| 'dark'</code>                       |
| **`fallbackStrategy`**                | <code>'none' \| 'deviceCredential' \| 'systemDefault'</code>     |
| **`allowCachedAuthentication`**       | <code>boolean</code>                                             |
| **`cachedAuthenticationTimeoutMs`**   | <code>number</code>                                              |
| **`maxBiometricAttempts`**            | <code>number</code>                                              |
| **`lockoutDurationMs`**               | <code>number</code>                                              |
| **`requireFreshAuthenticationMs`**    | <code>number</code>                                              |
| **`encryptionAlgorithm`**             | <code>'AES-GCM' \| 'AES-CBC'</code>                              |
| **`persistSessionState`**             | <code>boolean</code>                                             |

#### FortressConfig

Static configuration options for the Fortress plugin.

These values are defined in `capacitor.config.ts` and consumed
exclusively by native code during plugin initialization.

Configuration values:

- do NOT change the JavaScript API shape
- do NOT enable/disable methods
- are applied once during plugin load

| Prop                                  | Type                                                                      | Description                                                                                                                                                                                                                                                                                                                                            | Default                           | Since |
| ------------------------------------- | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------- | ----- |
| **`verboseLogging`**                  | <code>boolean</code>                                                      | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API.                                                                                                               | <code>false</code>                | 8.0.0 |
| **`logLevel`**                        | <code>'debug' \| 'error' \| 'warn' \| 'verbose'</code>                    | Native/Web logging threshold. - `error`: errors only - `warn`: warnings and errors - `debug`: debug/info/warn/error - `verbose`: maximum logging level                                                                                                                                                                                                 | <code>'info'</code>               | 8.0.0 |
| **`lockAfterMs`**                     | <code>number</code>                                                       | Global auto-lock timeout in milliseconds.                                                                                                                                                                                                                                                                                                              | <code>60000</code>                | 8.0.0 |
| **`accessControl`**                   | <code><a href="#biometricaccesscontrol">BiometricAccessControl</a></code> | Security level for biometric hardware access.                                                                                                                                                                                                                                                                                                          | <code>'biometryCurrentSet'</code> | 8.0.0 |
| **`enablePrivacyScreen`**             | <code>boolean</code>                                                      | Enables or disables privacy protection for app snapshots. Platform behavior: - Android relies on window snapshot protection in recents/task switcher. - iOS uses a visual privacy overlay. Note: On Android recents previews, system-protected cards may not render custom overlay text/image and can appear as a blank/protected preview.             | <code>true</code>                 | 8.0.0 |
| **`privacyOverlayText`**              | <code>string</code>                                                       | Optional text rendered on top of the privacy screen overlay. This is intended for lock-state messaging such as "Session Locked" or "Tap to Unlock". Platform note: - Android: text is shown on the in-app overlay. - Android recents/task switcher: system snapshot protection may hide custom text in preview cards.                                  |                                   | 8.0.0 |
| **`privacyOverlayImageName`**         | <code>string</code>                                                       | Optional native asset name rendered on top of the privacy screen overlay. Asset lookup rules: - iOS: Image from app asset catalog by name - Android: Drawable resource by name Platform note: - Android: image is shown on the in-app overlay. - Android recents/task switcher: system snapshot protection may hide custom images in preview cards.    |                                   | 8.0.0 |
| **`privacyOverlayShowText`**          | <code>boolean</code>                                                      | Controls whether privacy overlay text is visible.                                                                                                                                                                                                                                                                                                      | <code>true</code>                 | 8.0.0 |
| **`privacyOverlayShowImage`**         | <code>boolean</code>                                                      | Controls whether privacy overlay image is visible.                                                                                                                                                                                                                                                                                                     | <code>true</code>                 | 8.0.0 |
| **`privacyOverlayTextColor`**         | <code>string</code>                                                       | Optional text color (hex string) for the privacy overlay label. Example: `#FFFFFF`                                                                                                                                                                                                                                                                     |                                   | 8.0.0 |
| **`privacyOverlayBackgroundOpacity`** | <code>number</code>                                                       | Optional background opacity for the privacy overlay scrim. Allowed range: `0.0` to `1.0`.                                                                                                                                                                                                                                                              |                                   | 8.0.0 |
| **`privacyOverlayTheme`**             | <code>'system' \| 'light' \| 'dark'</code>                                | Controls the privacy overlay visual theme. - `system`: follow device appearance (light/dark) - `light`: force light overlay appearance - `dark`: force dark overlay appearance                                                                                                                                                                         | <code>'system'</code>             | 8.0.0 |
| **`obfuscationPrefix`**               | <code>string</code>                                                       | Prefix used by key obfuscation utilities.                                                                                                                                                                                                                                                                                                              | <code>'ftrss\_'</code>            | 8.0.0 |
| **`webAuthn`**                        | <code><a href="#webauthnconfig">WebAuthnConfig</a></code>                 | WebAuthn configuration for Web platform unlock behavior. - `local` mode stores credential metadata only in browser storage. - `server` mode uses backend challenge and assertion verification endpoints.                                                                                                                                               |                                   | 8.0.0 |
| **`allowCachedAuthentication`**       | <code>boolean</code>                                                      | Enables in-memory cached authentication for unlock operations. When enabled, repeated `unlock()` calls within `cachedAuthenticationTimeoutMs` can skip the interactive biometric prompt.                                                                                                                                                               | <code>false</code>                | 8.0.0 |
| **`cachedAuthenticationTimeoutMs`**   | <code>number</code>                                                       | Cached authentication validity window in milliseconds. This value is only used when `allowCachedAuthentication` is enabled.                                                                                                                                                                                                                            | <code>30000</code>                | 8.0.0 |
| **`cryptoStrategy`**                  | <code>'auto' \| 'ecc' \| 'rsa'</code>                                     | Asymmetric key-pair strategy for cryptographic operations. - `auto`: platform default strategy - `ecc`: force elliptic-curve key generation where supported - `rsa`: force RSA key generation where supported                                                                                                                                          | <code>'auto'</code>               | 8.0.0 |
| **`keySize`**                         | <code>2048 \| 4096</code>                                                 | RSA key size used when `cryptoStrategy` is set to `rsa`.                                                                                                                                                                                                                                                                                               | <code>2048</code>                 | 8.0.0 |
| **`maxBiometricAttempts`**            | <code>number</code>                                                       | Maximum failed biometric attempts before temporary lockout.                                                                                                                                                                                                                                                                                            | <code>5</code>                    | 8.0.0 |
| **`lockoutDurationMs`**               | <code>number</code>                                                       | Temporary lockout duration in milliseconds after reaching the biometric failure threshold.                                                                                                                                                                                                                                                             | <code>30000</code>                | 8.0.0 |
| **`requireFreshAuthenticationMs`**    | <code>number</code>                                                       | Maximum allowed age in milliseconds for the last successful biometric authentication before requiring a fresh authentication.                                                                                                                                                                                                                          | <code>0 (disabled)</code>         | 8.0.0 |
| **`encryptionAlgorithm`**             | <code>'AES-GCM' \| 'AES-CBC'</code>                                       | Symmetric encryption algorithm used by the Web secure storage layer. Native platforms keep hardware-backed secure defaults.                                                                                                                                                                                                                            | <code>'AES-GCM'</code>            | 8.0.0 |
| **`enableICloudKeychainSync`**        | <code>boolean</code>                                                      | Enables iCloud Keychain synchronization for iOS secure-storage entries. Platform behavior: - iOS: when enabled, generic-password vault items are created as synchronizable - Android/Web: ignored (no-op)                                                                                                                                              | <code>false</code>                | 8.0.0 |
| **`persistSessionState`**             | <code>boolean</code>                                                      | Persists web session lock/auth state across page reloads. Platform behavior: - Web: when enabled, vault/session state is restored from persisted storage - iOS/Android: ignored (no-op)                                                                                                                                                                | <code>false</code>                | 8.0.0 |
| **`fallbackStrategy`**                | <code>'none' \| 'deviceCredential' \| 'systemDefault'</code>              | Controls fallback behavior when biometric authentication is unavailable or fails during an interactive prompt. - `deviceCredential`: always allow device credential fallback when supported. - `none`: disallow device credential fallback and require biometrics only. - `systemDefault`: preserve legacy behavior (`allowDevicePasscode` on native). | <code>'systemDefault'</code>      | 8.0.0 |

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

| Prop         | Type                 |
| ------------ | -------------------- |
| **`key`**    | <code>string</code>  |
| **`value`**  | <code>string</code>  |
| **`secure`** | <code>boolean</code> |

#### ValueResult

Result returned by secure and insecure read operations.

| Prop        | Type                        |
| ----------- | --------------------------- |
| **`value`** | <code>string \| null</code> |

#### DeviceSecurityStatus

| Prop                        | Type                                                                    |
| --------------------------- | ----------------------------------------------------------------------- |
| **`isBiometricsAvailable`** | <code>boolean</code>                                                    |
| **`isBiometricsEnabled`**   | <code>boolean</code>                                                    |
| **`isDeviceSecure`**        | <code>boolean</code>                                                    |
| **`biometryType`**          | <code>'none' \| 'touchId' \| 'faceId' \| 'fingerprint' \| 'iris'</code> |

#### SetBiometryTypeOptions

Input payload for overriding detected biometry type in development/testing.

| Prop               | Type                                                                    |
| ------------------ | ----------------------------------------------------------------------- |
| **`biometryType`** | <code>'none' \| 'touchId' \| 'faceId' \| 'fingerprint' \| 'iris'</code> |

#### SetBiometryIsEnrolledOptions

Input payload for overriding biometrics enrollment state in development/testing.

| Prop                      | Type                 |
| ------------------------- | -------------------- |
| **`isBiometricsEnabled`** | <code>boolean</code> |

#### SetDeviceIsSecureOptions

Input payload for overriding device secure-state in development/testing.

| Prop                 | Type                 |
| -------------------- | -------------------- |
| **`isDeviceSecure`** | <code>boolean</code> |

#### UnlockOptions

Optional input payload for vault unlock.

| Prop                | Type                                                                      |
| ------------------- | ------------------------------------------------------------------------- |
| **`promptMessage`** | <code>string</code>                                                       |
| **`promptOptions`** | <code><a href="#biometricpromptoptions">BiometricPromptOptions</a></code> |

#### BiometricPromptOptions

Prompt customization options for interactive authentication.

Platform note:

- Android uses title/subtitle/description/negativeButtonText directly.
- Android BiometricPrompt layout/iconography remains system-controlled.
- iOS uses localized reason + cancel title best-effort mapping.
- Web keeps this shape for API parity.

| Prop                       | Type                 |
| -------------------------- | -------------------- |
| **`title`**                | <code>string</code>  |
| **`subtitle`**             | <code>string</code>  |
| **`description`**          | <code>string</code>  |
| **`negativeButtonText`**   | <code>string</code>  |
| **`confirmationRequired`** | <code>boolean</code> |

#### FortressSession

Current session status exposed to JavaScript.

| Prop               | Type                 |
| ------------------ | -------------------- |
| **`isLocked`**     | <code>boolean</code> |
| **`lastActiveAt`** | <code>number</code>  |

#### BiometricKeysExistResult

Result payload returned by key-pair existence checks.

| Prop            | Type                 |
| --------------- | -------------------- |
| **`keysExist`** | <code>boolean</code> |

#### KeyAliasOptions

Input payload for key-pair operations.

| Prop           | Type                |
| -------------- | ------------------- |
| **`keyAlias`** | <code>string</code> |

#### CreateKeysResult

Result payload returned after creating a biometric key pair.

| Prop            | Type                |
| --------------- | ------------------- |
| **`publicKey`** | <code>string</code> |

#### CreateSignatureResult

Result payload returned by biometric signature creation.

| Prop            | Type                |
| --------------- | ------------------- |
| **`success`**   | <code>true</code>   |
| **`signature`** | <code>string</code> |

#### CreateSignatureOptions

Input payload for biometric signature creation.

| Prop                | Type                                                                      |
| ------------------- | ------------------------------------------------------------------------- |
| **`payload`**       | <code>string</code>                                                       |
| **`keyAlias`**      | <code>string</code>                                                       |
| **`promptMessage`** | <code>string</code>                                                       |
| **`promptOptions`** | <code><a href="#biometricpromptoptions">BiometricPromptOptions</a></code> |

#### RegisterWithChallengeResult

Result payload returned by challenge registration.

| Prop            | Type                |
| --------------- | ------------------- |
| **`publicKey`** | <code>string</code> |
| **`signature`** | <code>string</code> |

#### ChallengeAuthOptions

Input payload for challenge-based registration/authentication.

| Prop                | Type                                                                      |
| ------------------- | ------------------------------------------------------------------------- |
| **`challenge`**     | <code>string</code>                                                       |
| **`promptMessage`** | <code>string</code>                                                       |
| **`promptOptions`** | <code><a href="#biometricpromptoptions">BiometricPromptOptions</a></code> |

#### AuthenticateWithChallengeResult

Result payload returned by challenge authentication.

| Prop            | Type                |
| --------------- | ------------------- |
| **`signature`** | <code>string</code> |

#### GenerateChallengePayloadResult

Result payload returned by backend challenge payload generation.

| Prop          | Type                |
| ------------- | ------------------- |
| **`payload`** | <code>string</code> |

#### GenerateChallengePayloadOptions

Input payload for canonical backend challenge payload generation.

| Prop        | Type                |
| ----------- | ------------------- |
| **`nonce`** | <code>string</code> |

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

#### VaultInvalidatedEvent

Payload emitted when the vault is invalidated by security posture changes.

| Prop         | Type                                                                             |
| ------------ | -------------------------------------------------------------------------------- |
| **`reason`** | <code>'security_state_changed' \| 'keypair_invalidated' \| 'keys_deleted'</code> |

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

| Code                 | Description                                           |
| -------------------- | ----------------------------------------------------- |
| `UNAVAILABLE`        | Feature not supported on this device or configuration |
| `CANCELLED`          | User cancelled the operation                          |
| `PERMISSION_DENIED`  | Required permission was denied                        |
| `INIT_FAILED`        | Native initialization or runtime failure              |
| `INVALID_INPUT`      | Invalid input provided                                |
| `NOT_FOUND`          | Requested resource not found                          |
| `CONFLICT`           | Operation conflicts with current state                |
| `TIMEOUT`            | Operation timed out                                   |
| `SECURITY_VIOLATION` | Security validation failed                            |
| `VAULT_LOCKED`       | Vault is locked, unlock required                      |

### Example

```ts
import { Fortress, FortressErrorCode } from '@cap-kit/fortress';

try {
  await Fortress.unlock();
} catch (err: any) {
  switch (err.code) {
    case FortressErrorCode.VAULT_LOCKED:
      // Vault is locked - prompt for biometric
      break;
    case FortressErrorCode.CANCELLED:
      // User cancelled - handle gracefully
      break;
    case FortressErrorCode.UNAVAILABLE:
      // Biometrics not available
      break;
    case FortressErrorCode.SECURITY_VIOLATION:
      // Security issue detected
      break;
    default:
      console.error(err.message);
  }
}
```

---

## Privacy Overlay

Fortress supports customizable privacy screen overlays that display when the vault is locked.

### Configuration

```typescript
// capacitor.config.ts
const config: CapacitorConfig = {
  plugins: {
    Fortress: {
      enablePrivacyScreen: true,
      privacyOverlayText: 'Session Locked',
      privacyOverlayImageName: 'lock_icon', // Optional: image from asset catalog
      privacyOverlayShowText: true,
      privacyOverlayShowImage: true,
      privacyOverlayTextColor: '#FFFFFF',
      privacyOverlayBackgroundOpacity: 0.8,
      privacyOverlayTheme: 'system', // 'system' | 'light' | 'dark'
    },
  },
};
```

### Runtime Updates

You can update the privacy overlay at runtime:

```ts
await Fortress.configure({
  privacyOverlayText: 'New text', // Updates immediately if overlay is visible
});
```

### Platform Behavior Notes

- **Android recents/task switcher:** Android applies snapshot protection with `FLAG_SECURE`. In this mode, the system usually shows a protected/blank preview card. Custom overlay text/image is not guaranteed to be visible in recents previews.
- **Android in-app lock overlay:** `privacyOverlayText`, `privacyOverlayImageName`, and `privacyOverlayTheme` apply to the plugin's in-app privacy overlay.
- **Biometric prompt UI on Android:** The lock icon/title style belongs to system `BiometricPrompt` and cannot be fully themed by the plugin. You can customize prompt content via `biometricPromptText` and `promptOptions` (title/subtitle/description/negative button).

### Asset Requirements

- **iOS**: Add images to your Xcode asset catalog (Assets.xcassets)
- **Android**: Add drawable resources to `android/app/src/main/res/drawable`

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## License

MIT
