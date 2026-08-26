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
- [`getBatteryExtras()`](#getbatteryextras)
- [`getDisplayInfo()`](#getdisplayinfo)
- [`getConfiguration()`](#getconfiguration)
- [`getPowerState()`](#getpowerstate)
- [`getMemoryInfo()`](#getmemoryinfo)
- [`getSystemUptime()`](#getsystemuptime)
- [`getAppVersion()`](#getappversion)
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

#### Example

```ts
const { identifier } = await Device.getId();
console.log('Device ID:', identifier);
```

---

### getInfo()

```typescript
getInfo() => Promise<DeviceInfo>
```

Return information about the underlying device/os/platform.

**Returns:** <code>Promise&lt;<a href="#deviceinfo">DeviceInfo</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const info = await Device.getInfo();
console.log(`${info.manufacturer} ${info.model}`);
console.log(`${info.operatingSystem} ${info.osVersion}`);
console.log(`Virtual: ${info.isVirtual}`);
```

---

### getBatteryInfo()

```typescript
getBatteryInfo() => Promise<BatteryInfo>
```

Return information about the battery.

**Returns:** <code>Promise&lt;<a href="#batteryinfo">BatteryInfo</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const battery = await Device.getBatteryInfo();
const percent = Math.round((battery.batteryLevel ?? 0) * 100);
console.log(`Battery: ${percent}% — ${battery.isCharging ? 'charging' : 'on battery'}`);
```

---

### getLanguageCode()

```typescript
getLanguageCode() => Promise<GetLanguageCodeResult>
```

Get the device's current language locale code.

**Returns:** <code>Promise&lt;<a href="#getlanguagecoderesult">GetLanguageCodeResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { value } = await Device.getLanguageCode();
console.log('Language:', value); // "en", "es", "it"
```

---

### getLanguageTag()

```typescript
getLanguageTag() => Promise<LanguageTag>
```

Get the device's current language locale tag.

**Returns:** <code>Promise&lt;<a href="#languagetag">LanguageTag</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { value } = await Device.getLanguageTag();
console.log('Language tag:', value); // "en-US", "es-ES", "it-IT"
```

---

### getBatteryExtras()

```typescript
getBatteryExtras() => Promise<BatteryExtras>
```

Return extended battery information including charge source and detailed state.

The `health` and `temperature` fields are only available on Android.
On web, throws `unavailable`.

**Returns:** <code>Promise&lt;<a href="#batteryextras">BatteryExtras</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const extras = await Device.getBatteryExtras();
console.log(`Charging via: ${extras.chargeSource}`); // "ac", "usb", "wireless"
console.log(`State: ${extras.detailedState}`); // "charging", "full", "unplugged"

// Android-only fields
if (extras.health) {
  console.log(`Battery health: ${extras.health}`); // "good", "overheat", "dead"
}
if (extras.temperature) {
  console.log(`Temperature: ${extras.temperature}°C`);
}
```

---

### getDisplayInfo()

```typescript
getDisplayInfo() => Promise<DisplayInfo>
```

Return display characteristics (resolution, density, refresh rate).

On web, returns partial data from `window.screen`. Refresh rate defaults to 60 Hz
as web does not reliably expose this value.

**Returns:** <code>Promise&lt;<a href="#displayinfo">DisplayInfo</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const display = await Device.getDisplayInfo();
console.log(`Screen: ${display.widthPx}×${display.heightPx}`);
console.log(`Density: ${display.densityDpi} DPI (${display.scale}x)`);
console.log(`Refresh rate: ${display.refreshRateHz} Hz`);
```

---

### getConfiguration()

```typescript
getConfiguration() => Promise<DeviceConfiguration>
```

Return device configuration and user preferences (orientation, dark mode, font scale).

On web, returns partial data from `matchMedia` and `screen.orientation`.
`idiom` defaults to `"phone"` and `screenSize` defaults to `"normal"` on web.

**Returns:** <code>Promise&lt;<a href="#deviceconfiguration">DeviceConfiguration</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const config = await Device.getConfiguration();
console.log(`Orientation: ${config.orientation}`);
console.log(`Dark mode: ${config.isDarkMode}`);
console.log(`Font scale: ${config.fontScale}x`);
console.log(`Device type: ${config.idiom}`); // "phone", "tablet", "desktop"
console.log(`Screen size: ${config.screenSize}`); // "small", "normal", "large", "xlarge"
```

---

### getPowerState()

```typescript
getPowerState() => Promise<PowerState>
```

Return power and thermal state information.

Use this to adapt app behavior when the device is in power-saving mode
or experiencing thermal throttling.

Only available on iOS and Android. On web, throws `unavailable`.

**Returns:** <code>Promise&lt;<a href="#powerstate">PowerState</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const power = await Device.getPowerState();
if (power.isLowPowerMode) {
  // Reduce animations, defer background sync, lower frame rate
  console.log('Low power mode active — reducing workload');
}
if (power.thermalState === 'serious' || power.thermalState === 'critical') {
  // Pause heavy computation, skip video processing
  console.log(`Thermal throttling detected: ${power.thermalState}`);
}
```

---

### getMemoryInfo()

```typescript
getMemoryInfo() => Promise<MemoryInfo>
```

Return memory information (physical RAM, CPU cores, memory class).

Use this to adapt cache sizes, parallelism, and feature gating based on
device capabilities.

Only available on iOS and Android. On web, throws `unavailable`.

**Returns:** <code>Promise&lt;<a href="#memoryinfo">MemoryInfo</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const memory = await Device.getMemoryInfo();
const ramMB = Math.round(memory.physicalRam / (1024 * 1024));
console.log(`RAM: ${ramMB} MB — ${memory.cpuCores} cores`);
console.log(`Memory budget: ${memory.memoryClassMb} MB`);

if (memory.isLowRamDevice) {
  // Reduce image cache, skip animations, use smaller thumbnails
  console.log('Low RAM device — using reduced features');
}
```

---

### getSystemUptime()

```typescript
getSystemUptime() => Promise<SystemUptime>
```

Return system uptime (time since last boot).

Useful for analytics, performance heuristics, and detecting long uptimes
that may correlate with degraded performance.

**Returns:** <code>Promise&lt;<a href="#systemuptime">SystemUptime</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { uptimeSeconds } = await Device.getSystemUptime();
const days = Math.floor(uptimeSeconds / 86400);
console.log(`Device uptime: ${days} day(s)`);

if (uptimeSeconds > 7 * 86400) {
  // Suggest restarting the device after 7+ days
  console.log('Device has been running for over a week');
}
```

---

### getAppVersion()

```typescript
getAppVersion() => Promise<AppVersion>
```

Return application version information (version string + build number).

**Returns:** <code>Promise&lt;<a href="#appversion">AppVersion</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const app = await Device.getAppVersion();
console.log(`App version: ${app.version} (build ${app.buildNumber})`);

// Use for update checks or analytics
analytics.setAppVersion(app.version);
```

---

### addListener('batteryChargingStateChange', ...)

```typescript
addListener(eventName: "batteryChargingStateChange", listenerFunc: BatteryChargingStateChangeListener) => Promise<PluginListenerHandle>
```

Listen for changes to whether the device is charging (including when the battery becomes full while plugged in).

The listener fires only when the charging state **changes**. It is not invoked at subscription time.

| Param              | Type                                                                                              |
| ------------------ | ------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'batteryChargingStateChange'</code>                                                         |
| **`listenerFunc`** | <code><a href="#batterychargingstatechangelistener">BatteryChargingStateChangeListener</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const handle = await Device.addListener('batteryChargingStateChange', (info) => {
  console.log(`Charging: ${info.isCharging ? 'yes' : 'no'}`);
});

// Later, remove the listener
await handle.remove();
```

---

### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Remove all listeners for this plugin.

**Since:** 8.0.0

#### Example

```ts
await Device.removeAllListeners();
```

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
console.log('Plugin version:', version);
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

#### BatteryExtras

Extended battery information including charge source and detailed state.

| Prop                | Type                                                                                | Description                                                                                                                                                                                                                                                                                  | Since |
| ------------------- | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`chargeSource`**  | <code>'unknown' \| 'ac' \| 'usb' \| 'wireless'</code>                               | The current charge source. - `"ac"` — AC adapter - `"usb"` — USB port - `"wireless"` — Wireless charging - `"unknown"` — Charge source not determinable                                                                                                                                      | 8.0.0 |
| **`detailedState`** | <code>'unknown' \| 'unplugged' \| 'charging' \| 'full' \| 'not-charging'</code>     | The detailed battery state. - `"unknown"` — Battery state is unknown - `"unplugged"` — Not connected to a power source - `"charging"` — Connected and charging - `"full"` — Connected and fully charged - `"not-charging"` — Connected but not charging (e.g. battery temperature limit)     | 8.0.0 |
| **`health`**        | <code>'unknown' \| 'good' \| 'overheat' \| 'dead' \| 'cold' \| 'unspecified'</code> | Battery health status. Only available on Android. - `"good"` — Battery is in good condition - `"overheat"` — Battery is overheating - `"dead"` — Battery is dead - `"unknown"` — Health status not available - `"cold"` — Battery is too cold - `"unspecified"` — Health status not reported | 8.0.0 |
| **`temperature`**   | <code>number</code>                                                                 | Battery temperature in degrees Celsius. Only available on Android. iOS does not expose battery temperature through public APIs.                                                                                                                                                              | 8.0.0 |

#### DisplayInfo

Display characteristics of the device screen.

| Prop                | Type                | Description                                                                                                                                                       | Since |
| ------------------- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`widthPx`**       | <code>number</code> | Screen width in physical pixels.                                                                                                                                  | 8.0.0 |
| **`heightPx`**      | <code>number</code> | Screen height in physical pixels.                                                                                                                                 | 8.0.0 |
| **`densityDpi`**    | <code>number</code> | Screen density in DPI (dots per inch). Common values: 160 (mdpi), 240 (hdpi), 320 (xhdpi), 480 (xxhdpi), 640 (xxxhdpi).                                           | 8.0.0 |
| **`scale`**         | <code>number</code> | Display scale factor. On iOS this corresponds to `UIScreen.main.scale` (e.g. 2.0, 3.0). On Android this is `DisplayMetrics.density` (e.g. 1.0, 1.5, 2.0, 3.0).    | 8.0.0 |
| **`refreshRateHz`** | <code>number</code> | Maximum display refresh rate in Hz. On iOS this corresponds to `UIScreen.main.maximumFramesPerWindow`. On Android this corresponds to `Display.getRefreshRate()`. | 8.0.0 |

#### DeviceConfiguration

Device configuration and user preferences.

| Prop              | Type                                                                 | Description                                                                                                                                                                                                                                                                                               | Since |
| ----------------- | -------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`orientation`** | <code>'unknown' \| 'portrait' \| 'landscape'</code>                  | Current interface orientation. - `"portrait"` — Device is in portrait orientation - `"landscape"` — Device is in landscape orientation - `"unknown"` — Orientation cannot be determined                                                                                                                   | 8.0.0 |
| **`isDarkMode`**  | <code>boolean</code>                                                 | Whether the device is in dark mode. On iOS this corresponds to `UITraitCollection.userInterfaceStyle`. On Android this corresponds to `Configuration.uiMode`.                                                                                                                                             | 8.0.0 |
| **`fontScale`**   | <code>number</code>                                                  | User's preferred text size multiplier. A value of `1.0` means default text size. Values above 1.0 indicate the user has increased text size for accessibility. On iOS this corresponds to `UIContentSizeCategory` converted to a numeric scale. On Android this corresponds to `Configuration.fontScale`. | 8.0.0 |
| **`idiom`**       | <code>'desktop' \| 'unknown' \| 'phone' \| 'tablet'</code>           | Device form factor / idiom. - `"phone"` — iPhone or small Android phone - `"tablet"` — iPad or Android tablet - `"desktop"` — Mac Catalyst or desktop mode - `"unknown"` — Cannot determine form factor                                                                                                   | 8.0.0 |
| **`screenSize`**  | <code>'unknown' \| 'small' \| 'normal' \| 'large' \| 'xlarge'</code> | Screen size category relative to standard phone dimensions. - `"small"` — Smaller than normal phone - `"normal"` — Standard phone - `"large"` — Large phone / small tablet - `"xlarge"` — Tablet or larger - `"unknown"` — Cannot determine                                                               | 8.0.0 |

#### PowerState

Device power and thermal state.

| Prop                 | Type                                                        | Description                                                                                                                                                                                                                                                                                                                                                                                            | Since |
| -------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----- |
| **`isLowPowerMode`** | <code>boolean</code>                                        | Whether the device is in low power / battery saver mode. On iOS this corresponds to `ProcessInfo.isLowPowerModeEnabled`. On Android this corresponds to `PowerManager.isPowerSaveMode()`.                                                                                                                                                                                                              | 8.0.0 |
| **`thermalState`**   | <code>'nominal' \| 'fair' \| 'serious' \| 'critical'</code> | Current thermal state of the device. - `"nominal"` — No thermal issues - `"fair"` — Thermal level is elevated but manageable - `"serious"` — Device is actively throttling performance - `"critical"` — Device is severely throttling; reduce workload immediately On iOS this corresponds to `ProcessInfo.thermalState`. On Android this is derived from battery temperature and system thermal APIs. | 8.0.0 |

#### MemoryInfo

Device memory information beyond the basic `memUsed` field.

| Prop                 | Type                 | Description                                                                                                                                                                                         | Since |
| -------------------- | -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`physicalRam`**    | <code>number</code>  | Total physical RAM available on the device, in bytes.                                                                                                                                               | 8.0.0 |
| **`cpuCores`**       | <code>number</code>  | Number of active CPU cores available for the app. This may be less than the total core count on devices with heterogeneous CPU architectures (big.LITTLE, etc.).                                    | 8.0.0 |
| **`memoryClassMb`**  | <code>number</code>  | Standard app memory budget in MB. On iOS this corresponds to `ProcessInfo.physicalMemory` (full RAM available to the app). On Android this corresponds to `ActivityManager.getMemoryClass()`.       | 8.0.0 |
| **`isLowRamDevice`** | <code>boolean</code> | Whether the device is classified as a low-RAM device. On Android this corresponds to `ActivityManager.isLowRamDevice()`. On iOS this is always `false` (Apple does not expose this classification). | 8.0.0 |

#### SystemUptime

System uptime information.

| Prop                | Type                | Description                                                                                                                                                              | Since |
| ------------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----- |
| **`uptimeSeconds`** | <code>number</code> | Time in seconds since the device was last booted. On iOS this corresponds to `ProcessInfo.systemUptime`. On Android this corresponds to `SystemClock.elapsedRealtime()`. | 8.0.0 |

#### AppVersion

Application version information.

| Prop              | Type                | Description                                                                                                                                                      | Since |
| ----------------- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`version`**     | <code>string</code> | Human-readable version string (e.g. "1.2.3"). On iOS this corresponds to `CFBundleShortVersionString`. On Android this corresponds to `PackageInfo.versionName`. | 8.0.0 |
| **`buildNumber`** | <code>number</code> | Numeric build number used for update detection. On iOS this corresponds to `CFBundleVersion`. On Android this corresponds to `PackageInfo.versionCode`.          | 8.0.0 |

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

The listener fires only when the charging state **changes** (e.g. the
charger is connected or disconnected). It is not invoked at subscription
time and emits nothing while the state stays the same; read
`getBatteryInfo()` for the current snapshot instead.

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
