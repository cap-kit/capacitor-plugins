---
"@cap-kit/privacy-screen": minor
---

### Changes

- **Android:** Use `FLAG_SECURE` only for app switcher/screenshot/recording protection; no steady overlay while the app is active. The transient scrim is only used when `privacyModeOnActivityHidden` is set.
- **Android:** Improve API 34+ screen capture callback handling (register/unregister on resume, cleanup when disabled).
- **Android:** Fix `kotlinx.serialization` setup (apply serialization plugin and pin compiler plugin) for reliable `@Serializable` model serialization.
- **TypeScript/ESM:** Use explicit `.js` extensions in relative imports for better ESM compatibility.
- **Config/docs:** Clarify `dimBackground` as legacy for API compatibility in config/docs.
