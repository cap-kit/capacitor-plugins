<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Biometrics</h3>
<p align="center">
  <strong>
    <code>@cap-kit/biometrics</code>
  </strong>
</p>

<p align="center">
  Standalone biometric authentication for Capacitor apps: Face ID, Touch ID,
  fingerprint, face and iris recognition with device-credential fallback.<br>
  API-compatible with the established community contracts, plus capability
  discovery and enrollment helpers.
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/biometrics">
    <img src="https://img.shields.io/npm/v/@cap-kit/biometrics?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/biometrics">
    <img src="https://img.shields.io/npm/dm/@cap-kit/biometrics?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/biometrics?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Overview

Biometrics is built for applications that must verify the user is present:

- Face ID, Touch ID, fingerprint, face and iris recognition
- Device credential (PIN/pattern/password/passcode) fallback
- Capability discovery before prompting (available, enrolled, types, strength)
- Enrollment helper and cancellable prompts

## Documentation

- [Usage guide](docs/guide.md) — capability checks, authentication flows, enrollment, and the demo app
- [Configuration](docs/configuration.md) — static config and per-call prompt options reference
- [Platform behavior](docs/privacy.md) — what Android, iOS, and web actually do
- [Security model](docs/security.md) — what it proves, what it does not, and best practices
- [API notes](docs/api-notes.md) — return shapes, availability, and error behavior
- [Contributing](CONTRIBUTING.md)

---

## Install

```bash
pnpm add @cap-kit/biometrics
# or
npm install @cap-kit/biometrics
# or
yarn add @cap-kit/biometrics
# then run:
npx cap sync
```

---

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the Biometrics plugin.

| Prop                 | Type                 | Description                     | Default            | Since |
| -------------------- | -------------------- | ------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. | <code>false</code> | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Biometrics": {
      "verboseLogging": false
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/biometrics" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Biometrics: {
      verboseLogging: false,
    },
  },
};

