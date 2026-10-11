# Fortress — Privacy Overlay

Fortress supports customizable privacy screen overlays that display when the
vault is locked.

## Configuration

```typescript
// capacitor.config.ts
const config: CapacitorConfig = {
  plugins: {
    Fortress: {
      enablePrivacyScreen: true,
      privacyOverlayText: 'Session Locked',
      privacyOverlayImageName: 'lock_icon', // Optional: image from asset catalog
      privacyOverlayShowText: true,
      privacyOverlayShowImage: true,
      privacyOverlayTextColor: '#FFFFFF',
      privacyOverlayBackgroundOpacity: 0.8,
      privacyOverlayTheme: 'system', // 'system' | 'light' | 'dark'
    },
  },
};
```

## Runtime updates

You can update the privacy overlay at runtime:

```ts
await Fortress.configure({
  privacyOverlayText: 'New text', // Updates immediately if overlay is visible
});
```

## Manual runtime control (Ionic API parity)

Beyond the follow-lock policy, privacy can be driven manually per screen —
the same `enable() / disable() / isEnabled()` contract as the official
`@capacitor/privacy-screen` plugin:

```ts
// Protect a sensitive screen while the vault stays unlocked.
await Fortress.enable({
  android: { dimBackground: true, privacyModeOnActivityHidden: 'splash' },
  ios: { blurEffect: 'dark' },
});

// Leave protection for a public screen.
await Fortress.disable();

// Independent from the vault lock state.
const { enabled } = await Fortress.isEnabled();
const { isLocked } = await Fortress.isLocked();
```

Semantics:

- `enable()` / `disable()` apply immediately and detach privacy from the
  follow-lock policy. Explicit manual control wins from that moment on.
- `configure({ enablePrivacyScreen })` or `resetRuntimeConfig()` re-attaches
  the policy and clears the manual override.
- `getRuntimeConfig()` snapshots the effective state in `privacyScreenEnabled`.
- Visual style stays Fortress-driven (`privacyOverlay*`); the
  `PrivacyScreenConfig` knobs are accepted for API compatibility.

Screenshot observability (Capawesome parity):

```ts
const handle = await Fortress.addListener('screenshotTaken', () => {
  console.log('Screenshot taken (after the fact — cannot be prevented)');
});
// iOS: delivered via system notification.
// Android: API 34+ only, delivered while protection is active.
await Fortress.removeAllListeners();
```

## Platform behavior notes

- **Android recents/task switcher:** Android applies snapshot protection with
  `FLAG_SECURE`. In this mode, the system usually shows a protected/blank
  preview card. Custom overlay text/image is not guaranteed to be visible in
  recents previews.
- **Android in-app lock overlay:** `privacyOverlayText`,
  `privacyOverlayImageName`, and `privacyOverlayTheme` apply to the plugin's
  in-app privacy overlay.
- **Biometric prompt UI on Android:** The lock icon/title style belongs to
  system `BiometricPrompt` and cannot be fully themed by the plugin. You can
  customize prompt content via `biometricPromptText` and `promptOptions`
  (title/subtitle/description/negative button).

## Asset requirements

- **iOS**: Add images to your Xcode asset catalog (Assets.xcassets)
- **Android**: Add drawable resources to `android/app/src/main/res/drawable`
