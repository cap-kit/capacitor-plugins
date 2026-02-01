<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Test</h3>
<p align="center">
  <strong>
    <code>@cap-kit/test-plugin</code>
  </strong>
</p>

<p align="center">
  The <strong>architectural reference implementation</strong> for the Cap-Kit ecosystem.<br>
  This package serves as the definitive <strong>boilerplate and validation ground</strong> for creating new Capacitor plugins.<br>
  It demonstrates the enforced monorepo structure, build configuration, and native bridges (Swift/Kotlin) required by our standards.<br>
  <em>Note: This is an internal reference package, primarily used for CI verification and scaffolding.</em>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/test-plugin">
    <img src="https://img.shields.io/npm/v/@cap-kit/test-plugin?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/test-plugin">
    <img src="https://img.shields.io/npm/dm/@cap-kit/test-plugin?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/test-plugin?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Install

```bash
pnpm add @cap-kit/test-plugin
npx cap sync

```

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the Test plugin.

| Prop                 | Type                 | Description                                                                                                                                                                                                                              | Default                       | Since |
| -------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- | ----- |
| **`customMessage`**  | <code>string</code>  | Custom message appended to the echoed value. This option exists mainly as an example showing how to pass static configuration data from JavaScript to native platforms.                                                                  | <code>" (from config)"</code> | 0.0.1 |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API. | <code>false</code>            | 1.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Test": {
      "customMessage": " - Hello from Config!",
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/test-plugin" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Test: {
      customMessage: ' - Hello from Config!',
      verboseLogging: true,
    },
  },
};

export default config;
```

</docgen-config>

## API

<docgen-index>

- [`echo(...)`](#echo)
- [`openAppSettings()`](#openappsettings)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Test Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

### echo(...)

```typescript
echo(options: EchoOptions) => Promise<EchoResult>
```

Echoes the provided value.

If the plugin is configured with a `customMessage`, that value
will be appended to the returned string.

This method is primarily intended as an example demonstrating
native ↔ JavaScript communication.

| Param         | Type                                                | Description                          |
| ------------- | --------------------------------------------------- | ------------------------------------ |
| **`options`** | <code><a href="#echooptions">EchoOptions</a></code> | Object containing the value to echo. |

**Returns:** <code>Promise&lt;<a href="#echoresult">EchoResult</a>&gt;</code>

**Since:** 0.0.1

#### Example

```ts
const { value } = await Test.echo({ value: 'Hello' });
console.log(value);
```

---

### openAppSettings()

```typescript
openAppSettings() => Promise<void>
```

Opens the operating system's application settings page.

**Since:** 1.0.0

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 0.0.1

#### Example

```ts
const { version } = await Test.getPluginVersion();
```

---

### Interfaces

#### EchoResult

Result object returned by the `echo` method.

This object represents the resolved value of the echo operation
after native processing has completed.

| Prop        | Type                | Description                                                                                                   |
| ----------- | ------------------- | ------------------------------------------------------------------------------------------------------------- |
| **`value`** | <code>string</code> | The echoed string value. If a `customMessage` is configured, it will be appended to the original input value. |

#### EchoOptions

Options object for the `echo` method.

This object defines the input payload sent from JavaScript
to the native plugin implementation.

| Prop        | Type                | Description                                                                                                                                                      |
| ----------- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`value`** | <code>string</code> | The string value to be echoed back by the plugin. This value is passed to the native layer and returned unchanged, optionally with a configuration-based suffix. |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

</docgen-api>

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## License

MIT
