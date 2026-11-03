---
"@cap-kit/privacy-screen": minor
---

- Static `color` option for the privacy cover: when
  `plugins.PrivacyScreen.color` is set to a valid hex color (`#RRGGBB` or
  `#AARRGGBB`), the cover uses that solid color instead of the platform
  default (blur on iOS, black scrim on Android). Invalid values are ignored
  and the default applies. Behavior is unchanged when the option is absent.
- Light/dark appearance variants for `color`: the option also accepts
  `{ light: hex, dark: hex }`; the variant matching the current appearance
  is used, and a single variant covers both appearances when only one is
  set. A plain string still applies to both appearances.
