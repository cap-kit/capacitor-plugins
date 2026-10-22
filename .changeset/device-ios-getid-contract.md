---
"@cap-kit/device": patch
---

Fix `getId()` on iOS so it honours the published `DeviceId` contract and matches the Android and web implementations:

- The vendor identifier is now returned under the `identifier` key. Previously iOS returned `uid`, so `Device.getId().identifier` resolved to `undefined` on iOS.
- The promise now rejects with the `UNAVAILABLE` error code when the vendor identifier cannot be read, instead of silently resolving an empty string.
