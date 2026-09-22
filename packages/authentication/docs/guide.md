# Authentication — Usage Guide

This guide covers how to use the Authentication plugin for **Apple** and **Facebook** sign-in: prerequisites, per-platform setup, usage examples, and result shapes. Both providers are dual-mode — native flows on iOS and Android, popup/WebView flows on Web — and neither has an `unimplemented` path.

For the static configuration model (provider sub-objects, flat parity keys, defaults) see the [Configuration guide](configuration.md). For the server-side token contracts and error mappings see [Security considerations](security.md).

## Sign in with Apple

`@cap-kit/authentication` supports Sign in with Apple as a second provider beside Google (`provider: 'apple'`). The provider is dual-mode: native `ASAuthorizationAppleIDProvider` on iOS, a WebView OAuth flow on Android, and the `appleid.auth.js` popup on Web — there is no `unimplemented` path.

### Prerequisites

- An Apple **Services ID** issued in the Apple Developer portal; its identifier becomes the `clientId`.
- The **Sign in with Apple** capability enabled for your app in Xcode (iOS) — see per-platform setup below.
- A registered **redirect URI** for the Services ID (`redirectURI`), used by the Web popup flow.
- If your app offers any third-party social login (e.g. Google), **App Store review requires Sign in with Apple to be offered as an equal option** — hosting Google sign-in makes Apple mandatory, not optional.

### Per-platform setup

