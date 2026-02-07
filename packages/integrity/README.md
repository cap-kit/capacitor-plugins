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

## Versioning & Release Policy (IMPORTANT)

The `@cap-kit/integrity` plugin is currently under **active development** toward
a stable `v8.0.0` release.

All versions published as `v8.0.0-next.x` are considered:

- **internal development milestones**
- **non-final**
- **subject to change without notice**
- **not intended for production use**

### What `next.x` versions represent

Each `next.x` version corresponds to a **focused implementation step**
(e.g. internal refactoring, architectural groundwork, or a single new capability),
not to a user-facing release milestone.

Examples:

- `next.4` introduces **real-time integrity listeners**
- other planned capabilities (attestation, early boot detection, advanced RASP)
  are intentionally deferred to later `next.x` stages

These intermediate versions exist to:

- keep development incremental and auditable
- reduce risk by isolating architectural changes
- avoid bundling unrelated features into a single step

### Publication policy

Until a stable `v8.0.0` release is published:

- `next.x` versions MAY be published for testing and feedback
- no backward-compatibility guarantees are provided
- API details MAY evolve as part of the stabilization process

Consumers should consider `next.x` builds as **pre-release artifacts**
and plan adoption accordingly.

The `v8.0.0` release will mark:

- API stabilization
- documented support guarantees
- readiness for production usage

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

## Native Configuration (Optional — Early Boot Enhancement)

To capture security signals at the earliest possible stage (before the Capacitor bridge is initialized), you can manually integrate the plugin into your App Host's native entry points. This is highly recommended for detecting advanced threats like Root/Jailbreak or early instrumentation.

### When should you use native early boot integration?

Native early boot integration is **optional** and exists to improve
**signal timing**, not signal correctness.

You SHOULD consider this integration if:

- you want to observe potential root / jailbreak conditions
  **as early as possible**
- you need visibility into instrumentation that may attach
  before the JavaScript runtime starts
- you are building high-risk or security-sensitive applications

You do NOT need this integration if:

- you only require integrity signals during normal runtime
- you rely exclusively on `Integrity.check()` from JavaScript
- early detection timing is not critical for your use case

### Android Integration

In your `MainActivity.kt`, call `IntegrityImpl.onApplicationCreate(context)` inside the `onCreate` method:

```diff

package io.ionic.starter // Use your actual package name

import android.os.Bundle
import com.getcapacitor.BridgeActivity
+ import io.capkit.integrity.IntegrityImpl

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
+         // Capture security signals during early boot
+         IntegrityImpl.onApplicationCreate(this)

        super.onCreate(savedInstanceState)
    }
}

```

### iOS Integration

In your `AppDelegate.swift`, call `IntegrityImpl.onAppLaunch()` inside the `application(_:didFinishLaunchingWithOptions:)` method:

```diff

import UIKit
import Capacitor
+ import Integrity // Import the plugin module

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

+        // Capture security signals at the earliest stage possible
+        IntegrityImpl.onAppLaunch()

        return true
    }
}

```

> **Important**
>
> If native early boot integration is not performed:
>
> - the plugin remains fully functional
> - no integrity capability is lost
> - detection simply starts when the JavaScript layer invokes `Integrity.check()`
>
> Native integration improves **timing**, not **coverage**.

---

### Early boot signals (IMPORTANT)

When integrated at the native application entry points
(`MainActivity.onCreate` on Android, `AppDelegate.didFinishLaunchingWithOptions`
on iOS), the Integrity plugin may capture **early boot integrity signals**
before the Capacitor bridge is fully initialized.

IMPORTANT NOTES:

- Early boot signals are **best-effort**.
- They are **not guaranteed** to be captured on every app launch.
- They may be affected by:
  - process restarts
  - OS-level lifecycle optimizations
  - multi-process behavior (Android)
  - warm launches vs cold starts

Behavioral guarantees:

- Early boot signals are **merged into the first `Integrity.check()` report**
  when available.
- If early boot integration is not performed, the plugin continues
  to function correctly without them.
- Applications MUST NOT rely exclusively on early boot signals
  for security decisions.

