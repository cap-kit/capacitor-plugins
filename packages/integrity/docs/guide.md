# Integrity — Usage Guide

This guide covers how to use the Integrity plugin: running integrity checks, choosing strictness levels, presenting the block page, handling errors, and platform-specific behavior on Web.

## Basic integrity check

```ts
import { Integrity } from '@cap-kit/integrity';

const report = await Integrity.check();

if (report.compromised) {
  // Decide what to do
}
```

## Integrity check options

`Integrity.check()` accepts optional parameters that control
the strictness and verbosity of the integrity checks.

```ts
const report = await Integrity.check({
  level: 'standard',
  includeDebugInfo: true,
});
```

### Options

| Option             | Type                                | Default   | Description                                          |
| ------------------ | ----------------------------------- | --------- | ---------------------------------------------------- |
| `level`            | `'basic' \| 'standard' \| 'strict'` | `'basic'` | Controls which categories of checks are executed     |
| `includeDebugInfo` | `boolean`                           | `false`   | Includes diagnostic descriptions in returned signals |

### Levels behavior

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
  - Explicit reporting of unavailable platform attestation (observational only)

> ⚠️ The returned integrity score remains provisional and
> must not be used as the sole security decision signal.

## Presenting a block / warning page

The plugin will **never** present UI automatically.

UI presentation is always explicitly triggered by the host application.

```ts
await Integrity.presentBlockPage({
  reason: 'integrity_failed',
});
```

### Dismissible block page (optional)

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

## Score Explanation Metadata

The Integrity plugin may include an optional `scoreExplanation`
field in the integrity report.

This field provides transparency about how the integrity score
was derived from detected signals.

### Important notes

- This metadata is informational only.
- It does NOT alter the integrity score.
- It MUST NOT be treated as a security decision.
- Individual signals remain the authoritative source of truth.

## Web Platform Notes

The Web platform is supported to preserve API parity.

However, native integrity checks are **not available** in browser environments.

The following methods will reject with `IntegrityErrorCode.UNAVAILABLE` on Web:

- `Integrity.check()`
- `Integrity.presentBlockPage()`

`Integrity.getPluginVersion()` is always available.
