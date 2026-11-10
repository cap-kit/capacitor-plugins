<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Local Network</h3>
<p align="center">
  <strong>
    <code>@cap-kit/local-network</code>
  </strong>
</p>

<p align="center">
  Check and request the Android 17+ local network access permission (<code>ACCESS_LOCAL_NETWORK</code>), with an honest
  parity contract on iOS and web where no queryable runtime permission exists.
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/local-network">
    <img src="https://img.shields.io/npm/v/@cap-kit/local-network?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/local-network">
    <img src="https://img.shields.io/npm/dm/@cap-kit/local-network?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/local-network?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Overview

Android 17 (API 37) introduces a runtime permission that gates local network
access. This plugin exposes it through the standard Capacitor permission
contract:

- `checkPermissions()` / `requestPermissions()` returning a permission state
- a `permissionChange` event emitted after the user returns from system settings
- no rejection for a denied decision: denial is reported as state

On SDKs below API 37, on iOS and on the web the permission is not enforced and
is always reported as `granted`, so no prompt can ever appear there.

## Documentation

- [Usage guide](docs/guide.md) — permission flow, events and platform behavior
- [Permissions](docs/permissions.md) — Android manifest entries, iOS `Info.plist` keys, and Web platform notes
- [Configuration](docs/configuration.md) — static config reference
- [API notes](docs/api-notes.md) — return shapes, error codes and edge cases
- [Contributing](CONTRIBUTING.md)

---

## Install

```bash
pnpm add @cap-kit/local-network
# or
npm install @cap-kit/local-network
# or
yarn add @cap-kit/local-network
# then run:
npx cap sync
```

---

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the LocalNetwork plugin.

| Prop                 | Type                 | Description                                                                                                                                                                                                                              | Default            | Since |
| -------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API. | <code>false</code> | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "LocalNetwork": {
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/local-network" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    LocalNetwork: {
      verboseLogging: true,
    },
  },
};

export default config;
```

</docgen-config>

---

## API

<docgen-index>

- [`checkPermissions()`](#checkpermissions)
- [`requestPermissions()`](#requestpermissions)
- [`addListener('permissionChange', ...)`](#addlistenerpermissionchange-)
- [`addListener(string, ...)`](#addlistenerstring-)
- [`removeAllListeners()`](#removealllisteners)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### checkPermissions()

```typescript
checkPermissions() => Promise<LocalNetworkPermissions>
```

Checks the current state of the local-network permission.

Android 17+ (API 37) reports the real runtime permission state
(`granted`, `denied`, `prompt` or `prompt-with-rationale`).

On older Android versions, iOS and web the permission is not enforced
and is always reported as `granted`, so calling this method never
triggers a system prompt.

**Returns:** <code>Promise&lt;<a href="#localnetworkpermissions">LocalNetworkPermissions</a>&gt;</code>

**Since:** 8.0.0

---

### requestPermissions()

```typescript
requestPermissions() => Promise<LocalNetworkPermissions>
```

Requests the local-network permission.

Android 17+ (API 37) shows the system permission prompt when the
permission has not been decided yet, then resolves with the resulting
state. A denied decision is reported as state, not as a rejection.

On older Android versions, iOS and web the permission is not enforced
and the call resolves immediately with `granted`.

**Returns:** <code>Promise&lt;<a href="#localnetworkpermissions">LocalNetworkPermissions</a>&gt;</code>

**Since:** 8.0.0

---

### addListener('permissionChange', ...)

```typescript
addListener(eventName: "permissionChange", listenerFunc: (event: LocalNetworkPermissionChangeEvent) => void) => Promise<PluginListenerHandle>
```

Listen for changes to the local-network permission state.
Emits when the permission state changes (e.g. after returning from system settings).
On platforms where the permission is not enforced, this will typically remain `granted`.

| Param              | Type                                                                                                                |
| ------------------ | ------------------------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'permissionChange'</code>                                                                                     |
| **`listenerFunc`** | <code>(event: <a href="#localnetworkpermissionchangeevent">LocalNetworkPermissionChangeEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { LocalNetwork } from '@cap-kit/local-network';

const handle = await LocalNetwork.addListener('permissionChange', (event) => {
  console.log('Local network permission changed:', event.localNetwork);
});

// To remove the listener later:
// await handle.remove();
```

---

### addListener(string, ...)

```typescript
addListener(eventName: string, listenerFunc: (...args: unknown[]) => void) => Promise<PluginListenerHandle>
```

Registers a listener for plugin events using a generic event name.

Prefer the typed `permissionChange` overload for full payload type safety.

| Param              | Type                                         |
| ------------------ | -------------------------------------------- |
| **`eventName`**    | <code>string</code>                          |
| **`listenerFunc`** | <code>(...args: unknown[]) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

---

### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Removes all registered listeners for this plugin.

**Since:** 8.0.0

#### Example

```typescript
import { LocalNetwork } from '@cap-kit/local-network';

await LocalNetwork.removeAllListeners();
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

```typescript
import { LocalNetwork } from '@cap-kit/local-network';

const { version } = await LocalNetwork.getPluginVersion();
```

---

### Interfaces

#### LocalNetworkPermissions

Result of the LocalNetwork permission flow.

The property is declared explicitly (not via an index signature) so that
consumers can use dot access (`result.localNetwork`) even with the
`noPropertyAccessFromIndexSignature` compiler option enabled.

| Prop               | Type                                                        | Description                                                                                                                                                                                                                                                       |
| ------------------ | ----------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`localNetwork`** | <code><a href="#permissionstate">PermissionState</a></code> | State of the local-network (`ACCESS_LOCAL_NETWORK`) permission. On Android 17+ (API 37) this reflects the runtime permission state. On older Android versions, iOS and web the permission does not gate local-network access and is always reported as `granted`. |

#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

#### LocalNetworkPermissionChangeEvent

Event emitted when the local-network permission state changes.
Useful when the user returns from system settings after changing the permission.

| Prop               | Type                                                        | Description                                   |
| ------------------ | ----------------------------------------------------------- | --------------------------------------------- |
| **`localNetwork`** | <code><a href="#permissionstate">PermissionState</a></code> | Current state of the local-network permission |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

### Type Aliases

#### PermissionState

<code>'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'</code>

</docgen-api>

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## License

MIT
