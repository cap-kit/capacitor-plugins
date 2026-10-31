# Local Network — Usage Guide

## Quick start

```bash
pnpm add @cap-kit/local-network
npx cap sync
```

```ts
import { LocalNetwork } from '@cap-kit/local-network';

const status = await LocalNetwork.checkPermissions();

if (status.localNetwork !== 'granted') {
  const result = await LocalNetwork.requestPermissions();
  console.log(result.localNetwork); // 'granted' | 'denied' | 'prompt' | 'prompt-with-rationale'
}
```

## Reading the state

`checkPermissions()` never shows a system prompt. Use it to decide whether
local network access is already allowed:

```ts
const { localNetwork } = await LocalNetwork.checkPermissions();

switch (localNetwork) {
  case 'granted':
    // Local network access is allowed.
    break;
  case 'denied':
    // Refused permanently (or blocked by the OS): guide the user to system settings.
    break;
  case 'prompt-with-rationale':
    // Refused once: asking again shows the system prompt.
    break;
  case 'prompt':
    // Undecided: asking will show the system prompt.
    break;
}
```

## Requesting access

`requestPermissions()` shows the system prompt when the decision is still
pending and resolves with the resulting state:

```ts
const { localNetwork } = await LocalNetwork.requestPermissions();

if (localNetwork !== 'granted') {
  // Denial is state, not an exception. Never retry in a loop.
}
```

A denied decision is reported as state. Rejections are reserved for real
errors, which carry a stable `LocalNetworkErrorCode` in `error.code`.

## Reacting to changes

The permission can change while the app is in the background. Listen for
`permissionChange` instead of polling:

```ts
const handle = await LocalNetwork.addListener('permissionChange', (event) => {
  console.log('Local network permission:', event.localNetwork);
});

// Later:
await handle.remove();
```

`removeAllListeners()` drops every listener registered by the plugin.

## Platform behavior

| Platform              | `checkPermissions()` | `requestPermissions()`                 | Prompt                                                                |
| --------------------- | -------------------- | -------------------------------------- | --------------------------------------------------------------------- |
| Android 17+ (API 37+) | Real runtime state   | Shows the system prompt when undecided | Yes                                                                   |
| Android < 17          | `granted`            | `granted`                              | Never                                                                 |
| iOS                   | `granted`            | `granted`                              | System privacy prompt on first local network access, outside this API |
| Web                   | `granted`            | `granted`                              | Never                                                                 |

iOS presents its own local network privacy prompt the first time the app
touches the local network. The platform exposes no API to query or trigger it,
so this plugin reports `granted` and never claims otherwise. Declaring
`NSLocalNetworkUsageDescription` and `NSBonjourServices` is the app's
responsibility — see [Permissions](permissions.md).

## Demo app

A working demo lives in `example-app/static/src/app/local-network/`: a
permission card with refresh and request actions, a live event log, and the
plugin version readout. Apply it to the shared demo via
`pnpm run app:demo:prepare` from the package root.
