# Authentication — Security Considerations

This guide covers the security contract of the Authentication plugin: where tokens are stored, the critical server-side exchange requirements for Apple and Facebook, and the shared error-code mapping.

## Token storage

- **Apple (iOS)**: the plugin persists the Apple token set in the iOS keychain under service `capkit.auth.apple`. The host app owns the keychain entitlement, so no plugin-side configuration is needed; if your app shares keychain items across extensions or app groups, keep the keychain access-group entitlements consistent with the service the plugin writes to.
- **Facebook (iOS)**: tokens are persisted in the iOS keychain under service `capkit.auth.facebook`; `signOut`/`logout` clears the Facebook token set from the secure vault. Facebook tokens live only in the secure vault, never JS-visible — mirroring the Apple parity contract.
- **Facebook client token**: a native-only secret. It is accepted for config-schema parity on Web but **NEVER sent to the browser**.

## Apple server-side contract

- **Apple issues NO refresh token on any platform.** `refreshToken({ provider: 'apple' })` rejects with `INVALID_INPUT` ("Apple issues no refresh token"). Do not attempt to refresh client-side.
- The `authorizationCode` returned by sign-in **MUST be exchanged server-side**: POST it to Apple's token endpoint (`https://appleid.apple.com/auth/token`) with `grant_type=authorization_code`, your Services ID as `client_id`, a `client_secret` (an ES256 JWT signed with your Apple Developer private key), and the `redirect_uri`. The token response carries the access token and the refresh token — keep the refresh token on your server; it never reaches the plugin.
- The `authorizationCode` is **never a refresh token itself** and the plugin never refreshes it.
- Apple delivers the user profile (`name`, `email`) **only on the first sign-in**. Persist it server-side at that moment; subsequent sign-ins omit it.

## Facebook server-side contract

- **Facebook issues NO refresh token on any platform.** `refreshToken({ provider: 'facebook' })` rejects with `INVALID_INPUT` ("Facebook issues no refresh token"). Do not attempt to refresh client-side.
- For **long-lived web tokens**, the only supported path is **server-side** via **`fb_exchange_token`** with your **app secret**: `GET https://graph.facebook.com/v<version>/oauth/access_token?grant_type=fb_exchange_token&client_id=<APP_ID>&client_secret=<APP_SECRET>&fb_exchange_token=<SHORT_LIVED_TOKEN>`. This exchange runs **only on your server** — the app secret must never reach the client, and the plugin performs **no client-side exchange**. Web access tokens are short-lived (about **1–2 hours**); native tokens are long-lived (about **60 days**). The plugin never substitutes the access token for a refresh token.
- Persist any long-lived token you obtain server-side; keep the app secret off every client.

## Facebook error codes

Facebook failures map through the shared **10-code** `AuthenticationErrorCode` set (`UNAVAILABLE`, `CANCELLED`, `USER_CANCELLED`, `PERMISSION_DENIED`, `INIT_FAILED`, `INVALID_INPUT`, `UNKNOWN_TYPE`, `NOT_FOUND`, `CONFLICT`, `TIMEOUT`) — **no new codes** are added:

| Facebook failure                                                                                               | Code             |
| -------------------------------------------------------------------------------------------------------------- | ---------------- |
| User dismisses/cancels the native or popup UI                                                                  | `USER_CANCELLED` |
| Missing app configuration or SDK initialization failure (incl. Android key-hash `1349094`, missing iOS app id) | `INIT_FAILED`    |
| Graph or network failure                                                                                       | `UNAVAILABLE`    |
| Blank `facebookAppId`, malformed input, or `refreshToken({ provider: 'facebook' })`                            | `INVALID_INPUT`  |
