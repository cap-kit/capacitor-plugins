<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Splash Motion</h3>
<p align="center">
  <strong>
    <code>@cap-kit/splash-motion</code>
  </strong>
</p>

<p align="center">
  A <strong>Capacitor v8 splash screen plugin</strong> for <strong>iOS and Android</strong>, with a Web
  overlay implementation.<br>
  API-compatible with the official <code>@capacitor/splash-screen</code> plugin, extended with
  <strong>Lottie animations</strong> and <strong>haptic feedback</strong>, built on the
  <strong>Cap-Kit architectural standards</strong> (kotlinx.serialization static config, strict
  separation between JavaScript, bridge, and native implementations — Swift / Kotlin).
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/splash-motion">
    <img src="https://img.shields.io/npm/v/@cap-kit/splash-motion?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/splash-motion">
    <img src="https://img.shields.io/npm/dm/@cap-kit/splash-motion?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/splash-motion?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Overview

This Capacitor plugin provides a unified splash screen API on **iOS and Android**:

- The method surface (`show` / `hide`, `ShowOptions`, `HideOptions`) is **API-compatible with the official `@capacitor/splash-screen`** plugin, so migrating is a drop-in swap of the package and the config key.
- **Lottie animations**: render a `.json` composition or a `.lottie` dotLottie archive (local asset or remote HTTPS URL) on the splash surface, with loop and speed control.
- **Dark mode**: dedicated dark variants for the splash background (`backgroundColorDark`) and the Lottie resource (`darkSource`).
- **Haptic feedback**: fire an impact haptic (`light` / `medium` / `heavy`) when the splash appears or disappears.
- Full-screen splash surface with background color, fade in/out transitions and auto-hide scheduling.

### Platform Support

| Platform | Status                                                                                                               |
| -------- | -------------------------------------------------------------------------------------------------------------------- |
| iOS      | Supported (Lottie + haptics)                                                                                         |
| Android  | Supported (Lottie + haptics)                                                                                         |
| Web      | Supported — full-viewport overlay with background color and optional Lottie; no images, gradient, spinner or haptics |

## Documentation

- [Usage guide](docs/guide.md) — quick start, launch splash, Lottie setup, haptics
- [Configuration](docs/configuration.md) — static config, per-call options, and defaults
- [Platform behavior](docs/platform-behavior.md) — Android Dialog surface, iOS splash window, and Web behavior
- [API notes](docs/api-notes.md) — idempotency, option validation, and platform availability
- [Contributing](CONTRIBUTING.md)

---

## Install

```bash
pnpm add @cap-kit/splash-motion
npx cap sync
```

---

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

These config values are available:

| Prop                            | Type                                                                                                                          | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | Default             | Since |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | ----- |
| **`verboseLogging`**            | <code>boolean</code>                                                                                                          | Enables verbose native logging.                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | <code>false</code>  | 8.0.0 |
| **`launchShowDuration`**        | <code>number</code>                                                                                                           | How long to show the launch splash screen when autoHide is enabled (in ms) When `launchAutoHide` is false this value acts as the MINIMUM hold, clamped to `max(launchShowDuration, lottie duration)`: a loaded launch lottie always plays to completion before the splash is released, and a shorter animation never shortens the configured minimum.                                                                                                                                                 | <code>500</code>    | 8.0.0 |
| **`launchAutoHide`**            | <code>boolean</code>                                                                                                          | Whether to auto hide the splash after launchShowDuration.                                                                                                                                                                                                                                                                                                                                                                                                                                             | <code>true</code>   | 8.0.0 |
| **`launchFadeOutDuration`**     | <code>number</code>                                                                                                           | Duration for the fade out animation of the launch splash screen (in ms) Only available for Android, when using the Android 12 Splash Screen API.                                                                                                                                                                                                                                                                                                                                                      | <code>200</code>    | 8.0.0 |
| **`backgroundColor`**           | <code>string</code>                                                                                                           | Color of the background of the Splash Screen in hex format, #RRGGBB or #RRGGBBAA. Doesn't work if `useDialog` is true or on launch when using the Android 12 API.                                                                                                                                                                                                                                                                                                                                     |                     | 8.0.0 |
| **`backgroundColorDark`**       | <code>string</code>                                                                                                           | Color of the background of the Splash Screen in dark mode, in hex format, #RRGGBB or #RRGGBBAA. When the system is in dark mode and this key is set, it overrides `backgroundColor`. Only available on Android and iOS.                                                                                                                                                                                                                                                                               |                     | 8.0.0 |
| **`androidSplashResourceName`** | <code>string</code>                                                                                                           | Name of the resource to be used as Splash Screen. Doesn't work on launch when using the Android 12 API. Only available on Android.                                                                                                                                                                                                                                                                                                                                                                    | <code>splash</code> | 8.0.0 |
| **`androidScaleType`**          | <code>'CENTER' \| 'CENTER_CROP' \| 'CENTER_INSIDE' \| 'FIT_CENTER' \| 'FIT_END' \| 'FIT_START' \| 'FIT_XY' \| 'MATRIX'</code> | The [ImageView.ScaleType](https://developer.android.com/reference/android/widget/ImageView.ScaleType) used to scale the Splash Screen image. Doesn't work if `useDialog` is true or on launch when using the Android 12 API. Only available on Android.                                                                                                                                                                                                                                               | <code>FIT_XY</code> | 8.0.0 |
| **`showSpinner`**               | <code>boolean</code>                                                                                                          | Show a loading spinner centered on the splash surface. The spinner renders as the topmost content layer, so the entrance animation covers it and it disappears with the splash on hide.                                                                                                                                                                                                                                                                                                               | <code>false</code>  | 8.0.0 |
| **`androidSpinnerStyle`**       | <code>'horizontal' \| 'small' \| 'large' \| 'inverse' \| 'smallInverse' \| 'largeInverse'</code>                              | Style of the Android spinner. Unknown values fall back to `large`. Only available on Android.                                                                                                                                                                                                                                                                                                                                                                                                         | <code>large</code>  | 8.0.0 |
| **`iosSpinnerStyle`**           | <code>'small' \| 'large'</code>                                                                                               | Style of the iOS spinner. Only `small` and `large` are differentiated; any other value falls back to the platform default. Only available on iOS.                                                                                                                                                                                                                                                                                                                                                     | <code>small</code>  | 8.0.0 |
| **`spinnerColor`**              | <code>string</code>                                                                                                           | Color of the spinner in hex format, #RRGGBB or #RRGGBBAA. An invalid value keeps the platform default spinner color.                                                                                                                                                                                                                                                                                                                                                                                  |                     | 8.0.0 |
| **`splashFullScreen`**          | <code>boolean</code>                                                                                                          | Hide the status bar on the Splash Screen. Doesn't work on launch when using the Android 12 API. Only available on Android.                                                                                                                                                                                                                                                                                                                                                                            |                     | 8.0.0 |
| **`splashImmersive`**           | <code>boolean</code>                                                                                                          | Hide the status bar and the software navigation buttons on the Splash Screen. Doesn't work on launch when using the Android 12 API. Only available on Android.                                                                                                                                                                                                                                                                                                                                        |                     | 8.0.0 |
| **`layoutName`**                | <code>string</code>                                                                                                           | If `useDialog` is set to true, configure the Dialog layout. If `useDialog` is not set or false, use a layout instead of the ImageView. Doesn't work on launch when using the Android 12 API. Only available on Android.                                                                                                                                                                                                                                                                               |                     | 8.0.0 |
| **`useDialog`**                 | <code>boolean</code>                                                                                                          | Use a Dialog instead of an ImageView. If `layoutName` is not configured, it will use a layout that uses the splash image as background. Doesn't work on launch when using the Android 12 API. Only available on Android.                                                                                                                                                                                                                                                                              |                     | 8.0.0 |
| **`imageResourceName`**         | <code>string</code>                                                                                                           | Name of the resource used as the static splash image. On Android this is a drawable resource name; on iOS it resolves from the asset catalog or the app bundle. When unset, the official `androidSplashResourceName` key is honored (default: `splash`). Only available on Android and iOS.                                                                                                                                                                                                           | <code>splash</code> | 8.0.0 |
| **`imageResourceNameDark`**     | <code>string</code>                                                                                                           | Name of the resource used as the static splash image in dark mode. When the system is in dark mode and this key is set, it overrides `imageResourceName`. Only available on Android and iOS.                                                                                                                                                                                                                                                                                                          |                     | 8.0.0 |
| **`imageGravity`**              | <code><a href="#splashgravity">SplashGravity</a></code>                                                                       | Placement of the static splash image inside the splash surface. When set, the image view is sized to its intrinsic bitmap size and positioned by gravity; `androidScaleType` still scales the bitmap inside that view. When unset, the image view fills the surface (official behavior). Only available on Android and iOS.                                                                                                                                                                           | <code>center</code> | 8.0.0 |
| **`backgroundImage`**           | <code><a href="#imageoptions">ImageOptions</a></code>                                                                         | Background image layer rendered behind the splash content. Only available on Android and iOS.                                                                                                                                                                                                                                                                                                                                                                                                         |                     | 8.0.0 |
| **`brandingImage`**             | <code><a href="#imageoptions">ImageOptions</a></code>                                                                         | Small branding image anchored to an edge of the splash surface (co-branding logo, Android 12 style). Only available on Android and iOS.                                                                                                                                                                                                                                                                                                                                                               |                     | 8.0.0 |
| **`entrance`**                  | <code><a href="#entranceoptions">EntranceOptions</a></code>                                                                   | Entrance animation applied to the splash content when it appears. Only available on Android and iOS.                                                                                                                                                                                                                                                                                                                                                                                                  |                     | 8.0.0 |
| **`backgroundGradient`**        | <code><a href="#backgroundgradientoptions">BackgroundGradientOptions</a></code>                                               | Vertical gradient background of the splash surface. When set, it overrides `backgroundColor` / `backgroundColorDark`. Only available on Android and iOS.                                                                                                                                                                                                                                                                                                                                              |                     | 8.0.0 |
| **`launchLottie`**              | <code><a href="#lottieoptions">LottieOptions</a></code>                                                                       | Lottie animation shown on top of (or instead of) the static splash image during launch. On Android and iOS it is played by the native Lottie runtimes. On web, the launch overlay plays it via `@lottiefiles/dotlottie-web` (lazy-loaded); the web launch defaults are supplied to `SplashMotionWeb.setConfig()` because web cannot read the static plugin config. The web player requires the app to serve `dotlottie-player.wasm` as a local asset and pass its URL as `wasmUrl` in the web config. |                     | 8.0.0 |
| **`launchHaptic`**              | <code><a href="#hapticoptions">HapticOptions</a></code>                                                                       | Haptic feedback fired when the launch splash screen appears. Only available on Android and iOS.                                                                                                                                                                                                                                                                                                                                                                                                       |                     | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "SplashMotion": {
      "verboseLogging": false,
      "launchShowDuration": 3000,
      "launchAutoHide": true,
      "launchFadeOutDuration": 3000,
      "backgroundColor": "#ffffffff",
      "backgroundColorDark": "#000000",
      "androidSplashResourceName": "splash",
      "androidScaleType": "CENTER_CROP",
      "showSpinner": true,
      "androidSpinnerStyle": "large",
      "iosSpinnerStyle": "small",
      "spinnerColor": "#999999",
      "splashFullScreen": true,
      "splashImmersive": true,
      "layoutName": "launch_screen",
      "useDialog": true,
      "imageResourceName": "splash",
      "imageResourceNameDark": "splash_dark",
      "imageGravity": "bottom",
      "backgroundImage": {
        "alpha": 1,
        "margin": 16
      },
      "brandingImage": {},
      "entrance": {
        "durationMs": 300
      },
      "backgroundGradient": {},
      "launchLottie": {
        "loop": true,
        "speed": 1
      },
      "launchHaptic": {
        "style": "medium"
      }
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/splash-motion" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    SplashMotion: {
      verboseLogging: false,
      launchShowDuration: 3000,
      launchAutoHide: true,
      launchFadeOutDuration: 3000,
      backgroundColor: '#ffffffff',
      backgroundColorDark: '#000000',
      androidSplashResourceName: 'splash',
      androidScaleType: 'CENTER_CROP',
      showSpinner: true,
      androidSpinnerStyle: 'large',
      iosSpinnerStyle: 'small',
      spinnerColor: '#999999',
      splashFullScreen: true,
      splashImmersive: true,
      layoutName: 'launch_screen',
      useDialog: true,
      imageResourceName: 'splash',
      imageResourceNameDark: 'splash_dark',
      imageGravity: 'bottom',
      backgroundImage: {
        alpha: 1,
        margin: 16,
      },
      brandingImage: {},
      entrance: {
        durationMs: 300,
      },
      backgroundGradient: {},
      launchLottie: {
        loop: true,
        speed: 1,
      },
      launchHaptic: {
        style: 'medium',
      },
    },
  },
};

