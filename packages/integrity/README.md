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

## Documentation

- [Usage guide](docs/guide.md) — integrity checks, block page presentation, error handling, and Web platform notes
- [Configuration guide](docs/configuration.md) — native early boot integration, jailbreak URL scheme probing, and permissions
- [Security considerations](docs/security.md)
- [Design & philosophy](docs/design.md)
- [API notes](docs/api-notes.md)
- [Versioning & release policy](docs/versioning.md)
- [Contributing](CONTRIBUTING.md)

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

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the Integrity plugin.

| Prop                      | Type                                   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Default            | Since |
| ------------------------- | -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`**      | <code>boolean</code>                   | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API or runtime behavior.                                                                                                                                                                                                                                             | <code>false</code> | 8.0.0 |
| **`blockPage`**           | <code>IntegrityBlockPageConfig</code>  | Optional configuration for the integrity block page. This configuration controls the availability and source of a developer-provided HTML page that may be presented to the end user when the host application decides to do so. This configuration is: - read only by native code - immutable at runtime - NOT accessible from JavaScript The Integrity plugin will NEVER automatically present the block page. Presentation is always explicitly triggered by the host application via the public API. |                    | 8.0.0 |
| **`jailbreakUrlSchemes`** | <code>JailbreakUrlSchemesConfig</code> | Optional configuration for jailbreak URL scheme probing (iOS only). When enabled, the native iOS implementation may probe for known jailbreak-related applications using URL schemes such as `cydia://`. This configuration: - is read natively at runtime - is immutable - is NOT accessible from JavaScript - does NOT alter the public JavaScript API                                                                                                                                                 |                    | 8.0.0 |

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

## API

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
check(options?: IntegrityCheckOptions) => Promise<IntegrityReport>
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
presentBlockPage(options?: PresentBlockPageOptions) => Promise<PresentBlockPageResult>
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
addListener(eventName: "integritySignal", listenerFunc: (signal: IntegritySignalEvent) => void) => Promise<PluginListenerHandle>
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

| Prop                   | Type                                                                            | Description                                                                                                                                                                                                       | Since |
| ---------------------- | ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`compromised`**      | <code>boolean</code>                                                            | Indicates whether the environment is considered compromised according to the current scoring model.                                                                                                               |       |
| **`score`**            | <code>number</code>                                                             | Provisional integrity score. The score ranges from 0 to 100 and is derived from the detected signals.                                                                                                             |       |
| **`signals`**          | <code>IntegritySignal[]</code>                                                  | List of detected integrity signals.                                                                                                                                                                               |       |
| **`environment`**      | <code><a href="#integrityenvironment">IntegrityEnvironment</a></code>           | Execution environment summary.                                                                                                                                                                                    |       |
| **`timestamp`**        | <code>number</code>                                                             | Unix timestamp (milliseconds) when the check was performed.                                                                                                                                                       |       |
| **`scoreExplanation`** | <code><a href="#integrityscoreexplanation">IntegrityScoreExplanation</a></code> | Optional explanation metadata describing how the integrity score was derived from the detected signals. This field is informational only and MUST NOT be treated as a security decision or enforcement mechanism. | 8.0.0 |

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

#### IntegrityScoreExplanation

Describes how the integrity score was derived.

This structure provides transparency and auditability
without exposing internal scoring algorithms.

| Prop               | Type                                                        | Description                                               |
| ------------------ | ----------------------------------------------------------- | --------------------------------------------------------- |
| **`totalSignals`** | <code>number</code>                                         | Total number of detected signals.                         |
| **`byConfidence`** | <code>{ high: number; medium: number; low: number; }</code> | Breakdown of signals by confidence level.                 |
| **`contributors`** | <code>string[]</code>                                       | List of signal identifiers that contributed to the score. |

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

| Prop              | Type                                       | Description                                                                                                                                | Default            | Since |
| ----------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------ | ----- |
| **`reason`**      | <code>string</code>                        | Optional reason code passed to the block page. This value may be used for analytics, localization, or user messaging.                      |                    | 8.0.0 |
| **`dismissible`** | <code>boolean</code>                       | Whether the block page can be dismissed by the user. Defaults to false. In production environments, this should typically remain disabled. | <code>false</code> | 8.0.0 |
| **`customUrl`**   | <code>string</code>                        | Optional override for the block page URL. Takes precedence over the static configuration. Maximum length: 2048 characters.                 |                    | 8.0.5 |
| **`context`**     | <code>Record&lt;string, unknown&gt;</code> | Optional context data to pass to the block page. This value is encoded as a JSON string and appended to the URL as a query parameter.      |                    | 8.0.5 |

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
