# SecureStorage — Platform Behavior

## Android

Values are encrypted with AES-GCM under a Keystore-held master key; only
opaque ciphertext lives in a private `SharedPreferences` file.

- No hardware prompt is ever shown: reads are always available, exactly
  like the community secure-storage contract.
- `clear()` wipes preferences **and deletes the master key**, so
  previously captured ciphertext can never be decrypted again (matches
  the documented community key-reset behavior).
- `setSynchronize()` / `setDefaultKeychainAccess()` are no-ops;
  `getSynchronize()` is always `false`.

## iOS

Values live as generic-password items in the Keychain under the
`io.capkit.securestorage` service.

- Default accessibility is `AfterFirstUnlockThisDeviceOnly` (hardware-backed,
  background-capable, never migrates) — or `AfterFirstUnlock` while iCloud
  sync is on. Per-item `access` and the session default override it; an
  explicit access implies device-only storage except for the one syncable
  class.
- `setSynchronize()` toggles iCloud sync for the session;
  `enableICloudKeychainSync` sets the startup value.
- Key enumeration scopes to the plugin service across local and
  synchronizable stores.

## Web

Values are AES-GCM encrypted (PBKDF2, 150k iterations) under an
origin-bound software key in `localStorage`. There is no hardware
backing and no sync or accessibility support — both are no-ops and
`getSynchronize()` is `false`.
