# PrivacyScreen — Security Model

## What it protects against

Opportunistic visual exposure of app content:

- Shoulder-surfing via the OS app switcher / recents view.
- Casual screenshots and screen recordings (Android blocks them outright
  while enabled).
- Content leaking onto non-secure external displays (Android).
- Live-view fragments that outlive navigation (covered by the temporary
  activity-hidden scrim on Android when configured).

## What it does NOT do

- **Prevent iOS screenshots.** No public API can. `screenshotTaken` is a
  post-facto signal — treat it as detection, never as a barrier.
- **Stop a camera pointed at the screen**, accessibility services reading
  the view hierarchy, or a compromised OS. This is a UI-hygiene control,
  not DRM.
- **Protect the web platform.** Browsers expose no equivalent primitive;
  the web implementation is state tracking only.
- **Replace authentication.** A hidden screen is not a locked vault. Pair
  this plugin with real access control (see `@cap-kit/fortress`) for
  sensitive data.

## Best practices

- Default-deny: enable at startup for sensitive apps, open holes per
  public screen — not the other way around.
- Always re-enable when navigating back into secure content, including
  back-gesture navigation the plugin cannot observe.
- On Android, prefer `disable()`/`enable()` pairs around capturable flows
  over leaving protection off: the deprecated `preventScreenshots` toggle
  does not exist here by design.
- Log `screenshotTaken` events server-side with timestamp and screen
  context; on Android they require API 34+ and active protection.
