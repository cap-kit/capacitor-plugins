# SplashMotion — Usage Guide

## Quick start

```bash
pnpm add @cap-kit/splash-motion
npx cap sync
```

```ts
import { SplashMotion } from '@cap-kit/splash-motion';

// Show a Lottie splash for 3 seconds, then auto-hide.
await SplashMotion.show({
  showDuration: 3000,
  autoHide: true,
  lottie: { source: 'public/lottie/splash.json' },
});
```

## Basic control

```ts
// Show with defaults (auto-hide after 3000ms)
await SplashMotion.show();

// Show manually and hide when your app is ready
await SplashMotion.show({ autoHide: false });
await SplashMotion.hide({ fadeOutDuration: 300 });

// Hide with a haptic confirmation
await SplashMotion.hide({ haptic: { style: 'light' } });
```

## Plugin version

```ts
const { version } = await SplashMotion.getPluginVersion();
```

Returns the native plugin version on Android and iOS. On the web it returns the
JavaScript package version.

## Launch splash

Configure the splash that appears while the app boots via the static
configuration in `capacitor.config.ts`:

```ts
const config: CapacitorConfig = {
  plugins: {
    SplashMotion: {
      launchShowDuration: 3000,
      launchAutoHide: true,
      backgroundColor: '#ffffff',
      launchLottie: {
        source: 'public/lottie/splash.json',
        loop: true,
        speed: 1,
      },
      launchHaptic: {
        style: 'medium',
      },
    },
  },
};
```

The plugin fires the launch splash during its native load phase and
auto-hides it after `launchShowDuration` when `launchAutoHide` is true.

## Image layers

Every splash is composed from stacked layers, rendered in this order:

1. **Background** — flat `backgroundColor` (with `backgroundColorDark`)
   or a vertical `backgroundGradient`.
2. **Background image** — `backgroundImage`, filling the whole surface.
3. **Main content** — the `lottie` animation when provided, otherwise the
   static image (`imageResourceName` / `imageResourceNameDark`).
4. **Branding image** — `brandingImage`, anchored to an edge
   (default `bottomEnd`, margin 16).

All content layers (2–4) live inside one container, so the `entrance`
animation moves them together:

```ts
await SplashMotion.show({
  backgroundImage: { source: 'public/images/bg.png', alpha: 0.6 },
  image: { source: 'splash_logo', gravity: 'center', scaleType: 'CENTER_INSIDE' },
  brandingImage: { source: 'brand', gravity: 'bottomEnd', margin: 16 },
  entrance: { type: 'scale', durationMs: 400 },
});
```

### Gravity and scale type

`gravity` positions the image view inside the splash surface (9 values:
`topStart`, `top`, `topEnd`, `start`, `center`, `end`, `bottomStart`,
`bottom`, `bottomEnd`; `start`/`end` follow the reading direction).
`scaleType` scales the bitmap inside its view and uses the official Android
`ImageView.ScaleType` names (`CENTER`, `CENTER_CROP`, `CENTER_INSIDE`,
`FIT_CENTER`, `FIT_END`, `FIT_START`, `FIT_XY`, `MATRIX`). Both coexist:
gravity decides _where_, scale type decides _how the image fills its view_.

Image sources: a drawable resource name on Android, an asset catalog / app
bundle name on iOS, or a web asset path containing `/` (resolved from the
web asset root on both platforms).

### Gradient background

```ts
await SplashMotion.show({
  backgroundGradient: {
    top: '#1a2a6c',
    bottom: '#b21f1f',
    topDark: '#0d1224',
    bottomDark: '#3a0d0d',
  },
});
```

The gradient replaces the flat background color when both `top` and `bottom`
are valid hex colors; in dark mode a valid `topDark`/`bottomDark` pair wins,
and an invalid dark pair silently falls back to the light pair.

All image/entrance/gradient options are also available as static config
keys (`backgroundImage`, `brandingImage`, `entrance`, `backgroundGradient`,
`imageResourceName`, `imageResourceNameDark`, `imageGravity`) and accept
per-call overrides in `show()`.

## Lottie setup

The `source` (and `darkSource`) accept two forms:

- **Local asset**: a path relative to the web asset root. Capacitor serves
  web assets from `public/` on disk, so a file placed at
  `public/lottie/splash.json` in your project resolves as
  `public/lottie/splash.json`. On both platforms the path is resolved
  against the native bundle (the `public/` prefix is kept, since Capacitor
  copies the web dir preserving its structure).
- **Remote URL**: a full `https://` URL. The composition is downloaded
  asynchronously; the splash background appears immediately and the
  animation starts as soon as it is ready.

