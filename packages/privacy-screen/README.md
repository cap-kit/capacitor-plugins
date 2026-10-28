<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Privacy Screen</h3>
<p align="center">
  <strong>
    <code>@cap-kit/privacy-screen</code>
  </strong>
</p>

<p align="center">
  A <strong>Capacitor v8 plugin</strong> for privacy-screen protection on <strong>iOS and Android</strong>, with a Web
  implementation for API parity.<br>
  Hides app content in the app switcher and recents view, blocks screenshots and screen recordings on Android, installs an
  app-switcher blur overlay on iOS, and exposes screenshot and screen-recording signals for detection.<br>
  Built following the <strong>Cap-Kit architectural standards</strong> with strict separation between JavaScript, bridge,
  and native implementations (Swift / Kotlin).
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/privacy-screen">
    <img src="https://img.shields.io/npm/v/@cap-kit/privacy-screen?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/privacy-screen">
    <img src="https://img.shields.io/npm/dm/@cap-kit/privacy-screen?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/privacy-screen?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Overview

This Capacitor plugin provides a unified API for privacy-screen protection on **iOS and Android**. It hides app content in the OS app switcher and recents view, blocks screenshots and screen recordings where the platform allows it, and exposes detection signals when it cannot prevent them.

- On Android, sets `FLAG_SECURE` while enabled to hide the app snapshot and block screenshots and screen recordings, with optional dim/scrim overlays.
- On iOS, installs a full-screen blur overlay over the app-switcher snapshot and emits `screenshotTaken` and screen-recording events.
- It does NOT prevent iOS screenshots — no public API can. `screenshotTaken` is a post-facto detection signal, never a barrier.
- It is a UI-hygiene control, not DRM or authentication.

### Platform Support

| Platform | Status                                                                      |
| -------- | --------------------------------------------------------------------------- |
| iOS      | Supported                                                                   |
| Android  | Supported                                                                   |
| Web      | State tracking only — no OS-level protection; `screenshotTaken` never fires |

## Documentation

- [Usage guide](docs/guide.md) — per-screen protection, screenshot detection, and the Web platform
- [Configuration](docs/configuration.md) — static config, per-call display options, and defaults
- [Platform behavior](docs/privacy.md) — Android `FLAG_SECURE`, iOS blur overlay, and Web behavior
- [Security model](docs/security.md) — what the plugin protects against and what it does not
- [API notes](docs/api-notes.md) — return shapes, idempotency, and platform availability
- [Contributing](CONTRIBUTING.md)

---

## Install

```bash
pnpm add @cap-kit/privacy-screen
npx cap sync
```

---

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the PrivacyScreen plugin.

| Prop                 | Type                 | Description                                                                                                           | Default            | Since |
| -------------------- | -------------------- | --------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging.                                                                                       | <code>false</code> | 8.0.0 |
| **`enabled`**        | <code>boolean</code> | Enables privacy protection automatically when the native plugin loads, without waiting for the first `enable()` call. | <code>false</code> | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "PrivacyScreen": {
      "verboseLogging": false,
      "enabled": false
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/privacy-screen" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    PrivacyScreen: {
      verboseLogging: false,
      enabled: false,
    },
  },
};

export default config;
```

</docgen-config>

---

## API

<docgen-index>

* [`enable(...)`](#enable)
* [`disable()`](#disable)
* [`isEnabled()`](#isenabled)
* [`addListener('screenshotTaken', ...)`](#addlistenerscreenshottaken-)
* [`addListener('screenRecordingStarted', ...)`](#addlistenerscreenrecordingstarted-)
* [`addListener('screenRecordingStopped', ...)`](#addlistenerscreenrecordingstopped-)
* [`removeAllListeners()`](#removealllisteners)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the PrivacyScreen Capacitor plugin.

This interface defines a stable, platform-agnostic API compatible with the
official `@capacitor/privacy-screen` contract, plus screenshot
observability. All methods behave consistently across Android, iOS and Web.

### enable(...)

```typescript
enable(config?: PrivacyScreenConfig) => Promise<PrivacyScreenActionResult>
```

Enables privacy-screen protection.

On Android this sets `FLAG_SECURE`, which hides app content in the app
switcher and blocks screenshots and screen recordings.

On iOS this installs an overlay that hides app content in the app
switcher.

Only available on Android and iOS.

| Param        | Type                                                                | Description                                    |
| ------------ | ------------------------------------------------------------------- | ---------------------------------------------- |
| **`config`** | <code><a href="#privacyscreenconfig">PrivacyScreenConfig</a></code> | - Optional platform-specific display behavior. |

**Returns:** <code>Promise&lt;<a href="#privacyscreenactionresult">PrivacyScreenActionResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
await PrivacyScreen.enable({ ios: { blurEffect: 'dark' } });
```

