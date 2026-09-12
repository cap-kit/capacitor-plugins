# Rank — Usage Guide

This guide covers best practices for requesting in-app reviews and how to handle errors returned by the Rank plugin.

## Best Practices

- Call `requestReview()` only after a **positive user interaction**
  (e.g. completed task, successful checkout, achieved milestone).
- Avoid calling the review prompt on app startup or without user context.
- Always check availability first:

```ts
const { value } = await Rank.isAvailable();
if (value) {
  await Rank.requestReview();
}
```

- Use `fireAndForget: true` only when you do not need to track completion
  and want to avoid blocking UI flows.

## Error Handling

All Rank plugin methods return Promises and may reject in case of failure.
Consumers should always handle errors using `try / catch`.

### Example

```ts
import { Rank, RankErrorCode } from '@cap-kit/rank';

try {
  await Rank.requestReview();
} catch (err: any) {
  switch (err.code) {
    case RankErrorCode.UNAVAILABLE:
      // Feature not supported on this device or platform
      break;

    case RankErrorCode.INIT_FAILED:
      // Native initialization or runtime failure
      break;

    default:
      // Unknown or unexpected error
      console.error(err.message);
  }
}
```

### Error Codes

The following error codes may be returned by the plugin:

- `UNAVAILABLE` — The feature is not supported on the current device or platform
- `PERMISSION_DENIED` — A required permission was denied (platform-dependent)
- `INIT_FAILED` — Native initialization or runtime failure
- `UNKNOWN_TYPE` — Invalid or unsupported input
