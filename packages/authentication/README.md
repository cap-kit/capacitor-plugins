<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Authentication</h3>
<p align="center">
  <strong>
    <code>@cap-kit/authentication</code>
  </strong>
</p>

<p align="center">
  Unified <strong>Google, Apple and Facebook</strong> sign-in for Capacitor v8.<br>
  Native <strong>OAuth 2.0 / OpenID Connect</strong> flows on iOS, Android, and Web with secure token storage,<br>
  a consistent 10-code error contract, and type-safe configuration.
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/authentication">
    <img src="https://img.shields.io/npm/v/@cap-kit/authentication?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/authentication">
    <img src="https://img.shields.io/npm/dm/@cap-kit/authentication?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/authentication?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Overview

This Capacitor plugin provides unified **Google, Apple and Facebook** sign-in for Capacitor v8.

- Runs native **OAuth 2.0 / OpenID Connect** flows on iOS, Android, and Web with secure native token storage (iOS keychain / secure vault) and a consistent 10-code error contract.
- Provider-keyed API: sign-in, sign-out, token refresh (Web), current-token inspection, Google Restore (Zero Tap) credentials, and type-safe static configuration (nested provider sub-objects with flat iOS parity keys).
- Apple and Facebook are dual-mode with no `unimplemented` path; neither issues a refresh token on any platform, so token exchange is a server-side contract — see [Security considerations](docs/security.md).

## Documentation

- [Usage guide](docs/guide.md) — Apple and Facebook sign-in: prerequisites, per-platform setup, usage examples
- [Configuration guide](docs/configuration.md) — provider sub-objects, flat parity keys, defaults, complete example
- [Security considerations](docs/security.md) — token storage, server-side token contracts, error codes
- [Contributing](CONTRIBUTING.md)

---

## Install

```bash
pnpm add @cap-kit/authentication
npx cap sync

```

---

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the Authentication plugin.

