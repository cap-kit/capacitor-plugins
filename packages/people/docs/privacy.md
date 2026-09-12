# People — Apple Privacy Manifest

This guide covers the Apple privacy manifest requirements for the People plugin, including the bundled skeleton `PrivacyInfo.xcprivacy` file and how to declare Required Reason APIs.

Apple mandates that app developers specify approved reasons for API usage to enhance user privacy.

This plugin includes a skeleton `PrivacyInfo.xcprivacy` file located in `ios/Sources/PeoplePlugin/PrivacyInfo.xcprivacy`.

**You must populate this file if your plugin uses any [Required Reason APIs](https://developer.apple.com/documentation/bundleresources/privacy_manifest_files/describing_use_of_required_reason_api).**

### Example: User Defaults

If your plugin uses `UserDefaults`, you must declare it in the manifest:

```xml
<dict>
    <key>NSPrivacyAccessedAPIType</key>
    <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
    <key>NSPrivacyAccessedAPITypeReasons</key>
    <array>
        <string>CA92.1</string>
    </array>
</dict>

```

For detailed steps, please see the [Capacitor Docs](https://capacitorjs.com/docs/ios/privacy-manifest).
