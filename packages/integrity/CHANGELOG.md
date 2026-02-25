# @cap-kit/test-plugin

## 8.0.4

### Patch Changes

- 6a7a6e2: chore: Update internal and external dependencies to latest stable versions

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

## 8.0.0

### Major Changes

- 99248b1: Final stable release of Integrity v8. Adds real-time RASP monitoring, overlay detection for Android, and entitlement validation for iOS. Includes a synchronized boot buffer to ensure no early security signals are lost during app startup.

### Minor Changes

- 89b40cf: feat(integrity): Initial release of the Integrity plugin for Capacitor v8

### Patch Changes

- 080cffe: feat(integrity): Add diagnostic metadata and enhanced runtime heuristics for Android and iOS.
- 47342a6: feat(integrity): Implement configurable integrity checks and debug environment detection
- aecad54: Refactor native architecture to Layered Pattern, introduce Signal Correlation layer, and enhance Frida/Jailbreak detection heuristics with score transparency.
- a7dd019: feat(integrity): Add real-time integrity signal listeners.

## 8.0.0-next.6

### Major Changes

- 99248b1: Final stable release of Integrity v8. Adds real-time RASP monitoring, overlay detection for Android, and entitlement validation for iOS. Includes a synchronized boot buffer to ensure no early security signals are lost during app startup.

## 8.0.0-next.5

### Patch Changes

- aecad54: Refactor native architecture to Layered Pattern, introduce Signal Correlation layer, and enhance Frida/Jailbreak detection heuristics with score transparency.

## 8.0.0-next.4

### Patch Changes

- a7dd019: feat(integrity): Add real-time integrity signal listeners.

## 8.0.0-next.3

### Patch Changes

- 080cffe: feat(integrity): Add diagnostic metadata and enhanced runtime heuristics for Android and iOS.

## 8.0.0-next.2

### Patch Changes

- 47342a6: feat(integrity): Implement configurable integrity checks and debug environment detection

## 8.0.0-next.1

### Minor Changes

- 89b40cf: feat(integrity): Initial release of the Integrity plugin for Capacitor v8

## 6.0.1

### Patch Changes

- fa8a831: Refactor error handling by introducing a dedicated TestError class, improve configuration parsing logic, and perform general code comment cleanup.

## 6.0.0

### Major Changes

- 9003cac: Refactor iOS version retrieval logic and update core dependencies to latest Capacitor v8 standards

## 5.0.0

### Major Changes

- 28ba943: refactor(test-plugin): architectural alignment with Capacitor v8 and v4.0.0 release

## 1.0.3

### Patch Changes

- ad5059e: Internal scaffolding updates

## 1.0.2

### Patch Changes

- b733b20: refactor: Rename native implementation classes to TestImpl and update Podspec to CapKitTest.

## 1.0.1

### Patch Changes

- 6fb768a: Update internal dependencies and maintenance cleanup.

## 1.0.0

### Major Changes

- 8a5d9f9: Breaking: Updated Android namespace from `com.capkit.test` to `io.capkit.test`. Consumers extending native classes must update their imports.

## 0.1.2

### Patch Changes

- db1f487: docs: Update readme logo to use centralized asset

## 0.1.1

### Patch Changes

- d4d10cb: fix: verify automated publishing workflow

## 0.1.0

### Minor Changes

- Refactor: Standardize TypeScript configuration to inherit from monorepo root base config.
