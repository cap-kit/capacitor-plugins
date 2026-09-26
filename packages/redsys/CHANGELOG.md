# @cap-kit/redsys

## 8.1.1

### Patch Changes

- 640ef3b: fix(redsys): Restore a buildable iOS pod and a working SDK setup command

  - Fixed an iOS compile error shipped in 8.1.0. `RedsysUtils.maskCardNumber` referenced an undeclared identifier, so `CapKitRedsys` failed to build on every consumer. The broken function sits on the successful payment response path. **8.1.1 is the first published version whose iOS sources compile** — upgrading is required, not optional.
  - Replaced the `npm run setup-redsys-sdk` command in `README.md` and `docs/configuration.md` with the working `node node_modules/@cap-kit/redsys/scripts/setup-redsys-sdk.mjs` invocation, and documented that it must run from the app root.
  - Wired the `setup-redsys-sdk` script in `package.json` to `scripts/setup-redsys-sdk.mjs`, replacing the `setup-sdk` placeholder that only echoed `Manual SDK setup required`.
  - Removed a non-existent `ios/App/build.gradle` entry from the tree printed by the orchestrator script.

  Two independent defects produced the `Missing script: "setup-redsys-sdk"` error: the documented name never existed in `package.json`, and `npm run` can never resolve a script from an installed dependency — package managers read only the current project's `package.json`, while the orchestrator resolves every path against the app root.

  The iOS compile error went unnoticed because every native verification script in this package is a stub that reports success without running anything. That is a separate, ongoing gap; this release only fixes the symptom.

## 8.1.0

### Minor Changes

- bdd5a6d: Bump minor versions for all published plugins.

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
