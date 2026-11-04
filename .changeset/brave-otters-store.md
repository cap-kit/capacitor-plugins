---
"@cap-kit/secure-storage": patch
---

- Web insecure tier now reads the same key the write path stores. `setInsecure`/`getInsecure`/`hasInsecure`/`removeInsecure`/`keysInsecure` no longer prepend the secure-tier `securestorage_encrypted_` name segment, matching the Android implementation, so values written through the insecure tier on web are retrievable instead of being silently lost.
