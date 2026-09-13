# Fortress — Configuration Guide

This guide covers the static configuration defined in `capacitor.config.ts`,
the full configuration reference including nested objects, runtime overrides
via `configure()`, and the v8 runtime persistence model.

## Static configuration

Configuration values for the Fortress plugin are defined under the
`plugins.Fortress` key inside `capacitor.config.ts` and consumed exclusively
by native code during plugin initialization.

Configuration values:

- do NOT change the JavaScript API shape
- do NOT enable/disable methods
- are applied once during plugin load

## Configuration reference

The following table documents every supported configuration property.

| Prop                                  | Type                                                         | Description                                                                                                                                                                                                                                                                                                                                            | Default                           | Since |
| ------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------- | ----- |
| **`verboseLogging`**                  | <code>boolean</code>                                         | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API.                                                                                                               | <code>false</code>                | 8.0.0 |
| **`logLevel`**                        | <code>'debug' \| 'error' \| 'warn' \| 'verbose'</code>       | Native/Web logging threshold. - `error`: errors only - `warn`: warnings and errors - `debug`: debug/info/warn/error - `verbose`: maximum logging level                                                                                                                                                                                                 | <code>'info'</code>               | 8.0.0 |
| **`lockAfterMs`**                     | <code>number</code>                                          | Global auto-lock timeout in milliseconds.                                                                                                                                                                                                                                                                                                              | <code>60000</code>                | 8.0.0 |
| **`accessControl`**                   | <code>BiometricAccessControl</code>                          | Security level for biometric hardware access.                                                                                                                                                                                                                                                                                                          | <code>'biometryCurrentSet'</code> | 8.0.0 |
| **`enablePrivacyScreen`**             | <code>boolean</code>                                         | Enables or disables privacy protection for app snapshots. Platform behavior: - Android relies on window snapshot protection in recents/task switcher. - iOS uses a visual privacy overlay. Note: On Android recents previews, system-protected cards may not render custom overlay text/image and can appear as a blank/protected preview.             | <code>true</code>                 | 8.0.0 |
| **`privacyOverlayText`**              | <code>string</code>                                          | Optional text rendered on top of the privacy screen overlay. This is intended for lock-state messaging such as "Session Locked" or "Tap to Unlock". Platform note: - Android: text is shown on the in-app overlay. - Android recents/task switcher: system snapshot protection may hide custom text in preview cards.                                  |                                   | 8.0.0 |
| **`privacyOverlayImageName`**         | <code>string</code>                                          | Optional native asset name rendered on top of the privacy screen overlay. Asset lookup rules: - iOS: Image from app asset catalog by name - Android: Drawable resource by name Platform note: - Android: image is shown on the in-app overlay. - Android recents/task switcher: system snapshot protection may hide custom images in preview cards.    |                                   | 8.0.0 |
| **`privacyOverlayShowText`**          | <code>boolean</code>                                         | Controls whether privacy overlay text is visible.                                                                                                                                                                                                                                                                                                      | <code>true</code>                 | 8.0.0 |
| **`privacyOverlayShowImage`**         | <code>boolean</code>                                         | Controls whether privacy overlay image is visible.                                                                                                                                                                                                                                                                                                     | <code>true</code>                 | 8.0.0 |
| **`privacyOverlayTextColor`**         | <code>string</code>                                          | Optional text color (hex string) for the privacy overlay label. Example: `#FFFFFF`                                                                                                                                                                                                                                                                     |                                   | 8.0.0 |
| **`privacyOverlayBackgroundOpacity`** | <code>number</code>                                          | Optional background opacity for the privacy overlay scrim. Allowed range: `0.0` to `1.0`.                                                                                                                                                                                                                                                              |                                   | 8.0.0 |
| **`privacyOverlayTheme`**             | <code>'system' \| 'light' \| 'dark'</code>                   | Controls the privacy overlay visual theme. - `system`: follow device appearance (light/dark) - `light`: force light overlay appearance - `dark`: force dark overlay appearance                                                                                                                                                                         | <code>'system'</code>             | 8.0.0 |
| **`obfuscationPrefix`**               | <code>string</code>                                          | Prefix used by key obfuscation utilities.                                                                                                                                                                                                                                                                                                              | <code>'ftrss\_'</code>            | 8.0.0 |
| **`webAuthn`**                        | <code>WebAuthnConfig</code>                                  | WebAuthn configuration for Web platform unlock behavior. - `local` mode stores credential metadata only in browser storage. - `server` mode uses backend challenge and assertion verification endpoints.                                                                                                                                               |                                   | 8.0.0 |
| **`allowCachedAuthentication`**       | <code>boolean</code>                                         | Enables in-memory cached authentication for unlock operations. When enabled, repeated `unlock()` calls within `cachedAuthenticationTimeoutMs` can skip the interactive biometric prompt.                                                                                                                                                               | <code>false</code>                | 8.0.0 |
| **`cachedAuthenticationTimeoutMs`**   | <code>number</code>                                          | Cached authentication validity window in milliseconds. This value is only used when `allowCachedAuthentication` is enabled.                                                                                                                                                                                                                            | <code>30000</code>                | 8.0.0 |
| **`cryptoStrategy`**                  | <code>'auto' \| 'ecc' \| 'rsa'</code>                        | Asymmetric key-pair strategy for cryptographic operations. - `auto`: platform default strategy - `ecc`: force elliptic-curve key generation where supported - `rsa`: force RSA key generation where supported                                                                                                                                          | <code>'auto'</code>               | 8.0.0 |
| **`keySize`**                         | <code>2048 \| 4096</code>                                    | RSA key size used when `cryptoStrategy` is set to `rsa`.                                                                                                                                                                                                                                                                                               | <code>2048</code>                 | 8.0.0 |
| **`maxBiometricAttempts`**            | <code>number</code>                                          | Maximum failed biometric attempts before temporary lockout.                                                                                                                                                                                                                                                                                            | <code>5</code>                    | 8.0.0 |
| **`lockoutDurationMs`**               | <code>number</code>                                          | Temporary lockout duration in milliseconds after reaching the biometric failure threshold.                                                                                                                                                                                                                                                             | <code>30000</code>                | 8.0.0 |
| **`requireFreshAuthenticationMs`**    | <code>number</code>                                          | Maximum allowed age in milliseconds for the last successful biometric authentication before requiring a fresh authentication.                                                                                                                                                                                                                          | <code>0 (disabled)</code>         | 8.0.0 |
| **`encryptionAlgorithm`**             | <code>'AES-GCM' \| 'AES-CBC'</code>                          | Symmetric encryption algorithm used by the Web secure storage layer. Native platforms keep hardware-backed secure defaults.                                                                                                                                                                                                                            | <code>'AES-GCM'</code>            | 8.0.0 |
| **`enableICloudKeychainSync`**        | <code>boolean</code>                                         | Enables iCloud Keychain synchronization for iOS secure-storage entries. Platform behavior: - iOS: when enabled, generic-password vault items are created as synchronizable - Android/Web: ignored (no-op)                                                                                                                                              | <code>false</code>                | 8.0.0 |
| **`persistSessionState`**             | <code>boolean</code>                                         | Persists web session lock/auth state across page reloads. Platform behavior: - Web: when enabled, vault/session state is restored from persisted storage - iOS/Android: ignored (no-op)                                                                                                                                                                | <code>false</code>                | 8.0.0 |
| **`fallbackStrategy`**                | <code>'none' \| 'deviceCredential' \| 'systemDefault'</code> | Controls fallback behavior when biometric authentication is unavailable or fails during an interactive prompt. - `deviceCredential`: always allow device credential fallback when supported. - `none`: disallow device credential fallback and require biometrics only. - `systemDefault`: preserve legacy behavior (`allowDevicePasscode` on native). | <code>'systemDefault'</code>      | 8.0.0 |

