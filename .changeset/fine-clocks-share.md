---
"@cap-kit/rank": patch
---

Fix stability and cross-platform parity for the Rank plugin.

- Enforce strict layer separation (Bridge-only resolve/reject; Impl Capacitor-free).
- Align iOS exported methods with the public JS API (parity with TS/README).
- Improve safety around Activity/UI/Intent execution on Android to prevent crashes.
- Normalize error codes/messages for more consistent behavior across platforms.
- Web now reports unsupported methods via explicit `unavailable()` instead of silent warnings.
