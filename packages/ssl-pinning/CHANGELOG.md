# @cap-kit/ssl-pinning

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

### Patch Changes

- a50024d: Official 8.0.0 release. Implement centralized SSLError class, refine certificate pinning configuration logic, and stabilize public APIs for production use.
