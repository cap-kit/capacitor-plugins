# @cap-kit/biometrics

## 8.1.0

### Minor Changes

- b4ef5b0: feat: Initial release

## Unreleased — 8.0.0

### Added

- `isDeviceSecure()` capability check reporting whether the device is
  secured by any lock-screen credential (Android
  `KeyguardManager.isDeviceSecure`, iOS
  `LAContext.canEvaluatePolicy(.deviceOwnerAuthentication)`). Reports the
  same value as `hasDeviceCredential()` under its honest name.
- `BiometricAuthError` enum exporting the stable rejection code strings
  (`CANCELLED`, `UNAVAILABLE`, `NOT_FOUND`, ...) so consumers match
  rejections against named constants instead of raw strings.
- Strict-Node-ESM packaging fix: relative specifiers in the emitted ESM
  build now carry `.js` extensions, so `dist/esm` is loadable under
  Node's strict ESM resolution.
- Biometric-bound secure key-value store: `setSecureData()`,
  `getSecureData()`, `deleteSecureData()` and `isSecureDataSaved()` with
  required `accessControl` (`'biometryAny' | 'biometryCurrentSet'`).
  Android: per-key Keystore AES-256-GCM keys with
  `setUserAuthenticationRequired(true)` and CryptoObject-bound
  BiometricPrompt on read and write. iOS: Keychain generic-password
  items with SecAccessControl and LAContext-authenticated reads.
  Missing-key reads reject with `NOT_FOUND`; web rejects as
  unavailable.