| Prop                      | Type                                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Default                                     | Since |
| ------------------------- | ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- | ----- |
| **`verboseLogging`**      | <code>boolean</code>                | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API.                                                                                                                                                                                                                                                           | <code>false</code>                          | 8.0.0 |
| **`webClientId`**         | <code>string</code>                 | Web OAuth client id (must end in `apps.googleusercontent.com`). Used only by the Web implementation to build the Google authorization URL.                                                                                                                                                                                                                                                                                                                                                         |                                             | 8.0.0 |
| **`webRedirectUri`**      | <code>string</code>                 | Web OAuth redirect URI registered with the provider.                                                                                                                                                                                                                                                                                                                                                                                                                                               |                                             | 8.0.0 |
| **`google`**              | <code>GoogleProviderConfig</code>   | Google provider configuration (Android sub-object). Consumed by the Android implementation via the `google` sub-object. The iOS implementation reads the FLAT `googleScopes` and `googleAutoSelect` keys instead (see [GoogleProviderConfig] parity note); both platforms derive the same defaults when the keys are absent. `serverClientId` is REQUIRED for native token exchange: absent → `INIT_FAILED`, blank → `INVALID_INPUT` (Android `GoogleConfigResolver`).                             |                                             | 8.0.0 |
| **`apple`**               | <code>AppleProviderConfig</code>    | Apple provider configuration (Android sub-object). Consumed by the Android implementation via the `apple` sub-object. The iOS implementation reads the FLAT `appleClientId`, `appleRedirectURI`, `appleScopes`, `appleNonce` and `appleState` keys instead (see [AppleProviderConfig] parity note); both platforms derive the same defaults when the keys are absent. `clientId` is REQUIRED for the native flow: absent → `INIT_FAILED`, blank → `INVALID_INPUT` (Android `AppleConfigResolver`). |                                             | 8.0.0 |
| **`facebook`**            | <code>FacebookProviderConfig</code> | Facebook provider configuration (Android sub-object). Consumed by the Android implementation via the `facebook` sub-object. The iOS implementation reads the FLAT `facebookAppId`, `facebookClientToken`, `facebookVersion` and `facebookScopes` keys instead (see [FacebookProviderConfig] parity note). `facebookAppId` is REQUIRED: absent → `INIT_FAILED`, blank → `INVALID_INPUT`.                                                                                                            |                                             | 8.0.0 |
| **`googleScopes`**        | <code>string[]</code>               | Google OAuth scopes (FLAT iOS key). Equivalent to `google.scopes`; the iOS implementation reads this flat key and splits it on spaces. Both platforms derive `['openid', 'email', 'profile']` when the key is absent.                                                                                                                                                                                                                                                                              | <code>['openid', 'email', 'profile']</code> | 8.0.0 |
| **`googleAutoSelect`**    | <code>boolean</code>                | Whether to attempt zero-UX auto-select for returning users (FLAT iOS key). Equivalent to `google.autoSelect`; the iOS implementation reads this flat key.                                                                                                                                                                                                                                                                                                                                          | <code>false</code>                          | 8.0.0 |
| **`appleClientId`**       | <code>string</code>                 | Apple client identifier / Services ID (FLAT iOS key). Equivalent to `apple.clientId`; the iOS implementation reads this flat key. Required for the iOS native flow: absent → `INIT_FAILED`, blank → `INVALID_INPUT`.                                                                                                                                                                                                                                                                               |                                             | 8.0.0 |
| **`appleRedirectURI`**    | <code>string</code>                 | Apple redirect URI registered with the Services ID (FLAT iOS key). Equivalent to `apple.redirectURI`; the iOS implementation reads this flat key.                                                                                                                                                                                                                                                                                                                                                  |                                             | 8.0.0 |
| **`appleScopes`**         | <code>string[]</code>               | Apple OAuth scopes (FLAT iOS key). Equivalent to `apple.scopes`; the iOS implementation reads this flat key.                                                                                                                                                                                                                                                                                                                                                                                       | <code>['name', 'email']</code>              | 8.0.0 |
| **`appleNonce`**          | <code>string</code>                 | Apple OpenID Connect `nonce` binding the issued id_token to this sign-in (FLAT iOS key). Equivalent to `apple.nonce`; the iOS implementation reads this flat key.                                                                                                                                                                                                                                                                                                                                  |                                             | 8.0.0 |
| **`appleState`**          | <code>string</code>                 | Apple OAuth `state` value for CSRF protection (FLAT iOS key). Equivalent to `apple.state`; the iOS implementation reads this flat key.                                                                                                                                                                                                                                                                                                                                                             |                                             | 8.0.0 |
| **`facebookAppId`**       | <code>string</code>                 | Facebook App ID, from the Meta developer portal (FLAT iOS key). Equivalent to `facebook.facebookAppId`; the iOS implementation reads this flat key. Required for the iOS native flow: absent → `INIT_FAILED`, blank → `INVALID_INPUT`.                                                                                                                                                                                                                                                             |                                             | 8.0.0 |
| **`facebookClientToken`** | <code>string</code>                 | Facebook client token, App Settings → Advanced (FLAT iOS key). Equivalent to `facebook.facebookClientToken`; the iOS implementation reads this flat key. Native-only secret: accepted for config-schema parity on Web but NEVER sent to the browser.                                                                                                                                                                                                                                               |                                             | 8.0.0 |
| **`facebookVersion`**     | <code>string</code>                 | Facebook Graph API version (FLAT key). Equivalent to `facebook.facebookVersion`; used by the Web and Android implementations. The native SDKs pin their own version and never apply it.                                                                                                                                                                                                                                                                                                            | <code>'v17.0'</code>                        | 8.0.0 |
| **`facebookScopes`**      | <code>string[]</code>               | Facebook OAuth scopes (FLAT iOS key). Equivalent to `facebook.scopes`; the iOS implementation reads this flat key.                                                                                                                                                                                                                                                                                                                                                                                 | <code>['public_profile', 'email']</code>    | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Authentication": {
      "verboseLogging": true,
      "webClientId": "1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com",
      "webRedirectUri": "https://example.com/auth/google",
      "googleScopes": ["openid", "email", "profile"],
      "googleAutoSelect": false,
      "appleClientId": "com.example.services",
      "appleRedirectURI": "https://example.com/auth/apple",
      "appleScopes": ["name", "email"],
      "facebookAppId": "123456789012345",
      "facebookClientToken": "YOUR_FACEBOOK_CLIENT_TOKEN",
      "facebookVersion": "v17.0",
      "facebookScopes": ["public_profile", "email"]
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
      googleScopes: ['openid', 'email', 'profile'],
      googleAutoSelect: false,
      appleClientId: 'com.example.services',
      appleRedirectURI: 'https://example.com/auth/apple',
      appleScopes: ['name', 'email'],
      facebookAppId: '123456789012345',
      facebookClientToken: 'YOUR_FACEBOOK_CLIENT_TOKEN',
      facebookVersion: 'v17.0',
      facebookScopes: ['public_profile', 'email'],
    },
  },
};

