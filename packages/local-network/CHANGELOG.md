# @cap-kit/local-network

## 8.0.0

### Minor Changes

- bb135a3: First stable release of the `@cap-kit/local-network` plugin.

  - **Android:** Reports and requests `android.permission.ACCESS_LOCAL_NETWORK` on Android 17+ (API 37 and above), where local network access requires an explicit runtime grant.
  - **iOS/Web:** Parity contract only — local network access is not a runtime permission, so `checkPermissions()` and `requestPermissions()` report `granted`.
  - **API:** `checkPermissions()`, `requestPermissions()`, `permissionChange` listener, `getPluginVersion()`, and the `LocalNetworkErrorCode` / `LocalNetworkPermissions` / `LocalNetworkPermissionChangeEvent` types.
  - **Config:** Optional `verboseLogging` plugin configuration for native diagnostics.
