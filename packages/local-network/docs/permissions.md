# Local Network — Permissions

This guide covers what the Local Network plugin requires on each platform:
Android manifest entries, iOS `Info.plist` keys, and Web behavior.

## Android

The plugin declares `android.permission.ACCESS_LOCAL_NETWORK` in its own
manifest, so it is merged into the host app automatically. No manual manifest
edit is required:

```xml
<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
```

The permission is a runtime permission introduced in Android 17 (API 37). On
lower API levels it is merged but never enforced, and both permission methods
short-circuit to `granted`.

Read about [Setting Permissions](https://capacitorjs.com/docs/android/configuration#setting-permissions)
in the [Android Guide](https://capacitorjs.com/docs/android).

## iOS

iOS does not expose local network access as a queryable runtime permission, so
this plugin reports `granted` and never prompts.

If your app talks to devices on the local network, iOS gates the first access
behind its own system privacy prompt. Two `Info.plist` keys drive it:

- `NSLocalNetworkUsageDescription` — required for any local network access; it
  is the message shown in the system prompt
- `NSBonjourServices` — additionally required when the app browses or
  advertises Bonjour services

```xml
<key>NSLocalNetworkUsageDescription</key>
<string>This app connects to devices on your local network.</string>
<key>NSBonjourServices</key>
<array>
  <string>_http._tcp</string>
  <string>_myapp._tcp</string>
</array>
```

Without `NSLocalNetworkUsageDescription` the system prompt cannot be shown and
local network requests fail. The plugin cannot declare these keys for you
because they are specific to your app.

Read about [Configuring `Info.plist`](https://capacitorjs.com/docs/ios/configuration#configuring-infoplist)
in the [iOS Guide](https://capacitorjs.com/docs/ios).

## Web

The browser has no local network permission, and this plugin performs no
network access of its own. Both permission methods always report `granted`.

## Summary

| Platform    | Declaration                                                                          | Managed by the plugin |
| ----------- | ------------------------------------------------------------------------------------ | --------------------- |
| Android 17+ | `ACCESS_LOCAL_NETWORK` in `AndroidManifest.xml`                                      | Yes (auto-merged)     |
| iOS         | `NSLocalNetworkUsageDescription` (+ `NSBonjourServices` for Bonjour) in `Info.plist` | No (app-specific)     |
| Web         | None                                                                                 | —                     |
