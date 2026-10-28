<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">SecureStorage</h3>
<p align="center">
  <strong>
    <code>@cap-kit/secure-storage</code>
  </strong>
</p>

<p align="center">
  Hardware-backed encrypted key/value storage for Capacitor apps.<br>
  Keychain on iOS, Keystore on Android — with iCloud sync, Keychain access
  levels, and batch operations.
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/secure-storage">
    <img src="https://img.shields.io/npm/v/@cap-kit/secure-storage?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/secure-storage">
    <img src="https://img.shields.io/npm/dm/@cap-kit/secure-storage?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/secure-storage?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Overview

SecureStorage is built for applications that must persist sensitive data safely:

- Hardware-backed encrypted key/value storage (Keychain / Keystore)
- Key enumeration, batch reads, and existence checks without decryption
- iCloud Keychain sync and accessibility levels on iOS
- Master-key rotation on wipe (Android)

## Documentation

- [Usage guide](docs/guide.md) — basic operations, batch flows, sync, and the demo app
- [Configuration](docs/configuration.md) — static config and per-call options reference
- [Platform behavior](docs/privacy.md) — what Android, iOS, and web actually do
- [Security model](docs/security.md) — guarantees, limits, and best practices
- [API notes](docs/api-notes.md) — return shapes, availability, and error behavior
- [Contributing](CONTRIBUTING.md)

---

## Install

```bash
pnpm add @cap-kit/secure-storage
# or
npm install @cap-kit/secure-storage
# or
yarn add @cap-kit/secure-storage
# then run:
npx cap sync
```

---

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the SecureStorage plugin.

The key MUST match the plugin's registered name (`SecureStorage`,
the cap-kit convention: no CapKit prefix), which Capacitor uses to
resolve static plugin config on both platforms (Android:
`@CapacitorPlugin(name=...)`; iOS: `CAPPlugin.getConfig()` -&gt;
`pluginName` -&gt; `jsName`).

| Prop                           | Type                 | Description                                                                                                                                                                                                                                   | Default            | Since |
| ------------------------------ | -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`**           | <code>boolean</code> | Enables verbose native logging.                                                                                                                                                                                                               | <code>false</code> | 8.0.0 |
| **`enableICloudKeychainSync`** | <code>boolean</code> | Enables iCloud Keychain synchronization for stored entries. iOS only; ignored on Android and Web.                                                                                                                                             | <code>false</code> | 8.0.0 |
| **`obfuscateKeys`**            | <code>boolean</code> | Base64-encodes stored key names (both tiers) on top of any prefix. Obfuscation is not encryption: it hides key names from casual inspection, nothing more. Reads transparently accept both forms, so toggling never orphans existing entries. | <code>false</code> | 8.0.0 |
| **`obfuscationPrefix`**        | <code>string</code>  | Prefix prepended to stored key names before optional encoding.                                                                                                                                                                                | <code>''</code>    | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "SecureStorage": {
      "verboseLogging": false,
      "enableICloudKeychainSync": false,
      "obfuscateKeys": false,
      "obfuscationPrefix": ""
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/secure-storage" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    SecureStorage: {
      verboseLogging: false,
      enableICloudKeychainSync: false,
      obfuscateKeys: false,
      obfuscationPrefix: '',
    },
  },
};

export default config;
```

</docgen-config>

## API

<docgen-index>

