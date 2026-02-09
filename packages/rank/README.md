<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Rank</h3>
<p align="center">
  <strong>
    <code>@cap-kit/rank</code>
  </strong>
</p>

<p align="center">
A high-performance Capacitor v8 plugin for unified <strong>In-App Reviews</strong> and <strong>Market Navigation</strong>.<br>
  Built with a strict <strong>layered architecture</strong>, it serves as both a production-ready tool for app growth and an architectural reference for the CapKit ecosystem.<br>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/rank">
    <img src="https://img.shields.io/npm/v/@cap-kit/rank?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/rank">
    <img src="https://img.shields.io/npm/dm/@cap-kit/rank?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/rank?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Install

```bash
pnpm add @cap-kit/rank
npx cap sync

```

## Configuration

Configuration options for the Rank plugin.

| Prop                     | Type                 | Description                                                                                                                                    | Default            | Since |
| ------------------------ | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`**     | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). | <code>false</code> | 8.0.0 |
| **`appleAppId`**         | <code>string</code>  | The Apple App ID used for App Store redirection on iOS. Example: '123456789' \* @since 8.0.0                                                   |                    |       |
| **`androidPackageName`** | <code>string</code>  | The Android Package Name used for Play Store redirection. Example: 'com.example.app' \* @since 8.0.0                                           |                    |       |
| **`fireAndForget`**      | <code>boolean</code> | If true, the `requestReview` method will resolve immediately without waiting for the native OS review flow to complete. \* @default false      |                    | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Rank": {
      "verboseLogging": true,
      "appleAppId": "123456789",
      "androidPackageName": "com.example.app",
      "fireAndForget": false
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/rank" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Rank: {
      verboseLogging: true,
      appleAppId: '123456789',
      androidPackageName: 'com.example.app',
      fireAndForget: false,
    },
  },
};

export default config;
```

## Native Requirements

### Android

- Requires **Google Play Services** for In-App Reviews.
- To support **Android 11+ (API 30+)** and allow navigation to the Play Store, you must include the following in your `AndroidManifest.xml`:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="market" />
    </intent>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="https" android:host="play.google.com" />
    </intent>
</queries>
```

### iOS

- Requires **Xcode 26** and **iOS 15+**.
- To allow the plugin to open the App Store review page, ensure your `Info.plist` includes the appropriate URL schemes if you perform programmatic checks.

---

## Permissions

### Android

This plugin requires the following permission, which is automatically merged into your application's `AndroidManifest.xml`:

- `android.permission.INTERNET`: Required to communicate with Google Play Services for the review flow and store navigation.

### iOS

No specific usage descriptions (Privacy Manifest) are required for the standard `SKStoreReviewController` flow.

---

## API

<docgen-index>

- [`isAvailable()`](#isavailable)
- [`checkReviewEnvironment()`](#checkreviewenvironment)
- [`requestReview(...)`](#requestreview)
- [`presentProductPage(...)`](#presentproductpage)
- [`openStore(...)`](#openstore)
- [`openStoreListing(...)`](#openstorelisting)
- [`search(...)`](#search)
- [`openDevPage(...)`](#opendevpage)
- [`openCollection(...)`](#opencollection)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Rank Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

### isAvailable()

```typescript
isAvailable() => Promise<AvailabilityResult>
```

Checks if the native In-App Review UI can be displayed.
On Android, it verifies Google Play Services availability.
On iOS, it checks the OS version compatibility.

**Returns:** <code>Promise&lt;<a href="#availabilityresult">AvailabilityResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { value } = await Rank.isAvailable();
if (value) {
  // Show review prompt or related UI
} else {
  // Fallback behavior for unsupported platforms
}
```

---

### checkReviewEnvironment()

```typescript
checkReviewEnvironment() => Promise<ReviewEnvironmentResult>
```

Performs a diagnostic check to determine whether the
Google Play In-App Review dialog can be displayed.

This does NOT trigger the review flow.
Android-only. On other platforms, it resolves as unavailable.

**Returns:** <code>Promise&lt;<a href="#reviewenvironmentresult">ReviewEnvironmentResult</a>&gt;</code>

**Since:** 8.0.0

---

### requestReview(...)

```typescript
requestReview(options?: ReviewOptions | undefined) => Promise<void>
```

Requests the display of the native review popup.
On Web, this operation calls unimplemented().

| Param         | Type                                                    | Description                              |
| ------------- | ------------------------------------------------------- | ---------------------------------------- |
| **`options`** | <code><a href="#reviewoptions">ReviewOptions</a></code> | Optional review configuration overrides. |

**Since:** 8.0.0

#### Example

```ts
// Basic usage with default configuration
await Rank.requestReview();

// Usage with fire-and-forget behavior
await Rank.requestReview({ fireAndForget: true });
```

---

### presentProductPage(...)

```typescript
presentProductPage(options?: StoreOptions | undefined) => Promise<void>
```

Opens the App Store product page internally (iOS) or redirects to the Store (Android/Web).

| Param         | Type                                                  | Description           |
| ------------- | ----------------------------------------------------- | --------------------- |
| **`options`** | <code><a href="#storeoptions">StoreOptions</a></code> | Store identification. |

**Since:** 8.0.0

#### Example

```ts
// On iOS, this will open an internal App Store overlay.
await Rank.presentProductPage({
  appId: '123456789', // iOS App ID for URL generation
});

// On Android, this will redirect to the Play Store.
await Rank.presentProductPage({
  packageName: 'com.example.app', // Android Package Name for URL generation
});
```

---

