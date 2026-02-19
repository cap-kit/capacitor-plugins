---
"@cap-kit/rank": patch
---

Improve Android In-App Review handling when Play Store is missing or unofficial.

- Prevents unhandled `ReviewException` when `PLAY_STORE_NOT_FOUND` occurs.
- Adds a pre-check to detect Play Store availability before requesting review flow.
- Maps `PLAY_STORE_NOT_FOUND` and similar environment issues to `UNAVAILABLE` instead of `INIT_FAILED`.
- Ensures the plugin fails gracefully instead of logging noisy Play Core errors.
