---
"@cap-kit/ssl-pinning": minor
---

Standardize error handling and cross-platform behavior for SSL Pinning.

- Fingerprint mismatches and excluded domains now resolve with a success result containing `fingerprintMatched: false` or `excludedDomain: true` instead of rejecting, ensuring consistent JS semantics.
- Align all error codes and messages across iOS and Android using a canonical master list (e.g., "url is required", "Invalid fingerprint format").
- Implement deterministic 10-second timeouts on both platforms to prevent hanging calls.
- Harden input validation for SHA-256 fingerprints and URL host extraction.
- Improve Android architecture with nominal result models and iOS lifecycle with proper URLSession invalidation.
- Normalize fingerprint output format to lowercase hex without colons for consistency.
