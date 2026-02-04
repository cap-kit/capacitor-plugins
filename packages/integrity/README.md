<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Integrity</h3>

<p align="center">
  <strong>
    <code>@cap-kit/integrity</code>
  </strong>
</p>

<p align="center">
  Runtime integrity and environment signal detection for Capacitor applications.<br>
  Provides <strong>observational signals</strong> about the execution environment,
  such as rooting, jailbreaking, emulators, instrumentation, and basic tampering
  indicators on <strong>Android</strong> and <strong>iOS</strong>.<br><br>
  Designed for <strong>Capacitor v8</strong>, with a stable, platform-agnostic
  JavaScript API and first-class support for
  <strong>Swift Package Manager</strong> and modern Android toolchains.
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/integrity">
    <img src="https://img.shields.io/npm/v/@cap-kit/integrity?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/integrity">
    <img src="https://img.shields.io/npm/dm/@cap-kit/integrity?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/integrity?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>

<br />

---

## Overview

`@cap-kit/integrity` provides **runtime integrity and environment signals**
for Capacitor applications on **Android** and **iOS**.

The plugin performs **best-effort detection** of conditions such as:

- Rooted / jailbroken devices
- Emulators and simulators
- Debug and instrumentation indicators
- Basic tampering and repackaging signals
- Frida and runtime hooking heuristics

The plugin **does NOT**:

- enforce security policies
- block or terminate the application
- present UI automatically
- guarantee protection against advanced attackers

All decisions are explicitly delegated to the **host application**.

---

## Security Model (IMPORTANT)

This plugin follows an **observational security model**.

It reports **signals**, not decisions.

- Signals are heuristic observations
- Signals may produce false positives or false negatives
- The returned score is **provisional** and **non-normative**
- Consumers MUST combine signals with business logic

> ⚠️ This plugin is **NOT** a replacement for a full RASP or DRM solution.

---

## Install

```bash
pnpm add @cap-kit/integrity
# or
npm install @cap-kit/integrity
# or
yarn add @cap-kit/integrity
# then run:
npx cap sync
```

---

## Configuration

Configuration is **static** and read **natively** from `capacitor.config`.

These values are:

- read once at plugin initialization
- immutable at runtime
- NOT accessible from JavaScript

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the Integrity plugin.

| Prop                 | Type                                  | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Default            | Since |
| -------------------- | ------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code>                  | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API or runtime behavior.                                                                                                                                                                                                                                             | <code>false</code> | 1.0.0 |
| **`blockPage`**      | <code>IntegrityBlockPageConfig</code> | Optional configuration for the integrity block page. This configuration controls the availability and source of a developer-provided HTML page that may be presented to the end user when the host application decides to do so. This configuration is: - read only by native code - immutable at runtime - NOT accessible from JavaScript The Integrity plugin will NEVER automatically present the block page. Presentation is always explicitly triggered by the host application via the public API. |                    | 1.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Integrity": {
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/integrity" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Integrity: {
      verboseLogging: true,
    },
  },
};

export default config;
```

</docgen-config>

---

## Usage

### Basic integrity check

```ts
import { Integrity } from '@cap-kit/integrity';

const report = await Integrity.check();

if (report.compromised) {
  // Decide what to do
}
```

### Integrity check options

`Integrity.check()` accepts optional parameters that control
the strictness and verbosity of the integrity checks.

```ts
const report = await Integrity.check({
  level: 'standard',
  includeDebugInfo: true,
});
```

#### Options

| Option             | Type                                | Default   | Description                                          |
| ------------------ | ----------------------------------- | --------- | ---------------------------------------------------- |
| `level`            | `'basic' \| 'standard' \| 'strict'` | `'basic'` | Controls which categories of checks are executed     |
| `includeDebugInfo` | `boolean`                           | `false`   | Includes diagnostic descriptions in returned signals |

#### Levels behavior

- **basic**
  - Root / jailbreak detection
  - Emulator / simulator detection

- **standard**
  - All `basic` checks
  - Debugger / debug build detection
  - Instrumentation / hooking heuristics

- **strict**
  - All `standard` checks
  - Additional tamper and integrity heuristics

> ⚠️ The returned integrity score remains provisional and
> must not be used as the sole security decision signal.

---

### Presenting a block / warning page

The plugin will **never** present UI automatically.

UI presentation is always explicitly triggered by the host application.

```ts
await Integrity.presentBlockPage({
  reason: 'integrity_failed',
});
```

#### Dismissible block page (optional)

By default, the block page is **not dismissible** (secure-by-default).

For demos, testing, or controlled environments, dismissal can be explicitly enabled:

```ts
await Integrity.presentBlockPage({
  reason: 'integrity_failed',
  dismissible: true,
});
```

Platform behavior:

| Platform | dismissible = false    | dismissible = true              |
| -------- | ---------------------- | ------------------------------- |
| Android  | Back & close disabled  | Native close button + back      |
| iOS      | Swipe & close disabled | Swipe-to-dismiss + native close |
| Web      | Not supported          | Not supported                   |

> ⚠️ In production environments, it is recommended to keep `dismissible` disabled.

---

## Error Handling

All Integrity methods use **Promise rejection** for error handling.

Errors are returned as a structured object:

```ts
{
  message: string;
  code: IntegrityErrorCode;
}
```

Error handling is **consistent across Android, iOS, and Web**.

### Example

```ts
import { Integrity, IntegrityErrorCode } from '@cap-kit/integrity';

