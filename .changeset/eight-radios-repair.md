---
"@cap-kit/settings": patch
---

feat: Unify error handling, improve DX, and align native configurations for @cap-kit/settings plugin

This release introduces significant improvements to the `@cap-kit/settings` plugin, focusing on consistency, developer experience, and maintainability.

Key changes include:

- **Unified Error Handling:** Standardized to a Promise rejection model across TypeScript, Web, Android, and iOS. The `README.md` and JSDoc have been updated to reflect this, providing clear guidan and `try/catch` examples for error handling.
- **Improved PlatformOptions:** Made `PlatformOptions` more flexible, allowing developers to provide only platform-relevant options (`optionIOS` or `optionAndroid`). Robust input validation (`INVALID_INPUT`) is now enforced across native layers for missing options.
- **Android Main Thread Safety:** Ensured all UI operations on Android are safely dispatched to the main thread, enhancing runtime robustness.
- **Cleaned Up API Surface:** Clarified the roles of `open()`, `openIOS()`, and `openAndroid()` in documentation, promoting `open()` as the primary, cross-platform entry point.
- **Native Configuration Alignment:** Removed unsupported enum members from `AndroidSettings` to ensure all publicly exposed values have a reliable native mapping.
- **Architectural Improvements:** Addressed package/path mismatches in Android's `SettingsConfig.kt` and relocated the Capacitor-dependent logger in iOS to a dedicated `logger/` folder, improving maintainability and layer separation.
- **Enhanced Debugging:** Added plugin version logging during load time on both platforms to aid remote debugging.
