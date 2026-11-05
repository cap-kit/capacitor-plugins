---
"@cap-kit/secure-storage": patch
---

- Web `keys()` and `keysInsecure()` now preserve raw key names that happen to be valid Base64 values when key obfuscation is disabled, while retaining fallback decoding for entries written with legacy obfuscation prefixes.
