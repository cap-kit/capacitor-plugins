# SplashMotion — API Notes

## Idempotency

- `show()` while a splash is already visible does not stack surfaces: the
  existing surface is kept, the auto-hide schedule is refreshed with the new
  options and haptic feedback is re-fired.
- `hide()` when nothing is visible is a safe no-op.
- The auto-hide timer is cancelled before every manual hide, so calling
  `hide()` after `autoHide` already fired never dismisses twice.

## Option validation

Validation is lenient by design — invalid values fall back to defaults
instead of rejecting the call:

- Durations are clamped to the 0–60000 ms range.
- Unknown `haptic.style` values fall back to `medium`.
- Lottie `speed` is clamped to the 0.1–10 range.
- A `lottie` object without a `source` is ignored.
- Invalid `backgroundColor` / `backgroundColorDark` hex strings fall back to
  the platform default.
- Image layers are lenient: unknown `gravity` values fall back to `center`,
  unknown `scaleType` values to `CENTER_CROP`, `alpha` is clamped 0–1,
  `margin` is clamped 0–200, and an image whose source cannot be resolved is
  silently skipped.
- Unknown `entrance.type` values fall back to `none`; `durationMs` is
  clamped 0–2000 (default 300).
- Spinner options are lenient: unknown `androidSpinnerStyle` values fall
  back to `large`, iOS styles other than `small`/`large` fall back to the
  platform default, and an invalid `spinnerColor` hex keeps the platform
  default spinner color. Passing a per-call `spinner` object implies
  `show: true` unless explicitly disabled.
- `backgroundGradient` requires valid `top` and `bottom` colors; an invalid
  dark pair falls back to the light pair, and an invalid light pair makes the
  gradient ignored (flat background applies).
- A `.lottie` archive without any animation JSON logs an error and renders
  the splash without an animation.
- If `darkSource` is unset, `source` is used for both light and dark modes;
  dark mode only changes the background color when `backgroundColorDark` is
  configured.

## Web availability

The web implementation renders a DOM overlay (background color and optional
Lottie), so `show()` / `hide()` are not no-ops there. Call sites still do not
need platform guards: the options that web ignores (static images, gradient,
entrance animation, spinner, haptics) are simply ignored, never rejected.

## Launch behavior

The launch splash is driven exclusively by static configuration. There is no
JS-side toggle: if you do not want a launch splash, do not set
`launchLottie` and keep the defaults (the plugin still shows the static
splash image with the default 500 ms duration and 200 ms fade-out on native;
set `launchAutoHide: false` and call `hide()` yourself for full control).

## Relation to the official plugin

The method surface mirrors the official `@capacitor/splash-screen` plugin
(`show` / `hide` with the same option names). SplashMotion does not manage
the Android 12 system splash screen — that stays with the system/official
plugin. SplashMotion is a rendering surface for custom splash content
(static image, Lottie) with haptic feedback.
