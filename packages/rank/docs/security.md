# Rank — Security Considerations

The Rank plugin relies on the operating system to decide when and how often the native review prompt is shown. The following limitations apply.

## Limitations

### General

- **`openCollection`**: This feature is specific to the Google Play Store and is unavailable on iOS/Web.
- **`openDevPage`**: On iOS, this method performs a store search for the developer name as a fallback, as direct developer page IDs are not consistently supported via deep links.

### iOS

- The in-app review prompt is **not guaranteed to appear**.
  Apple internally controls when and how often the review dialog is shown.
- Calling `requestReview()` may result in **no visible UI**, even if the API is available.
- On iOS, `requestReview()` is effectively always fire-and-forget because StoreKit does not provide a completion callback; the `fireAndForget` option does not change this behavior on iOS.

### Android

- Google Play In-App Review requires **Google Play Services** to be available on the device.
- The review flow may silently fail if Play Services are missing, outdated, or restricted.
- As with iOS, the system ultimately decides whether the review dialog is displayed.
