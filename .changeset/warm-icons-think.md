---
"@cap-kit/fortress": minor
---

Deliver the first functional Fortress cross-platform security baseline with secure storage, biometric unlock, WebAuthn support, and cryptographic challenge-signing helpers.

This release implements iOS Keychain + LocalAuthentication, Android Keystore/StrongBox + BiometricPrompt, and Web storage with hardened WebAuthn flows (including optional backend-attested mode).

**Core Storage**: Adds AES-GCM encryption with hardware-backed security (Secure Enclave on iOS, Keystore on Android), vault lock gating for secure CRUD operations, and SECURITY_VIOLATION rejection on corrupt data.

**Key Pairs**: Exposes native biometric key-pair generation (iOS Secure Enclave + Android Keystore) via `biometricKeysExist`, `createKeys`, `deleteKeys`, plus challenge-based registration/authentication (`registerWithChallenge`, `authenticateWithChallenge`) and canonical `generateChallengePayload` helpers for backend verification workflows.

**Session Management**: Implements deterministic lock-state transition notifications with 1000ms touch throttle, centralized lock evaluation, and configurable auto-lock timeout.

**Privacy Screen**: Adds config-aware privacy protection for app snapshots that respects vault lock state (only shows when locked).

**Atomic Multi-Set**: Provides transactional `setMany` with snapshot/rollback pattern to ensure all-or-nothing commits.

**Posture API**: Differentiates `isBiometricsAvailable` (hardware capability) from `isBiometricsEnabled` (enrollment status) across all platforms.

**Security Events**: Exposes `onSecurityStateChanged`, `onLockStatusChanged`, and `onVaultInvalidated` with typed payloads and canonical reasons (`security_state_changed`, `keypair_invalidated`, `keys_deleted`).

**Enhancements (implemented subset)**: Adds prompt customization (`promptOptions`), cached authentication (`allowCachedAuthentication`, `cachedAuthenticationTimeoutMs`), app resume event (`onAppResume`), cryptographic strategy settings (`cryptoStrategy`, `keySize`), lockout counter (`maxBiometricAttempts`, `lockoutDurationMs`), biometric freshness enforcement (`requireFreshAuthenticationMs`), debug logging levels (`logLevel`), internal vault state machine formalization, and iOS iCloud Keychain sync support (`enableICloudKeychainSync`).

**Privacy Overlay**: Adds customizable privacy screen overlay with optional centered text and/or image rendering (`privacyOverlayText`, `privacyOverlayImageName`, `privacyOverlayShowText`, `privacyOverlayShowImage`, `privacyOverlayTextColor`, `privacyOverlayBackgroundOpacity`), runtime configuration updates via `configure()` for i18n support, fallback-to-blur behavior for missing assets/invalid colors, and combined vertical layout (image + text stacked).

**Security Documentation**: Adds comprehensive security documentation including threat model with explicit guarantees and limitations, canonical error matrix with remediation guidance (10 error codes), resume event contract with listener guarantees and payload schemas, runtime config effect semantics (immediate vs. next-transition), host integration checklists for iOS (Info.plist, capabilities) and Android (Manifest, SDK versions), migration policy with backward compatibility guarantees, and event payload stability contract with versioning policy.

**Web Cryptography Updates**: Adds configurable symmetric encryption mode for web secure storage (`AES-GCM`/`AES-CBC`) with payload algorithm metadata and compatibility validation.

**iOS Hardening**: Adds synchronizable Keychain query support (`kSecAttrSynchronizable`) with safe accessibility handling for sync-enabled entries and strict Swift 6 concurrency/type-safety fixes in Keychain helpers.

**iOS Maintainability Refactor**: Splits oversized bridge and implementation files into focused extensions (`FortressPlugin+Bridge`, `FortressPlugin+DebugBridge`, `FortressPlugin+Internal`, `Fortress+Operations`, `Fortress+CryptoOperations`) and resolves cross-file access-control issues introduced by the split, keeping strict SwiftLint compliance without inline disable pragmas.

Also includes standard storage/key obfuscation plumbing, harmonized error-code behavior across iOS/Android/Web, and validates strict-concurrency/lint/build checks through plugin-local verification scripts.
