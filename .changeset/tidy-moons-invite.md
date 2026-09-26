---
"@cap-kit/redsys": patch
---

fix(redsys): Restore a buildable iOS pod and a working SDK setup command

- Fixed an iOS compile error shipped in 8.1.0. `RedsysUtils.maskCardNumber` referenced an undeclared identifier, so `CapKitRedsys` failed to build on every consumer. The broken function sits on the successful payment response path. **8.1.1 is the first published version whose iOS sources compile** — upgrading is required, not optional.
- Replaced the `npm run setup-redsys-sdk` command in `README.md` and `docs/configuration.md` with the working `node node_modules/@cap-kit/redsys/scripts/setup-redsys-sdk.mjs` invocation, and documented that it must run from the app root.
- Wired the `setup-redsys-sdk` script in `package.json` to `scripts/setup-redsys-sdk.mjs`, replacing the `setup-sdk` placeholder that only echoed `Manual SDK setup required`.
- Removed a non-existent `ios/App/build.gradle` entry from the tree printed by the orchestrator script.

Two independent defects produced the `Missing script: "setup-redsys-sdk"` error: the documented name never existed in `package.json`, and `npm run` can never resolve a script from an installed dependency — package managers read only the current project's `package.json`, while the orchestrator resolves every path against the app root.

The iOS compile error went unnoticed because every native verification script in this package is a stub that reports success without running anything. That is a separate, ongoing gap; this release only fixes the symptom.
