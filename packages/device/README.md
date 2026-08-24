<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Device</h3>
<p align="center">
  <strong>
    <code>@cap-kit/device</code>
  </strong>
</p>

<p align="center">
  The <strong>Device</strong> for the Cap-Kit ecosystem.<br>
  This package serves as the definitive <strong>boilerplate and validation ground</strong> for creating new Capacitor plugins.<br>
  It demonstrates the enforced monorepo structure, build configuration, and native bridges (Swift/Kotlin) required by our standards.<br>
  <em>Note: This is an internal reference package, primarily used for CI verification and scaffolding.</em>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/device">
    <img src="https://img.shields.io/npm/v/@cap-kit/device?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/device">
    <img src="https://img.shields.io/npm/dm/@cap-kit/device?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/device?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Install

```bash
pnpm add @cap-kit/device
npx cap sync
```

## Apple Privacy Manifest Requirements

Apple mandates that app developers now specify approved reasons for API usage to enhance user privacy. By May 1st, 2024, it's required to include these reasons when submitting apps to the App Store Connect.

When using this specific plugin in your app, you must create a `PrivacyInfo.xcprivacy` file in `/ios/App` or use the VS Code Extension to generate it, specifying the usage reasons.

For detailed steps on how to do this, please see the [Capacitor Docs](https://capacitorjs.com/docs/ios/privacy-manifest).

**For this plugin, the required dictionary key is [NSPrivacyAccessedAPICategoryDiskSpace](https://developer.apple.com/documentation/bundleresources/privacy_manifest_files/describing_use_of_required_reason_api#4278397) and the recommended reason is [85F4.1](https://developer.apple.com/documentation/bundleresources/privacy_manifest_files/describing_use_of_required_reason_api#4278397).**

### Example PrivacyInfo.xcprivacy

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
  <dict>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
      <!-- Add this dict entry to the array if the PrivacyInfo file already exists -->
      <dict>
        <key>NSPrivacyAccessedAPIType</key>
        <string>NSPrivacyAccessedAPICategoryDiskSpace</string>
        <key>NSPrivacyAccessedAPITypeReasons</key>
        <array>
          <string>85F4.1</string>
        </array>
      </dict>
    </array>
  </dict>
</plist>
```

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the Device plugin.

| Prop                 | Type                 | Description                                                                                                                                                                                                                              | Default            | Since |
| -------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API. | <code>false</code> | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Device": {
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/device" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Device: {
      verboseLogging: true,
    },
  },
};

export default config;
```

</docgen-config>

## API

<docgen-index>

- [`getId()`](#getid)
- [`getInfo()`](#getinfo)
- [`getBatteryInfo()`](#getbatteryinfo)
- [`getLanguageCode()`](#getlanguagecode)
- [`getLanguageTag()`](#getlanguagetag)
- [`addListener('batteryChargingStateChange', ...)`](#addlistenerbatterychargingstatechange-)
- [`removeAllListeners()`](#removealllisteners)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Device Capacitor plugin.

This interface defines a stable, platform-agnostic API.
All methods behave consistently across Android, iOS, and Web.

### getId()

```typescript
getId() => Promise<DeviceId>
```

Return an unique identifier for the device.

**Returns:** <code>Promise&lt;<a href="#deviceid">DeviceId</a>&gt;</code>

**Since:** 8.0.0

---

### getInfo()

```typescript
getInfo() => Promise<DeviceInfo>
```

Return information about the underlying device/os/platform.

**Returns:** <code>Promise&lt;<a href="#deviceinfo">DeviceInfo</a>&gt;</code>

**Since:** 8.0.0

---

### getBatteryInfo()

```typescript
getBatteryInfo() => Promise<BatteryInfo>
```

Return information about the battery.

**Returns:** <code>Promise&lt;<a href="#batteryinfo">BatteryInfo</a>&gt;</code>

**Since:** 8.0.0

---

### getLanguageCode()

```typescript
getLanguageCode() => Promise<GetLanguageCodeResult>
```

Get the device's current language locale code.

**Returns:** <code>Promise&lt;<a href="#getlanguagecoderesult">GetLanguageCodeResult</a>&gt;</code>

**Since:** 8.0.0

---

### getLanguageTag()

```typescript
getLanguageTag() => Promise<LanguageTag>
```

Get the device's current language locale tag.

**Returns:** <code>Promise&lt;<a href="#languagetag">LanguageTag</a>&gt;</code>

**Since:** 8.0.0

---

### addListener('batteryChargingStateChange', ...)

```typescript
addListener(eventName: "batteryChargingStateChange", listenerFunc: BatteryChargingStateChangeListener) => Promise<PluginListenerHandle>
```

Listen for changes to whether the device is charging (including when the battery becomes full while plugged in).

| Param              | Type                                                                                              |
| ------------------ | ------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'batteryChargingStateChange'</code>                                                         |
| **`listenerFunc`** | <code><a href="#batterychargingstatechangelistener">BatteryChargingStateChangeListener</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

---

### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Remove all listeners for this plugin.

**Since:** 8.0.0

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { version } = await Device.getPluginVersion();
```

---

### Interfaces

#### DeviceId

| Prop             | Type                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Since |
| ---------------- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`identifier`** | <code>string</code> | The identifier of the device as available to the app. This identifier may change on modern mobile platforms that only allow per-app install ids. On iOS, the identifier is a UUID that uniquely identifies a device to the app's vendor ([read more](https://developer.apple.com/documentation/uikit/uidevice/1620059-identifierforvendor)). On Android 8+, **the identifier is a 64-bit number (expressed as a hexadecimal string)**, unique to each combination of app-signing key, user, and device ([read more](https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID)). On web, a random identifier is generated and stored on localStorage for subsequent calls. If localStorage is not available a new random identifier will be generated on every call. | 8.0.0 |

#### DeviceInfo

| Prop                    | Type                                                        | Description                                                                                                                                                                                                                                                                                                                                      | Since |
| ----------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----- |
| **`name`**              | <code>string</code>                                         | The name of the device. For example, "John's iPhone". This is only supported on iOS and Android 7.1 or above. On iOS 16+ this will return a generic device name without the appropriate [entitlements](https://developer.apple.com/documentation/bundleresources/entitlements/com_apple_developer_device-information_user-assigned-device-name). | 8.0.0 |
| **`model`**             | <code>string</code>                                         | The device model. For example, "iPhone13,4".                                                                                                                                                                                                                                                                                                     | 8.0.0 |
| **`platform`**          | <code>'ios' \| 'android' \| 'web'</code>                    | The device platform (lowercase).                                                                                                                                                                                                                                                                                                                 | 8.0.0 |
| **`operatingSystem`**   | <code><a href="#operatingsystem">OperatingSystem</a></code> | The operating system of the device.                                                                                                                                                                                                                                                                                                              | 8.0.0 |
| **`osVersion`**         | <code>string</code>                                         | The version of the device OS.                                                                                                                                                                                                                                                                                                                    | 8.0.0 |
| **`iOSVersion`**        | <code>number</code>                                         | The iOS version number. Only available on iOS. Multi-part version numbers are crushed down into an integer padded to two-digits, ex: `"16.3.1"` -&gt; `160301`                                                                                                                                                                                   | 8.0.0 |
| **`androidSDKVersion`** | <code>number</code>                                         | The Android SDK version number. Only available on Android.                                                                                                                                                                                                                                                                                       | 8.0.0 |
| **`manufacturer`**      | <code>string</code>                                         | The manufacturer of the device.                                                                                                                                                                                                                                                                                                                  | 8.0.0 |
| **`isVirtual`**         | <code>boolean</code>                                        | Whether the app is running in a simulator/emulator.                                                                                                                                                                                                                                                                                              | 8.0.0 |
| **`memUsed`**           | <code>number</code>                                         | Approximate memory used by the current app, in bytes. Divide by 1048576 to get the number of MBs used.                                                                                                                                                                                                                                           | 8.0.0 |
| **`diskFree`**          | <code>number</code>                                         | How much free disk space is available on the normal data storage path for the os, in bytes. On Android it returns the free disk space on the "system" partition holding the core Android OS. On iOS this value is not accurate.                                                                                                                  | 8.0.0 |
| **`diskTotal`**         | <code>number</code>                                         | The total size of the normal data storage path for the OS, in bytes. On Android it returns the disk space on the "system" partition holding the core Android OS.                                                                                                                                                                                 | 8.0.0 |
| **`realDiskFree`**      | <code>number</code>                                         | How much free disk space is available on the normal data storage, in bytes.                                                                                                                                                                                                                                                                      | 8.0.0 |
| **`realDiskTotal`**     | <code>number</code>                                         | The total size of the normal data storage path, in bytes.                                                                                                                                                                                                                                                                                        | 8.0.0 |
| **`webViewVersion`**    | <code>string</code>                                         | The web view browser version                                                                                                                                                                                                                                                                                                                     | 8.0.0 |

#### BatteryInfo

| Prop               | Type                 | Description                                                       | Since |
| ------------------ | -------------------- | ----------------------------------------------------------------- | ----- |
| **`batteryLevel`** | <code>number</code>  | A percentage (0 to 1) indicating how much the battery is charged. | 8.0.0 |
| **`isCharging`**   | <code>boolean</code> | Whether the device is charging.                                   | 8.0.0 |

#### GetLanguageCodeResult

| Prop        | Type                | Description                  | Since |
| ----------- | ------------------- | ---------------------------- | ----- |
| **`value`** | <code>string</code> | Two character language code. | 8.0.0 |

#### LanguageTag

| Prop        | Type                | Description                                     | Since |
| ----------- | ------------------- | ----------------------------------------------- | ----- |
| **`value`** | <code>string</code> | Returns a well-formed IETF BCP 47 language tag. | 8.0.0 |

#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

### Type Aliases

#### OperatingSystem

<code>'ios' | 'android' | 'windows' | 'mac' | 'unknown'</code>

#### BatteryChargingStateChangeListener

Callback for battery charging state changes.

<code>
  (info: <a href="#batteryinfo">BatteryInfo</a>): void
</code>

</docgen-api>

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## Credits

This plugin is based on prior work from the Community and
has been refactored and modernized for **Capacitor v8** and
**Swift Package Manager** compatibility.

Original inspiration:

- [https://github.com/](https://github.com/)

---

## License

MIT
