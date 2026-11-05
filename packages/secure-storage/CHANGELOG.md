# @cap-kit/secure-storage

## 8.1.2

### Patch Changes

- 063a773: - Web `keys()` and `keysInsecure()` now preserve raw key names that happen to be valid Base64 values when key obfuscation is disabled, while retaining fallback decoding for entries written with legacy obfuscation prefixes.

## 8.1.1

### Patch Changes

- 819ddb4: - Web insecure tier now reads the same key the write path stores. `setInsecure`/`getInsecure`/`hasInsecure`/`removeInsecure`/`keysInsecure` no longer prepend the secure-tier `securestorage_encrypted_` name segment, matching the Android implementation, so values written through the insecure tier on web are retrievable instead of being silently lost.

## 8.1.0

### Minor Changes

- b4ef5b0: feat: Initial release