### Nested configuration objects

#### `accessControl`

Security level for biometric hardware access. Accepts one of:

- `biometryAny`
- `biometryCurrentSet`
- `passcodeAny`
- `devicePasscode`

#### `webAuthn`

WebAuthn behavior and backend integration options (Web platform only).

| Prop                          | Type                                      | Description                                                                      | Default              | Since |
| ----------------------------- | ----------------------------------------- | -------------------------------------------------------------------------------- | -------------------- | ----- |
| **`mode`**                    | <code>'local' \| 'server'</code>          | WebAuthn operating mode.                                                         | <code>'local'</code> | 8.0.0 |
| **`registrationStartUrl`**    | <code>string</code>                       | HTTP endpoint that starts WebAuthn registration and returns challenge payload.   |                      | 8.0.0 |
| **`registrationFinishUrl`**   | <code>string</code>                       | HTTP endpoint that verifies registration attestation.                            |                      | 8.0.0 |
| **`authenticationStartUrl`**  | <code>string</code>                       | HTTP endpoint that starts WebAuthn authentication and returns challenge payload. |                      | 8.0.0 |
| **`authenticationFinishUrl`** | <code>string</code>                       | HTTP endpoint that verifies authentication assertion.                            |                      | 8.0.0 |
| **`headers`**                 | <code>Record&lt;string, string&gt;</code> | Optional extra headers attached to server WebAuthn requests.                     |                      | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Fortress": {
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/fortress" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Fortress: {
      verboseLogging: true,
    },
  },
};

export default config;
```

## Runtime configuration

You can override configuration at runtime using `configure()`:

```ts
import { Fortress } from '@cap-kit/fortress';

// Update session timeout
await Fortress.configure({
  lockAfterMs: 300000, // 5 minutes
});

// Update privacy overlay text
await Fortress.configure({
  privacyOverlayText: 'Session Expired',
});
```

Runtime precedence is deterministic:

1. Static baseline loaded from `capacitor.config.ts`
2. Persisted runtime overrides (if present and valid)
3. Baseline fallback for missing/invalid persisted fields

Runtime overrides are persisted in standard platform storage (not secure
storage):

- iOS: `UserDefaults`
- Android: `SharedPreferences`
- Web: `localStorage`

Some configuration changes take effect immediately (for example privacy
overlay UI), while others apply on the next lifecycle transition (for example
session timeout).

To clear runtime overrides and return to startup baseline:

```ts
await Fortress.resetRuntimeConfig();
```

### Migration note (v8 runtime persistence)

Before runtime-config persistence, values set via `configure()` were
session-scoped and reset on app restart.

From this version onward:

- Valid runtime overrides persist across app restarts
- Invalid values are ignored and baseline values are used
- `resetRuntimeConfig()` clears persisted overrides and restores startup
  baseline
