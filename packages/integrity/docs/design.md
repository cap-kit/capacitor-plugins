# Integrity — Design & Philosophy

This guide explains the design rationale behind the Integrity plugin: why it observes instead of enforces, why it exposes a single entry point, how strictness levels work, and how the API is meant to evolve.

## Observation, not enforcement

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

## Why there are no single-purpose checks

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

## Integrity levels

Instead of selecting individual checks, applications choose a **strictness level**:

- **basic** – lightweight and safe checks
- **standard** – includes debug and hooking detection
- **strict** – adds tampering and signature integrity checks

The selected level controls **how deeply the environment is inspected**, not which single signal is exposed.

## Future extensibility

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

## Summary

- The Integrity plugin **detects signals**, it does not block or enforce
- Security decisions are **always owned by the application**
- The API is designed for **long-term stability and flexibility**
- Future enhancements will extend capabilities without breaking this model

## Roadmap (Non-binding)

> The roadmap below is indicative and non-binding.
> Items may be implemented across multiple `next.x` iterations.

- Optional platform attestation helpers
- Extended tamper heuristics
- Improved scoring models
- Documentation examples