export default config;
```

</docgen-config>

---

## API

<docgen-index>

- [`getPluginVersion()`](#getpluginversion)
- [`initialize(...)`](#initialize)
- [`signIn(...)`](#signin)
- [`signOut(...)`](#signout)
- [`getCurrentAccessToken(...)`](#getcurrentaccesstoken)
- [`refreshToken(...)`](#refreshtoken)
- [`logout(...)`](#logout)
- [`createRestoreCredential(...)`](#createrestorecredential)
- [`getRestoreCredential(...)`](#getrestorecredential)
- [`clearRestoreCredential(...)`](#clearrestorecredential)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Authentication Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

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
const { version } = await Authentication.getPluginVersion();
```

---

### initialize(...)

```typescript
initialize(options: { provider: AuthProvider; }) => Promise<void>
```

Initializes the provider, loading any provider-specific configuration.

| Param         | Type                                                                 | Description                                      |
| ------------- | -------------------------------------------------------------------- | ------------------------------------------------ |
| **`options`** | <code>{ provider: <a href="#authprovider">AuthProvider</a>; }</code> | Options carrying the provider key to initialize. |

**Since:** 8.0.0

---

### signIn(...)

```typescript
signIn(options: SignInOptions & { provider: AuthProvider; }) => Promise<SocialAuthResult>
```

Signs the user in with the given provider and returns typed tokens.

Sign-in is single-flight per provider: concurrent calls for the same
provider do not start a second native flow.

| Param         | Type                                                                                                              | Description                                                                                      |
| ------------- | ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| **`options`** | <code><a href="#signinoptions">SignInOptions</a> & { provider: <a href="#authprovider">AuthProvider</a>; }</code> | Sign-in options carrying the provider key plus any per-call settings defined in [SignInOptions]. |

**Returns:** <code>Promise&lt;<a href="#socialauthresult">SocialAuthResult</a>&gt;</code>

**Since:** 8.0.0

---

### signOut(...)

```typescript
signOut(options: { provider: AuthProvider; }) => Promise<void>
```

Ends the session for the provider, removing persisted tokens.

| Param         | Type                                                                 | Description                        |
| ------------- | -------------------------------------------------------------------- | ---------------------------------- |
| **`options`** | <code>{ provider: <a href="#authprovider">AuthProvider</a>; }</code> | Options carrying the provider key. |

**Since:** 8.0.0

---

### getCurrentAccessToken(...)

```typescript
getCurrentAccessToken(options: { provider: AuthProvider; }) => Promise<{ accessToken?: string; }>
```

Returns the current access token for the provider, if any.

| Param         | Type                                                                 | Description                        |
| ------------- | -------------------------------------------------------------------- | ---------------------------------- |
| **`options`** | <code>{ provider: <a href="#authprovider">AuthProvider</a>; }</code> | Options carrying the provider key. |

**Returns:** <code>Promise&lt;{ accessToken?: string; }&gt;</code>

**Since:** 8.0.0

---

### refreshToken(...)

```typescript
refreshToken(options: { provider: AuthProvider; }) => Promise<OAuthTokenSet>
```

Refreshes the access token using a stored refresh token. Available on Web.

| Param         | Type                                                                 | Description                        |
| ------------- | -------------------------------------------------------------------- | ---------------------------------- |
| **`options`** | <code>{ provider: <a href="#authprovider">AuthProvider</a>; }</code> | Options carrying the provider key. |

**Returns:** <code>Promise&lt;<a href="#oauthtokenset">OAuthTokenSet</a>&gt;</code>

**Since:** 8.0.0

---

### logout(...)

```typescript
logout(options: { provider: AuthProvider; }) => Promise<void>
```

Alias for [signOut]: clears the session and persisted tokens.

| Param         | Type                                                                 | Description                        |
| ------------- | -------------------------------------------------------------------- | ---------------------------------- |
| **`options`** | <code>{ provider: <a href="#authprovider">AuthProvider</a>; }</code> | Options carrying the provider key. |

**Since:** 8.0.0

---

### createRestoreCredential(...)

