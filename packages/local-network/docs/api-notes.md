# API notes

## Return shapes

Both permission methods resolve to the same shape:

```ts
interface LocalNetworkPermissions {
  localNetwork: PermissionState; // 'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'
}
```

The property is declared explicitly so consumers can use dot access even with
`noPropertyAccessFromIndexSignature` enabled.

`getPluginVersion()` resolves to:

```ts
interface PluginVersionResult {
  version: string; // native version on Android/iOS, JS package version on web
}
```

On Android and iOS the value is the native version injected from
`package.json` at build time. On the web there is no native implementation, so
the same field carries the JavaScript package version.

## Error codes

Rejections carry a stable code matching `LocalNetworkErrorCode`:

| Code                | Meaning                                                         |
| ------------------- | --------------------------------------------------------------- |
| `UNAVAILABLE`       | The capability is not available on this device or platform.     |
| `CANCELLED`         | The user cancelled an interactive flow.                         |
| `PERMISSION_DENIED` | The permission was denied or the feature is disabled by the OS. |
| `INIT_FAILED`       | The plugin failed to initialize or perform an operation.        |
| `INVALID_INPUT`     | The input is invalid, missing, or malformed.                    |
| `UNKNOWN_TYPE`      | The requested type is not valid or supported.                   |
| `NOT_FOUND`         | The requested resource does not exist.                          |
| `CONFLICT`          | The operation conflicts with the current state.                 |
| `TIMEOUT`           | The operation did not complete within the expected time.        |

A denied permission decision is **not** an error: it is reported as state
(`denied` or `prompt-with-rationale`) in the resolved result.

## Edge cases

- **Android below API 37** — `PackageManager` does not know the permission.
  Both methods short-circuit to `granted` without touching it.
- **Android state mapping** — Capacitor reports `prompt-with-rationale` after a
  first refusal, when the system prompt can still be shown again, and `denied`
  only when the permission is permanently refused or blocked by the OS.
  Returning from system settings with the permission enabled emits
  `permissionChange`.
- **Event duplication** — the plugin keeps the last reported state and only
  emits `permissionChange` when it actually changed.
- **No listeners** — `permissionChange` is only emitted when at least one
  listener is registered.
- **Web** — the browser exposes no local network permission; the web
  implementation always reports `granted`.
