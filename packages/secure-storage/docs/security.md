# SecureStorage — Security Model

## Guarantees

- **At rest, on native:** AES-GCM ciphertext only. Android keys never
  leave the Keystore; iOS items sit in the Keychain with an explicit
  accessibility class, hardware-backed on modern devices.
- **Forward secrecy on wipe:** deleting the Android master key in
  `clear()` cryptographically shreds everything, including backups of
  the preferences file. iOS deletion removes the items; device-level
  guarantees apply per accessibility class.
- **No silent exfiltration surface:** `has()` and `keys()` never decrypt
  values; enumeration returns names only.

## Limits

- **No authentication.** Anyone holding an unlocked device with the app
  installed reads everything. This plugin verifies nothing — pair it
  with `@cap-kit/biometrics` (gate the screen) or `@cap-kit/fortress`
  (gate the data behind a lock) when access must require the user.
- **No backup exclusion control beyond accessibility.** `ThisDeviceOnly`
  classes never migrate; the rest follow the platform backup policy.
  Choose per item.
- **Web is software-grade.** Origin-bound AES-GCM raises the bar over
  plaintext but cannot match hardware backing. Never treat browser
  storage as a vault. Persistence runs on an engine chain (IndexedDB,
  then LocalStorage, then in-memory) with legacy migration, so
  private-mode quota failures degrade instead of crashing.
- **Memory hygiene is yours.** Decrypted strings cross the bridge as
  JS values; avoid logging them and clear UI state on lock/background.

## Write durability (Android)

Every `SharedPreferences` write commits synchronously (`commit()`, not
`apply()`): a slower write that survives abrupt process death (recents
swipe, task manager), matching the upstream official fix. iOS needs no
equivalent — the system flushes `UserDefaults` on backgrounding.

## Best practices

- Prefer `has()` over `get()` for existence checks to avoid needless
  decryption (and Keychain AuthenticationUI prompts on locked items).
- Batch with `getMany()` instead of N round-trips.
- Set the strictest accessibility each item tolerates; use the session
  default only for the common case.
- Rotate sensitive values with `remove()` + `set()` rather than
  overwriting when the old ciphertext must die immediately on Android
  (overwrite keeps the same master key, which is correct — rotation
  happens on `clear()`).
