# PrivacyScreen — Platform Behavior

## Android

While enabled, the plugin sets `FLAG_SECURE` on the window. The system then:

- Shows a black/blank card for the app in the recents / app-switcher view
  instead of a live snapshot.
- Blocks screenshots and screen recordings of the app.
- Blocks the window on non-secure displays (TVs, projectors, untrusted
  screen mirroring).

On top of the flag, the plugin renders a neutral full-screen scrim so
in-app transitions never flash protected content:

- Default: opaque scrim.
- `dimBackground: true`: translucent scrim.
- `privacyModeOnActivityHidden`: temporary scrim while the activity is
  hidden (e.g. a system biometric prompt is on top). `none` shows nothing
  extra, `dim` a translucent scrim, `splash` the opaque scrim.

## iOS

iOS offers no `FLAG_SECURE` equivalent, so the plugin installs a
full-screen blur overlay on the key window while enabled. App-switcher
snapshots then capture the blur instead of app content.

- `blurEffect: 'light'` / `'dark'`: matching system material blur.
- `'none'` (default): system material blur.
- The overlay is non-interactive and fades in/out over ~200ms.

Screenshots on iOS cannot be prevented by any public API. The plugin
observes `userDidTakeScreenshotNotification` and emits `screenshotTaken`
instead — use it for logging and incident response, not prevention.

## Web

No OS-level protection exists in browsers. `enable()` / `disable()` only
flip the state reported by `isEnabled()`; `screenshotTaken` never fires.
