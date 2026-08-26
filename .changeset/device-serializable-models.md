---
"@cap-kit/device": minor
---

Add @Serializable data classes for all 9 new methods on Android, replacing untyped Map returns with strongly-typed models via kotlinx.serialization.

Add `isEstimated` field to MemoryInfo and StorageInfo across all platforms to indicate when values are approximations.

Complete web implementations for getBatteryExtras, getMemoryInfo, getStorageInfo, getPowerState, getSystemUptime.

Add JSDoc platform availability notes (iOS-only, Android-only, web partial/throws) to all new method signatures.