export default config;
```

</docgen-config>

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "SplashMotion": {
      "launchShowDuration": 3000,
      "launchAutoHide": true,
      "backgroundColor": "#ffffffff",
      "launchLottie": {
        "source": "public/lottie/splash.json",
        "darkSource": "public/lottie/splash-dark.json",
        "loop": true,
        "speed": 1
      },
      "launchHaptic": {
        "style": "medium"
      }
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/splash-motion" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    SplashMotion: {
      launchShowDuration: 3000,
      launchAutoHide: true,
      backgroundColor: '#ffffffff',
      launchLottie: {
        source: 'public/lottie/splash.json',
        darkSource: 'public/lottie/splash-dark.json',
        loop: true,
        speed: 1,
      },
      launchHaptic: {
        style: 'medium',
      },
    },
  },
};

export default config;
```

---

## API

<docgen-index>

- [`show(...)`](#show)
- [`hide(...)`](#hide)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the SplashMotion Capacitor plugin.

The method surface is API-compatible with the official
`@capacitor/splash-screen` plugin, so it works as a drop-in
replacement, with Lottie animation and haptic feedback extensions.

### show(...)

```typescript
show(options?: ShowOptions) => Promise<void>
```

Show the splash screen.

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#showoptions">ShowOptions</a></code> |

**Since:** 8.0.0

---

### hide(...)

```typescript
hide(options?: HideOptions) => Promise<void>
```

Hide the splash screen.

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#hideoptions">HideOptions</a></code> |

**Since:** 8.0.0

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Get the plugin version.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { SplashMotion } from '@cap-kit/splash-motion';

const { version } = await SplashMotion.getPluginVersion();
```

---

### Interfaces

#### ShowOptions

Options for showing the splash screen.

Extends the official `@capacitor/splash-screen` <a href="#showoptions">`ShowOptions`</a> with
Lottie and haptic feedback controls, keeping full API compatibility.

| Prop                     | Type                                                                            | Description                                                                                                                                                                                                                                                                                                                               | Default           | Since |
| ------------------------ | ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ----- |
| **`autoHide`**           | <code>boolean</code>                                                            | Whether to auto hide the splash after showDuration.                                                                                                                                                                                                                                                                                       |                   | 8.0.0 |
| **`fadeInDuration`**     | <code>number</code>                                                             | How long (in ms) to fade in.                                                                                                                                                                                                                                                                                                              | <code>200</code>  | 8.0.0 |
| **`fadeOutDuration`**    | <code>number</code>                                                             | How long (in ms) to fade out.                                                                                                                                                                                                                                                                                                             | <code>200</code>  | 8.0.0 |
| **`showDuration`**       | <code>number</code>                                                             | How long to show the splash screen when autoHide is enabled (in ms).                                                                                                                                                                                                                                                                      | <code>3000</code> | 8.0.0 |
| **`lottie`**             | <code><a href="#lottieoptions">LottieOptions</a></code>                         | Lottie animation to render while the splash is visible. Overrides the static `launchLottie` configuration for this show call. Available on Android, iOS and web. On web, `darkSource` is resolved against `prefers-color-scheme` and the WASM player URL comes from the web config (`SplashMotionWeb.setConfig()`), not from this option. |                   | 8.0.0 |
| **`image`**              | <code><a href="#imageoptions">ImageOptions</a></code>                           | Static image to render while the splash is visible. Overrides the static `imageResourceName` configuration for this show call. Only available on Android and iOS.                                                                                                                                                                         |                   | 8.0.0 |
| **`backgroundImage`**    | <code><a href="#imageoptions">ImageOptions</a></code>                           | Background image layer rendered behind the splash content. Overrides the static `backgroundImage` configuration for this show call. Only available on Android and iOS.                                                                                                                                                                    |                   | 8.0.0 |
| **`brandingImage`**      | <code><a href="#imageoptions">ImageOptions</a></code>                           | Small branding image anchored to an edge of the splash surface. Overrides the static `brandingImage` configuration for this show call. Only available on Android and iOS.                                                                                                                                                                 |                   | 8.0.0 |
| **`entrance`**           | <code><a href="#entranceoptions">EntranceOptions</a></code>                     | Entrance animation applied to the splash content when it appears. Overrides the static `entrance` configuration for this show call. Only available on Android and iOS.                                                                                                                                                                    |                   | 8.0.0 |
| **`backgroundGradient`** | <code><a href="#backgroundgradientoptions">BackgroundGradientOptions</a></code> | Vertical gradient background of the splash surface. Overrides the static `backgroundGradient` configuration for this show call. Only available on Android and iOS.                                                                                                                                                                        |                   | 8.0.0 |
| **`spinner`**            | <code><a href="#spinneroptions">SpinnerOptions</a></code>                       | Loading spinner rendered centered on top of the splash content. Overrides the static `showSpinner` / `androidSpinnerStyle` / `iosSpinnerStyle` / `spinnerColor` configuration for this show call. Only available on Android and iOS.                                                                                                      |                   | 8.0.0 |
| **`haptic`**             | <code><a href="#hapticoptions">HapticOptions</a></code>                         | Haptic feedback fired when the splash appears. Only available on Android and iOS.                                                                                                                                                                                                                                                         |                   | 8.0.0 |

#### LottieOptions

Options for the Lottie animation rendered by the splash screen.

The `source` (and `darkSource`) accept:

- a path relative to the web asset root (e.g. `'public/lottie/splash.json'`)
  for `.json` Lottie files or `.lottie` dotLottie archives
- a full remote HTTPS URL (e.g. `'https://example.com/splash.lottie'`)

When the system is in dark mode and `darkSource` is set, `darkSource`
is rendered instead of `source`.

| Prop             | Type                 | Description                                                                                                                                         | Default           | Since |
| ---------------- | -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ----- |
| **`source`**     | <code>string</code>  | Path to the Lottie JSON file (`.json`) or dotLottie archive (`.lottie`): local asset or remote HTTPS URL.                                           |                   | 8.0.0 |
| **`darkSource`** | <code>string</code>  | Alternative Lottie resource rendered when the system is in dark mode. Same accepted forms as `source`. When unset, `source` is used for both modes. |                   | 8.0.0 |
| **`loop`**       | <code>boolean</code> | Whether the animation loops while the splash is visible.                                                                                            | <code>true</code> | 8.0.0 |
| **`speed`**      | <code>number</code>  | Playback speed multiplier.                                                                                                                          | <code>1</code>    | 8.0.0 |

#### ImageOptions

Options describing a splash image layer (background layer, branding
image, or per-call static image).

| Prop             | Type                                                        | Description                                                                                                                                                                             | Default                  | Since |
| ---------------- | ----------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------ | ----- |
| **`source`**     | <code>string</code>                                         | Image resource: a drawable resource name on Android, an asset catalog or app bundle name on iOS. A value containing `/` is resolved as a web asset path relative to the web asset root. |                          | 8.0.0 |
| **`darkSource`** | <code>string</code>                                         | Alternative image rendered when the system is in dark mode. Same accepted forms as `source`. When unset, `source` is used for both modes.                                               |                          | 8.0.0 |
| **`gravity`**    | <code><a href="#splashgravity">SplashGravity</a></code>     | Placement of the image inside the splash surface.                                                                                                                                       | <code>center</code>      | 8.0.0 |
| **`scaleType`**  | <code><a href="#splashscaletype">SplashScaleType</a></code> | Scaling applied to the bitmap inside its view.                                                                                                                                          | <code>CENTER_CROP</code> | 8.0.0 |
| **`alpha`**      | <code>number</code>                                         | Opacity of the image, from 0 (transparent) to 1 (opaque).                                                                                                                               | <code>1</code>           | 8.0.0 |
| **`margin`**     | <code>number</code>                                         | Margin between the image and the splash surface edge, in dp (Android) or points (iOS). Only applies when the image is anchored to an edge.                                              | <code>16</code>          | 8.0.0 |

#### EntranceOptions

Entrance animation applied to the splash content when it appears.

| Prop             | Type                                     | Description                                                                                                                    | Default           | Since |
| ---------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | ----------------- | ----- |
| **`type`**       | <code>'none' \| 'fade' \| 'scale'</code> | Animation type applied to the whole content of the splash surface (all layers together). The surface fade in/out is unchanged. | <code>none</code> | 8.0.0 |
| **`durationMs`** | <code>number</code>                      | Duration of the entrance animation, in ms. Clamped to 0–2000.                                                                  | <code>300</code>  | 8.0.0 |

#### BackgroundGradientOptions

Vertical gradient background of the splash surface.

Colors use hex format, #RRGGBB or #RRGGBBAA. When the system is in
dark mode, the dark variants win when set; otherwise the light colors
are used for both modes. When this option is set it overrides
`backgroundColor` / `backgroundColorDark`.

| Prop             | Type                | Description                                                      | Since |
| ---------------- | ------------------- | ---------------------------------------------------------------- | ----- |
| **`top`**        | <code>string</code> | Gradient color at the top of the splash surface.                 | 8.0.0 |
| **`bottom`**     | <code>string</code> | Gradient color at the bottom of the splash surface.              | 8.0.0 |
| **`topDark`**    | <code>string</code> | Gradient color at the top of the splash surface in dark mode.    | 8.0.0 |
| **`bottomDark`** | <code>string</code> | Gradient color at the bottom of the splash surface in dark mode. | 8.0.0 |

#### SpinnerOptions

Loading spinner rendered centered on top of the splash content.

| Prop        | Type                                                  | Description                                                                                                          | Default            | Since |
| ----------- | ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`show`**  | <code>boolean</code>                                  | Whether the spinner is shown.                                                                                        | <code>false</code> | 8.0.0 |
| **`style`** | <code><a href="#spinnerstyle">SpinnerStyle</a></code> | Style of the native spinner.                                                                                         | <code>large</code> | 8.0.0 |
| **`color`** | <code>string</code>                                   | Color of the spinner in hex format, #RRGGBB or #RRGGBBAA. An invalid value keeps the platform default spinner color. |                    | 8.0.0 |

#### HapticOptions

Haptic feedback options fired on splash visibility transitions.

| Prop        | Type                                        | Description                             | Default               | Since |
| ----------- | ------------------------------------------- | --------------------------------------- | --------------------- | ----- |
| **`style`** | <code>'light' \| 'medium' \| 'heavy'</code> | Impact strength of the haptic feedback. | <code>'medium'</code> | 8.0.0 |

#### HideOptions

Options for hiding the splash screen.

| Prop                  | Type                                                    | Description                                                                                                                                                       | Default          | Since |
| --------------------- | ------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- | ----- |
| **`fadeOutDuration`** | <code>number</code>                                     | How long (in ms) to fade out. On Android, if using the Android 12 Splash Screen API, it's not being used. Use launchFadeOutDuration configuration option instead. | <code>200</code> | 8.0.0 |
| **`haptic`**          | <code><a href="#hapticoptions">HapticOptions</a></code> | Haptic feedback fired when the splash disappears. Only available on Android and iOS.                                                                              |                  | 8.0.0 |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

### Type Aliases

#### SplashGravity

Placement of a splash layer inside the splash surface.

Nine named positions combining a vertical (top/center/bottom) and a
horizontal (start/center/end) anchor. `start`/`end` follow the reading
direction of the current layout direction (left in LTR layouts).

<code>'topStart' | 'top' | 'topEnd' | 'start' | 'center' | 'end' | 'bottomStart' | 'bottom' | 'bottomEnd'</code>

#### SplashScaleType

Scaling applied to a splash image bitmap inside its view.

Uses the official [ImageView.ScaleType](https://developer.android.com/reference/android/widget/ImageView.ScaleType)
names so the value is shared with the legacy `androidScaleType` key.
On iOS, `MATRIX` falls back to `CENTER`.

<code>'CENTER' | 'CENTER_CROP' | 'CENTER_INSIDE' | 'FIT_CENTER' | 'FIT_END' | 'FIT_START' | 'FIT_XY' | 'MATRIX'</code>

#### SpinnerStyle

Style of the native loading spinner rendered on the splash surface.

Android supports all six values (official `ProgressBar` styles). iOS
only differentiates small and large: `small` maps to the medium
activity indicator style, `large` to the large style, and the inverse
values fall back to the platform default.

<code>'horizontal' | 'small' | 'large' | 'inverse' | 'smallInverse' | 'largeInverse'</code>

</docgen-api>

---

## Usage

```ts
import { SplashMotion } from '@cap-kit/splash-motion';
```

Show the splash screen:

```ts
await SplashMotion.show({
  showDuration: 3000,
  autoHide: true,
  fadeInDuration: 200,
  fadeOutDuration: 200,
  lottie: {
    source: 'public/lottie/splash.json', // or a remote https URL
    darkSource: 'public/lottie/splash-dark.json', // rendered in dark mode
    loop: true,
    speed: 1,
  },
  haptic: {
    style: 'medium', // 'light' | 'medium' | 'heavy'
  },
});
```

Hide it manually (no-op if `autoHide` already fired):

```ts
await SplashMotion.hide({ fadeOutDuration: 200, haptic: { style: 'light' } });
```

### Migrating from `@capacitor/splash-screen`

1. Replace the dependency: `pnpm remove @capacitor/splash-screen && pnpm add @cap-kit/splash-motion`.
2. Rename the config key `plugins.SplashScreen` → `plugins.SplashMotion` and the import `SplashScreen` → `SplashMotion`.
3. Static config keys are accepted unchanged, plus the new `launchLottie` / `launchHaptic` extensions.
4. `show()` / `hide()` keep their official option shapes; the new `lottie` and `haptic` options are additive.

---

## Demo app

A working demo lives in `example-app/static/src/app/splash-motion/`: show/hide buttons with live
option controls (Lottie source, haptic style, durations), and a version readout. Apply it to the
shared demo via `pnpm run app:demo:prepare` from the package root.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

See [LICENSE](LICENSE).
