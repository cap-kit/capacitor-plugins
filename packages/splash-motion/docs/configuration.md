# SplashMotion — Configuration

The plugin has two configuration layers: one static value read natively at
startup, and per-call options passed to `show()` / `hide()`.

## Static configuration

Define under the `SplashMotion` key in `capacitor.config.ts`. Consumed
exclusively by native code during plugin initialization. Static keys are
API-compatible with the official `@capacitor/splash-screen` configuration.

```ts
// capacitor.config.ts
const config: CapacitorConfig = {
  plugins: {
    SplashMotion: {
      verboseLogging: false,
      launchShowDuration: 3000,
      launchAutoHide: true,
      backgroundColor: '#ffffffff',
      imageResourceName: 'splash',
      imageResourceNameDark: 'splash_dark',
      brandingImage: { source: 'branding', gravity: 'bottomEnd', margin: 16 },
      entrance: { type: 'fade', durationMs: 300 },
      launchLottie: { source: 'public/lottie/splash.json' },
      launchHaptic: { style: 'medium' },
    },
  },
};
```

### Launch keys

| Key                     | Type            | Description                                                                     | Default | Since |
| ----------------------- | --------------- | ------------------------------------------------------------------------------- | ------- | ----- |
| `verboseLogging`        | `boolean`       | Extra native logs (Logcat on Android, Xcode console on iOS).                    | `false` | 8.0.0 |
| `launchShowDuration`    | `number`        | How long to show the launch splash when autoHide is enabled (ms).               | `500`   | 8.0.0 |
| `launchAutoHide`        | `boolean`       | Auto hide the launch splash after launchShowDuration.                           | `true`  | 8.0.0 |
| `launchFadeOutDuration` | `number`        | Fade out of the launch splash (ms). Applies to the plugin's own splash surface. | `200`   | 8.0.0 |
| `launchLottie`          | `LottieOptions` | Lottie animation shown during launch.                                           | —       | 8.0.0 |
| `launchHaptic`          | `HapticOptions` | Haptic feedback fired when the launch splash appears.                           | —       | 8.0.0 |

### Appearance keys

| Key                     | Type                        | Description                                                                                                                                                    | Default  | Since |
| ----------------------- | --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ----- |
| `backgroundColor`       | `string`                    | Splash background color, `#RRGGBB` or `#RRGGBBAA`.                                                                                                             | system   | 8.0.0 |
| `backgroundColorDark`   | `string`                    | Splash background color in dark mode. Overrides `backgroundColor` when the system is in dark mode.                                                             | —        | 8.0.0 |
| `backgroundGradient`    | `BackgroundGradientOptions` | Vertical gradient background. When valid, it overrides `backgroundColor` / `backgroundColorDark`.                                                              | —        | 8.0.0 |
| `imageResourceName`     | `string`                    | Static splash image resource (drawable on Android, asset catalog / bundle name on iOS). Falls back to the official `androidSplashResourceName`, then `splash`. | `splash` | 8.0.0 |
| `imageResourceNameDark` | `string`                    | Static splash image resource in dark mode. When unset, the light resource is used.                                                                             | —        | 8.0.0 |
| `imageGravity`          | `SplashGravity`             | Placement of the static image. When unset, the image view fills the surface (official behavior).                                                               | —        | 8.0.0 |
| `backgroundImage`       | `ImageOptions`              | Background image layer rendered behind the splash content.                                                                                                     | —        | 8.0.0 |
| `brandingImage`         | `ImageOptions`              | Small branding image anchored to an edge (default `bottomEnd`, 16 margin).                                                                                     | —        | 8.0.0 |
| `entrance`              | `EntranceOptions`           | Entrance animation applied to the splash content when it appears.                                                                                              | —        | 8.0.0 |

### Official parity keys

Accepted with the same meaning as the official `@capacitor/splash-screen`:

