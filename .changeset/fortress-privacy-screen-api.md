---
"@cap-kit/fortress": minor
---

Add Ionic-compatible privacy-screen runtime API: manual `enable(config?)` / `disable()` / `isEnabled()` independent from the vault lock state, `PrivacyScreenConfig` (Android `dimBackground` / `privacyModeOnActivityHidden`, iOS `blurEffect`), `screenshotTaken` listener and `removeAllListeners()`. Manual control detaches privacy from the follow-lock policy until `configure({ enablePrivacyScreen })` or `resetRuntimeConfig()` re-attaches it. `getRuntimeConfig()` now also snapshots `privacyScreenEnabled`.