--------------------


### disable()

```typescript
disable() => Promise<PrivacyScreenActionResult>
```

Disables privacy-screen protection.

Use only when the current screen must stay visible in system previews.

Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#privacyscreenactionresult">PrivacyScreenActionResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
await PrivacyScreen.disable();
```

--------------------


### isEnabled()

```typescript
isEnabled() => Promise<PrivacyScreenStatus>
```

Returns the current privacy-screen enabled state.

Only available on Android and iOS.

**Returns:** <code>Promise&lt;<a href="#privacyscreenstatus">PrivacyScreenStatus</a>&gt;</code>

**Since:** 8.0.0

--------------------


### addListener('screenshotTaken', ...)

```typescript
addListener(eventName: "screenshotTaken", listenerFunc: () => void) => Promise<PluginListenerHandle>
```

Listens for when the user takes a screenshot of the app.

The event fires after the screenshot has been taken and therefore
cannot prevent it. On Android it is only emitted on API 34+ while
privacy protection is active; on older versions the listener is
never called.

On iOS, this event is emitted when the user takes a screenshot,
regardless of whether privacy protection is active.

Only available on Android and iOS.

| Param              | Type                           |
| ------------------ | ------------------------------ |
| **`eventName`**    | <code>'screenshotTaken'</code> |
| **`listenerFunc`** | <code>() =&gt; void</code>     |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

--------------------


### addListener('screenRecordingStarted', ...)

```typescript
addListener(eventName: "screenRecordingStarted", listenerFunc: () => void) => Promise<PluginListenerHandle>
```

Listens for when the user starts a screen recording of the app.

On iOS 11+, this event fires when the user starts screen recording
(e.g., via Control Center or screenshot UI). The event can only be
observed after the recording has started and therefore cannot be
prevented.

Only available on iOS.

| Param              | Type                                  |
| ------------------ | ------------------------------------- |
| **`eventName`**    | <code>'screenRecordingStarted'</code> |
| **`listenerFunc`** | <code>() =&gt; void</code>            |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

--------------------


### addListener('screenRecordingStopped', ...)

```typescript
addListener(eventName: "screenRecordingStopped", listenerFunc: () => void) => Promise<PluginListenerHandle>
```

Listens for when the user stops a screen recording of the app.

On iOS 11+, this event fires when the user stops screen recording.
The event can only be observed after the recording has stopped and
therefore cannot be prevented.

Only available on iOS.

| Param              | Type                                  |
| ------------------ | ------------------------------------- |
| **`eventName`**    | <code>'screenRecordingStopped'</code> |
| **`listenerFunc`** | <code>() =&gt; void</code>            |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Removes all listeners for this plugin.

**Since:** 8.0.0

--------------------


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
const { version } = await PrivacyScreen.getPluginVersion();
```

--------------------


### Interfaces


#### PrivacyScreenActionResult

Result returned when privacy protection is toggled.

Mirrors the official `@capacitor/privacy-screen` API.

| Prop          | Type                 | Description                             | Since |
| ------------- | -------------------- | --------------------------------------- | ----- |
| **`success`** | <code>boolean</code> | Whether the native operation completed. | 8.0.0 |


#### PrivacyScreenConfig

Platform-specific display options for the privacy screen.

This shape mirrors the official `@capacitor/privacy-screen` configuration
so this plugin stays a drop-in replacement.

| Prop          | Type                                                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| **`android`** | <code>{ dimBackground?: boolean; preventScreenshots?: boolean; privacyModeOnActivityHidden?: 'none' \| 'dim' \| 'splash'; }</code> |
| **`ios`**     | <code>{ blurEffect?: 'none' \| 'light' \| 'dark'; }</code>                                                                         |


#### PrivacyScreenStatus

Current privacy-screen state.

Mirrors the official `@capacitor/privacy-screen` API.

| Prop          | Type                 | Description                                      | Since |
| ------------- | -------------------- | ------------------------------------------------ | ----- |
| **`enabled`** | <code>boolean</code> | Whether privacy protection is currently enabled. | 8.0.0 |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


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

## Credits

This plugin mirrors the official `@capacitor/privacy-screen` contract and has been
reimplemented for the CapKit ecosystem.

Original inspiration:

- [https://capacitorjs.com/docs/apis/privacy-screen](https://capacitorjs.com/docs/apis/privacy-screen)

---

## License

MIT
