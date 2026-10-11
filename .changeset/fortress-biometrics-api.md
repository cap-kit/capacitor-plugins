---
"@cap-kit/fortress": minor
---

Add standalone biometrics API aligned with the official-style contract: pure `authenticate(options?)` (identity proof without vault/session side effects), `isAvailable()` / `isEnrolled()` split, `cancelAuthentication()`, Android `enroll()`, `getBiometricType(s)`, `hasDeviceCredential()`, `getBiometricStrengthLevel()`, `getAuthenticationType()`, per-call `allowDeviceCredential` override, and enriched `checkStatus()` (`biometryTypes`, `strongBiometryIsAvailable`).
