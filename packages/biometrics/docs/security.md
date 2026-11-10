# Biometrics — Security Model

## What it proves

That the person holding the device right now satisfies the requested
authenticator: a biometric match or the device credential. Nothing more.
It does not identify _who_ — it proves _presence with entitlement_.

## What it does NOT do

- **Authorize by itself.** Authentication is not authorization: always bind
  a successful ceremony to a concrete action (open the vault, confirm the
  payment) immediately, in the same flow.
- **Store secrets biometry-free.** The secure store (`setSecureData()` /
  `getSecureData()`, 8.0.0) is hardware-backed and biometry-bound by
  design: `accessControl` is required and there is no unprotected mode.
  It is a small encrypted KV store (Android Keystore + CryptoObject-bound
  BiometricPrompt; iOS Keychain + SecAccessControl), not a general key
  manager. With `biometryCurrentSet` the data is lost when enrollment
  changes — that is by design.
- **Guarantee strength.** Weak Android modalities satisfy the ceremony when
  allowed. Check `getBiometricStrengthLevel()` and require `strong` for
  high-risk actions.
- **Survive enrollment changes silently.** New enrollments can invalidate
  hardware-bound keys created elsewhere; re-verify and re-provision after
  `isEnrolled` flips unexpectedly.
- **Protect the web platform.** WebAuthn assertions prove a local ceremony
  happened, without hardware attestation in this configuration.

## Best practices

- Check `isAvailable()` + `isEnrolled()` before every prompt; never cache
  capability results across resumes without re-checking.
- Cancel in-flight prompts on navigation away (`cancelAuthentication()`).
- Treat `CANCELLED` as user intent (stay silent), `UNAVAILABLE` as a cue
  to offer an alternative path (passcode screen, support, settings).
- For payments or destructive actions, require `strong` strength and a
  fresh ceremony — never reuse an old success.
- For the secure store, expect `NOT_FOUND` on read after a
  `biometryCurrentSet` enrollment change; data loss there is by design —
  re-provision on that signal instead of hiding it.
