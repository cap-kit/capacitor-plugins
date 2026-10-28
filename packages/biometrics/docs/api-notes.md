# Biometrics — API Notes

## Promise model

- `authenticate()` resolves `void` on success. Rejections carry
  `CANCELLED` (user dismissed, fallback chosen where it dismisses, or
  prompt cancelled programmatically) and `UNAVAILABLE` (no hardware, not
  enrolled where enrollment is required, passcode missing, or platform
  without the capability).
- Concurrent `authenticate()` calls reject: one ceremony at a time.
  Cancel the first before starting another.
- Capability getters (`isAvailable`, `isEnrolled`, `getBiometricType(s)`,
  `hasDeviceCredential`, `isDeviceSecure`, `getBiometricStrengthLevel`,
  `getAuthenticationType`) never reject under normal use.
- `isDeviceSecure()` reports the broad lock check: PIN/pattern/password/
  passcode (Android `KeyguardManager.isDeviceSecure`, iOS
  `LAContext.canEvaluatePolicy(.deviceOwnerAuthentication)`). Biometrics
  always require a device credential, so this doubles as the
  "any secure lock" check (the device-security `pinCheck()` use case).

## Display vs decision

`getBiometricType()` / `getBiometricTypes()` describe hardware for UI
labels. Gate logic on `isAvailable()` / `isEnrolled()` instead: a listed
sensor may have zero enrollments.

## Platform availability

- Android and iOS for ceremonies; capability getters work everywhere
  including web (WebAuthn probe).
- `enroll()`: Android API 30+ only.
- `cancelAuthentication()`: Android SDK 29+ and iOS; no-op elsewhere.
- `getAuthenticationType()`: native only; `'unknown'` on web and before
  the first ceremony of the session.

## Errors

Native failures map to JS code strings — `CANCELLED`, `UNAVAILABLE`,
`NOT_FOUND` (secure store: no value exists for the key — never stored,
deleted, or invalidated by a `biometryCurrentSet` enrollment change), plus
`INIT_FAILED` for unexpected internal errors. The full stable set is
exported as the `BiometricAuthError` enum; rejections still arrive as
`error.code` strings, so match against the enum constants.

## Secure store (8.0.0)

- `setSecureData({ key, value, accessControl })` stores a string under a
  hardware-backed, biometry-bound key. `accessControl` is required:
  `'biometryAny'` survives enrollment changes (recommended) or
  `'biometryCurrentSet'` is invalidated when the biometric set changes.
  Android shows a BiometricPrompt bound to the encryption key at write
  time; iOS applies SecAccessControl at write (no prompt).
- `getSecureData({ key, ... })` prompts for biometric authentication and
  returns `{ value }`. Rejects `NOT_FOUND` when nothing exists for the
  key — stable, expected behavior: handle it in your catch block or
  check `isSecureDataSaved()` first. Prompt failures reuse the ceremony
  codes (`CANCELLED`, `UNAVAILABLE`).
- `deleteSecureData({ key })` and `isSecureDataSaved({ key })` never
  prompt. A protected-but-locked item counts as saved.
- Web: all four methods reject — no WebAuthn equivalent.
- Payloads are small-string oriented (a few KB).

## Versioning

`getPluginVersion()` returns the native implementation version
synchronized from `package.json` at build time. Use it in diagnostics
to prove which native binary a report came from.