```typescript
createRestoreCredential(options: RestoreCredentialOptions) => Promise<{ available: boolean; }>
```

Creates a Google Restore (Zero Tap) credential.

| Param         | Type                                                                          | Description                                      |
| ------------- | ----------------------------------------------------------------------------- | ------------------------------------------------ |
| **`options`** | <code><a href="#restorecredentialoptions">RestoreCredentialOptions</a></code> | Restore credential options carrying requestJson. |

**Returns:** <code>Promise&lt;{ available: boolean; }&gt;</code>

**Since:** 8.0.0

---

### getRestoreCredential(...)

```typescript
getRestoreCredential(options: { provider: AuthProvider; }) => Promise<{ available: boolean; requestJson?: string; }>
```

Retrieves a previously created restore credential.

| Param         | Type                                                                 | Description                        |
| ------------- | -------------------------------------------------------------------- | ---------------------------------- |
| **`options`** | <code>{ provider: <a href="#authprovider">AuthProvider</a>; }</code> | Options carrying the provider key. |

**Returns:** <code>Promise&lt;{ available: boolean; requestJson?: string; }&gt;</code>

**Since:** 8.0.0

---

### clearRestoreCredential(...)

```typescript
clearRestoreCredential(options: { provider: AuthProvider; }) => Promise<{ available: boolean; }>
```

Clears a previously created restore credential.

| Param         | Type                                                                 | Description                        |
| ------------- | -------------------------------------------------------------------- | ---------------------------------- |
| **`options`** | <code>{ provider: <a href="#authprovider">AuthProvider</a>; }</code> | Options carrying the provider key. |

**Returns:** <code>Promise&lt;{ available: boolean; }&gt;</code>

**Since:** 8.0.0

---

### Interfaces

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

#### SocialAuthResult

Normalized result of a successful sign-in for a provider.

| Prop           | Type                                                    | Description                             |
| -------------- | ------------------------------------------------------- | --------------------------------------- |
| **`provider`** | <code><a href="#authprovider">AuthProvider</a></code>   | The provider that produced this result. |
| **`tokens`**   | <code><a href="#oauthtokenset">OAuthTokenSet</a></code> | The typed token data for that provider. |

#### OAuthTokenSet

Normalized OAuth2/OIDC token set returned from a completed sign-in or refresh.

Mirrors the shape used by all three platforms so the result is homogeneous
across Web, iOS and Android. Optional fields are omitted when absent.

| Prop                       | Type                                                                  | Description                                                                                                                                                                                                                                                                                                                          |
| -------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **`accessToken`**          | <code>string</code>                                                   | OAuth access token. Omitted on the zero-UX auto-select path with default scopes (no consent UI — only the ID token is issued). Present on interactive sign-in and refresh paths.                                                                                                                                                     |
| **`refreshToken`**         | <code>string</code>                                                   | OAuth refresh token, when issued.                                                                                                                                                                                                                                                                                                    |
| **`idToken`**              | <code>string</code>                                                   | OpenID Connect ID token, when issued.                                                                                                                                                                                                                                                                                                |
| **`serverAuthCode`**       | <code>string</code>                                                   | Google server auth code, when issued.                                                                                                                                                                                                                                                                                                |
| **`authorizationCode`**    | <code>string</code>                                                   | Provider authorization code, when issued (Apple: all platforms). For Apple this is the OAuth `code` returned by the native/WebView/popup flow. It MUST be exchanged server-side (POST to Apple with a `client_secret`) for access/refresh tokens — it is never a refresh token itself and the plugin never refreshes it client-side. |
| **`user`**                 | <code><a href="#socialauthresultuser">SocialAuthResultUser</a></code> | Provider user profile, when shared (Apple: first sign-in only; facebook: `/me`).                                                                                                                                                                                                                                                     |
| **`userId`**               | <code>string</code>                                                   | Facebook shared access-token slot: provider-scoped user id (string\|null). Populated only by the facebook flow; google/apple return it null/absent (shared-data-model slots ADDED by facebook).                                                                                                                                      |
| **`grantedPermissions`**   | <code>string[]</code>                                                 | Facebook shared access-token slot: granted read permissions (string[]\|null).                                                                                                                                                                                                                                                        |
| **`declinedPermissions`**  | <code>string[]</code>                                                 | Facebook shared access-token slot: declined read permissions (string[]\|null).                                                                                                                                                                                                                                                       |
| **`expiresAt`**            | <code>number</code>                                                   | Facebook shared access-token slot: absolute epoch-seconds token expiry (number\|null). Canonical slot name is `expiresAt`, not `expires`.                                                                                                                                                                                            |
| **`accessTokenExpiresIn`** | <code>number</code>                                                   | Access token lifetime in seconds, when known.                                                                                                                                                                                                                                                                                        |

