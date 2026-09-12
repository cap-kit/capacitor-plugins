# Settings — Usage Guide

This guide covers how to choose between the generic and platform-specific settings methods, usage examples, and how to configure the plugin.

## Choosing the right method

The plugin provides a generic `open()` method for cross-platform usage, and platform-specific aliases (`openIOS()`, `openAndroid()`) for direct invocation. While `openIOS()` and `openAndroid()` can be used, it's generally recommended to use `open()` with `PlatformOptions` for a more unified approach.

## Usage Examples

### Open a settings screen with the generic `open()` method

Provide options for both platforms in a single call; the plugin uses the option that matches the current runtime platform:

```ts
import { Settings, IOSSettings, AndroidSettings } from '@cap-kit/settings';

await Settings.open({
  optionIOS: IOSSettings.App,
  optionAndroid: AndroidSettings.Wifi,
});
```

### Platform-specific aliases

```ts
import { Settings, IOSSettings, AndroidSettings } from '@cap-kit/settings';

// iOS only
await Settings.openIOS({ option: IOSSettings.AppNotification });

// Android only
await Settings.openAndroid({ option: AndroidSettings.ApplicationDetails });
```

### Checking the native plugin version

```ts
import { Settings } from '@cap-kit/settings';

const { version } = await Settings.getPluginVersion();
```

## Configuration

The plugin is configured statically in `capacitor.config.ts` under the `plugins.Settings` key. Configuration values are read natively during plugin initialization and are NOT accessible from JavaScript at runtime.

| Prop                 | Type                 | Description                                                                                                  | Default            | Since |
| -------------------- | -------------------- | ------------------------------------------------------------------------------------------------------------ | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console. | <code>false</code> | 8.0.0 |

## Error Handling

All Settings plugin methods return Promises and may reject when the operation fails or is not supported.

When rejected, the error object contains a machine-readable `code` from `SettingsErrorCode`. Consumers are expected to handle this case using standard `try / catch` error handling.

```ts
import { Settings, SettingsErrorCode, IOSSettings, AndroidSettings } from '@cap-kit/settings';

try {
  await Settings.open({
    optionIOS: IOSSettings.App,
    optionAndroid: AndroidSettings.Wifi,
  });
} catch (err: any) {
  switch (err.code) {
    case SettingsErrorCode.UNAVAILABLE:
      // The settings screen is not supported on the current device or platform
      break;

    case SettingsErrorCode.INVALID_INPUT:
      // The provided option is missing or empty
      break;

    default:
      console.error(err.message);
  }
}
```
