# Biometrics — Platform Behavior

## Android

Ceremonies run on `androidx.biometric.BiometricPrompt`:

- `allowDeviceCredential: true` combines `BIOMETRIC_STRONG` with
  `DEVICE_CREDENTIAL`; the system owns the fallback button and no custom
  negative text is shown (platform constraint).
- `false` restricts to `BIOMETRIC_STRONG` with a custom negative button.
- Strength reflects the class check: devices whose only biometry is weak
  (e.g. some face unlocks) report `weak`.
- `getBiometricTypes()` reads hardware features (fingerprint, face, iris)
  — advertised sensors, not enrollments. Decide with `isAvailable()`.
- `enroll()` opens the system enrollment screen (API 30+).
- `cancelAuthentication()` dismisses an in-flight prompt (SDK 29+); the
  pending call rejects as cancelled.

## iOS

Ceremonies run on `LocalAuthentication`:

- All iOS biometry (Face ID, Touch ID, Optic ID reported as iris) is
  strong-class: strength is `strong` whenever available, `weak` is
  unreachable.
- `allowDeviceCredential: true` uses device-owner authentication (system
  passcode fallback with system-managed button); `false` restricts to
  biometrics with a custom cancel title.
- iOS asks biometric permission on first use; denial rejects as cancelled.
- No public enrollment API exists: `enroll()` rejects `UNAVAILABLE`.
- `cancelAuthentication()` invalidates the live context and dismisses
  the system dialog.

## Web

Ceremonies run through WebAuthn against a locally created platform
credential (`userVerification: preferred`). Availability reflects the
platform authenticator probe; the credential set reports enrollment.
`enroll()` rejects, cancellation is a no-op, and the authentication type
is always `unknown`.
