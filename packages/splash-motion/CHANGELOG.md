# @cap-kit/splash-motion

## 8.0.0

### Minor Changes

- 6238002: First stable release of the `@cap-kit/splash-motion` plugin.

  - **Android launch parity fix**: the launch splash uses the Android 12
    SplashScreen compat API (`androidx.core:core-splashscreen`), holding the
    system-rendered launch theme splash on screen instead of drawing a second
    dialog layer on top of it (identical to the official plugin). Dialog-based
    rendering remains as fallback and for `show()`/`hide()` calls.
  - `show()` / `hide()` API-compatible with the official `@capacitor/splash-screen`
    (`ShowOptions` / `HideOptions` shapes preserved)
  - Full static config parity with the official plugin (launch keys, background
    color, Android image resource and scale type, full-screen/immersive flags)
  - Lottie animation support (`LottieOptions`: `source` local asset or HTTPS URL,
    `loop`, `speed`) via `lottie-android` / `lottie-ios`
  - dotLottie archive support: `.lottie` sources (local or remote) are read as
    zip containers and the first animation JSON is played
  - Dark mode variants: `backgroundColorDark` static config key and `darkSource`
    Lottie option, resolved against the system theme at render time
  - Haptic feedback (`HapticOptions`: `light` / `medium` / `heavy`) via Android
    `Vibrator` and iOS `UIImpactFeedbackGenerator`
  - Splash surface composition (Android + iOS, full parity):

    - Static splash image renders on both platforms, with
      `imageResourceName` / `imageResourceNameDark` dark-variant selection and
      optional `imageGravity` placement; the official `androidSplashResourceName`
      key stays honored as fallback
    - Background image layer (`backgroundImage: ImageOptions`) rendered behind
      the splash content
    - Branding image (`brandingImage: ImageOptions`), anchored to an edge
      (default `bottomEnd`, 16dp/pt margin) — Android 12 style co-branding
    - `SplashGravity` (9 positions) + `SplashScaleType` (official
      `ImageView.ScaleType` names) on every image layer; gravity positions the
      view, scale type scales the bitmap inside it
    - Content entrance animation (`entrance`: `fade` / `scale`, duration clamped
      0–2000ms) applied to all content layers together
    - Vertical gradient background (`backgroundGradient`: `top`/`bottom` with
      dark variants), overriding the flat background color when configured
    - All of the above accept per-call overrides in `ShowOptions`
      (`image`, `backgroundImage`, `brandingImage`, `entrance`,
      `backgroundGradient`)

  - Loading spinner (Android + iOS, full parity): `showSpinner`,
    `androidSpinnerStyle` (official `ProgressBar` style mapping, default
    `large`), `iosSpinnerStyle` (`small` → `.medium`, `large` → `.large`,
    anything else → platform default) and `spinnerColor` hex tint; rendered
    centered as the topmost content layer with an optional per-call
    `ShowOptions.spinner` override. `spinnerTimeout` is intentionally not
    implemented (documented by the official plugin but absent from its 8.x
    source)
  - Per-call option parsing fix: `ShowOptions` extension keys
    (`image`, `backgroundImage`, `brandingImage`, `entrance`,
    `backgroundGradient`, `spinner`) were parsed on iOS only; the Android
    bridge now parses all of them too
  - Launch splash driven by static config (`launchShowDuration`, `launchAutoHide`,
    `launchFadeOutDuration`, `launchLottie`, `launchHaptic`)
  - `getPluginVersion()` method: native version string on Android and iOS,
    JavaScript package version on web (new `PluginVersionResult` interface)
  - Cap-Kit architectural standards: kotlinx.serialization static config,
    bridge/impl separation, canonical cross-platform error messages
  - **Launch duration clamp (all platforms)**: when `launchAutoHide` is off,
    the effective launch minimum hold is now clamped to
    `max(launchShowDuration, lottie duration)` — the splash is never released
    mid-animation, even when the configured `launchShowDuration` is shorter
    than the loaded launch animation. The lottie duration never shortens a
    configured hold, and auto-hide schedules are untouched.
    - Web: the player's `load` event reports the animation duration; `hide()`
      honors the larger of the configured minimum and the loaded duration.
    - Android: the launch composition's duration is captured when it loads and
      clamps `launchMinimumHoldRemainingMs` (pure helper
      `SplashMotionValidators.effectiveLaunchMinimumHoldMs`, unit-tested).
    - iOS: the launch animation's duration is captured in `configureAndPlay`
      and clamps `launchMinimumHoldRemaining`.
  - **Web animated splash**: the web implementation is no longer a no-op stub.
    `show()` renders a full-viewport DOM overlay with the resolved background
    color (light/dark via `prefers-color-scheme`) and, when a Lottie source is
    configured, plays it with `@lottiefiles/dotlottie-web` (WASM player,
    lazy-loaded via dynamic import; supports both `.json` and `.lottie`
    sources).
    - `SplashMotionWeb.setConfig()` supplies the web launch defaults, mirroring
      the native static config keys (`launchLottie`, `backgroundColor`,
      `backgroundColorDark`, `launchAutoHide`, `launchShowDuration`) plus the
      web-only `wasmUrl`.
    - `wasmUrl` points at a locally served `dotlottie-player.wasm`; without it
      the player falls back to the library's public CDN (avoid in offline
      deployments).
    - Launch semantics match native: `launchAutoHide: true` hides after
      `launchShowDuration`; `launchAutoHide: false` keeps the splash until
      `hide()` is called, honoring `launchShowDuration` as a minimum hold.
    - Fade in/out (`fadeInDuration` / `fadeOutDuration`, `ShowOptions` /
      `HideOptions`) implemented with CSS opacity transitions.
    - Still native-only on web: static images, background image/gradient,
      entrance animation, spinner and haptics are ignored. The overlay also
      cannot cover the pre-bundle launch gap (before app JS executes) — that
      requires a static splash in the host app's `index.html`.
    - `@lottiefiles/dotlottie-web` added as a runtime dependency and kept
      external in every rollup output (lazy dynamic import).
  - Web-layer test suite (`tests/web.test.ts`, 15 Vitest tests, jsdom + fake
    timers) covering light/dark resolution, player wiring, `setWasmUrl`
    ordering, min-hold/auto-hide timing, fade durations, replacement and
    player-failure resilience, and `getPluginVersion()`.
  - Documentation corrections: docs no longer describe the web implementation
    as a no-op stub — they document the real DOM overlay surface, the
    `SplashMotionWeb.setConfig()` web launch defaults and `getPluginVersion()`.
  - Package metadata fixes: corrected the npm description and `removePacked`
    glob (both carried over from sibling plugin templates); added
    `splash-screen` and `lottie` keywords.