export default config;
```

</docgen-config>

## API

<docgen-index>

* [`authenticate(...)`](#authenticate)
* [`cancelAuthentication()`](#cancelauthentication)
* [`isAvailable()`](#isavailable)
* [`isEnrolled()`](#isenrolled)
* [`getBiometricType()`](#getbiometrictype)
* [`getBiometricTypes()`](#getbiometrictypes)
* [`hasDeviceCredential()`](#hasdevicecredential)
* [`isDeviceSecure()`](#isdevicesecure)
* [`getBiometricStrengthLevel()`](#getbiometricstrengthlevel)
* [`getAuthenticationType()`](#getauthenticationtype)
* [`enroll()`](#enroll)
* [`setSecureData(...)`](#setsecuredata)
* [`getSecureData(...)`](#getsecuredata)
* [`deleteSecureData(...)`](#deletesecuredata)
* [`isSecureDataSaved(...)`](#issecuredatasaved)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Biometrics Capacitor plugin.

Standalone biometric authentication: Face ID, Touch ID, fingerprint,
face and iris recognition with device-credential fallback.

### authenticate(...)

```typescript
authenticate(options?: AuthenticateOptions) => Promise<void>
```

Authenticates the user with biometrics or device credentials.

Shows the system prompt. Resolves on success, rejects with
`CANCELLED` (user dismissed) or `UNAVAILABLE` (no usable
authenticator). Rejects if another authentication is in progress.

Only available on Android and iOS.

| Param         | Type                                                                | Description                                     |
| ------------- | ------------------------------------------------------------------- | ----------------------------------------------- |
| **`options`** | <code><a href="#authenticateoptions">AuthenticateOptions</a></code> | - Prompt customization and credential fallback. |

**Since:** 8.0.0

#### Example

```ts
await Biometrics.authenticate({
  title: 'Authentication Required',
  subtitle: 'Please authenticate to continue',
  cancelButtonText: 'Cancel',
  allowDeviceCredential: true,
});
```

--------------------


### cancelAuthentication()

```typescript
cancelAuthentication() => Promise<void>
```

Cancels an ongoing authentication prompt, if any.

Resolves immediately when idle. Only available on Android
(SDK 29+) and iOS.

**Since:** 8.0.0

--------------------


### isAvailable()

```typescript
isAvailable() => Promise<IsAvailableResult>
```

Reports whether biometric authentication can be used
(hardware present and usable, regardless of enrollment).

Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#isavailableresult">IsAvailableResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### isEnrolled()

```typescript
isEnrolled() => Promise<IsEnrolledResult>
```

Reports whether the user enrolled biometrics.

Check both `isAvailable()` and `isEnrolled()` before
`authenticate()`. Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#isenrolledresult">IsEnrolledResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### getBiometricType()

```typescript
getBiometricType() => Promise<BiometricTypeResult>
```

Returns the primary biometry modality (display helper;
decide with `isAvailable()` instead).

Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#biometrictyperesult">BiometricTypeResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### getBiometricTypes()

```typescript
getBiometricTypes() => Promise<BiometricTypesResult>
```

Returns every biometry modality known to the hardware.
Only Android reports more than one entry.

Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#biometrictypesresult">BiometricTypesResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### hasDeviceCredential()

```typescript
hasDeviceCredential() => Promise<HasDeviceCredentialResult>
```

Reports whether a device credential (PIN/pattern/password)
is set and usable as fallback.

Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#hasdevicecredentialresult">HasDeviceCredentialResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### isDeviceSecure()

```typescript
isDeviceSecure() => Promise<IsDeviceSecureResult>
```

Reports whether the device is secured by any lock-screen credential
(PIN/pattern/password/passcode; biometrics always require one).
Reports the same value as `hasDeviceCredential()`.

Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#isdevicesecureresult">IsDeviceSecureResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### getBiometricStrengthLevel()

```typescript
getBiometricStrengthLevel() => Promise<BiometricStrengthResult>
```

Reports the strength class of the available biometry.
Android only reports beyond `none`; iOS is always `strong`
when available.

**Returns:** <code>Promise&lt;<a href="#biometricstrengthresult">BiometricStrengthResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### getAuthenticationType()

```typescript
getAuthenticationType() => Promise<AuthenticationTypeResult>
```

Reports which credential satisfied the last successful ceremony
in this session.

**Returns:** <code>Promise&lt;<a href="#authenticationtyperesult">AuthenticationTypeResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### enroll()

```typescript
enroll() => Promise<void>
```

Opens the system biometric enrollment screen.

Android only (API 30+). iOS and Web reject with `UNAVAILABLE`.

**Since:** 8.0.0

--------------------


### setSecureData(...)

```typescript
setSecureData(options: SetSecureDataOptions) => Promise<void>
```

Stores a string value encrypted under a biometry-bound key.

The store is biometry-bound by design: there is no unprotected
mode. Android shows a biometric prompt bound to the encryption
key at write time; iOS applies SecAccessControl to the Keychain
item (no prompt at write time). Requires enrolled biometrics.

Only available on Android and iOS.

| Param         | Type                                                                  | Description                                  |
| ------------- | --------------------------------------------------------------------- | -------------------------------------------- |
| **`options`** | <code><a href="#setsecuredataoptions">SetSecureDataOptions</a></code> | - Key, payload and biometric access control. |

**Since:** 8.0.0

#### Example

```ts
await Biometrics.setSecureData({
  key: 'session.token',
  value: JSON.stringify(token),
  accessControl: 'biometryAny',
});
```

--------------------


### getSecureData(...)

```typescript
getSecureData(options: GetSecureDataOptions) => Promise<SecureDataResult>
```

Reads a biometry-protected value, prompting for biometric
authentication (system prompt on both platforms).

Rejects with `NOT_FOUND` when no value exists for the key — never
stored, deleted, or invalidated by biometric enrollment changes
(with `biometryCurrentSet`). This is stable, expected behavior:
handle it in your catch block or check `isSecureDataSaved()`
first.

Only available on Android and iOS.

| Param         | Type                                                                  | Description                     |
| ------------- | --------------------------------------------------------------------- | ------------------------------- |
| **`options`** | <code><a href="#getsecuredataoptions">GetSecureDataOptions</a></code> | - Key and prompt customization. |

**Returns:** <code>Promise&lt;<a href="#securedataresult">SecureDataResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { value } = await Biometrics.getSecureData({
  key: 'session.token',
  reason: 'Unlock your session',
});
```

--------------------


### deleteSecureData(...)

```typescript
deleteSecureData(options: DeleteSecureDataOptions) => Promise<void>
```

Deletes the value stored for the key.

Only available on Android and iOS.

| Param         | Type                                                                        | Description                   |
| ------------- | --------------------------------------------------------------------------- | ----------------------------- |
| **`options`** | <code><a href="#deletesecuredataoptions">DeleteSecureDataOptions</a></code> | - Key of the value to delete. |

**Since:** 8.0.0

--------------------


### isSecureDataSaved(...)

```typescript
isSecureDataSaved(options: IsSecureDataSavedOptions) => Promise<IsSecureDataSavedResult>
```

