---
"@cap-kit/ssl-pinning": patch
"@cap-kit/integrity": patch
"@cap-kit/settings": patch
"@cap-kit/redsys": patch
"@cap-kit/rank": patch
---

Fix CJS packaging conflict by renaming the CommonJS bundle to .cjs. This prevents ReferenceError: require is not defined during capacitor sync in ESM environments.
