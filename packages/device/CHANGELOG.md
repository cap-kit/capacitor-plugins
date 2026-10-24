# @cap-kit/device

## 8.1.1

### Patch Changes

- ccead2e: Fix `getId()` on iOS so it honours the published `DeviceId` contract and matches the Android and web implementations:

  - The vendor identifier is now returned under the `identifier` key. Previously iOS returned `uid`, so `Device.getId().identifier` resolved to `undefined` on iOS.
  - The promise now rejects with the `UNAVAILABLE` error code when the vendor identifier cannot be read, instead of silently resolving an empty string.

## 8.1.0

### Minor Changes

- bdd5a6d: Bump minor versions for all published plugins.

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
