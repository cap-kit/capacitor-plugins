---
"@cap-kit/rank": minor
---

Hardened Android and iOS implementations with improved thread-safety, concurrency control, and input validation.

- Added atomic concurrency guard for Android review flow
- Standardized error propagation across platforms
- Hardened input validation for packageName, devId, collection name and search terms
- Moved Android validators to dedicated utils layer
- Moved iOS App ID validator to Utils layer
- Ensured main-thread safety for iOS review and product page flows
- Removed dead iOS legacy fallback code (iOS 15+ target)
