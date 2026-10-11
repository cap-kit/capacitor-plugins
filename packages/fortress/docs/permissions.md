# Fortress — Permissions

This guide covers the permissions required by the Fortress plugin on each
platform: Android manifest entries, iOS `Info.plist` keys, and the optional
Keychain Sharing capability.

## Android

The plugin automatically adds the following permissions to your app's
manifest:

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

BiometricPrompt requires **Android API 24+** (see the
[usage guide](guide.md) for full native requirements).

## iOS

You must add the following to your `Info.plist`:

```xml
<key>NSFaceIDUsageDescription</key>
<string>Fortress uses Face ID to secure your data.</string>
```

Read about [Configuring `Info.plist`](https://capacitorjs.com/docs/ios/configuration#configuring-infoplist)
in the [iOS Guide](https://capacitorjs.com/docs/ios) for more information on
setting iOS permissions in Xcode.

### Optional capabilities

For iCloud Keychain sync support, enable the **Keychain Sharing** capability
in your Xcode project:

1. Select your app target in Xcode
2. Go to **Signing & Capabilities**
3. Click **+ Capability** → **Keychain Sharing**
4. Add an appropriate Keychain Access Group (optional)

This enables the `enableICloudKeychainSync` configuration option.
