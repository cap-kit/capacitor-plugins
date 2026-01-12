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

<p align="center">Test plugin for Cap-Kit.</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/test-plugin">
    <img src="https://img.shields.io/npm/v/@cap-kit/test-plugin?style=flat-square" />
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/test-plugin">
    <img src="https://img.shields.io/npm/dm/@cap-kit/test-plugin?style=flat-square" />
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/test-plugin">
    <img src="https://img.shields.io/npm/l/@cap-kit/test-plugin?style=flat-square" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" />
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

| Prop                | Type                | Description                                                                                                                   | Default                       | Since |
| ------------------- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------- | ----------------------------- | ----- |
| **`customMessage`** | <code>string</code> | A custom message to append to the echo response. This demonstrates how to pass data from `capacitor.config.ts` to the plugin. | <code>" (from config)"</code> | 0.0.1 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Test": {
      "customMessage": " - Hello from Config!"
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
    },
  },
};

export default config;
```

</docgen-config>

## API

<docgen-index>

- [`echo(...)`](#echo)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Capacitor Test plugin interface.

### echo(...)

```typescript
echo(options: EchoOptions) => Promise<EchoResult>
```

Echoes the provided value.

If the plugin is configured with a `customMessage`, it will be appended
to the response.

| Param         | Type                                                | Description                                 |
| ------------- | --------------------------------------------------- | ------------------------------------------- |
| **`options`** | <code><a href="#echooptions">EchoOptions</a></code> | - The options containing the value to echo. |

**Returns:** <code>Promise&lt;<a href="#echoresult">EchoResult</a>&gt;</code>

**Since:** 0.0.1

#### Example

```typescript
import { Test } from '@cap-kit/test-plugin';

const result = await Test.echo({ value: 'Hello, World!' });
console.log(result.value); // Output: 'Hello, World!'
```

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Get the native Capacitor plugin version.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 0.0.1

#### Example

```typescript
import { Test } from '@cap-kit/test-plugin';

const { version } = await Test.getPluginVersion();
console.log('Plugin version:', version); // Output: Plugin version: 0.0.1
```

---

### Interfaces

#### EchoResult

Result returned by the echo method.

| Prop        | Type                | Description       |
| ----------- | ------------------- | ----------------- |
| **`value`** | <code>string</code> | The echoed value. |

#### EchoOptions

Options for the echo method.

| Prop        | Type                | Description             |
| ----------- | ------------------- | ----------------------- |
| **`value`** | <code>string</code> | The value to be echoed. |

#### PluginVersionResult

Result returned by the getPluginVersion method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

</docgen-api>

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.
