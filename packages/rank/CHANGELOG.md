# @cap-kit/rank

## 8.1.0

### Minor Changes

- fb1eb22: Hardened Android and iOS implementations with improved thread-safety, concurrency control, and input validation.
  - Added atomic concurrency guard for Android review flow
  - Standardized error propagation across platforms
  - Hardened input validation for packageName, devId, collection name and search terms
  - Moved Android validators to dedicated utils layer
  - Moved iOS App ID validator to Utils layer
  - Ensured main-thread safety for iOS review and product page flows
  - Removed dead iOS legacy fallback code (iOS 15+ target)

## 8.0.3

### Patch Changes

- 04c071c: Fix CJS packaging conflict by renaming the CommonJS bundle to .cjs. This prevents ReferenceError: require is not defined during capacitor sync in ESM environments.

## 8.0.2

### Patch Changes

- 873768a: Refactor package exports for better ESM compatibility and remove deprecated kotlinOptions in build.gradle.

## 8.0.1

### Patch Changes

- cc8d34f: Enhance package metadata and update dependencies:
  - Add `sideEffects: false` to enable tree-shaking in consumer bundlers.
  - Add `funding` information for project sustainability.
  - Define explicit `exports` for ESM and CJS compatibility.
  - Update Capacitor minor dependencies to ensure alignment with v8+ standards.