| Key                         | Type      | Description                                                                                                                                    | Default  |
| --------------------------- | --------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| `androidSplashResourceName` | `string`  | Fallback resource name for the static splash image (used when `imageResourceName` is unset). Honored on Android and iOS.                       | `splash` |
| `androidScaleType`          | `string`  | Scale type applied to the static splash image (honored on Android and iOS).                                                                    | `FIT_XY` |
| `splashFullScreen`          | `boolean` | Hide the status bar on the splash surface (Android).                                                                                           | `false`  |
| `splashImmersive`           | `boolean` | Hide status bar and navigation buttons (Android).                                                                                              | `false`  |
| `showSpinner`               | `boolean` | Show a loading spinner centered on the splash surface.                                                                                         | `false`  |
| `androidSpinnerStyle`       | `string`  | Style of the Android spinner (`horizontal`, `small`, `large`, `inverse`, `smallInverse`, `largeInverse`). Unknown values fall back to `large`. | `large`  |
| `iosSpinnerStyle`           | `string`  | Style of the iOS spinner (`small` or `large`). Unknown values fall back to the platform default.                                               | `small`  |
| `spinnerColor`              | `string`  | Color of the spinner in hex format (#RRGGBB or #RRGGBBAA). Invalid values keep the platform default color.                                     | —        |
| `layoutName`                | `string`  | Accepted for API parity. Currently inert.                                                                                                      | —        |
| `useDialog`                 | `boolean` | Accepted for API parity. Currently inert.                                                                                                      | `false`  |

Notes:

- The plugin always renders its own full-screen surface, so `useDialog`
  behavior is effectively built in.
- `layoutName` and `useDialog` remain inert: they are validated and
  stored but have no native effect, and are kept so existing
  `@capacitor/splash-screen` configs keep working unchanged.
- The spinner renders centered as the topmost layer of the splash
  content: it appears and disappears with the surface, and the entrance
  animation covers it like every other layer.

## Per-call options

`show(options?: ShowOptions)`:

| Option               | Type                        | Default | Description                                                     |
| -------------------- | --------------------------- | ------- | --------------------------------------------------------------- |
| `autoHide`           | `boolean`                   | `true`  | Auto hide the splash after showDuration.                        |
| `fadeInDuration`     | `number`                    | `200`   | Fade in duration (ms).                                          |
| `fadeOutDuration`    | `number`                    | `200`   | Fade out duration (ms).                                         |
| `showDuration`       | `number`                    | `3000`  | Show duration when autoHide is enabled (ms).                    |
| `lottie`             | `LottieOptions`             | —       | Lottie animation; overrides the static `launchLottie`.          |
| `image`              | `ImageOptions`              | —       | Static image; overrides the static `imageResourceName`.         |
| `backgroundImage`    | `ImageOptions`              | —       | Background image layer; overrides the static `backgroundImage`. |
| `brandingImage`      | `ImageOptions`              | —       | Branding image; overrides the static `brandingImage`.           |
| `entrance`           | `EntranceOptions`           | —       | Entrance animation; overrides the static `entrance`.            |
| `backgroundGradient` | `BackgroundGradientOptions` | —       | Gradient background; overrides the static `backgroundGradient`. |
| `spinner`            | `SpinnerOptions`            | —       | Loading spinner; overrides the static spinner configuration.    |
| `haptic`             | `HapticOptions`             | —       | Haptic feedback fired when the splash appears.                  |

`hide(options?: HideOptions)`:

| Option            | Type            | Default | Description                                       |
| ----------------- | --------------- | ------- | ------------------------------------------------- |
| `fadeOutDuration` | `number`        | `200`   | Fade out duration (ms).                           |
| `haptic`          | `HapticOptions` | —       | Haptic feedback fired when the splash disappears. |

`LottieOptions`:

| Option       | Type      | Default | Description                                                    |
| ------------ | --------- | ------- | -------------------------------------------------------------- |
| `source`     | `string`  | —       | Lottie `.json` / dotLottie `.lottie` path or HTTPS URL.        |
| `darkSource` | `string`  | —       | Alternative resource rendered when the system is in dark mode. |
| `loop`       | `boolean` | `true`  | Loop the animation while the splash is visible.                |
| `speed`      | `number`  | `1`     | Playback speed multiplier (clamped 0.1–10).                    |

`HapticOptions`:

| Option  | Type     | Default    | Description                   |
| ------- | -------- | ---------- | ----------------------------- |
| `style` | `string` | `'medium'` | `light`, `medium` or `heavy`. |

`SpinnerOptions`:

| Option  | Type           | Default                                      | Description                                                                         |
| ------- | -------------- | -------------------------------------------- | ----------------------------------------------------------------------------------- |
| `show`  | `boolean`      | `true` (when passed)                         | Whether the spinner is shown.                                                       |
| `style` | `SpinnerStyle` | `'large'` (Android) / platform default (iOS) | Spinner style. Unknown values fall back.                                            |
| `color` | `string`       | platform default                             | Spinner color in hex (#RRGGBB or #RRGGBBAA). Invalid values keep the default color. |

Passing a `spinner` object replaces the whole static spinner configuration
for that call; the spinner shows unless `show: false` is explicit.

`ImageOptions` (used by `image`, `backgroundImage`, `brandingImage`):

| Option       | Type              | Default         | Description                                                                                                                                                |
| ------------ | ----------------- | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `source`     | `string`          | —               | Drawable resource name (Android) or asset catalog / bundle name (iOS). A value containing `/` resolves as a web asset path relative to the web asset root. |
| `darkSource` | `string`          | —               | Alternative resource rendered when the system is in dark mode.                                                                                             |
| `gravity`    | `SplashGravity`   | `'center'`      | Placement of the image inside the splash surface.                                                                                                          |
| `scaleType`  | `SplashScaleType` | `'CENTER_CROP'` | Scaling applied to the bitmap inside its view.                                                                                                             |
| `alpha`      | `number`          | `1`             | Opacity, clamped 0–1.                                                                                                                                      |
| `margin`     | `number`          | `16`            | Margin to the surface edge, clamped 0–200.                                                                                                                 |

`EntranceOptions`:

| Option       | Type     | Default  | Description                                                    |
| ------------ | -------- | -------- | -------------------------------------------------------------- |
| `type`       | `string` | `'none'` | `none`, `fade` or `scale`. Unknown values fall back to `none`. |
| `durationMs` | `number` | `300`    | Entrance animation duration (ms), clamped 0–2000.              |

`BackgroundGradientOptions`:

| Option       | Type     | Default | Description                                        |
| ------------ | -------- | ------- | -------------------------------------------------- |
| `top`        | `string` | —       | Gradient color at the top (`#RRGGBB`/`#RRGGBBAA`). |
| `bottom`     | `string` | —       | Gradient color at the bottom.                      |
| `topDark`    | `string` | —       | Gradient top color in dark mode.                   |
| `bottomDark` | `string` | —       | Gradient bottom color in dark mode.                |

Notes:

- Durations are clamped to the 0–60000 ms range.
- Unknown `haptic.style` values fall back to `medium` instead of
  rejecting the call.
- Image layers are lenient: unknown `gravity`/`scaleType` values fall
  back to their defaults, and an image whose source cannot be resolved
  is silently skipped.
- `backgroundGradient` requires both `top` and `bottom` to be valid
  colors; in dark mode a valid `topDark`/`bottomDark` pair wins, and an
  invalid dark pair silently falls back to the light pair.
