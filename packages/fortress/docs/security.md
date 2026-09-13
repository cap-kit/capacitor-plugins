# Fortress — Security Model

Fortress is a complete security container that unifies hardware-backed secure
storage, biometric authentication, session management, and privacy protection
into a single platform-consistent API. This guide explains the security model,
the biometric policies, platform caveats, and what Fortress deliberately does
NOT do.

## Hardware-backed security

Secure values are encrypted before they are stored:

- iOS uses the **Secure Enclave**.
- Android uses the **Keystore**, with optional **StrongBox** hardware.
- Biometric prompts run through **BiometricPrompt (BIOMETRIC_STRONG)**.

Native platforms keep hardware-backed secure defaults for symmetric encryption;
only the Web secure storage layer is configurable via `encryptionAlgorithm`
(`AES-GCM` by default, `AES-CBC` as an alternative).

## What Fortress does NOT do

- It is not a policy engine: it enforces storage, session, and biometric
  access, but the host application decides the product-level security policy.
- Web platform keys are not hardware-backed the same way native keys are.
  Treat Web as a convenience tier, not a hardware security anchor.
- The insecure storage tier (`setInsecureValue` and friends) provides no
  encryption. Use it for non-sensitive data only, such as UI preferences.

## Biometric policies

The following options control how biometric authentication behaves:

| Option                          | Purpose                                                                                                                                                                                                |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `accessControl`                 | Security level for biometric hardware access: `biometryAny`, `biometryCurrentSet`, `passcodeAny`, `devicePasscode`.                                                                                    |
| `fallbackStrategy`              | What happens when biometrics are unavailable or fail: `deviceCredential` (always allow device credential fallback), `none` (biometrics only), `systemDefault` (legacy `allowDevicePasscode` behavior). |
| `allowCachedAuthentication`     | Allows repeated `unlock()` calls within `cachedAuthenticationTimeoutMs` to skip the interactive prompt.                                                                                                |
| `cachedAuthenticationTimeoutMs` | Validity window for cached authentication.                                                                                                                                                             |
| `requireFreshAuthenticationMs`  | Maximum age of the last successful biometric authentication before a fresh one is required.                                                                                                            |
| `maxBiometricAttempts`          | Failed attempts before a temporary lockout.                                                                                                                                                            |
| `lockoutDurationMs`             | Lockout duration after the failure threshold is reached.                                                                                                                                               |

A device credential fallback (passcode / PIN / pattern) is only reachable when
the configured strategy allows it. This is a deliberate tradeoff between
security strength and usability.

## Device security status and vault invalidation

`checkStatus()` reports the device posture:

- `isBiometricsAvailable` / `isBiometricsEnabled`
- `isDeviceSecure`
- `biometryType` (`none`, `touchId`, `faceId`, `fingerprint`, `iris`)

When the security posture changes (for example the user removes a biometric
enrollment or the device stops being secure), Fortress emits
`onSecurityStateChanged` and may invalidate the vault via
`onVaultInvalidated`, with reasons:

- `security_state_changed`
- `keypair_invalidated`
- `keys_deleted`

After invalidation, stored values that can no longer be decrypted or validated
are surfaced as `SECURITY_VIOLATION` errors on read. Handle these events and
errors explicitly: this is why enterprise applications typically re-prompt for
credentials and re-create keys after a state change.

## Cached authentication

When `allowCachedAuthentication` is enabled, successful unlock results are
cached in memory for `cachedAuthenticationTimeoutMs`. The user is not prompted
again for repeat `unlock()` calls inside that window.

Consider the risk profile of your application before enabling this option:
cached authentication shortens the time an attacker with a stolen, unlocked
device would need to act, while improving UX during frequent re-locks.

## Web platform security

On Web, the unlock flow uses WebAuthn:

- `local` mode stores credential metadata only in browser storage. This mode
  never contacts a backend but provides weaker guarantees than a hardware
  authenticator-backed flow.
- `server` mode uses backend challenge and assertion verification endpoints
  (`registrationStartUrl`, `registrationFinishUrl`, `authenticationStartUrl`,
  `authenticationFinishUrl`, optional `headers`), which enables replay
  protection and server-side verification.

`persistSessionState` restores web session lock/auth state across page
reloads. If you disable it, every reload starts with a fresh locked session.

Web storage (`localStorage`) is not encrypted at rest by the platform; the
plugin's Web secure layer applies `encryptionAlgorithm` on top. Never store
long-lived secrets on Web that you would not store on a native device.

## iCloud Keychain sync

`enableICloudKeychainSync` (iOS only, default `false`) creates
generic-password vault items as synchronizable, so secure entries follow the
user across devices signed into the same iCloud account.

Consider the security implications:

- Synchronized secrets are recoverable from the user's iCloud account.
- This is a product decision; enable it only when cross-device recovery is
  required and accepted.

Requires the **Keychain Sharing** capability in Xcode, see the
[permissions guide](permissions.md).

## Layered architecture

Fortress follows a strict layered architecture (Bridge → Implementation →
Config → Utils), ensuring clean separation of concerns, predictable behavior,
and production-grade reliability. Deterministic cross-platform error handling
is part of that contract: all methods reject with a machine-readable code from
`FortressErrorCode` (see the [API notes](api-notes.md) guide).
