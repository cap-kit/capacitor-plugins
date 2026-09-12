# Integrity — Configuration Guide

This guide covers the optional native early boot integration, static configuration properties, iOS jailbreak URL scheme probing, and the platform permissions the plugin requires.

## Native Configuration (Optional — Early Boot Enhancement)

To capture security signals at the earliest possible stage (before the Capacitor bridge is initialized), you can manually integrate the plugin into your App Host's native entry points. This is highly recommended for detecting advanced threats like Root/Jailbreak or early instrumentation.

### When should you use native early boot integration?

Native early boot integration is **optional** and exists to improve
**signal timing**, not signal correctness.

You SHOULD consider this integration if:

- you want to observe potential root / jailbreak conditions
  **as early as possible**
- you need visibility into instrumentation that may attach
  before the JavaScript runtime starts
- you are building high-risk or security-sensitive applications

You do NOT need this integration if:

- you only require integrity signals during normal runtime
- you rely exclusively on `Integrity.check()` from JavaScript
- early detection timing is not critical for your use case

### Android Integration

In your `MainActivity.kt`, call `Integrity.onApplicationCreate(context)` inside the `onCreate` method:

```diff

package io.ionic.starter // Use your actual package name

import android.os.Bundle
import com.getcapacitor.BridgeActivity
+ import io.capkit.integrity.Integrity

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
+         // Capture security signals during early boot
+         Integrity.onApplicationCreate(this)

        super.onCreate(savedInstanceState)
    }
}

```

### iOS Integration

In your `AppDelegate.swift`, call `Integrity.onAppLaunch()` inside the `application(_:didFinishLaunchingWithOptions:)` method:

```diff

import UIKit
import Capacitor
+ import IntegrityPlugin // Import the plugin module

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

+        // Capture security signals at the earliest stage possible
+        Integrity.onAppLaunch()

        return true
    }
}

```

> **Important**
>
> If native early boot integration is not performed:
>
> - the plugin remains fully functional
> - no integrity capability is lost
> - detection simply starts when the JavaScript layer invokes `Integrity.check()`
>
> Native integration improves **timing**, not **coverage**.

## Early boot signals (IMPORTANT)

When integrated at the native application entry points
(`MainActivity.onCreate` on Android, `AppDelegate.didFinishLaunchingWithOptions`
on iOS), the Integrity plugin may capture **early boot integrity signals**
before the Capacitor bridge is fully initialized.

IMPORTANT NOTES:

- Early boot signals are **best-effort** and opportunistic.
- They are **not guaranteed** to be captured on every app launch.
- Their absence MUST NOT be interpreted as a clean environment.
- They may be affected by:
  - process restarts
  - OS-level lifecycle optimizations
  - multi-process behavior (Android)
  - warm launches vs cold starts

Behavioral guarantees:

- Early boot signals are **merged into the first `Integrity.check()` report**
  when available.
- If early boot integration is not performed, the plugin continues
  to function correctly without them.
- Applications MUST NOT rely exclusively on early boot signals
  for security decisions.

> Early boot detection improves visibility, not certainty.

## Static configuration

Configuration is **static** and read **natively** from `capacitor.config`.

These values are:

- read once at plugin initialization
- immutable at runtime
- NOT accessible from JavaScript

The available configuration properties are documented in the
[Configuration section](../README.md#configuration) of the README.

### blockPage

Controls the availability and source of a developer-provided HTML page
that may be presented to the end user when the host application decides
to do so.

| Prop                | Type      | Description                                                                                                                            | Default |
| ------------------- | --------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| `enabled`           | `boolean` | Enables the block page feature. When set to `false` or omitted, calls to `presentBlockPage()` are ignored or resolve as not presented. | `false` |
| `url`               | `string`  | URL or local path of the HTML page to present (a local file bundled with the application or a remote HTTPS URL).                       |         |
| `preventTapJacking` | `boolean` | Enables tap-jacking prevention on the block page (Android only). iOS has no effect as it does not support tap-jacking protection.      | `false` |

### jailbreakUrlSchemes (iOS only)

Enables probing for known jailbreak-related applications by checking
whether specific URL schemes can be opened by the system.

| Prop      | Type       | Description                                                           | Default |
| --------- | ---------- | --------------------------------------------------------------------- | ------- |
| `enabled` | `boolean`  | Enables jailbreak URL scheme probing.                                 | `false` |
| `schemes` | `string[]` | List of URL schemes to probe, each provided WITHOUT the `://` suffix. |         |

### Example with nested options

```ts
/// <reference types="@cap-kit/integrity" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Integrity: {
      verboseLogging: true,
      blockPage: {
        enabled: true,
        url: 'public/integrity-block.html',
        preventTapJacking: true,
      },
      jailbreakUrlSchemes: {
        enabled: true,
        schemes: ['cydia', 'sileo', 'zbra'],
      },
    },
  },
};

export default config;
```

## iOS Jailbreak URL Scheme Probing (Opt-In)

The Integrity plugin can optionally probe for known jailbreak-related
applications using URL schemes such as `cydia://`.

This feature is **disabled by default** and must be explicitly enabled
via native configuration.

### Requirements

You MUST declare the queried schemes in `Info.plist`:

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
  <string>cydia</string>
  <string>sileo</string>
  <string>zbra</string>
</array>
```

## Permissions

### Android

On Android, this plugin requires the `android.permission.INTERNET` permission. This permission is necessary for internal integrity checks, specifically for attempting socket connections to `localhost` with a controlled timeout to detect hooking frameworks like Frida.

This permission is automatically added to your `AndroidManifest.xml` via the plugin's native configuration.

#### Package Visibility (Android 11+)

To perform expanded root detection (scanning for apps like Magisk or SuperUser), the plugin declares a `<queries>` block in its manifest. This allow-list is required by Google Play policies to "see" security-related packages on devices with API level 30+.

**Note:** If your app is submitted to Google Play, you may need to justify the use of this expanded visibility during the app review process if you are targeting security-sensitive functionality.

### iOS

No additional runtime permissions are required for iOS.

#### ⚠️ Toolchain Requirement

This plugin is designed exclusively for **Capacitor v8**. In accordance with the global strict rules for this version, **Xcode 26 is the MANDATORY requirement** for building the iOS native implementation.

Any issues or behaviors observed on Xcode 15, 16, or earlier versions are considered environment misconfigurations and are not supported by the plugin.