### openStore(...)

```typescript
openStore(options?: StoreOptions | undefined) => Promise<void>
```

Opens the app's page in the App Store (iOS) or Play Store (Android).
On Web, it performs a URL redirect if parameters are provided.

| Param         | Type                                                  | Description                              |
| ------------- | ----------------------------------------------------- | ---------------------------------------- |
| **`options`** | <code><a href="#storeoptions">StoreOptions</a></code> | Optional store identification overrides. |

**Since:** 8.0.0

#### Example

```ts
// On Web, this will open the store page in a new tab if identifiers are provided.
await Rank.openStore({
  appId: '123456789', // iOS App ID for URL generation
  packageName: 'com.example.app', // Android Package Name for URL generation
});
```

---

### openStoreListing(...)

```typescript
openStoreListing(options?: { appId?: string | undefined; } | undefined) => Promise<void>
```

Opens the App Store listing page for a specific app.
If no appId is provided, it uses the one from the plugin configuration.

| Param         | Type                             |
| ------------- | -------------------------------- |
| **`options`** | <code>{ appId?: string; }</code> |

**Since:** 8.0.0

#### Example

```ts
// Opens the store listing page.
// Uses the provided appId or falls back to the one in capacitor.config.ts
await Rank.openStoreListing({
  appId: '123456789',
});
```

---

### search(...)

```typescript
search(options: { terms: string; }) => Promise<void>
```

Performs a search in the app store for the given terms.

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ terms: string; }</code> |

**Since:** 8.0.0

#### Example

```ts
// Searches the store for specific terms.
// Android: market://search | iOS: itms-apps search
await Rank.search({
  terms: 'Capacitor Plugins',
});
```

---

### openDevPage(...)

```typescript
openDevPage(options: { devId: string; }) => Promise<void>
```

Opens the developer's page in the app store.

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ devId: string; }</code> |

**Since:** 8.0.0

#### Example

```ts
// Navigates to a developer or brand page.
await Rank.openDevPage({
  devId: '543216789',
});
```

---

### openCollection(...)

```typescript
openCollection(options: { name: string; }) => Promise<void>
```

Opens a specific app collection (Android Only).

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ name: string; }</code> |

**Since:** 8.0.0

#### Example

```ts
// Opens a curated collection (Android only).
await Rank.openCollection({
  name: 'editors_choice',
});
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
const { version } = await Rank.getPluginVersion();
```

---

### Interfaces

#### AvailabilityResult

Result object returned by the `isAvailable()` method.

| Prop        | Type                 | Description                                                 |
| ----------- | -------------------- | ----------------------------------------------------------- |
| **`value`** | <code>boolean</code> | Indicates whether the native In-App Review UI is available. |

#### ReviewEnvironmentResult

Diagnostic result for Android In-App Review availability.

This result describes whether the Google Play Review
flow can actually be displayed in the current environment.

| Prop                   | Type                                                                       | Description                                                        |
| ---------------------- | -------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| **`canRequestReview`** | <code>boolean</code>                                                       | True if the environment supports showing the review dialog.        |
| **`reason`**           | <code>'PLAY_STORE_NOT_AVAILABLE' \| 'NOT_INSTALLED_FROM_PLAY_STORE'</code> | Optional diagnostic reason when the review dialog cannot be shown. |

#### ReviewOptions

Options for the `requestReview` method.

| Prop                | Type                 | Description                                                                                                           |
| ------------------- | -------------------- | --------------------------------------------------------------------------------------------------------------------- |
| **`fireAndForget`** | <code>boolean</code> | Override the global configuration to determine if the promise should resolve immediately or wait for the native flow. |

#### StoreOptions

Options for the `openStore` method.

| Prop              | Type                | Description                                    |
| ----------------- | ------------------- | ---------------------------------------------- |
| **`appId`**       | <code>string</code> | Runtime override for the Apple App ID on iOS.  |
| **`packageName`** | <code>string</code> | Runtime override for the Android Package Name. |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

</docgen-api>

---

## Limitations

### General

- **`openCollection`**: This feature is specific to the Google Play Store and is unavailable on iOS/Web.
- **`openDevPage`**: On iOS, this method performs a store search for the developer name as a fallback, as direct developer page IDs are not consistently supported via deep links.

### iOS

- The in-app review prompt is **not guaranteed to appear**.
  Apple internally controls when and how often the review dialog is shown.
- Calling `requestReview()` may result in **no visible UI**, even if the API is available.

### Android

- Google Play In-App Review requires **Google Play Services** to be available on the device.
- The review flow may silently fail if Play Services are missing, outdated, or restricted.
- As with iOS, the system ultimately decides whether the review dialog is displayed.

---

## Best practices

- Call `requestReview()` only after a **positive user interaction**
  (e.g. completed task, successful checkout, achieved milestone).
- Avoid calling the review prompt on app startup or without user context.
- Always check availability first:

```ts
const { value } = await Rank.isAvailable();
if (value) {
  await Rank.requestReview();
}
```

- Use `fireAndForget: true` only when you do not need to track completion
  and want to avoid blocking UI flows.

---

## Error handling

All Rank plugin methods return Promises and may reject in case of failure.
Consumers should always handle errors using `try / catch`.

### Example

```ts
import { Rank, RankErrorCode } from '@cap-kit/rank';

try {
  await Rank.requestReview();
} catch (err: any) {
  switch (err.code) {
    case RankErrorCode.UNAVAILABLE:
      // Feature not supported on this device or platform
      break;

    case RankErrorCode.INIT_FAILED:
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
