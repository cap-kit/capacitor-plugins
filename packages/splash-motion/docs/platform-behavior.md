# SplashMotion — Platform Behavior

## Android

The splash surface is a full-screen `Dialog` owned by the host activity:

- **Background**: `backgroundColor` from static config, or white when unset.
  `backgroundColorDark` overrides it when the system is in dark mode. When
  `backgroundGradient` is configured with a valid color pair, a vertical
  `GradientDrawable` (top→bottom) replaces the flat color; in dark mode a
  valid dark pair wins, otherwise the light pair applies.
- **Content container**: all content layers (background image, lottie or
  static image, branding image, spinner) render inside a single transparent
  `FrameLayout` stacked above the background, so the entrance animation
  covers them together.
- **Background image**: when `backgroundImage` is configured, its drawable is
  rendered first, filling the whole surface.
- **Static image**: when no `lottie` option is present, the resource named by
  `imageResourceName` (fallback chain: `androidSplashResourceName`, `splash`)
  is rendered, using `imageResourceNameDark` in dark mode when provided. With
  no `imageGravity` the view fills the surface and `androidScaleType` scales
  the bitmap (official behavior, default `FIT_XY`); with a gravity the view
  wraps its bitmap and is positioned by `imageGravity` (default `CENTER_CROP`).
- **Branding image**: rendered last before the spinner, anchored by its
  `gravity` (default `bottomEnd`) with its `margin` (default 16dp).
- **Spinner**: when enabled (`showSpinner`, or the per-call `spinner`
  option), an indeterminate `ProgressBar` renders centered as the topmost
  content layer, styled by `androidSpinnerStyle` (official mapping, default
  `large`) and tinted by `spinnerColor` when a valid hex is given. It appears
  and disappears with the surface.
- **Lottie**: when a `lottie` option is present, a centered
  `LottieAnimationView` renders the animation. Local sources resolve from the
  Capacitor web asset directory (`assets/public/<source>`) and accept both
  `.json` compositions and `.lottie` dotLottie archives (the first animation
  JSON inside the zip is extracted and parsed); remote sources are downloaded
  (`.lottie` archives are fetched on a background thread). In dark mode,
  `darkSource` replaces `source` when provided.
- **Entrance animation**: `entrance.type` `fade` (alpha 0→1) or `scale`
  (alpha 0→1 + scale 0.85→1.0) applies to the whole content container over
  `entrance.durationMs` (clamped 0–2000, default 300) after the fade-in.
- **Fades**: the root view animates alpha 0→1 over `fadeInDuration` and 1→0
  over `fadeOutDuration` before dismissal.
- **Auto-hide**: a `Handler` callback hides the splash after `showDuration`.
  A `show()` while visible only refreshes the schedule and re-fires haptics.
- **Immersive/full-screen**: `splashFullScreen` adds `FLAG_FULLSCREEN`;
  `splashImmersive` additionally hides navigation with immersive sticky flags.
- **Haptics**: `Vibrator` one-shot vibration (20/40/80 ms for
  light/medium/heavy, amplitude 64/128/255). Requires `VIBRATE`, declared in
  the plugin manifest. No-ops silently on devices without a vibrator.

## iOS

The splash surface is a dedicated `UIWindow` at `windowLevel = .alert + 1`:

- **Background**: `backgroundColor` from static config, or
  `UIColor.systemBackground` when unset. `backgroundColorDark` overrides it
  when the system is in dark mode. When `backgroundGradient` is configured
  with a valid color pair, a full-screen `CAGradientLayer` (top→bottom)
  replaces the flat color; in dark mode a valid dark pair wins, otherwise the
  light pair applies.
- **Content container**: all content layers (background image, lottie or
  static image, branding image, spinner) render inside a single transparent
  container pinned edge-to-edge above the background, so the entrance
  animation covers them together.
- **Background image**: when `backgroundImage` is configured, its image is
  rendered first, filling the whole surface.
- **Static image**: when no `lottie` option is present, the resource named by
  `imageResourceName` (fallback chain: `androidSplashResourceName`, `splash`)
  is resolved from the asset catalog or the app bundle (a value containing
  `/` resolves from the `public` web asset folder), using
  `imageResourceNameDark` in dark mode when provided. With no `imageGravity`
  the view fills the surface and `androidScaleType` scales the bitmap
  (official behavior, default `FIT_XY`); with a gravity the view keeps its
  intrinsic size and is positioned by Auto Layout `imageGravity` constraints
  (default `CENTER_CROP`).
