---
"@cap-kit/redsys": patch
---

fix(redsys): Correct the SDK orchestrator invocation in the setup docs

- Replaced the `npm run setup-redsys-sdk` command in `README.md` and `docs/configuration.md` with the working `node node_modules/@cap-kit/redsys/scripts/setup-redsys-sdk.mjs` invocation, and documented that it must run from the app root.
- Wired the `setup-redsys-sdk` script in `package.json` to `scripts/setup-redsys-sdk.mjs`, replacing the `setup-sdk` placeholder that only echoed `Manual SDK setup required`.
- Removed a non-existent `ios/App/build.gradle` entry from the tree printed by the orchestrator script.

The documented command could never work: package managers resolve `run` scripts only from the `package.json` of the current project, never from an installed dependency, and the orchestrator resolves every path relative to the app root. Users following the guide on 8.1.0 hit `Missing script: "setup-redsys-sdk"`.
