# @cap-kit/settings

## 8.1.4

### Patch Changes

- 8a19c0f: feat: Unify error handling, improve DX, and align native configurations for @cap-kit/settings plugin

  This release introduces significant improvements to the `@cap-kit/settings` plugin, focusing on consistency, developer experience, and maintainability.

  Key changes include:
  - **Unified Error Handling:** Standardized to a Promise rejection model across TypeScript, Web, Android, and iOS. The `README.md` and JSDoc have been updated to reflect this, providing clear guidan and `try/catch` examples for error handling.
  - **Improved PlatformOptions:** Made `PlatformOptions` more flexible, allowing developers to provide only platform-relevant options (`optionIOS` or `optionAndroid`). Robust input validation (`INVALID_INPUT`) is now enforced across native layers for missing options.
  - **Android Main Thread Safety:** Ensured all UI operations on Android are safely dispatched to the main thread, enhancing runtime robustness.
  - **Cleaned Up API Surface:** Clarified the roles of `open()`, `openIOS()`, and `openAndroid()` in documentation, promoting `open()` as the primary, cross-platform entry point.
  - **Native Configuration Alignment:** Removed unsupported enum members from `AndroidSettings` to ensure all publicly exposed values have a reliable native mapping.
  - **Architectural Improvements:** Addressed package/path mismatches in Android's `SettingsConfig.kt` and relocated the Capacitor-dependent logger in iOS to a dedicated `logger/` folder, improving maintainability and layer separation.
  - **Enhanced Debugging:** Added plugin version logging during load time on both platforms to aid remote debugging.

## 8.1.3

### Patch Changes

- 04c071c: Fix CJS packaging conflict by renaming the CommonJS bundle to .cjs. This prevents ReferenceError: require is not defined during capacitor sync in ESM environments.

## 8.1.2

### Patch Changes

- 873768a: Refactor package exports for better ESM compatibility and remove deprecated kotlinOptions in build.gradle.

## 8.1.1

### Patch Changes

- cc8d34f: Enhance package metadata and update dependencies:
  - Add `sideEffects: false` to enable tree-shaking in consumer bundlers.
  - Add `funding` information for project sustainability.
  - Define explicit `exports` for ESM and CJS compatibility.
  - Update Capacitor minor dependencies to ensure alignment with v8+ standards.

## 8.0.2

### Patch Changes

- ed8d37e: Update README documentation and bump internal dependencies for improved stability.

## 8.0.1

### Patch Changes

- 5d7bd9c: Implement centralized error handling via SettingsError class, refine configuration parsing logic, and update internal documentation for better maintainability.

## 8.0.0

### Patch Changes

- ecdb901: Initial release for Capacitor v8 alignment.