* [`set(...)`](#set)
* [`get(...)`](#get)
* [`remove(...)`](#remove)
* [`has(...)`](#has)
* [`keys()`](#keys)
* [`getMany(...)`](#getmany)
* [`clear()`](#clear)
* [`configure(...)`](#configure)
* [`setInsecure(...)`](#setinsecure)
* [`getInsecure(...)`](#getinsecure)
* [`removeInsecure(...)`](#removeinsecure)
* [`hasInsecure(...)`](#hasinsecure)
* [`keysInsecure()`](#keysinsecure)
* [`clearInsecure()`](#clearinsecure)
* [`setSynchronize(...)`](#setsynchronize)
* [`getSynchronize()`](#getsynchronize)
* [`setDefaultKeychainAccess(...)`](#setdefaultkeychainaccess)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the SecureStorage Capacitor plugin.

Transparent encrypted key/value storage: hardware-backed on Android
(Keystore) and iOS (Keychain), with iCloud sync and Keychain access
levels on iOS. No vault, no lock state, no authentication — reads are
always available, exactly like the community secure-storage contract.

### set(...)

```typescript
set(options: SetOptions) => Promise<void>
```

Stores a value under a key, overwriting any previous entry.

| Param         | Type                                              | Description                                   |
| ------------- | ------------------------------------------------- | --------------------------------------------- |
| **`options`** | <code><a href="#setoptions">SetOptions</a></code> | - Key, value, and optional iOS accessibility. |

**Since:** 8.0.0

#### Example

```ts
await SecureStorage.set({ key: 'theme', value: 'dark' });
```

--------------------


### get(...)

```typescript
get(options: KeyOptions) => Promise<GetResult>
```

Reads the value for a key, or `null` when absent.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#keyoptions">KeyOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#getresult">GetResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### remove(...)

```typescript
remove(options: KeyOptions) => Promise<RemoveResult>
```

Removes the entry for a key.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#keyoptions">KeyOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#removeresult">RemoveResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### has(...)

```typescript
has(options: KeyOptions) => Promise<HasResult>
```

Checks whether an entry exists without reading it.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#keyoptions">KeyOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#hasresult">HasResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### keys()

```typescript
keys() => Promise<KeysResult>
```

Lists every stored key.

**Returns:** <code>Promise&lt;<a href="#keysresult">KeysResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### getMany(...)

```typescript
getMany(options: GetManyOptions) => Promise<GetManyResult>
```

Reads several keys in one call. Missing keys map to `null`.

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#getmanyoptions">GetManyOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#getmanyresult">GetManyResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### clear()

```typescript
clear() => Promise<void>
```

Removes every stored entry.

On Android this also rotates the Keystore keys, so previously
captured ciphertext can never be decrypted again.

**Since:** 8.0.0

--------------------


### configure(...)

```typescript
configure(options: ConfigureOptions) => Promise<void>
```

Configures the insecure tier at runtime.

Only `group` is supported; `undefined` values leave the current
setting untouched. Mirrors the official Preferences contract.

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#configureoptions">ConfigureOptions</a></code> |

**Since:** 8.0.0

--------------------


### setInsecure(...)

```typescript
setInsecure(options: SetOptions) => Promise<void>
```

Stores a plaintext value in the insecure tier under the current group.

For non-sensitive data only (UI preferences, flags). Values are NOT
encrypted; keys follow the same prefix/obfuscation rules as the
secure tier.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#setoptions">SetOptions</a></code> |

**Since:** 8.0.0

--------------------


### getInsecure(...)

```typescript
getInsecure(options: KeyOptions) => Promise<GetResult>
```

Reads a plaintext value from the insecure tier, or `null` when absent.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#keyoptions">KeyOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#getresult">GetResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### removeInsecure(...)

```typescript
removeInsecure(options: KeyOptions) => Promise<RemoveResult>
```

Removes an insecure entry.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#keyoptions">KeyOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#removeresult">RemoveResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### hasInsecure(...)

```typescript
hasInsecure(options: KeyOptions) => Promise<HasResult>
```

Checks insecure existence without reading.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#keyoptions">KeyOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#hasresult">HasResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### keysInsecure()

```typescript
keysInsecure() => Promise<KeysResult>
```

Lists insecure keys of the current group.

**Returns:** <code>Promise&lt;<a href="#keysresult">KeysResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### clearInsecure()

```typescript
clearInsecure() => Promise<void>
```

Removes every insecure entry of the current group.

**Since:** 8.0.0

--------------------


### setSynchronize(...)

```typescript
setSynchronize(options: SynchronizeOptions) => Promise<void>
```

Enables or disables iCloud Keychain synchronization at runtime.

iOS only; a no-op on Android and Web.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#synchronizeoptions">SynchronizeOptions</a></code> |

**Since:** 8.0.0

--------------------


### getSynchronize()

```typescript
getSynchronize() => Promise<SynchronizeResult>
```

Reports whether iCloud Keychain synchronization is active.

iOS only; always `false` elsewhere.

**Returns:** <code>Promise&lt;<a href="#synchronizeresult">SynchronizeResult</a>&gt;</code>

**Since:** 8.0.0

--------------------


### setDefaultKeychainAccess(...)

```typescript
setDefaultKeychainAccess(options: DefaultAccessOptions) => Promise<void>
```

Sets the default iOS Keychain accessibility for subsequently
stored items. Per-item `access` in `set()` wins.

iOS only; a no-op on Android and Web.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#defaultaccessoptions">DefaultAccessOptions</a></code> |

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


#### SetOptions

Key/value payload for writes.

| Prop         | Type                                                      | Description                                                                                                                        | Since |
| ------------ | --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`key`**    | <code>string</code>                                       |                                                                                                                                    |       |
| **`value`**  | <code>string</code>                                       |                                                                                                                                    |       |
| **`access`** | <code><a href="#keychainaccess">KeychainAccess</a></code> | iOS Keychain accessibility for this item, overriding the default set via `setDefaultKeychainAccess()`. Ignored on Android and Web. | 8.0.0 |


#### GetResult

Read result. `value` is `null` when the key does not exist.

| Prop        | Type                        |
| ----------- | --------------------------- |
| **`value`** | <code>string \| null</code> |


#### KeyOptions

Key payload for reads, removals, and existence checks.

| Prop      | Type                |
| --------- | ------------------- |
| **`key`** | <code>string</code> |


#### RemoveResult

Removal result. `existed` reports whether an entry was present.

| Prop          | Type                 |
| ------------- | -------------------- |
| **`existed`** | <code>boolean</code> |


#### HasResult

Existence-check result.

| Prop         | Type                 |
| ------------ | -------------------- |
| **`exists`** | <code>boolean</code> |


#### KeysResult

Key-enumeration result.

| Prop       | Type                  |
| ---------- | --------------------- |
| **`keys`** | <code>string[]</code> |


#### GetManyResult

Batch-read result. Missing keys map to `null`.

| Prop         | Type                                              |
| ------------ | ------------------------------------------------- |
| **`values`** | <code>Record&lt;string, string \| null&gt;</code> |


#### GetManyOptions

Batch-read input.

| Prop       | Type                  |
| ---------- | --------------------- |
| **`keys`** | <code>string[]</code> |


#### ConfigureOptions

Runtime group configuration for the insecure tier.

Groups namespace insecure key/value pairs, exactly like the official
Preferences plugin. The default group is `CapacitorStorage`.

| Prop                    | Type                 | Description                                                                                                            | Since |
| ----------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------- | ----- |
| **`group`**             | <code>string</code>  |                                                                                                                        |       |
| **`obfuscateKeys`**     | <code>boolean</code> | Base64-encodes stored key names (both tiers) at runtime, overriding the static `obfuscateKeys` value for this session. | 8.0.0 |
| **`obfuscationPrefix`** | <code>string</code>  | Prefix prepended to stored key names at runtime, overriding the static `obfuscationPrefix` value for this session.     | 8.0.0 |


#### SynchronizeOptions

Synchronization-toggle input.

| Prop              | Type                 |
| ----------------- | -------------------- |
| **`synchronize`** | <code>boolean</code> |


#### SynchronizeResult

Synchronization-state result.

| Prop              | Type                 |
| ----------------- | -------------------- |
| **`synchronize`** | <code>boolean</code> |


#### DefaultAccessOptions

Default-accessibility input.

| Prop         | Type                                                      |
| ------------ | --------------------------------------------------------- |
| **`access`** | <code><a href="#keychainaccess">KeychainAccess</a></code> |


#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |


### Type Aliases


#### KeychainAccess

iOS Keychain accessibility level for stored items.

Mirrors the platform `kSecAttrAccessible` constants. Applies to iOS
only; ignored on Android and Web.

<code>'whenUnlocked' | 'whenUnlockedThisDeviceOnly' | 'afterFirstUnlock' | 'afterFirstUnlockThisDeviceOnly' | 'whenPasscodeSetThisDeviceOnly'</code>

</docgen-api>

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## Credits

Contract inspired by [Capawesome Secure Preferences](https://capawesome.io/docs/sdks/capacitor/secure-preferences/),
[Aparajita Secure Storage](https://github.com/aparajita/capacitor-secure-storage) and the
official [Preferences](https://github.com/ionic-team/capacitor-plugins/tree/main/preferences) plugin.
Native engine extracted from [`@cap-kit/fortress`](../fortress#readme).

---

## License

MIT
