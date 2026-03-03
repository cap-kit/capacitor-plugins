---
"@cap-kit/fortress": minor
---

Deliver the first functional Fortress cross-platform security baseline with secure storage, biometric unlock, and WebAuthn support.

This release implements iOS Keychain + LocalAuthentication, Android Keystore/StrongBox + BiometricPrompt, and Web storage with hardened WebAuthn flows (including optional backend-attested mode). It also harmonizes error-code behavior across iOS/Android/Web, completes standard storage/key obfuscation plumbing, and validates strict-concurrency/lint/build checks through plugin-local verification scripts.
