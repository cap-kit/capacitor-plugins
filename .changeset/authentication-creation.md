---
"@cap-kit/authentication": minor
---

Add Authentication plugin: unified Google, Apple and Facebook sign-in for Capacitor v8.

Introduce the first release of `@cap-kit/authentication`, a Capacitor v8 plugin providing native OAuth 2.0 / OpenID Connect sign-in flows for Google, Apple, and Facebook across iOS, Android, and Web, with consistent, type-safe configuration and secure token storage.

- Add RORO method surface (`initialize`, `signIn`, `signOut`, `getCurrentAccessToken`, `refreshToken`, `logout`, `createRestoreCredential`, `getRestoreCredential`, `clearRestoreCredential`, `getPluginVersion`) with `provider` passed as an options object, matching the Capacitor bridge contract.
- Add native Google sign-in using Credential Manager on Android (with optional `autoSelect` mode) and Google Sign-In / ASWebAuthenticationSession flows on iOS.
- Add Apple sign-in through `ASAuthorizationController` (iOS) and the Apple Sign-In activity (Android), with nonce/state validation and server-side authorization-code exchange support.
- Add Facebook Login through the official SDK on both platforms, with runtime SDK initialization, client token support, and graceful `MISSING_CONFIGURATION` errors when no app is configured.
- Add secure token storage: Android Keystore-backed `TokenVault` (AES256-GCM values, AES256-SIV keys) and iOS Keychain vault per provider, plus restore/clear credential helpers.
- Add a consistent 10-code error contract (`INVALID_INPUT`, `INTT_FAILED`, `UNAVAILABLE`, `CANCELLED`, `USER_CANCELLED`, `PERMISSION_DENIED`, `UNKNOWN_TYPE`, `NOT_FOUND`, `CONFLICT`, `TIMEOUT`) mapped identically across Web, iOS, and Android.
- Add single-flight `signIn` guard on the Web facade to prevent concurrent in-flight sign-ins for the same provider.
- Add a shared example app under `demo/` (via `prepare-demo.ts`) showcasing the plugin across providers.