Reports whether a value exists for the key, without prompting
for authentication.

Only available on Android and iOS.

| Param         | Type                                                                          | Description     |
| ------------- | ----------------------------------------------------------------------------- | --------------- |
| **`options`** | <code><a href="#issecuredatasavedoptions">IsSecureDataSavedOptions</a></code> | - Key to check. |

**Returns:** <code>Promise&lt;<a href="#issecuredatasavedresult">IsSecureDataSavedResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### Interfaces


#### AuthenticateOptions

Prompt customization for `authenticate()`.

Field names follow the Capawesome-style contract.

| Prop                        | Type                 | Description                                                                                                                               | Default           | Since |
| --------------------------- | -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ----- |
| **`reason`**                | <code>string</code>  | Reason shown in the system dialog. Falls back to `description`.                                                                           |                   | 8.0.0 |
| **`title`**                 | <code>string</code>  | Dialog title (Android; iOS uses the system dialog).                                                                                       |                   | 8.0.0 |
| **`subtitle`**              | <code>string</code>  | Dialog subtitle (Android).                                                                                                                |                   | 8.0.0 |
| **`description`**           | <code>string</code>  | Dialog description (Android and iOS localized reason).                                                                                    |                   | 8.0.0 |
| **`cancelButtonText`**      | <code>string</code>  | Cancel button text. Android: negative button (not shown when device credential is in the authenticator set). iOS: localized cancel title. |                   | 8.0.0 |
| **`iosFallbackButtonText`** | <code>string</code>  | iOS only: fallback button title ("Use Passcode"). An empty string hides the fallback button.                                              |                   | 8.0.0 |
| **`allowDeviceCredential`** | <code>boolean</code> | Allows device credential (PIN/pattern/password/passcode) fallback.                                                                        | <code>true</code> | 8.0.0 |


#### IsAvailableResult

Result of `isAvailable()`.

| Prop              | Type                 |
| ----------------- | -------------------- |
| **`isAvailable`** | <code>boolean</code> |


#### IsEnrolledResult

Result of `isEnrolled()`.

| Prop             | Type                 |
| ---------------- | -------------------- |
| **`isEnrolled`** | <code>boolean</code> |


#### BiometricTypeResult

Result of `getBiometricType()`.

| Prop               | Type                                                  |
| ------------------ | ----------------------------------------------------- |
| **`biometryType`** | <code><a href="#biometrytype">BiometryType</a></code> |


#### BiometricTypesResult

Result of `getBiometricTypes()`.

| Prop                | Type                        |
| ------------------- | --------------------------- |
| **`biometryTypes`** | <code>BiometryType[]</code> |


#### HasDeviceCredentialResult

Result of `hasDeviceCredential()`.

| Prop                      | Type                 |
| ------------------------- | -------------------- |
| **`hasDeviceCredential`** | <code>boolean</code> |


#### IsDeviceSecureResult

Result of `isDeviceSecure()`.

| Prop                 | Type                 | Description                                                                                                                                                                                                                                                                                   | Since |
| -------------------- | -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`isDeviceSecure`** | <code>boolean</code> | Whether the device is secured by any lock-screen credential: PIN/pattern/password (Android `KeyguardManager.isDeviceSecure`) or the passcode required by `.deviceOwnerAuthentication` (iOS). Biometrics cannot be enrolled without one, so this doubles as the broad "any secure lock" check. | 8.0.0 |


#### BiometricStrengthResult

Result of `getBiometricStrengthLevel()`.

| Prop                | Type                                                            |
| ------------------- | --------------------------------------------------------------- |
| **`strengthLevel`** | <code><a href="#biometricstrength">BiometricStrength</a></code> |


#### AuthenticationTypeResult

Result of `getAuthenticationType()`.

| Prop                     | Type                                                              |
| ------------------------ | ----------------------------------------------------------------- |
| **`authenticationType`** | <code><a href="#authenticationtype">AuthenticationType</a></code> |


#### SetSecureDataOptions

Options for `setSecureData()`.

