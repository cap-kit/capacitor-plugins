# People — Usage Guide

This guide covers how to use the People plugin: the Promise-based API paradigm, correct and incorrect usage patterns, and standardized error handling with `PeopleErrorCode`.

## Correct Usage

All People plugin APIs are based on Promise and follow the standard Capacitor v8 reject paradigm. Use `try / catch` to handle native cancellations and errors.

```ts
import { People, PeopleErrorCode } from '@cap-kit/people';

try {
  const { contact } = await People.pickContact({
    projection: ['name', 'phones', 'emails'],
  });
  console.log('Picked contact:', contact);
} catch (err: any) {
  if (err.code === PeopleErrorCode.CANCELLED) {
    // User canceled selection
    console.log('Picker cancelled');
  } else {
    console.error('Error:', err.message);
  }
}
```

## Incorrect Usage

Do not use checks based on the `success` property in the result, as the methods reject the Promise on error.

```ts
// ❌ DO NOT DO THIS
const result = await People.pickContact();
if (result.success) { ... }
```

## Error Handling

All People plugin methods can reject the Promise if they fail. It is recommended to handle standardized error codes using `PeopleErrorCode`.

### Error Codes

All error codes are standardized and exposed via `PeopleErrorCode`:

- `UNAVAILABLE` – Feature not available or OS limitation
- `CANCELLED` – User cancelled an interactive flow (e.g., contact picker)
- `PERMISSION_DENIED` – Permission denied or restricted
- `INIT_FAILED` – Internal initialization or processing failure
- `INVALID_INPUT` – Invalid, missing, or malformed input
- `UNKNOWN_TYPE` – Invalid or unsupported projection/type

These codes are consistent across **iOS**, **Android**, and **Web**.

### Example

```ts
import { People, PeopleErrorCode } from '@cap-kit/people';

try {
  const { contact } = await People.pickContact();
  console.log('Contact selected:', contact);
} catch (err: any) {
  switch (err.code) {
    case PeopleErrorCode.CANCELLED:
      // The user canceled the selection
      console.log('Picker cancelled by user');
      break;

    case PeopleErrorCode.UNAVAILABLE:
      // The user canceled the selection or the picker is not available
      console.log('Picker unavailable or cancelled by user');
      break;

    case PeopleErrorCode.PERMISSION_DENIED:
      // User has denied access to contacts (for methods that require permissions)
      console.error('Permission to access contacts was denied');
      break;

    case PeopleErrorCode.INIT_FAILED:
      // Internal error while processing native data
      console.error('Native initialization or processing failure');
      break;

    default:
      // Generic or unexpected error
      console.error('An unexpected error occurred:', err.message);
      break;
  }
}
```