> Early boot detection improves visibility, not certainty.

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
| **`verboseLogging`** | <code>boolean</code>                  | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API or runtime behavior.                                                                                                                                                                                                                                             | <code>false</code> | 8.0.0 |
| **`blockPage`**      | <code>IntegrityBlockPageConfig</code> | Optional configuration for the integrity block page. This configuration controls the availability and source of a developer-provided HTML page that may be presented to the end user when the host application decides to do so. This configuration is: - read only by native code - immutable at runtime - NOT accessible from JavaScript The Integrity plugin will NEVER automatically present the block page. Presentation is always explicitly triggered by the host application via the public API. |                    | 8.0.0 |

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
  - Instrumentation / hooking heuristics (Frida, Substrate)
  - **Memory map & Runtime image inspection**
  - **Heuristic signal correlation**

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

## Permissions

On Android, this plugin requires the `android.permission.INTERNET` permission. This permission is necessary for internal integrity checks, specifically for attempting socket connections to `localhost` to detect hooking frameworks like Frida.

This permission is automatically added to your AndroidManifest.xml when you install the plugin. However, it's important to be aware of its presence.

No additional permissions are required for iOS.

---

## API

The public API is fully typed and documented via TypeScript definitions.

See `definitions.ts` for the complete contract.

<docgen-index>