- **iOS**: enable the **Sign in with Apple** capability in the host app's entitlements. The plugin persists the Apple token set in the iOS keychain under service `capkit.auth.apple`; the host app owns the keychain entitlement, so no plugin-side configuration is needed. If your app shares keychain items across extensions or app groups, keep the keychain access-group entitlements consistent with the service the plugin writes to.
- **Android**: nothing beyond the same Services ID. The plugin uses a WebView OAuth flow (`response_type=code id_token`, `response_mode=form_post`) restricted to Apple auth hosts — no Firebase or Google configuration is involved.
- **Web**: include the `appleid.auth.js` script (Apple's CDN) so the popup flow can load. Because the flow opens a popup, browsers may block it: call `signIn({ provider: 'apple', … })` from a user-gesture context or allow popups for your domain.

### Usage

```ts
import { Authentication } from '@cap-kit/authentication';

const result = await Authentication.signIn({
  provider: 'apple', // provider key is part of the options object
  clientId: 'com.example.app.service', // Services ID; overrides webClientId config on Web
  redirectURI: 'https://example.com/apple/callback', // overrides webRedirectUri config on Web
  scopes: ['name', 'email'], // defaults to both when omitted
  nonce: '<one-time-random-nonce>', // binds the issued id_token to this sign-in
  state: '<csrf-state>', // CSRF protection (Web popup flow)
});

// Resolved shape:
result.provider; // 'apple'
result.tokens.authorizationCode; // OAuth code — MUST be exchanged server-side (never a refresh token)
result.tokens.idToken; // OpenID Connect ID token
result.tokens.user; // { id?, email?, name? } — first sign-in only (see below)
```

On Web, per-call `clientId`/`redirectURI` override the static `webClientId`/`webRedirectUri` Capacitor config values for that call; when omitted, the static values are used. `tokens.user.name` is delivered as `{ firstName?, lastName? }` parts or a display string, and `tokens.user.realUserStatus` (iOS only) uses Apple's literal spellings — including the historical `'likleyRealUser'` typo, which is preserved for API stability.

### Server-side contract

Apple issues **no refresh token on any platform**, the `authorizationCode` **MUST be exchanged server-side**, and the user profile is delivered **only on the first sign-in**. See [Security considerations](security.md#apple-server-side-contract) for the full contract.

## Sign in with Facebook

`@cap-kit/authentication` supports Sign in with Facebook as a third provider beside Google and Apple (`provider: 'facebook'`). The provider is dual-mode: native `LoginManager` on iOS and Android, and the Facebook JavaScript SDK popup on Web — there is no `unimplemented` path. Facebook issues **no refresh token on any platform**, and `refreshToken({ provider: 'facebook' })` rejects with `INVALID_INPUT`.

### Prerequisites

- A **Facebook App** created in the [Meta for Developers](https://developers.facebook.com/) portal. You need the **App ID** (`facebookAppId`, required) and, for the native flows, the App's **Client Token** (`facebookClientToken`, optional but recommended for the iOS SDK).
- For Web, an **OAuth client** and a **Valid OAuth Redirect URI** configured against the app's Facebook Login settings (the popup domain). Configure the redirect URI as the exact origin (scheme + host + port) from which the popup is launched.
- Access to the app's **Android key hash** for the native Android flow (see below) — the `1349094` mismatch is the classic silent-failure trap.
- The Facebook **Graph API version** (`facebookVersion`, default `v17.0`) and the requested **scopes** (default `['public_profile','email']`).

### Per-platform setup

- **iOS**: add the following keys to the host app's `Info.plist`: `FacebookAppID` (the numeric App ID), `FacebookClientToken` (the Client Token) and `FacebookDisplayName` (the app display name). Add a URL scheme of **`fb<APPID>`** (for example `fb123456789012345`) so the native login can return to the app — this scheme is handled by the plugin's open-URL observer when using the SDK facade. Tokens are persisted in the iOS keychain under service `capkit.auth.facebook`; `facebookAppId` is also tokenized from Capacitor config (the `facebook` config sub-object with flat `facebookAppId`/`facebookClientToken`/`facebookVersion`/`facebookScopes` keys).
- **Android**: add the following **string resources to the host app** (the SDK reads them at the application level, not the plugin):
  - `facebook_app_id` — the numeric App ID
  - `facebook_client_token` — the Client Token
  - `fb_login_protocol_scheme` — must be exactly `fb<APPID>` (for example `fb123456789012345`)

  The `FacebookActivity` (and the SDK's custom-tab/initializer components) are contributed by the `facebook-login` SDK manifest **merger** — you do not declare them yourself. **Register the development key hash** for this app in the Meta developer settings: compute it with the debug keystore and register it, otherwise the native flow fails silently with error `1349094`. The registration is keyed to the exact signing certificate used to build — a release build uses a different key hash than a debug build. A key-hash mismatch maps to `INIT_FAILED` (actionable, not a generic error).

- **Web**: the Facebook JavaScript SDK is lazy-loaded from Meta's CDN (`connect.facebook.net`) and the popup flow runs `FB.login` with the configured scopes and `return_scopes: true`. Set the **Valid OAuth Redirect URI** to the popup domain. `facebookVersion` (default `v17.0`) selects the Graph API version passed to `FB.init`; when you update the version, re-test that the requested scopes and `/me` fields still resolve under that Graph version. Because this flow opens a popup, browsers may block it: call `signIn({ provider: 'facebook', … })` from a user-gesture context or allow popups for your domain.

### Usage

```ts
import { Authentication } from '@cap-kit/authentication';

const result = await Authentication.signIn({ provider: 'facebook' });

// Resolved shape:
result.provider; // 'facebook'
result.tokens.accessToken; // Facebook access token
result.tokens.userId; // Facebook user id
result.tokens.grantedPermissions; // granted read permissions, e.g. ['public_profile','email']
result.tokens.declinedPermissions; // declined read permissions
result.tokens.expiresAt; // absolute epoch-seconds expiry
result.tokens.user; // { id, name?, email?, picture? } — granted fields only
```

#### Contract notes

- **Result shape.** Facebook populates the four shared token slots — `userId`, `grantedPermissions`, `declinedPermissions` and `expiresAt` (absolute **epoch-seconds**, canonical slot name `expiresAt` not `expires`) — plus `user.picture` (profile image URL from `picture.width(720).height(720)`). `authorizationCode` and `idToken` are **NOT provided by Facebook** and are `null`; `realUserStatus` is **NOT applicable** (that is an Apple-only field). Google and Apple results return these facebook slots null/absent.
- **Granted fields only.** `user` holds only the fields the user actually granted. Default scopes are `['public_profile','email']`; requesting granular scopes (e.g. `user_photos`, pages) requires Meta **App Review** before the app can read them.
- **Web state.** The popup `state` is validated (CSRF); a mismatched `state` aborts the flow with `INVALID_INPUT`.

### Error codes

Facebook failures map through the shared **10-code** `AuthenticationErrorCode` set — **no new codes** are added. See [Security considerations](security.md#facebook-error-codes) for the full mapping.

### Logout

`signOut({ provider: 'facebook' })` / `logout({ provider: 'facebook' })` calls the native `LoginManager.logOut()` (or `FB.logout()` on Web) **and** clears the Facebook token set from the secure vault — mirroring the Apple parity contract. Facebook tokens live only in the secure vault, never JS-visible.

### Server-side contract

Facebook issues **no refresh token on any platform**; long-lived web tokens require the **server-side** `fb_exchange_token` path with your app secret. See [Security considerations](security.md#facebook-server-side-contract) for the full contract.
