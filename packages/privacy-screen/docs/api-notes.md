# PrivacyScreen — API Notes

## Return shapes

The contract mirrors the official `@capacitor/privacy-screen` exactly:

- `enable()` / `disable()` resolve `{ success: true }` when the native
  operation completes.
- `isEnabled()` resolves `{ enabled: boolean }`.

## Method behavior notes

- `enable()` is idempotent: calling it twice keeps protection on and
  applies the latest display options.
- `disable()` is idempotent: calling it while already disabled still
  resolves `{ success: true }`.
- Display options are validated leniently: unknown `blurEffect` or
  `privacyModeOnActivityHidden` values fall back to defaults instead of
  rejecting the call.
- `removeAllListeners()` clears every listener registered on this plugin,
  including `screenshotTaken`.

## Platform availability

- Android and iOS only. On web, methods resolve normally but protection
  is state tracking without OS enforcement.
- `screenshotTaken`:
  - iOS: always delivered while the app runs, via the system screenshot
    notification — independent of the enabled state.
  - Android: delivered only on API 34+, only while protection is active,
    re-registered on every resume so activity recreation never leaves a
    stale handle. Never fires below API 34.

## Errors

The three control methods do not reject under normal use. Unexpected
native failures surface as `INIT_FAILED` through the standard bridge
rejection path. Input parsing never throws for malformed display
options — they degrade to defaults per the lenient-validation rule.

## Versioning

`getPluginVersion()` returns the native implementation version
synchronized from `package.json` at build time. Use it in diagnostics
to prove which native binary a report came from.
