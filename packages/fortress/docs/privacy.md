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
