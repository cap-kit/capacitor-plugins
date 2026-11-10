# Configuration

The plugin exposes one optional static option under `plugins.LocalNetwork` in
`capacitor.config.ts` or `capacitor.config.json`.

## Options

| Prop                 | Type      | Default | Description                                                       |
| -------------------- | --------- | ------- | ----------------------------------------------------------------- |
| **`verboseLogging`** | `boolean` | `false` | Enables verbose native logging (Logcat on Android, Xcode on iOS). |

Configuration is read once during plugin initialization and never changes the
JavaScript API shape.

## Example

### `capacitor.config.json`

```json
{
  "plugins": {
    "LocalNetwork": {
      "verboseLogging": true
    }
  }
}
```

### `capacitor.config.ts`

```ts
/// <reference types="@cap-kit/local-network" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    LocalNetwork: {
      verboseLogging: true,
    },
  },
};

export default config;
```

## Android manifest

The plugin declares `android.permission.ACCESS_LOCAL_NETWORK` in its own
manifest, so it is merged into the host app automatically. No manual manifest
edit is required. See [Permissions](permissions.md) for the platform-by-platform
declaration reference.
