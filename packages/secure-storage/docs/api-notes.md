# SecureStorage — API Notes

## Return shapes

- `get()` resolves `{ value: string | null }` — `null` means absent,
  never an error.
- `remove()` resolves `{ existed: boolean }` — unlike the void community
  remove, you learn whether anything was there.
- `has()` resolves `{ exists: boolean }` without decrypting.
- `keys()` resolves `{ keys: string[] }`, possibly empty.
- `getMany()` resolves `{ values: Record<string, string | null> }` —
  missing keys map to `null`, never reject the batch.
- `getSynchronize()` resolves `{ synchronize: boolean }`.

## Method behavior notes

- `set()` overwrites atomically per key. There is no transactional
  multi-key write in the standalone contract (that is a vault concern —
  see `@cap-kit/fortress` `setMany()`).
- `clear()` is destructive and, on Android, rotates the master key:
  old ciphertext is unrecoverable even from backups of the preferences
  file.
- Unknown `access` names reject with `INVALID_INPUT` on iOS; Android
  and Web ignore the field entirely (validated nowhere — pass anything,
  it is dropped).
- `setSynchronize()` / `setDefaultKeychainAccess()` resolve
  successfully on every platform; outside iOS they change nothing.

## Platform availability

Every method exists on Android, iOS, and Web. Only the strength of the
guarantee varies (hardware vs software). iCloud sync, accessibility
classes, and hardware prompts are iOS-only concepts surfaced as
documented no-ops elsewhere.

## Errors

- Missing `key`/`value` reject with `INVALID_INPUT`.
- Undecryptable payloads reject with `INIT_FAILED` (Android) or a
  Keychain failure (iOS) — treat as corruption, not absence.
- There is no `NOT_FOUND` for reads: absence is `null`, by contract.

## Versioning

`getPluginVersion()` returns the native implementation version
synchronized from `package.json` at build time. Use it in diagnostics
to prove which native binary a report came from.
