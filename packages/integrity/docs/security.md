# Integrity — Security Considerations

This guide documents the Integrity plugin's security model, score interpretation, limitations, and the behavior of specific detections.

## Security Model (IMPORTANT)

This plugin follows an **observational security model**.

It reports **signals**, not decisions.

- Signals are heuristic observations
- Signals may produce false positives or false negatives
- The returned score is **provisional** and **non-normative**
- Consumers MUST combine signals with business logic

> ⚠️ This plugin is **NOT** a replacement for a full RASP or DRM solution.

### Internal performance optimizations

To reduce repeated execution of expensive integrity checks,
the plugin applies a short-lived **negative cache** internally.

Key characteristics:

- Applies only to `standard` and `strict` levels
- Caches only **clean executions** (no detected signals)
- Time-to-live: **~30 seconds**
- Automatically invalidated as soon as any signal is detected
- Completely transparent to the JavaScript API

This optimization improves performance and battery usage
without affecting detection semantics or signal correctness.

> The cache never suppresses or hides detected integrity signals.

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

## Limitations

- **Heuristic Bypass**: Root / jailbreak detection can be bypassed by advanced cloaking tools.
- **Package Scanning**: Root detection on Android includes scanning for known management apps (e.g., Magisk). This is subject to OS-level package visibility restrictions.
- **Emulator Detection**: Detection is heuristic and relies on build properties that may vary between providers.
- Frida detection uses memory and runtime inspection
- **No cryptographic or remote attestation is performed**
- **Apple App Attest and Google Play Integrity are NOT implemented**
- Attestation-related signals, when present, explicitly report unavailability
- No device identity is established

These limitations are **intentional** to:

- avoid store policy violations
- reduce false positives
- keep the plugin portable and maintainable

### Debug environment detection

The plugin provides parity between platforms for debug detection. A `debug` integrity signal is reported when:

- **Debugger Attached**: A debugger is currently attached to the running process (detected via `Debug.isDebuggerConnected()` on Android or `sysctl` on iOS).
- **Debuggable Environment**: The application is running in a debuggable state or signed with a development profile (detected via `FLAG_DEBUGGABLE` on Android or `get-task-allow` entitlement on iOS).

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
