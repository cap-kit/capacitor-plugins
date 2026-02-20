---
"@cap-kit/ssl-pinning": minor
---

Standardize error handling, introduce advanced domain-based pinning, and harden infrastructure across platforms.

- **Advanced Pinning Configuration**: Added `certsByDomain` support for specific host matching (including most-specific subdomain logic) and an optional `certsManifest` for automated certificate discovery.
- **Fail-Fast Validation**: Implemented mandatory certificate validation at config load time to prevent silent failures from missing or unreadable cert files.
- **JS Semantics Alignment**: Fingerprint mismatches and excluded domains now resolve with descriptive success objects (`fingerprintMatched: false` or `excludedDomain: true`) instead of rejecting, ensuring consistent JavaScript behavior.
- **Error & Validation Hardening**: Unified all error codes (e.g., `INVALID_INPUT`, `TIMEOUT`, `NETWORK_ERROR`) and canonical messages across iOS and Android using a master list. Added strict SHA-256 fingerprint format validation.
- **Infrastructure & Stability**:
  - Implemented deterministic 10-second timeouts on both platforms to prevent hanging calls.
  - Fixed iOS `URLSession` lifecycle leaks through proper invalidation and hardened Android `execute` block safety.
  - Improved Android architecture with nominal result models and centralized fingerprint normalization in pure utilities.
  - Added comprehensive unit testing for normalization and domain matching rules.
- **Platform Parity**: Normalized fingerprint output format to lowercase hex without colons and ensured Android performs real TLS handshakes for excluded domains using system trust to match iOS behavior.
