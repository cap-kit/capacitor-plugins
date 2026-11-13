# Fortress — API Notes and Error Handling

This guide covers the Promise model used by every Fortress method, the
`FortressErrorCode` reference, and the development/testing overrides offered
by the plugin.

## Promise model

All Fortress plugin methods return Promises and may reject in case of failure.
Consumers should always handle errors using `try / catch`. When rejected, the
error object contains a machine-readable `code` from `FortressErrorCode`.

### Example

```ts
import { Fortress, FortressErrorCode } from '@cap-kit/fortress';

try {
  await Fortress.xxx();
} catch (err: any) {
  switch (err.code) {
    case FortressErrorCode.UNAVAILABLE:
      // Feature not supported on this device or platform
      break;

    case FortressErrorCode.INIT_FAILED:
      // Native initialization or runtime failure
      break;

    default:
      // Unknown or unexpected error
      console.error(err.message);
  }
}
```

## Error codes

The following error codes may be returned by the plugin:

| Code                 | Description                                           |
| -------------------- | ----------------------------------------------------- |
| `UNAVAILABLE`        | Feature not supported on this device or configuration |
| `CANCELLED`          | User cancelled the operation                          |
| `PERMISSION_DENIED`  | Required permission was denied                        |
| `INIT_FAILED`        | Native initialization or runtime failure              |
| `INVALID_INPUT`      | Invalid input provided                                |
| `NOT_FOUND`          | Requested resource not found                          |
| `CONFLICT`           | Operation conflicts with current state                |
| `TIMEOUT`            | Operation timed out                                   |
| `SECURITY_VIOLATION` | Security validation failed                            |
| `VAULT_LOCKED`       | Vault is locked, unlock required                      |

### Example

```ts
import { Fortress, FortressErrorCode } from '@cap-kit/fortress';

try {
  await Fortress.unlock();
} catch (err: any) {
  switch (err.code) {
    case FortressErrorCode.VAULT_LOCKED:
      // Vault is locked - prompt for biometric
      break;
    case FortressErrorCode.CANCELLED:
      // User cancelled - handle gracefully
      break;
    case FortressErrorCode.UNAVAILABLE:
      // Biometrics not available
      break;
    case FortressErrorCode.SECURITY_VIOLATION:
      // Security issue detected
      break;
    default:
      console.error(err.message);
  }
}
```

## Development and testing overrides

The plugin exposes three overrides intended for QA and simulator/device
mocking flows:

- `setBiometryType({ biometryType })` — overrides the detected biometry type.
- `setBiometryIsEnrolled({ isBiometricsEnabled })` — overrides the biometrics
  enrollment state.
- `setDeviceIsSecure({ isDeviceSecure })` — overrides the device secure-state.

Use these ONLY in development/testing scenarios. In production, the values
reported by `checkStatus()` come from the device.

## Vault behavior notes

- `getValue` rejects with `VAULT_LOCKED` when the vault is locked and with
  `SECURITY_VIOLATION` when stored data cannot be decrypted/validated.
- `getObfuscatedKey` is an internal utility to mask keys in standard storage.
  Useful for consistent key naming across storage tiers.
- `hasKey` is an optimized existence check that does not retrieve the value,
  making it useful for checking session tokens without triggering decryption.

## Signature encoding

`createSignature` returns Base64 on iOS/Android and Base64URL on Web
(WebAuthn). Backend verification must normalize Base64URL to Base64 when
verifying WebAuthn assertions.
