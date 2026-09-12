# Settings — Platform Limitations

## iOS

Apple officially supports opening only the app-specific settings screen.
Other settings destinations rely on undocumented URL schemes and may change
or be restricted by future iOS versions or App Store review policies.

## Android

Some Android system settings are not guaranteed to be available on all devices.

Certain options (such as Zen Mode / Do Not Disturb related settings) rely on
device-specific or undocumented system intents. Availability may vary depending on:

- Android version
- device manufacturer (OEM)
- system configuration or user restrictions

When a requested settings screen is not supported on the current device,
the plugin rejects the Promise with the error code:

- `UNAVAILABLE`

Consumers are expected to handle this case using standard `try / catch`
error handling.

This behavior is intentional and aligns with real-world Android platform constraints.

For historical context on Android intent limitations, see the discussion in the
original implementation:
[https://github.com/RaphaelWoude/capacitor-native-settings/pull/63](https://github.com/RaphaelWoude/capacitor-native-settings/pull/63)
Note that this plugin does not use the state-based result model found in that implementation.