#### SocialAuthResultUser

Provider-agnostic user profile delivered with a sign-in result.

Present for providers that share profile data (Apple delivers it on first
sign-in only). Optional fields are omitted when absent. No field here is
ever a credential; treat `id`, `email` and `name` as profile data only.

| Prop                 | Type                                                              | Description                                                                                                                                                                                       |
| -------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`id`**             | <code>string</code>                                               | Provider-specific user id, when shared.                                                                                                                                                           |
| **`email`**          | <code>string</code>                                               | User email address, when shared by the provider.                                                                                                                                                  |
| **`name`**           | <code>string \| { firstName?: string; lastName?: string; }</code> | Display name. Apple delivers first/last name parts on first sign-in; other providers may deliver a single display string.                                                                         |
| **`realUserStatus`** | <code>'likleyRealUser' \| 'unknown' \| 'unsupported'</code>       | Apple's real-user estimation, when shared. Literal spellings are preserved exactly as Apple issues them (including the historical `likleyRealUser` typo, which is intentional for API stability). |
| **`picture`**        | <code>string</code>                                               | Provider profile image URL, when shared (facebook `/me` picture; google/apple return it null/absent). Social-auth-facade homogeneous shape.                                                       |

#### SignInOptions

Options for a provider sign-in.

| Prop              | Type                                                 | Description                                                                                                                                                                                                                                   |
| ----------------- | ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`prompt`**      | <code>'none' \| 'consent' \| 'select_account'</code> | User-visible prompt behavior for the provider consent screen. Google: 'none', 'consent', or 'select_account'.                                                                                                                                 |
| **`scopes`**      | <code>string[]</code>                                | Additional OAuth scopes requested for this sign-in. Apple: 'name' and/or 'email' (defaults to both when omitted). Facebook: granted read permissions (defaults to `['public_profile','email']` when omitted).                                 |
| **`autoSelect`**  | <code>boolean</code>                                 | Whether to attempt zero-UX auto-select for returning users (Google One Tap).                                                                                                                                                                  |
| **`clientId`**    | <code>string</code>                                  | Apple client id (Services ID) for the Web popup flow. Overrides the static `webClientId` Capacitor config value for this call.                                                                                                                |
| **`redirectURI`** | <code>string</code>                                  | Apple redirect URI registered with the Services ID, for the Web popup flow. Overrides the static `webRedirectUri` Capacitor config value for this call.                                                                                       |
| **`state`**       | <code>string</code>                                  | OAuth `state` value for the Apple Web popup flow; CSRF protection. When omitted, the Web implementation generates one. Facebook (Web): the popup `state` is also validated (CSRF); a mismatched `state` aborts the flow with `INVALID_INPUT`. |
| **`nonce`**       | <code>string</code>                                  | OpenID Connect `nonce` for the Apple Web popup flow; binds the issued id_token to this sign-in. When omitted, the Web implementation generates one.                                                                                           |

#### RestoreCredentialOptions

Options for creating a Google Restore (Zero Tap) credential.

| Prop                       | Type                                                  | Description                                                                 |
| -------------------------- | ----------------------------------------------------- | --------------------------------------------------------------------------- |
| **`provider`**             | <code><a href="#authprovider">AuthProvider</a></code> | The provider for which the restore credential is created.                   |
| **`requestJson`**          | <code>string</code>                                   | The requestJson payload describing the WebAuthn-style credential operation. |
| **`isCloudBackupEnabled`** | <code>boolean</code>                                  | Whether cloud backup is enabled for the credential.                         |

### Type Aliases

#### AuthProvider

Supported authentication provider key.

The facade is provider-keyed; Google is the first concrete provider, Apple
the second, and Facebook the third.

<code>'google' | 'apple' | 'facebook'</code>

</docgen-api>

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## Credits

This plugin is based on prior work from the Community and
has been refactored and modernized for **Capacitor v8** and
**Swift Package Manager** compatibility.

Original inspiration:

- [https://github.com/](https://github.com/)

---

## License

MIT
