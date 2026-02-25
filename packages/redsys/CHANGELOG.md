# @cap-kit/redsys

## 8.0.2

### Patch Changes

- 6a7a6e2: chore: Update internal and external dependencies to latest stable versions

## 8.0.1

### Patch Changes

- 04c071c: Fix CJS packaging conflict by renaming the CommonJS bundle to .cjs. This prevents ReferenceError: require is not defined during capacitor sync in ESM environments.

## 8.0.0

### Patch Changes

- c0778b6: chore: bypass native CI checks for initial release
- 2d4278b: feat(redsys): Initial release with dynamic SDK injection logic for Android and iOS
- adcf29d: fix(redsys): Update build logic to allow CI checks to pass without native SDK
- a7e0a0f: feat(redsys): Initial plugin structure for Redsys InApp SDK integration
