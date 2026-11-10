# Biometrics — Configuration

The plugin has two configuration layers: one static value read natively at
startup, and per-call prompt options passed to `authenticate()`.

## Static configuration

Define under the `Biometrics` key in `capacitor.config.ts`. Consumed
exclusively by native code during plugin initialization.

```ts
// capacitor.config.ts
const config: CapacitorConfig = {
  plugins: {
    Biometrics: {
      verboseLogging: false,
    },
  },
};
```

| Key              | Type      | Description                                       | Default | Since |
| ---------------- | --------- | ------------------------------------------------- | ------- | ----- |
| `verboseLogging` | `boolean` | Extra native logs (Logcat on Android, Xcode/iOS). | `false` | 8.0.0 |

## Per-call prompt options

`authenticate(options?: AuthenticateOptions)`:

| Option                   | Platform | Description                                                              | Default        |
| ------------------------ | -------- | ------------------------------------------------------------------------ | -------------- |
| `reason`                 | Both     | Reason shown in the dialog; falls back to `description`.                 | System default |
| `title`                  | Android  | Dialog title.                                                            | System default |
| `subtitle`               | Android  | Dialog subtitle.                                                         | System default |
| `description`            | Both     | Dialog description (also the iOS localized reason).                      | System default |
| `cancelButtonText`       | Both     | Cancel button text. Not shown on Android when device credential is in the authenticator set. | `"Cancel"` |
| `iosFallbackButtonText`  | iOS      | Fallback button title. Empty string hides it.                            | System default |
| `allowDeviceCredential`  | Both     | Offer device credential (PIN/pattern/password/passcode) fallback.        | `true`         |

Notes:

- Unknown option values are ignored, never rejected.
- This plugin is intentionally stateless: there is no runtime `configure()`
  — the last `authenticate()` options apply to that ceremony only.