- **Branding image**: rendered last before the spinner, anchored by its
  `gravity` (default `bottomEnd`) with its `margin` (default 16pt).
- **Spinner**: when enabled (`showSpinner`, or the per-call `spinner`
  option), a `UIActivityIndicatorView` renders centered as the topmost
  content layer, styled by `iosSpinnerStyle` (`small` → `.medium`,
  `large` → `.large`, anything else → platform default) and tinted by
  `spinnerColor` when a valid hex is given. It appears and disappears with
  the surface.
- **Lottie**: a centered `LottieAnimationView` (300×300) renders the
  animation. Bundle sources resolve inside the `public` folder reference of
  the app bundle and accept both `.json` compositions and `.lottie` dotLottie
  archives (the zip central directory is parsed natively and the selected
  animation JSON inflated); remote URLs load asynchronously. In dark mode,
  `darkSource` replaces `source` when provided.
- **Entrance animation**: `entrance.type` `fade` (alpha 0→1) or `scale`
  (alpha 0→1 + scale 0.85→1.0) applies to the whole content container over
  `entrance.durationMs` (clamped 0–2000, default 300) after the fade-in.
- **Fades**: `UIView.animate` transitions the root view alpha.
- **Auto-hide**: a `DispatchWorkItem` hides the splash after `showDuration`.
  A `show()` while visible only refreshes the schedule and re-fires haptics.
- **Haptics**: `UIImpactFeedbackGenerator` with `.light`/`.medium`/`.heavy`
  impact styles. Requires a device with haptic hardware; no-ops in Simulator.

## Web

`show()` renders a full-viewport DOM overlay with the resolved background
color (light/dark via `prefers-color-scheme`) and, when a Lottie source is
configured, plays it with `@lottiefiles/dotlottie-web` (WASM player, lazily
imported). Fade in/out and the auto-hide schedule match the native
semantics.

The web launch defaults are not read from the static plugin config (web
cannot access it): they are supplied with `SplashMotionWeb.setConfig()`,
which also carries the web-only `wasmUrl` for a locally served
`dotlottie-player.wasm`.

Native-only on web: static images, background image and gradient, entrance
animation, spinner and haptics are ignored. The overlay cannot cover the
pre-bundle launch gap (before the app JavaScript executes) — that requires a
static splash in the host app `index.html`.

`getPluginVersion()` resolves with the JavaScript package version instead of a
native version string.

## Launch flow

Both native platforms fire the launch splash during the plugin's native load
phase, before the WebView is visible, using the static config keys
(`launchShowDuration`, `launchAutoHide`, `launchFadeOutDuration`,
`launchLottie`, `launchHaptic`). Per-call options passed to `show()`/`hide()`
always override the static values for that call.

**Android launch mechanism** (parity with the official `@capacitor/splash-screen`
plugin): the launch splash uses the Android 12 SplashScreen compat API
(`androidx.core:core-splashscreen`) and holds the SYSTEM-rendered splash from
the activity's launch theme (`Theme.SplashScreen` +
`windowSplashScreenAnimatedIcon`) on screen until `hide()` is called (or until
`launchShowDuration` elapses when `launchAutoHide` is on). No dialog surface is
drawn on top of the system splash, so the launch visual is exactly what the
launch theme declares. When the platform API fails (e.g. the theme is not
`Theme.SplashScreen`-based), the implementation falls back to the dialog-based
rendering path below. A `show()` call while the system launch splash is visible
is a no-op (the surface is not duplicated); `hide()` releases the keep-on-screen
condition and the platform fires the exit animation using
`launchFadeOutDuration`. Like the official plugin, the app content's drawing is
blocked (pre-draw listener) while the launch splash is held, so the exit
animation fades over freshly painted content instead of a blank frame
(prevents perceived flicker at the transition). Like the official plugin, the
launch splash is skipped
when the installed WebView is below the configured minimum and an error path is
set (the error page loads instead and nothing would ever call `hide()`).