- [`check(...)`](#check)
- [`presentBlockPage(...)`](#presentblockpage)
- [`getPluginVersion()`](#getpluginversion)
- [`addListener('integritySignal', ...)`](#addlistenerintegritysignal-)
- [`removeAllListeners()`](#removealllisteners)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)
- [Enums](#enums)

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

**Since:** 8.0.0

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

**Since:** 8.0.0

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

**Since:** 8.0.0

#### Example

```ts
const { version } = await Integrity.getPluginVersion();
```

---

### addListener('integritySignal', ...)

```typescript
addListener(eventName: 'integritySignal', listenerFunc: (signal: IntegritySignalEvent) => void) => Promise<PluginListenerHandle>
```

Registers a listener for real-time integrity signals.

The provided callback is invoked every time a new integrity
signal is detected by the native layer.

BEHAVIOR:

- Signals may be emitted at any time after plugin initialization.
- Signals detected before listener registration MAY be delivered
  immediately after registration.
- No guarantees are made about signal frequency or ordering
  across platforms.

IMPORTANT:

- This listener is non-blocking.
- The plugin does NOT enforce any policy based on signals.

| Param              | Type                                                                             | Description                                  |
| ------------------ | -------------------------------------------------------------------------------- | -------------------------------------------- |
| **`eventName`**    | <code>'integritySignal'</code>                                                   | The event to listen for ('integritySignal'). |
| **`listenerFunc`** | <code>(signal: <a href="#integritysignal">IntegritySignal</a>) =&gt; void</code> | Callback invoked with the detected signal.   |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

---

### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Removes all registered listeners for this plugin.

NOTE:

- Removing listeners does NOT stop signal detection natively.
- Signals may continue to be detected and buffered
  until a listener is registered again.

**Since:** 8.0.0

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
Multiple signals MAY be combined by the host application
to derive a security policy.

Signals:

- are emitted asynchronously
- may occur at any time during the app lifecycle
- may be emitted before or after the first call to `check()`

| Prop              | Type                                                                          | Description                                                                                                                                                                                                                                                                                                                                                                                                                     |
| ----------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`id`**          | <code>string</code>                                                           | Stable identifier for the signal. This value: - is stable across releases - MUST NOT be parsed or pattern-matched - is intended for analytics, logging, and policy evaluation                                                                                                                                                                                                                                                   |
| **`category`**    | <code><a href="#integritysignalcategory">IntegritySignalCategory</a></code>   | High-level category of the signal. Categories allow grouping related signals without relying on specific identifiers.                                                                                                                                                                                                                                                                                                           |
| **`confidence`**  | <code><a href="#integrityconfidencelevel">IntegrityConfidenceLevel</a></code> | Confidence level of the detection. This value expresses how strongly the signal correlates with a potentially compromised or risky environment. NOTE: Although typed as a string union in the public API, native implementations MUST only emit values defined by the internal <a href="#integrityconfidencelevel">IntegrityConfidenceLevel</a> enum.                                                                           |
| **`description`** | <code>string</code>                                                           | Optional human-readable description. This field: - is intended for diagnostics and debugging only - MAY be omitted or redacted in production builds - MUST NOT be relied upon programmatically                                                                                                                                                                                                                                  |
| **`metadata`**    | <code>Record&lt;string, string \| number \| boolean&gt;</code>                | Additional diagnostic metadata associated with the signal. Metadata provides granular details about the detection (e.g. matched filesystem paths, runtime artifacts, or environment properties) without altering the stable signal identifier. IMPORTANT: - Metadata is informational only. - Keys and values are NOT guaranteed to be stable. - Applications MUST NOT rely on specific metadata fields for security decisions. |

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
| **`reason`**      | <code>string</code>  | Optional reason code passed to the block page. This value may be used for analytics, localization, or user messaging.                      |                    | 8.0.0 |
| **`dismissible`** | <code>boolean</code> | Whether the block page can be dismissed by the user. Defaults to false. In production environments, this should typically remain disabled. | <code>false</code> | 8.0.0 |

#### PluginVersionResult

Result returned by the getPluginVersion method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

### Type Aliases

#### IntegritySignalCategory

Category of a detected integrity signal.

Categories are intentionally broad and stable.
New detection techniques MUST reuse existing categories
whenever possible to avoid breaking consumers.

<code>'root' | 'jailbreak' | 'emulator' | 'debug' | 'hook' | 'tamper' | 'environment'</code>

#### IntegritySignalEvent

Event payload emitted when a new integrity signal is detected.

This event represents a _real-time observation_ of a potential
integrity-relevant condition on the device.

IMPORTANT:

- Signals are observational only.
- Emitting a signal does NOT imply that the environment is compromised.
- No blocking or enforcement is performed by the plugin.

The host application is responsible for:

- interpreting signals
- correlating multiple signals
- applying any security or UX policy

<code>
  <a href="#integritysignal">IntegritySignal</a>
</code>

### Enums

#### IntegrityConfidenceLevel

| Members      | Value                 |
| ------------ | --------------------- |
| **`LOW`**    | <code>'low'</code>    |
| **`MEDIUM`** | <code>'medium'</code> |
| **`HIGH`**   | <code>'high'</code>   |

</docgen-api>

---

## Integrity events semantics (IMPORTANT)

The `integritySignal` event documented above represents a
**real-time observational snapshot**, not an incremental update.

Important clarifications:

- Integrity events **may include signals already returned**
  by a previous `Integrity.check()` call.
- Events are **NOT incremental** and **NOT deltas**.
- Each emitted event is a **standalone integrity observation**
  produced at a specific moment in time.

Implications for applications:

- Consumers MUST be prepared to receive duplicate signals.
- Applications SHOULD implement their own de-duplication or
  correlation logic if required.
- Events MUST NOT be interpreted as a continuous stream of
  unique integrity changes.

> Events report observations, not transitions.

---

## Limitations

- Root / jailbreak detection can be bypassed
- Emulator detection is heuristic
- Frida detection uses memory and runtime inspection
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

### Hooking detection signal behavior (iOS)

On iOS, runtime hooking detection is intentionally designed to emit
**at most one hooking-related signal per check execution**.

Design characteristics:

- The detector stops at the **first confirmed hooking artifact**.
- Only a single signal is emitted, even if multiple suspicious
  libraries or runtime indicators are present.
- This behavior is **intentional** and optimized for:
  - low noise
  - predictable signal volume
  - reduced false-positive amplification

Implications:

- The absence of multiple hooking signals does **not** imply
  that only one artifact was present.
- Diagnostic depth is intentionally limited in favor of
  stability and signal clarity.

> This is a design choice, not a detection limitation.

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

## Score interpretation (IMPORTANT)

The integrity `score` returned by `Integrity.check()` is a **heuristic indicator**, not a guarantee.

Key points:

- The score is derived from detected integrity signals and their confidence levels.
- A score **greater than or equal to 30** currently marks the environment as `compromised`.
- This threshold is **internal, heuristic-based, and subject to change**.
- The score **must NOT** be treated as:
  - a security guarantee
  - a cryptographic proof
  - a definitive compromise verdict

Applications MUST:

- interpret the score in context
- combine it with individual signals
- apply their own business or security logic

> ⚠️ Do not rely on the score alone to make irreversible security decisions.

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

> The roadmap below is indicative and non-binding.
> Items may be implemented across multiple `next.x` iterations.

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
