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
A high-performance Capacitor v8 plugin for unified <strong>Biometrics</strong> and <strong>Secure Storage</strong>.<br>
  Built with a strict <strong>layered architecture</strong>, it serves as both a production-ready tool for app growth and an architectural reference for the CapKit ecosystem.<br>
</p>

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

- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Fortress Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

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

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

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
