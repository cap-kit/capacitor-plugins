# @cap-kit/privacy-screen

## 8.1.1

### Patch Changes

- 5a926bd: Bump Capacitor Swift PM to 8.6.0

## 8.1.0

### Minor Changes

- 975c981: ### Changes

  - **Android:** Use `FLAG_SECURE` only for app switcher/screenshot/recording protection; no steady overlay while the app is active. The transient scrim is only used when `privacyModeOnActivityHidden` is set.
  - **Android:** Improve API 34+ screen capture callback handling (register/unregister on resume, cleanup when disabled).
  - **Android:** Fix `kotlinx.serialization` setup (apply serialization plugin and pin compiler plugin) for reliable `@Serializable` model serialization.
  - **TypeScript/ESM:** Use explicit `.js` extensions in relative imports for better ESM compatibility.
  - **Config/docs:** Clarify `dimBackground` as legacy for API compatibility in config/docs.

### Patch Changes

- 123fea5: Add unit tests for Android and iOS and improve input validation

## 8.0.0

### Minor Changes

- d7b3ff5: First stable release of @cap-kit/privacy-screen plugin.

  - iOS screen recording detection (screenRecordingStarted/Stopped)
  - ScreenshotTaken listener management
  - getPluginVersion() method
  - removeAllListeners() method
  - Full Android and iOS configuration support
  - Web implementation with API parity
  - Package.swift: iOS deployment target iOS 11, capacitor-swift-pm 8.5.3
  - PrivacyScreenPlugin.swift: fixed screen recording notification names
  - Example app updates and documentation
  - API-compatible with @capacitor/privacy-screen contract

## 8.0.0

### Minor Changes

- First stable release of @cap-kit/privacy-screen plugin.

  - iOS screen recording detection (screenRecordingStarted/Stopped)
  - ScreenshotTaken listener management
  - getPluginVersion() method
  - removeAllListeners() method
  - Full Android and iOS configuration support
  - Web implementation with API parity
  - Example app updates and documentation
  - API-compatible with @capacitor/privacy-screen contract
