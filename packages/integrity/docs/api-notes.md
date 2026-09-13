# Integrity — API Notes

This guide documents behavioral notes for the Integrity API: the semantics of the `integritySignal` event and the iOS jailbreak + hooking cross-correlation signal.

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

## Cross-correlation: Jailbreak + Hooking (iOS)

The Integrity plugin may emit a derived signal when it detects
both jailbreak indicators and runtime hooking signals during
the same execution window.

This signal represents a **cross-category correlation**, indicating
that the device is not only modified but also actively instrumented.

### Signal details

- **id**: `ios_jailbreak_and_hook_detected`
- **category**: `tamper`
- **confidence**: `high`

### Important notes

- This signal is observational only.
- It does not replace or suppress individual signals.
- It must not be treated as an automatic enforcement trigger.

## API contract

The public API is fully typed and documented via TypeScript definitions.
See `definitions.ts` for the complete contract.
