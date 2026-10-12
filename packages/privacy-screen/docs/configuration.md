# PrivacyScreen — Configuration

The plugin has two configuration layers: one static value read natively at
startup, and per-call display options passed to `enable()`.

## Static configuration

Define under the `PrivacyScreen` key in `capacitor.config.ts`. Consumed
exclusively by native code during plugin initialization.

```ts
// capacitor.config.ts
const config: CapacitorConfig = {
  plugins: {
    PrivacyScreen: {
      verboseLogging: false,
    },
  },
};
```

| Key              | Type      | Description                                                                          | Default | Since |
| ---------------- | --------- | ------------------------------------------------------------------------------------ | ------- | ----- |
| `verboseLogging` | `boolean` | Extra native logs (Logcat on Android, Xcode/iOS).                                    | `false` | 8.0.0 |
| `enabled`        | `boolean` | Auto-enable protection at plugin load, without waiting for `enable()`.              | `false` | 8.1.0 |

## Per-call display options

`enable(config?: PrivacyScreenConfig)` accepts the official
`@capacitor/privacy-screen` shape:

```ts
await PrivacyScreen.enable({
  android: {
    dimBackground: true, // dim scrim instead of the opaque fallback
    privacyModeOnActivityHidden: 'splash', // 'none' | 'dim' | 'splash'
  },
  ios: {
    blurEffect: 'dark', // 'light' | 'dark' | 'none'
  },
});
```

| Option                                  | Platform | Description                                              | Default |
| --------------------------------------- | -------- | -------------------------------------------------------- | ------- |
| `android.dimBackground`                 | Android  | Translucent scrim instead of the opaque one.             | `false` |
| `android.privacyModeOnActivityHidden`   | Android  | Temporary scrim while the activity is hidden (e.g. a system biometric prompt): `none`, `dim`, or `splash`. | `'none'` |
| `android.preventScreenshots`            | Android  | Deprecated, ignored. `FLAG_SECURE` is always applied while enabled; use `disable()`/`enable()` around screens that may be captured. | — |
| `ios.blurEffect`                        | iOS      | Overlay blur style: `light`, `dark`, or `none` (system style). | `'none'` |

Notes:

- Unknown `blurEffect` / `privacyModeOnActivityHidden` values fall back to
  their defaults instead of rejecting.
- This plugin is intentionally stateless: there is no `getRuntimeConfig()`
  or `configure()` — the enabled flag plus the last `enable()` options are
  the whole state. Query it with `isEnabled()`.
