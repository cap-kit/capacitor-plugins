# Authentication — Configuration Guide

This guide documents the static configuration model of the Authentication plugin: the provider sub-objects consumed by Android, the flat parity keys consumed by iOS, the Web keys, the defaults, and the complete configuration example including the nested provider objects.

The full property reference (types, defaults, descriptions) is rendered in the [Configuration section](../README.md#configuration) of the README.

## Configuration model

These values are defined in `capacitor.config.ts` or `capacitor.config.json` and consumed exclusively by native code during plugin initialization. Configuration values:

- do NOT change the JavaScript API shape
- do NOT enable/disable methods
- are applied once during plugin load

## Provider sub-objects (Android)

Android consumes the grouped `google`, `apple` and `facebook` sub-objects under `plugins.Authentication` at init time. The iOS implementation does **NOT** read these sub-objects: it reads the FLAT parity keys instead and derives the same defaults when they are absent. The flat iOS keys are the documentation point of reference for iOS; the sub-objects are the point of reference for Android.

### google — `GoogleProviderConfig`

| Prop             | Type       | Description                                                                                                                                                         |
| ---------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `serverClientId` | `string`   | Google OAuth web client (server) id used for native token exchange. **REQUIRED**: absent → `INIT_FAILED`, blank → `INVALID_INPUT` (Android `GoogleConfigResolver`). |
| `scopes`         | `string[]` | Additional OAuth scopes requested for the Google provider. Default `['openid', 'email', 'profile']`.                                                                |
| `autoSelect`     | `boolean`  | Whether to attempt zero-UX auto-select for returning users (Google One Tap). Default `false`.                                                                       |
| `nonce`          | `string`   | OpenID Connect `nonce` binding the issued id_token to this sign-in.                                                                                                 |

### apple — `AppleProviderConfig`

| Prop          | Type       | Description                                                                                                                                                           |
| ------------- | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `clientId`    | `string`   | Apple client identifier (Services ID) from the Apple Developer portal. **REQUIRED**: absent → `INIT_FAILED`, blank → `INVALID_INPUT` (Android `AppleConfigResolver`). |
| `redirectURI` | `string`   | Apple redirect URI registered with the Services ID.                                                                                                                   |
| `scopes`      | `string[]` | OAuth scopes requested for the Apple flow. Default `['name', 'email']`.                                                                                               |
| `nonce`       | `string`   | OpenID Connect `nonce` binding the issued id_token to this sign-in. When omitted, the platform generates a fresh per-flow nonce.                                      |
| `state`       | `string`   | OAuth `state` value for CSRF protection.                                                                                                                              |

### facebook — `FacebookProviderConfig`

| Prop                  | Type       | Description                                                                                                                                                                                                       |
| --------------------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `facebookAppId`       | `string`   | Facebook App ID, from the Meta developer portal. **REQUIRED**: absent → `INIT_FAILED`, blank → `INVALID_INPUT`.                                                                                                   |
| `facebookClientToken` | `string`   | Facebook client token (App Settings → Advanced), for native token exchange. Optional, but blank-when-present invalid. Native-only secret: accepted for config-schema parity on Web but NEVER sent to the browser. |
| `facebookVersion`     | `string`   | Facebook Graph API version passed to the Web provider. Default `'v17.0'`. Web/Android artifact; the native SDKs pin their own version and never apply it.                                                         |
| `scopes`              | `string[]` | OAuth read permissions granted to the app. Default `['public_profile', 'email']`.                                                                                                                                 |

## Flat parity keys (iOS)

The iOS implementation reads FLAT keys instead of the sub-objects and derives the same defaults when the keys are absent. Android only reads the sub-objects; providing both is allowed, keep them consistent.

| Flat key              | Equivalent (sub-object)        | Default                          | Notes                                                                              |
| --------------------- | ------------------------------ | -------------------------------- | ---------------------------------------------------------------------------------- |
| `googleScopes`        | `google.scopes`                | `['openid', 'email', 'profile']` | iOS splits the value on spaces.                                                    |
| `googleAutoSelect`    | `google.autoSelect`            | `false`                          | Zero-UX auto-select for returning users.                                           |
| `appleClientId`       | `apple.clientId`               | —                                | Required for the iOS native flow: absent → `INIT_FAILED`, blank → `INVALID_INPUT`. |
| `appleRedirectURI`    | `apple.redirectURI`            | —                                |                                                                                    |
| `appleScopes`         | `apple.scopes`                 | `['name', 'email']`              |                                                                                    |
| `appleNonce`          | `apple.nonce`                  | —                                |                                                                                    |
| `appleState`          | `apple.state`                  | —                                | OAuth `state` for CSRF protection.                                                 |
| `facebookAppId`       | `facebook.facebookAppId`       | —                                | Required for the iOS native flow: absent → `INIT_FAILED`, blank → `INVALID_INPUT`. |
| `facebookClientToken` | `facebook.facebookClientToken` | —                                | Native-only secret: NEVER sent to the browser.                                     |
| `facebookVersion`     | `facebook.facebookVersion`     | `'v17.0'`                        | Used by the Web and Android implementations; native SDKs pin their own.            |
| `facebookScopes`      | `facebook.scopes`              | `['public_profile', 'email']`    |                                                                                    |

## Web keys

- **`webClientId`** — Web OAuth client id (must end in `apps.googleusercontent.com`). Used only by the Web implementation to build the Google authorization URL.
- **`webRedirectUri`** — Web OAuth redirect URI registered with the provider.

## Complete example

The `<docgen-config>` example in the README renders the flat-key shape (docgen does not expand nested provider sub-objects). The full configuration shape, including the nested `google`, `apple` and `facebook` objects, is:

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Authentication": {
      "verboseLogging": true,
      "webClientId": "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com",
      "webRedirectUri": "https://example.com/auth/google",
      "google": {
        "serverClientId": "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com",
        "scopes": ["openid", "email", "profile"],
        "autoSelect": false
      },
      "apple": {
        "clientId": "com.example.services",
        "redirectURI": "https://example.com/auth/apple",
        "scopes": ["name", "email"],
        "nonce": "<one-time-random-nonce>",
        "state": "<csrf-state>"
      },
      "facebook": {
        "facebookAppId": "123456789012345",
        "facebookClientToken": "YOUR_FACEBOOK_CLIENT_TOKEN",
        "facebookVersion": "v17.0",
        "scopes": ["public_profile", "email"]
      }
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/authentication" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Authentication: {
      verboseLogging: true,
      webClientId: '1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com',
      webRedirectUri: 'https://example.com/auth/google',
      google: {
        serverClientId: '1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com',
        scopes: ['openid', 'email', 'profile'],
        autoSelect: false,
      },
      apple: {
        clientId: 'com.example.services',
        redirectURI: 'https://example.com/auth/apple',
        scopes: ['name', 'email'],
        nonce: '<one-time-random-nonce>',
        state: '<csrf-state>',
      },
      facebook: {
        facebookAppId: '123456789012345',
        facebookClientToken: 'YOUR_FACEBOOK_CLIENT_TOKEN',
        facebookVersion: 'v17.0',
        scopes: ['public_profile', 'email'],
      },
    },
  },
};

export default config;
```