try {
  await Integrity.check();
} catch (e) {
  if (e.code === IntegrityErrorCode.UNAVAILABLE) {
    // Feature not available on this platform
  }
}
```

---

## Web Platform Notes

The Web platform is supported to preserve API parity.

However, native integrity checks are **not available** in browser environments.

The following methods will reject with `IntegrityErrorCode.UNAVAILABLE` on Web:

- `Integrity.check()`
- `Integrity.presentBlockPage()`

`Integrity.getPluginVersion()` is always available.

---

## API

The public API is fully typed and documented via TypeScript definitions.

See `definitions.ts` for the complete contract.

<docgen-index>

- [`check(...)`](#check)
- [`presentBlockPage(...)`](#presentblockpage)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Integrity Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

### check(...)

```typescript
check(options?: IntegrityCheckOptions | undefined) => Promise<IntegrityReport>
```

Executes a runtime integrity check.

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code><a href="#integritycheckoptions">IntegrityCheckOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#integrityreport">IntegrityReport</a>&gt;</code>

**Since:** 1.0.0

#### Example

```ts
const report = await Integrity.check();
```

---

### presentBlockPage(...)

```typescript
presentBlockPage(options?: PresentBlockPageOptions | undefined) => Promise<PresentBlockPageResult>
```

Presents the configured integrity block page, if enabled.

The plugin never decides _when_ this method should be called.
Invocation is entirely controlled by the host application.

| Param         | Type                                                                        |
| ------------- | --------------------------------------------------------------------------- |
| **`options`** | <code><a href="#presentblockpageoptions">PresentBlockPageOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#presentblockpageresult">PresentBlockPageResult</a>&gt;</code>

**Since:** 1.0.0

#### Example

```ts
await Integrity.presentBlockPage({ reason: 'integrity_failed' });
```

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 0.0.1

#### Example

```ts
const { version } = await Integrity.getPluginVersion();
```

---

### Interfaces

#### IntegrityReport

Result object returned by `Integrity.check()`.

This object aggregates all detected signals
and provides a provisional integrity score.

| Prop              | Type                                                                  | Description                                                                                           |
| ----------------- | --------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| **`compromised`** | <code>boolean</code>                                                  | Indicates whether the environment is considered compromised according to the current scoring model.   |
| **`score`**       | <code>number</code>                                                   | Provisional integrity score. The score ranges from 0 to 100 and is derived from the detected signals. |
| **`signals`**     | <code>IntegritySignal[]</code>                                        | List of detected integrity signals.                                                                   |
| **`environment`** | <code><a href="#integrityenvironment">IntegrityEnvironment</a></code> | Execution environment summary.                                                                        |
| **`timestamp`**   | <code>number</code>                                                   | Unix timestamp (milliseconds) when the check was performed.                                           |

#### IntegritySignal

A single integrity signal detected on the current device.

Signals represent _observations_, not decisions.
Multiple signals may be combined by the host application
to derive a security policy.

| Prop              | Type                                                                        | Description                                                                                                                         |
| ----------------- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| **`id`**          | <code>string</code>                                                         | Stable identifier for the signal. This value is intended for analytics, logging, and policy evaluation.                             |
| **`category`**    | <code><a href="#integritysignalcategory">IntegritySignalCategory</a></code> | High-level category of the signal.                                                                                                  |
| **`confidence`**  | <code>'low' \| 'medium' \| 'high'</code>                                    | Confidence level of the detection. Confidence expresses how strongly the signal correlates with a compromised or risky environment. |
| **`description`** | <code>string</code>                                                         | Optional human-readable description. This field may be omitted in production builds and SHOULD NOT be relied upon programmatically. |

#### IntegrityEnvironment

Summary of the execution environment in which
the integrity check was performed.

| Prop               | Type                                     | Description                                                                   |
| ------------------ | ---------------------------------------- | ----------------------------------------------------------------------------- |
| **`platform`**     | <code>'ios' \| 'android' \| 'web'</code> | Current platform.                                                             |
| **`isEmulator`**   | <code>boolean</code>                     | Indicates whether the app is running in an emulator or simulator environment. |
| **`isDebugBuild`** | <code>boolean</code>                     | Indicates whether the app was built in debug/development mode.                |

#### IntegrityCheckOptions

Options controlling the behavior of `Integrity.check()`.

These options influence _how_ checks are performed,
not _what_ the public API returns.

| Prop                   | Type                                           | Description                                                                                          |
| ---------------------- | ---------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| **`level`**            | <code>'basic' \| 'standard' \| 'strict'</code> | Desired strictness level. Higher levels may enable additional heuristics at the cost of performance. |
| **`includeDebugInfo`** | <code>boolean</code>                           | Includes additional debug information in the returned signals when enabled.                          |

#### PresentBlockPageResult

Result object returned by `presentBlockPage()`.

| Prop            | Type                 | Description                                              |
| --------------- | -------------------- | -------------------------------------------------------- |
| **`presented`** | <code>boolean</code> | Indicates whether the block page was actually presented. |

#### PresentBlockPageOptions

Options for presenting the integrity block page.

| Prop              | Type                 | Description                                                                                                                                | Default            | Since |
| ----------------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------ | ----- |
| **`reason`**      | <code>string</code>  | Optional reason code passed to the block page. This value may be used for analytics, localization, or user messaging.                      |                    | 1.0.0 |
| **`dismissible`** | <code>boolean</code> | Whether the block page can be dismissed by the user. Defaults to false. In production environments, this should typically remain disabled. | <code>false</code> | 1.0.0 |

#### PluginVersionResult

Result returned by the getPluginVersion method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

### Type Aliases

#### IntegritySignalCategory

Category of a detected integrity signal.

Categories are intentionally broad and stable.
New detection techniques MUST reuse existing categories
whenever possible to avoid breaking consumers.

<code>'root' | 'jailbreak' | 'emulator' | 'debug' | 'hook' | 'tamper' | 'environment'</code>

</docgen-api>

---

## Limitations

- Root / jailbreak detection can be bypassed
- Emulator detection is heuristic
- Frida detection is best-effort only
- No cryptographic attestation is performed
- No device identity is established

These limitations are **intentional** to:

- avoid store policy violations
- reduce false positives
- keep the plugin portable and maintainable

### Debug environment detection

The plugin may report a `debug` integrity signal when:

- a debugger is attached to the process
- the application is running in a debuggable build

This signal is **informational only** and does not necessarily
indicate a compromised device.

Consumers MUST interpret debug signals in context and
combine them with other integrity signals.

---

## 🔒 Integrity philosophy

### Observation, not enforcement

The Integrity plugin is designed as an **observation layer**, not as an enforcement or decision engine.

Its responsibility is to **detect and report runtime integrity signals** in a consistent, cross-platform way.
It deliberately **does not decide what action should be taken** when a signal is detected.

This design allows applications to:

- define their own security policies
- adapt behavior to different environments
- avoid hard-coded or platform-specific assumptions

In short:

> **The plugin observes.
> The application decides.**

---

## 🧩 Why there are no single-purpose checks

The public API intentionally exposes a **single entry point**:

```ts
Integrity.check(options?)
```

Rather than providing individual methods such as:

- `checkRoot()`
- `checkEmulator()`
- `checkDebug()`

the plugin returns a **structured integrity report** containing multiple **signals**, each classified by:

- category (e.g. root, emulator, debug, hook, tamper)
- confidence level (low / medium / high)

This approach avoids:

- API fragmentation
- platform-specific behavior leaks
- misuse of isolated checks
- rigid or unsafe security decisions

Applications can still derive fine-grained logic by inspecting the returned signals:

```ts
const hasRoot = report.signals.some((s) => s.category === 'root');
const hasEmulator = report.signals.some((s) => s.category === 'emulator');
```

This keeps the API **stable, extensible, and policy-agnostic**.

---

## 🧠 Integrity levels

Instead of selecting individual checks, applications choose a **strictness level**:

- **basic** – lightweight and safe checks
- **standard** – includes debug and hooking detection
- **strict** – adds tampering and signature integrity checks

The selected level controls **how deeply the environment is inspected**, not which single signal is exposed.

---

## 🔮 Future extensibility

The current API focuses on a clean and minimal surface, but the architecture is designed to be **extensible**.

In the future, the plugin may introduce:

- more advanced signal classification
- additional integrity signals
- configurable inclusion or exclusion of signal groups
- richer metadata for diagnostics and auditing

Any future extension will preserve the same core principle:

- **one entry point**
- **no forced policy**
- **no platform-specific leakage**

Applications should always remain in full control of how integrity information is interpreted and enforced.

---

## ✅ Summary

- The Integrity plugin **detects signals**, it does not block or enforce
- Security decisions are **always owned by the application**
- The API is designed for **long-term stability and flexibility**
- Future enhancements will extend capabilities without breaking this model

---

## Roadmap (Non-binding)

- Optional platform attestation helpers
- Extended tamper heuristics
- Improved scoring models
- Documentation examples

---

## Contributing

Contributions are welcome! Please read the
[contributing guide](CONTRIBUTING.md)
before submitting a pull request.

---

## Credits

This plugin is based on prior work from the Community and
has been refactored and modernized for **Capacitor v8** and
**Swift Package Manager** compatibility.

Original inspiration:

- [https://github.com/capacitor-community/device-security-detect](https://github.com/capacitor-community/device-security-detect)

---

## License

MIT
