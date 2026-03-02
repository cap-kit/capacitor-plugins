---
"@cap-kit/integrity": minor
---

refactor(integrity): Normalize native naming, centralize error handling, and align configuration structures.

- Renamed native implementation classes to `Integrity` for cross-platform symmetry.
- Normalized support file naming (removed `Integrity` prefix from internal helpers).
- Unified error architecture between iOS and Android with `NativeError` mapping.
- Added `TIMEOUT`, `NETWORK_ERROR`, and `INTERNAL_ERROR` to `IntegrityErrorCode`.
- Aligned `blockPage` configuration to a nested structure `{ enabled, url }` across all platforms.
- Removed legacy and undocumented configuration keys in Android.
- Added `customUrl` and `context` options to `presentBlockPage()` for dynamic block page content.
- Added `INVALID_INPUT` error code for URL validation (max 2048 chars).
- Added Android Window.Callback overlay detection in BlockActivity.
- Added tap-jacking prevention via `setFilterTouchesWhenObscured` (API < 31) and `setHideOverlayWindows` (API >= 31).
- Added `preventTapJacking` configuration option for block page.
- Added `warn()` method to Android Logger.
- Replaced direct `android.util.Log` calls with centralized `Logger` usage.
