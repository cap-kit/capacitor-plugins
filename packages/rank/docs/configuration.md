# Rank — Configuration Guide

This guide covers the Rank plugin configuration options, the native platform requirements, and the permissions the plugin uses.

## Plugin Configuration

Configuration options for the Rank plugin.

| Prop                     | Type                 | Description                                                                                                                                    | Default            | Since |
| ------------------------ | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`**     | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). | <code>false</code> | 8.0.0 |
| **`appleAppId`**         | <code>string</code>  | The Apple App ID used for App Store redirection on iOS. Example: '123456789' \* @since 8.0.0                                                   |                    |       |
| **`androidPackageName`** | <code>string</code>  | The Android Package Name used for Play Store redirection. Example: 'com.example.app' \* @since 8.0.0                                           |                    |       |
| **`fireAndForget`**      | <code>boolean</code> | If true, the `requestReview` method will resolve immediately without waiting for the native OS review flow to complete. \* @default false      |                    | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Rank": {
      "verboseLogging": true,
      "appleAppId": "123456789",
      "androidPackageName": "com.example.app",
      "fireAndForget": false
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/rank" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Rank: {
      verboseLogging: true,
      appleAppId: '123456789',
      androidPackageName: 'com.example.app',
      fireAndForget: false,
    },
  },
};

export default config;
```

## Native Requirements

### Android

- Requires **Google Play Services** for In-App Reviews.
- To support **Android 11+ (API 30+)** and allow navigation to the Play Store, you must include the following in your `AndroidManifest.xml`:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="market" />
    </intent>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="https" android:host="play.google.com" />
    </intent>
</queries>
```

### iOS

- Requires **Xcode 26** and **iOS 15+**.
- To allow the plugin to open the App Store review page, ensure your `Info.plist` includes the appropriate URL schemes if you perform programmatic checks.

## Permissions

### Android

This plugin requires the following permission, which is automatically merged into your application's `AndroidManifest.xml`:

- `android.permission.INTERNET`: Required to communicate with Google Play Services for the review flow and store navigation.

### iOS

No specific usage descriptions (Privacy Manifest) are required for the standard `SKStoreReviewController` flow.
