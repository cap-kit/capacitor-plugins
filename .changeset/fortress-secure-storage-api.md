---
"@cap-kit/fortress": minor
---

Add secure-storage enumeration and sync API aligned with community contracts: `keys()` / `getMany()` for both tiers, runtime `setSynchronize()` / `getSynchronize()` (iCloud Keychain), iOS `KeychainAccess` levels with `setDefaultKeychainAccess()` plus per-item `access` in `setValue()`. Android `clearAll()` already rotates Keystore keys (Capawesome parity confirmed).
