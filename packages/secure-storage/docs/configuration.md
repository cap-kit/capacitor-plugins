# SecureStorage — Configuration

The plugin has two configuration layers: static values read natively at
startup, and per-call options passed to `set()`.

## Static configuration

Define under the `SecureStorage` key in `capacitor.config.ts`. Consumed
exclusively by native code during plugin initialization.

```ts
// capacitor.config.ts
const config: CapacitorConfig = {
  plugins: {
    SecureStorage: {
      verboseLogging: false,
      enableICloudKeychainSync: false,
      obfuscateKeys: false,
      obfuscationPrefix: '',
    },
  },
};
```

| Key                         | Type      | Description                                        | Default | Since |
| --------------------------- | --------- | -------------------------------------------------- | ------- | ----- |
| `verboseLogging`            | `boolean` | Extra native logs (Logcat on Android, Xcode/iOS).  | `false` | 8.0.0 |
| `enableICloudKeychainSync`  | `boolean` | iCloud Keychain sync for entries. iOS only.        | `false` | 8.0.0 |
| `obfuscateKeys`             | `boolean` | Base64-encodes stored key names (both tiers).      | `false` | 8.1.0 |
| `obfuscationPrefix`         | `string`  | Prefix prepended before optional encoding.         | `''`    | 8.1.0 |

## Runtime configuration

`configure({ group, obfuscateKeys, obfuscationPrefix })` overrides the
insecure group and the obfuscation settings for the session. Absent
values leave current settings untouched. Precedence: runtime call >
static config > defaults.

## Key-name obfuscation

When `obfuscateKeys` is on, stored names become
`<group-or-service-scope><prefix><base64(key)>`. Obfuscation hides names
from casual inspection — it is **not encryption**. Reads transparently
accept both encoded and plain forms and `keys()` decodes tolerantly, so
toggling the flag never orphans existing entries.

## Per-call options

`set({ key, value, access })` accepts an iOS Keychain accessibility
override:

| `access` value                      | Behavior                                              |
| ----------------------------------- | ----------------------------------------------------- |
| `whenUnlocked`                      | Foreground-only, migrates with encrypted backups.     |
| `whenUnlockedThisDeviceOnly`        | Foreground-only, never migrates.                      |
| `afterFirstUnlock`                  | Background-capable, migrates.                         |
| `afterFirstUnlockThisDeviceOnly`    | Background-capable, never migrates.                   |
| `whenPasscodeSetThisDeviceOnly`     | Requires device passcode, never migrates.             |

Unknown `access` names reject with `INVALID_INPUT`. The value also feeds
`setDefaultKeychainAccess()`, which changes the default for subsequently
stored items (session-scoped). Android and Web ignore both.
