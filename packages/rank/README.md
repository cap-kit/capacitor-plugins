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

## Overview

This Capacitor plugin provides a unified API for **In-App Reviews** and **Market Navigation** on iOS and Android, with store navigation fallbacks on Web.

- Requests the native store review prompt and opens App Store / Play Store pages, listings, developer pages, collections, and searches.
- It does NOT control whether the native review dialog is displayed — the operating system decides when and how often the review prompt appears.

## Documentation

- [Usage guide](docs/guide.md) — best practices and error handling
- [Configuration guide](docs/configuration.md) — plugin configuration, native requirements, and permissions
- [Security considerations](docs/security.md) — platform limitations
- [Contributing](CONTRIBUTING.md)

---

## Install

```bash
pnpm add @cap-kit/rank
# or
npm install @cap-kit/rank
# or
yarn add @cap-kit/rank
# then run:
npx cap sync
```

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
requestReview(options?: ReviewOptions) => Promise<void>
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
presentProductPage(options?: StoreOptions) => Promise<void>
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
openStore(options?: StoreOptions) => Promise<void>
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
openStoreListing(options?: { appId?: string; }) => Promise<void>
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

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## License

MIT
