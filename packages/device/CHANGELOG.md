# @cap-kit/device

## 8.0.0

### Minor Changes

- Add 7 cross-platform device features: getBatteryExtras, getDisplayInfo, getConfiguration, getPowerState, getMemoryInfo, getSystemUptime, getAppVersion.

  Add CPU usage (delta-based), storage info, and memory pressure to the existing memory API, and complete the web implementations for all methods.

  Add @Serializable data classes for all 9 public methods on Android, replacing untyped Map returns with strongly-typed models via kotlinx.serialization.

  Add `isEstimated` field to MemoryInfo and StorageInfo across all platforms to indicate when values are approximations.

  Add JSDoc platform availability notes (iOS-only, Android-only, web partial/throws) to all new method signatures.

## 8.0.0-next.0

### Minor Changes

- Initial implementation of the Device plugin on Capacitor v8: device identifier and info (including real disk metrics), battery level/status with `batteryChargingStateChange` events, locale helpers and full Web parity.