| Prop                     | Type                                                    | Description                                                                                               | Since |
| ------------------------ | ------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ----- |
| **`key`**                | <code>string</code>                                     | Unique, stable, app-specific key for the stored value.                                                    | 8.0.0 |
| **`value`**              | <code>string</code>                                     | String payload. Serialize objects with `JSON.stringify()` before storing. Keep payloads small (a few KB). | 8.0.0 |
| **`accessControl`**      | <code><a href="#accesscontrol">AccessControl</a></code> | Biometric access control applied to the value. Required: the store is biometry-bound by design.           | 8.0.0 |
| **`title`**              | <code>string</code>                                     | Android only: title for the biometric prompt shown while protecting the value at write time.              | 8.0.0 |
| **`negativeButtonText`** | <code>string</code>                                     | Android only: negative/cancel button text for the write-time prompt.                                      | 8.0.0 |
| **`maxAttempts`**        | <code>number</code>                                     | Android only: maximum prompt attempts (device maximum is 5).                                              | 8.0.0 |


#### SecureDataResult

Result of `getSecureData()`.

| Prop        | Type                | Description                 | Since |
| ----------- | ------------------- | --------------------------- | ----- |
| **`value`** | <code>string</code> | The decrypted stored value. | 8.0.0 |


#### GetSecureDataOptions

Options for `getSecureData()`.

| Prop                     | Type                | Description                                                                                 | Since |
| ------------------------ | ------------------- | ------------------------------------------------------------------------------------------- | ----- |
| **`key`**                | <code>string</code> | Key the value was stored under.                                                             | 8.0.0 |
| **`reason`**             | <code>string</code> | iOS only: reason shown as the prompt subtitle (Touch ID only; Face ID does not display it). | 8.0.0 |
| **`title`**              | <code>string</code> | Android only: title for the biometric prompt.                                               | 8.0.0 |
| **`subtitle`**           | <code>string</code> | Android only: subtitle for the biometric prompt.                                            | 8.0.0 |
| **`description`**        | <code>string</code> | Android only: description for the biometric prompt.                                         | 8.0.0 |
| **`negativeButtonText`** | <code>string</code> | Android only: negative/cancel button text for the prompt.                                   | 8.0.0 |
| **`fallbackTitle`**      | <code>string</code> | iOS only: fallback button title. An empty string hides the fallback button.                 | 8.0.0 |
| **`maxAttempts`**        | <code>number</code> | Android only: maximum prompt attempts (device maximum is 5).                                | 8.0.0 |


#### DeleteSecureDataOptions

Options for `deleteSecureData()`.

| Prop      | Type                | Description                 | Since |
| --------- | ------------------- | --------------------------- | ----- |
| **`key`** | <code>string</code> | Key of the value to delete. | 8.0.0 |


#### IsSecureDataSavedResult

Result of `isSecureDataSaved()`.

| Prop          | Type                 | Description                         | Since |
| ------------- | -------------------- | ----------------------------------- | ----- |
| **`isSaved`** | <code>boolean</code> | Whether a value exists for the key. | 8.0.0 |


#### IsSecureDataSavedOptions

Options for `isSecureDataSaved()`.

| Prop      | Type                | Description   | Since |
| --------- | ------------------- | ------------- | ----- |
| **`key`** | <code>string</code> | Key to check. | 8.0.0 |


#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |


### Type Aliases


#### BiometryType

Biometry modality reported by the device.

<code>'none' | 'touchId' | 'faceId' | 'fingerprint' | 'face' | 'iris'</code>


#### BiometricStrength

Strength class of the available biometry.

- `strong`: Face ID / Touch ID / strong-class Android biometrics.
- `weak`: only weak modalities (e.g. some Android face unlocks).
- `none`: no usable biometry.

<code>'strong' | 'weak' | 'none'</code>


#### AuthenticationType

Credential class that satisfied an authentication ceremony.

`'unknown'` when nothing authenticated yet in this session or the
platform does not report it.

<code>'biometric' | 'deviceCredential' | 'unknown'</code>


#### AccessControl

Access control applied to a value stored with `setSecureData()`.

The secure store is biometry-bound by design: there is no
unprotected mode. Every stored value requires biometric
authentication on read.

- `biometryAny`: the value survives biometric enrollment changes
  (e.g. adding a new fingerprint). Recommended for most apps.
- `biometryCurrentSet`: the value is invalidated when the enrolled
  biometric set changes (more secure; data is lost on enrollment
  changes).

<code>'biometryAny' | 'biometryCurrentSet'</code>

</docgen-api>

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## Credits

Contract inspired by [Capawesome Biometrics](https://capawesome.io/docs/sdks/capacitor/biometrics/),
[Capgo Native Biometric](https://github.com/Cap-go/capacitor-native-biometric) and
[Aparajita Biometric Auth](https://github.com/aparajita/capacitor-biometric-auth).
Native engine extracted from [`@cap-kit/fortress`](../fortress#readme).

---

## License

MIT
