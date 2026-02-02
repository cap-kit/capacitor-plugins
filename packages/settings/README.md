<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">Settings</h3>
<p align="center">
  <strong>
    <code>@cap-kit/settings</code>
  </strong>
</p>

<p align="center">
  A <strong>Capacitor plugin</strong> for opening system and application settings on <strong>iOS and Android</strong>.<br>
  Provides a unified, state-based API to navigate users to relevant settings screens,
  including app settings, notifications, connectivity, and other system sections.<br>
  Built following the <strong>Cap-Kit architectural standards</strong> with strict separation
  between JavaScript, bridge, and native implementations (Swift / Kotlin).
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/settings">
    <img src="https://img.shields.io/npm/v/@cap-kit/settings?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/settings">
    <img src="https://img.shields.io/npm/dm/@cap-kit/settings?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/settings?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Install

```bash
pnpm add @cap-kit/settings
npx cap sync

```

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the Settings plugin.

| Prop                 | Type                 | Description                                                                                                                                                                                                                              | Default            | Since |
| -------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API. | <code>false</code> | 1.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "Settings": {
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/settings" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    Settings: {
      verboseLogging: true,
    },
  },
};

export default config;
```

</docgen-config>

## API

<docgen-index>

- [`open(...)`](#open)
- [`openIOS(...)`](#openios)
- [`openAndroid(...)`](#openandroid)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Public JavaScript API for the Settings Capacitor plugin.

This plugin uses a state-based result model:

- operations never throw
- Promise rejection is not used
- failures are reported via `{ success, error?, code? }`

This design ensures consistent behavior across Android, iOS, and Web.

### open(...)

```typescript
open(options: PlatformOptions) => Promise<void>
```

Opens the specified settings option on the current platform.
On Web, this method is not supported.

| Param         | Type                                                        | Description                         |
| ------------- | ----------------------------------------------------------- | ----------------------------------- |
| **`options`** | <code><a href="#platformoptions">PlatformOptions</a></code> | Platform-specific settings options. |

**Since:** 1.0.0

---

### openIOS(...)

```typescript
openIOS(options: IOSOptions) => Promise<void>
```

Opens a specific system settings section. (iOS Only)

| Param         | Type                                              | Description           |
| ------------- | ------------------------------------------------- | --------------------- |
| **`options`** | <code><a href="#iosoptions">IOSOptions</a></code> | iOS settings options. |

**Since:** 1.0.0

---

### openAndroid(...)

```typescript
openAndroid(options: AndroidOptions) => Promise<void>
```

Opens a specific Android Intent. (Android Only)
On Web, this method is not supported.

| Param         | Type                                                      | Description               |
| ------------- | --------------------------------------------------------- | ------------------------- |
| **`options`** | <code><a href="#androidoptions">AndroidOptions</a></code> | Android settings options. |

**Since:** 1.0.0

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 1.0.0

#### Example

```ts
const { version } = await Settings.getPluginVersion();
```

---

### Interfaces

#### PlatformOptions

Platform-specific options for opening system settings.

This interface allows specifying settings options for both
Android and iOS in a single call.

| Prop                | Type                                                        | Description                                                                                                  |
| ------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| **`optionAndroid`** | <code><a href="#androidsettings">AndroidSettings</a></code> | Android settings option to open. Used only when running on Android. Mapped internally to a system Intent.    |
| **`optionIOS`**     | <code><a href="#iossettings">IOSSettings</a></code>         | iOS settings option to open. Used only when running on iOS. Mapped internally to an iOS settings URL scheme. |

#### IOSOptions

iOS-specific options for opening system settings.

| Prop         | Type                                                | Description                                                                                                                                             |
| ------------ | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`option`** | <code><a href="#iossettings">IOSSettings</a></code> | The iOS settings section to open. Most values correspond to internal iOS URL schemes. Availability depends on the iOS version and system configuration. |

#### AndroidOptions

Android-specific options for opening system settings.

| Prop         | Type                                                        | Description                                                                                                                                       |
| ------------ | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`option`** | <code><a href="#androidsettings">AndroidSettings</a></code> | The Android settings section to open. Each value maps to a specific Android system Intent. Support varies depending on the device and OS version. |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

### Enums

#### AndroidSettings

| Members                      | Value                                   | Description                                                                                              |
| ---------------------------- | --------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| **`Accessibility`**          | <code>'accessibility'</code>            | Opens Accessibility settings.                                                                            |
| **`Account`**                | <code>'account'</code>                  | Opens the Add Account screen.                                                                            |
| **`AirplaneMode`**           | <code>'airplane_mode'</code>            | Opens Airplane Mode settings.                                                                            |
| **`Apn`**                    | <code>'apn'</code>                      | Opens Access Point Name (APN) settings.                                                                  |
| **`ApplicationDetails`**     | <code>'application_details'</code>      | Opens the Application Details screen for the current app.                                                |
| **`ApplicationDevelopment`** | <code>'application_development'</code>  | Opens Application Development settings. Availability depends on developer options being enabled.         |
| **`Application`**            | <code>'application'</code>              | Opens Application settings.                                                                              |
| **`AppNotification`**        | <code>'app_notification'</code>         | Opens app-specific notification settings.                                                                |
| **`BatteryOptimization`**    | <code>'battery_optimization'</code>     | Opens Battery Optimization settings. Allows managing apps excluded from battery optimizations.           |
| **`Bluetooth`**              | <code>'bluetooth'</code>                | Opens Bluetooth settings.                                                                                |
| **`Captioning`**             | <code>'captioning'</code>               | Opens Captioning settings.                                                                               |
| **`Cast`**                   | <code>'cast'</code>                     | Opens Cast device settings.                                                                              |
| **`DataRoaming`**            | <code>'data_roaming'</code>             | Opens Data Roaming settings.                                                                             |
| **`Date`**                   | <code>'date'</code>                     | Opens Date & Time settings.                                                                              |
| **`Display`**                | <code>'display'</code>                  | Opens Display settings.                                                                                  |
| **`Dream`**                  | <code>'dream'</code>                    | Opens Dream (Daydream / Screensaver) settings.                                                           |
| **`Home`**                   | <code>'home'</code>                     | Opens Home app selection settings.                                                                       |
| **`Keyboard`**               | <code>'keyboard'</code>                 | Opens Input Method (Keyboard) settings.                                                                  |
| **`KeyboardSubType`**        | <code>'keyboard_subtype'</code>         | Opens Input Method Subtype settings.                                                                     |
| **`Locale`**                 | <code>'locale'</code>                   | Opens Language & Input (Locale) settings.                                                                |
| **`Location`**               | <code>'location'</code>                 | Opens Location Services settings.                                                                        |
| **`ManageApplications`**     | <code>'manage_applications'</code>      | Opens Manage Applications settings.                                                                      |
| **`ManageAllApplications`**  | <code>'manage_all_applications'</code>  | Opens Manage All Applications settings. Availability depends on Android version and OEM.                 |
| **`MemoryCard`**             | <code>'memory_card'</code>              | Show settings for memory card storage                                                                    |
| **`Network`**                | <code>'network'</code>                  | Opens Network Operator settings.                                                                         |
| **`Nfc`**                    | <code>'nfc'</code>                      | Opens NFC settings.                                                                                      |
| **`NfcSharing`**             | <code>'nfcsharing'</code>               | Opens NFC Sharing settings.                                                                              |
| **`NfcPayment`**             | <code>'nfc_payment'</code>              | Opens NFC Payment settings.                                                                              |
| **`Print`**                  | <code>'print'</code>                    | Opens Print settings.                                                                                    |
| **`Privacy`**                | <code>'privacy'</code>                  | Opens Privacy settings.                                                                                  |
| **`QuickLaunch`**            | <code>'quick_launch'</code>             | Opens Quick Launch settings.                                                                             |
| **`Search`**                 | <code>'search'</code>                   | Opens Search settings.                                                                                   |
| **`Security`**               | <code>'security'</code>                 | Opens Security settings.                                                                                 |
| **`Settings`**               | <code>'settings'</code>                 | Opens the main System Settings screen.                                                                   |
| **`ShowRegulatoryInfo`**     | <code>'show_regulatory_info'</code>     | Opens Regulatory Information screen.                                                                     |
| **`Sound`**                  | <code>'sound'</code>                    | Opens Sound & Volume settings.                                                                           |
| **`Storage`**                | <code>'storage'</code>                  | Opens Internal Storage settings.                                                                         |
| **`Sync`**                   | <code>'sync'</code>                     | Opens Sync settings.                                                                                     |
| **`TextToSpeech`**           | <code>'text_to_speech'</code>           | Opens Text-to-Speech (TTS) settings. Uses a non-public intent on some devices.                           |
| **`Usage`**                  | <code>'usage'</code>                    | Opens Usage Access settings. Allows managing apps with access to usage data.                             |
| **`UserDictionary`**         | <code>'user_dictionary'</code>          | Opens User Dictionary settings.                                                                          |
| **`VoiceInput`**             | <code>'voice_input'</code>              | Opens Voice Input settings.                                                                              |
| **`VPN`**                    | <code>'vpn'</code>                      | Opens VPN settings.                                                                                      |
| **`Wifi`**                   | <code>'wifi'</code>                     | Opens Wi-Fi settings.                                                                                    |
| **`WifiIp`**                 | <code>'wifi_ip'</code>                  | Opens Wi-Fi IP settings. Availability varies by device and Android version.                              |
| **`Wireless`**               | <code>'wireless'</code>                 | Opens Wireless & Networks settings.                                                                      |
| **`ZenMode`**                | <code>'zen_mode'</code>                 | Opens Zen Mode (Do Not Disturb) settings. This uses a non-public intent and may not work on all devices. |
| **`ZenModePriority`**        | <code>'zen_mode_priority'</code>        | Opens Zen Mode Priority settings.                                                                        |
| **`ZenModeBlockedEffects`**  | <code>'zen_mode_blocked_effects'</code> | Opens Zen Mode Blocked Effects settings.                                                                 |

#### IOSSettings

| Members                        | Value                                   | Description                                                                                                                                                   |
| ------------------------------ | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`App`**                      | <code>'app'</code>                      | Opens the app-specific settings screen. This is the ONLY settings destination officially supported by Apple and is considered App Store safe.                 |
| **`AppNotification`**          | <code>'appNotification'</code>          | Opens the app-specific notification settings. - iOS 16+: opens the dedicated notification settings screen - iOS &lt;16: falls back to the app settings screen |
| **`About`**                    | <code>'about'</code>                    | Opens iOS "About" settings.                                                                                                                                   |
| **`AutoLock`**                 | <code>'autoLock'</code>                 | Opens Auto-Lock settings.                                                                                                                                     |
| **`Bluetooth`**                | <code>'bluetooth'</code>                | Opens Bluetooth settings.                                                                                                                                     |
| **`DateTime`**                 | <code>'dateTime'</code>                 | Opens Date & Time settings.                                                                                                                                   |
| **`FaceTime`**                 | <code>'facetime'</code>                 | Opens FaceTime settings.                                                                                                                                      |
| **`General`**                  | <code>'general'</code>                  | Opens General settings.                                                                                                                                       |
| **`Keyboard`**                 | <code>'keyboard'</code>                 | Opens Keyboard settings.                                                                                                                                      |
| **`ICloud`**                   | <code>'iCloud'</code>                   | Opens iCloud settings.                                                                                                                                        |
| **`ICloudStorageBackup`**      | <code>'iCloudStorageBackup'</code>      | Opens iCloud Storage & Backup settings.                                                                                                                       |
| **`International`**            | <code>'international'</code>            | Opens Language & Region (International) settings.                                                                                                             |
| **`LocationServices`**         | <code>'locationServices'</code>         | Opens Location Services settings.                                                                                                                             |
| **`Music`**                    | <code>'music'</code>                    | Opens Music settings.                                                                                                                                         |
| **`Notes`**                    | <code>'notes'</code>                    | Opens Notes settings.                                                                                                                                         |
| **`Notifications`**            | <code>'notifications'</code>            | Opens Notifications settings. Note: this is the global notifications screen, not app-specific notifications.                                                  |
| **`Phone`**                    | <code>'phone'</code>                    | Opens Phone settings.                                                                                                                                         |
| **`Photos`**                   | <code>'photos'</code>                   | Opens Photos settings.                                                                                                                                        |
| **`ManagedConfigurationList`** | <code>'managedConfigurationList'</code> | Opens Managed Configuration profiles list.                                                                                                                    |
| **`Reset`**                    | <code>'reset'</code>                    | Opens Reset settings.                                                                                                                                         |
| **`Ringtone`**                 | <code>'ringtone'</code>                 | Opens Ringtone settings.                                                                                                                                      |
| **`Sounds`**                   | <code>'sounds'</code>                   | Opens Sounds settings.                                                                                                                                        |
| **`SoftwareUpdate`**           | <code>'softwareUpdate'</code>           | Opens Software Update settings.                                                                                                                               |
| **`Store`**                    | <code>'store'</code>                    | Opens App Store settings.                                                                                                                                     |
| **`Tracking`**                 | <code>'tracking'</code>                 | Opens App Tracking Transparency settings. Available on iOS 14+.                                                                                               |
| **`Wallpaper`**                | <code>'wallpaper'</code>                | Opens Wallpaper settings.                                                                                                                                     |
| **`WiFi`**                     | <code>'wifi'</code>                     | Opens Wi-Fi settings.                                                                                                                                         |
| **`Tethering`**                | <code>'tethering'</code>                | Opens Personal Hotspot (Tethering) settings.                                                                                                                  |
| **`DoNotDisturb`**             | <code>'doNotDisturb'</code>             | Opens Do Not Disturb settings.                                                                                                                                |
| **`TouchIdPasscode`**          | <code>'touchIdPasscode'</code>          | Opens Touch ID / Passcode settings.                                                                                                                           |
| **`ScreenTime`**               | <code>'screenTime'</code>               | Opens Screen Time settings.                                                                                                                                   |
| **`Accessibility`**            | <code>'accessibility'</code>            | Opens Accessibility settings.                                                                                                                                 |
| **`VPN`**                      | <code>'vpn'</code>                      | Opens VPN settings.                                                                                                                                           |

</docgen-api>

---

## Platform limitations

### iOS

Apple officially supports opening only the app-specific settings screen.
Other settings destinations rely on undocumented URL schemes and may change
or be restricted by future iOS versions or App Store review policies.

### Android

Some Android system settings are not guaranteed to be available on all devices.

Certain options (such as Zen Mode / Do Not Disturb related settings) rely on
device-specific or undocumented system intents. Availability may vary depending on:

- Android version
- device manufacturer (OEM)
- system configuration or user restrictions

When a requested settings screen is not supported on the current device,
the plugin will return a structured result with:

```ts
{
  success: false,
  code: 'UNAVAILABLE'
}
```

This behavior is intentional and aligns with real-world Android platform constraints.

For additional context, see the discussion in the original implementation:
[https://github.com/RaphaelWoude/capacitor-native-settings/pull/63](https://github.com/RaphaelWoude/capacitor-native-settings/pull/63)

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## Credits

This plugin is based on prior work from the Community and
has been refactored and modernized for **Capacitor v8** and
**Swift Package Manager** compatibility.

Original inspiration:

- [https://github.com/RaphaelWoude/capacitor-native-settings](https://github.com/RaphaelWoude/capacitor-native-settings)

---

## License

MIT