Accepted file formats:

- **`.json`**: a raw Lottie composition.
- **`.lottie`**: a dotLottie archive (zip). The plugin extracts the first
  animation JSON under `animations/` (falling back to the first non-
  `manifest.json` JSON entry) and plays it like a plain composition.
  Works for both local assets and remote URLs.

Options:

| Option       | Type      | Default | Description                                 |
| ------------ | --------- | ------- | ------------------------------------------- |
| `source`     | `string`  | —       | Lottie path or HTTPS URL (required).        |
| `darkSource` | `string`  | —       | Alternative resource rendered in dark mode. |
| `loop`       | `boolean` | `true`  | Loop while the splash is visible.           |
| `speed`      | `number`  | `1`     | Playback speed multiplier (clamped 0.1–10). |

A `lottie` option passed to `show()` overrides the static
`launchLottie` configuration for that call.

## Dark mode

The plugin detects the system theme natively at render time:

- `backgroundColorDark` (static config) overrides `backgroundColor`
  when the system is in dark mode.
- `darkSource` inside `lottie` options replaces `source` when the
  system is in dark mode. When unset, `source` is used for both modes.

```ts
SplashMotion.show({
  lottie: {
    source: 'public/lottie/splash.json',
    darkSource: 'public/lottie/splash-dark.json',
  },
});
```

```json
{
  "plugins": {
    "SplashMotion": {
      "backgroundColor": "#ffffff",
      "backgroundColorDark": "#000000"
    }
  }
}
```

## Haptic feedback

Both `show()` and `hide()` accept a `haptic` option:

```ts
await SplashMotion.show({ haptic: { style: 'medium' } });
await SplashMotion.hide({ haptic: { style: 'light' } });
```

| Style    | Android                           | iOS                       |
| -------- | --------------------------------- | ------------------------- |
| `light`  | 20 ms vibration, low amplitude    | `.light` impact feedback  |
| `medium` | 40 ms vibration, medium amplitude | `.medium` impact feedback |
| `heavy`  | 80 ms vibration, full amplitude   | `.heavy` impact feedback  |

Unknown style values fall back to `medium`. The Android implementation
requires the `VIBRATE` permission (declared by the plugin's own
manifest) and silently no-ops on devices without a vibrator.

## Loading spinner

Enable a native loading indicator centered on the splash surface:

```ts
// Static config (capacitor.config.ts)
plugins: {
  SplashMotion: {
    showSpinner: true,
    androidSpinnerStyle: 'large',
    iosSpinnerStyle: 'small',
    spinnerColor: '#999999',
  },
}
```

```ts
// Per-call override
await SplashMotion.show({ spinner: { show: true, style: 'small', color: '#FFFFFF' } });
```

The spinner renders as the topmost layer of the splash content: it
appears and disappears with the surface, and the entrance animation
covers it like every other layer. Android renders an indeterminate
`ProgressBar` (official style mapping; unknown styles fall back to
`large`), iOS a `UIActivityIndicatorView` (`small` → `.medium`,
`large` → `.large`, anything else → platform default). An invalid
`spinnerColor` keeps the platform default color. `spinnerTimeout` is
intentionally not implemented: it is documented by the official plugin
but not present in its 8.x source.

## Web platform

The web implementation renders a real surface: `show()` creates a
full-viewport DOM overlay with the resolved background color (light/dark
via `prefers-color-scheme`) and, when a Lottie source is configured, plays
it with `@lottiefiles/dotlottie-web` (WASM player, lazily imported).
Fade in/out and the auto-hide schedule behave like the native platforms.

Web cannot read the static plugin config, so the launch defaults are
supplied programmatically:

```ts
import { SplashMotionWeb } from '@cap-kit/splash-motion';

SplashMotionWeb.setConfig({
  launchLottie: { source: 'public/lottie/splash.json' },
  backgroundColor: '#ffffff',
  launchAutoHide: true,
  launchShowDuration: 3000,
  // The WASM player is loaded from the library CDN unless the app serves
  // `dotlottie-player.wasm` locally and points `wasmUrl` at it.
  wasmUrl: 'assets/dotlottie-player.wasm',
});
```

Still native-only on web: static images, background image and gradient,
entrance animation, spinner and haptics are ignored. The overlay also
cannot cover the pre-bundle launch gap (before the app JavaScript runs) —
that requires a static splash in the host app `index.html`.

## Demo app

A working demo lives in `example-app/static/src/app/splash-motion/`:
show/hide buttons with live option controls (Lottie source, haptic
style, durations) and a version readout. Apply it to the shared demo via
`pnpm run app:demo:prepare` from the package root.
