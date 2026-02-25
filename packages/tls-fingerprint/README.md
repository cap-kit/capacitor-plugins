<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">TLS Fingerprinting</h3>
<p align="center">
  <strong>
    <code>@cap-kit/tls-fingerprint</code>
  </strong>
</p>

<p align="center">
  Runtime TLS leaf certificate SHA-256 fingerprint validation for Capacitor applications. This plugin establishes a TLS
  connection to a remote HTTPS endpoint, extracts the server’s leaf certificate, computes its SHA-256 fingerprint, and
  compares it against one or more expected fingerprints defined at runtime or via static configuration. It performs
  fingerprint equality validation only and does not override or modify the system trust store.
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/tls-fingerprint">
    <img src="https://img.shields.io/npm/v/@cap-kit/tls-fingerprint?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/tls-fingerprint">
    <img src="https://img.shields.io/npm/dm/@cap-kit/tls-fingerprint?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/tls-fingerprint?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Overview

This Capacitor plugin validates the SHA-256 fingerprint of a server's TLS leaf certificate at runtime.

### What this plugin does

- Extracts the leaf certificate from an HTTPS connection
- Computes its SHA-256 fingerprint
- Compares against expected fingerprints provided at runtime or in static configuration

### What this plugin does NOT do

- It does NOT perform anchor-based certificate pinning
- It does NOT load local certificate files
- It does NOT modify or override the system trust store
- It does NOT validate the certificate chain

### Platform Support

| Platform | Status                                              |
| -------- | --------------------------------------------------- |
| iOS      | Supported                                           |
| Android  | Supported                                           |
| Web      | Unsupported - methods reject with `unimplemented()` |

---

## Install

```bash
pnpm add @cap-kit/tls-fingerprint
# or
npm install @cap-kit/tls-fingerprint
# or
yarn add @cap-kit/tls-fingerprint
# then run:
npx cap sync
```

---

## Obtaining Fingerprints

To use this plugin, you need the SHA-256 fingerprint of the server certificate.

### Method 1 — Using OpenSSL

```bash
openssl x509 -noout -fingerprint -sha256 -inform pem -in /path/to/cert.pem
```

Example output:

```bash
SHA256 Fingerprint=EF:BA:26:D8:C1:CE:37:79:AC:77:63:0A:90:F8:21:63:A3:D6:89:2E:D6:AF:EE:40:86:72:CF:19:EB:A7:A3:62
```

> The plugin normalizes fingerprints to lowercase hex with no separators.
> For example, `EF:BA:26:...` becomes `efba26...`

### Method 2 — Using the Built-in CLI Tool

This project includes a CLI utility to retrieve certificates from remote servers:

```bash
npx cap-kit-tls-fingerprint example.com
```

```bash
npx cap-kit-tls-fingerprint example.com api.example.com --mode multi
```

The CLI is for development-time certificate inspection only. It does not perform runtime validation.

---

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the TLSFingerprint plugin.

| Prop                  | Type                  | Description                                                                                                                                                                                                                              | Default            | Since |
| --------------------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`**  | <code>boolean</code>  | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API. | <code>false</code> | 8.0.0 |
| **`fingerprint`**     | <code>string</code>   | Default fingerprint used by `checkCertificate()` when `options.fingerprint` is not provided at runtime.                                                                                                                                  |                    | 8.0.0 |
| **`fingerprints`**    | <code>string[]</code> | Default fingerprints used by `checkCertificates()` when `options.fingerprints` is not provided at runtime.                                                                                                                               |                    | 8.0.0 |
| **`excludedDomains`** | <code>string[]</code> | Domains to bypass. Matches exact domain or subdomains. Do not include schemes or paths.                                                                                                                                                  |                    | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "TLSFingerprint": {
      "verboseLogging": true,
      "fingerprint": "50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26",
      "fingerprints": [
        "50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26"
      ]
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/tls-fingerprint" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    TLSFingerprint: {
      verboseLogging: true,
      fingerprint: '50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26',
      fingerprints: ['50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26'],
    },
  },
};

export default config;
```

</docgen-config>

> **Note:** All network operations have a 10-second timeout. If the server does not respond within this time, the Promise is rejected with `TLSFingerprintErrorCode.TIMEOUT`.

---

## API

<docgen-index>

- [`checkCertificate(...)`](#checkcertificate)
- [`checkCertificates(...)`](#checkcertificates)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)
- [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

TLS Fingerprint Capacitor Plugin interface.

### checkCertificate(...)

```typescript
checkCertificate(options: TLSFingerprintOptions) => Promise<TLSFingerprintResult>
```

Checks the SSL certificate of a server using a single fingerprint.

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code><a href="#tlsfingerprintoptions">TLSFingerprintOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#tlsfingerprintresult">TLSFingerprintResult</a>&gt;</code>

**Since:** 8.0.0

---

### checkCertificates(...)

```typescript
checkCertificates(options: TLSFingerprintMultiOptions) => Promise<TLSFingerprintResult>
```

Checks the SSL certificate of a server using multiple allowed fingerprints.

| Param         | Type                                                                              |
| ------------- | --------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#tlsfingerprintmultioptions">TLSFingerprintMultiOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#tlsfingerprintresult">TLSFingerprintResult</a>&gt;</code>

**Since:** 8.0.0

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { version } = await TLSFingerprint.getPluginVersion();
```

---

### Interfaces

#### TLSFingerprintResult

Result returned by an TLS fingerprint operation.

This object is returned for ALL outcomes:

- Success: `fingerprintMatched: true`
- Mismatch: `fingerprintMatched: false` with error info (RESOLVED, not rejected)

Only operation failures (invalid input, config missing, network errors,
timeout, internal errors) reject the Promise.

| Prop                     | Type                                                                        | Description                                                                                              |
| ------------------------ | --------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| **`actualFingerprint`**  | <code>string</code>                                                         | The actual SHA-256 fingerprint of the server certificate. Present in fingerprint and excluded modes.     |
| **`fingerprintMatched`** | <code>boolean</code>                                                        | Indicates whether the certificate validation succeeded. - true → Pinning passed - false → Pinning failed |
| **`matchedFingerprint`** | <code>string</code>                                                         | The fingerprint that successfully matched, if any.                                                       |
| **`excludedDomain`**     | <code>boolean</code>                                                        | Indicates that TLS fingerprint was skipped because the request host matched an excluded domain.          |
| **`mode`**               | <code>'fingerprint' \| 'excluded'</code>                                    | Indicates which pinning mode was used. - "fingerprint" - "excluded"                                      |
| **`error`**              | <code>string</code>                                                         | Human-readable error message when pinning fails. Present when `fingerprintMatched: false`.               |
| **`errorCode`**          | <code><a href="#tlsfingerprinterrorcode">TLSFingerprintErrorCode</a></code> | Standardized error code aligned with <a href="#tlsfingerprinterrorcode">TLSFingerprintErrorCode</a>.     |

#### TLSFingerprintOptions

Options for checking a single SSL certificate.

| Prop              | Type                | Description                                                                                                                                                                                                                                        |
| ----------------- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`url`**         | <code>string</code> | HTTPS URL of the server whose SSL certificate must be checked. This value is REQUIRED and cannot be provided via configuration.                                                                                                                    |
| **`fingerprint`** | <code>string</code> | Expected SHA-256 fingerprint of the certificate. Resolution order: 1. `options.fingerprint` (runtime) 2. `plugins.TLSFingerprint.fingerprint` (config) If neither is provided, the Promise is rejected with `TLSFingerprintErrorCode.UNAVAILABLE`. |

#### TLSFingerprintMultiOptions

Options for checking an SSL certificate using multiple allowed fingerprints.

| Prop               | Type                  | Description                                                                                                                                                                                                                                           |
| ------------------ | --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`url`**          | <code>string</code>   | HTTPS URL of the server whose SSL certificate must be checked. This value is REQUIRED and cannot be provided via configuration.                                                                                                                       |
| **`fingerprints`** | <code>string[]</code> | Expected SHA-256 fingerprints of the certificate. Resolution order: 1. `options.fingerprints` (runtime) 2. `plugins.TLSFingerprint.fingerprints` (config) If neither is provided, the Promise is rejected with `TLSFingerprintErrorCode.UNAVAILABLE`. |

#### PluginVersionResult

Result returned by the getPluginVersion method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

### Enums

#### TLSFingerprintErrorCode

| Members                 | Value                            | Description                                                                    |
| ----------------------- | -------------------------------- | ------------------------------------------------------------------------------ |
| **`UNAVAILABLE`**       | <code>'UNAVAILABLE'</code>       | Required data is missing or the feature is not available.                      |
| **`CANCELLED`**         | <code>'CANCELLED'</code>         | The user cancelled an interactive flow.                                        |
| **`PERMISSION_DENIED`** | <code>'PERMISSION_DENIED'</code> | The user denied a required permission or the feature is disabled.              |
| **`INIT_FAILED`**       | <code>'INIT_FAILED'</code>       | The TLS fingerprint operation failed due to a runtime or initialization error. |
| **`INVALID_INPUT`**     | <code>'INVALID_INPUT'</code>     | The input provided to the plugin method is invalid, missing, or malformed.     |
| **`UNKNOWN_TYPE`**      | <code>'UNKNOWN_TYPE'</code>      | Invalid or unsupported input was provided.                                     |
| **`NOT_FOUND`**         | <code>'NOT_FOUND'</code>         | The requested resource does not exist.                                         |
| **`CONFLICT`**          | <code>'CONFLICT'</code>          | The operation conflicts with the current state.                                |
| **`TIMEOUT`**           | <code>'TIMEOUT'</code>           | The operation did not complete within the expected time.                       |
| **`PINNING_FAILED`**    | <code>'PINNING_FAILED'</code>    | The server certificate fingerprint did not match any expected fingerprint.     |
| **`EXCLUDED_DOMAIN`**   | <code>'EXCLUDED_DOMAIN'</code>   | The request host matched an excluded domain.                                   |
| **`NETWORK_ERROR`**     | <code>'NETWORK_ERROR'</code>     | Network connectivity or TLS handshake error.                                   |
| **`SSL_ERROR`**         | <code>'SSL_ERROR'</code>         | SSL/TLS specific error (certificate expired, handshake failure, etc.).         |

</docgen-api>

---

## Security Considerations

This plugin validates fingerprint equality only.

### What this means

- The plugin compares the server's leaf certificate SHA-256 fingerprint against expected values
- It does NOT replace TLS validation
- It does NOT override trust evaluation
- Expired or self-signed certificates will validate if the fingerprint matches

### Limitations

- Fingerprint validation requires active maintenance
- Certificate rotation requires configuration updates
- Misconfiguration may result in loss of network connectivity

This plugin is provided as-is, without warranty. Always test thoroughly before production deployment.

---

## Usage Examples

### Single fingerprint check

```ts
import { TLSFingerprint } from '@cap-kit/tls-fingerprint';

const result = await TLSFingerprint.checkCertificate({
  url: 'https://example.com',
  fingerprint: 'aabbccdd...',
});

if (result.fingerprintMatched) {
  console.log('Certificate is trusted');
} else {
  console.log('Fingerprint mismatch:', result.error);
}
```

### Multiple fingerprints (certificate rotation)

```ts
import { TLSFingerprint } from '@cap-kit/tls-fingerprint';

const result = await TLSFingerprint.checkCertificates({
  url: 'https://example.com',
  fingerprints: ['aabbccdd...', '11223344...'],
});

if (result.fingerprintMatched) {
  console.log('Certificate matched:', result.matchedFingerprint);
}
```

### Using static configuration

```ts
// capacitor.config.ts
plugins: {
  TLSFingerprint: {
    fingerprint: 'aabbccdd...',
    excludedDomains: ['localhost', 'analytics.example.com']
  }
}

// App code
const result = await TLSFingerprint.checkCertificate({
  url: 'https://example.com',
});
```

---

## Contributing

Contributions are welcome. Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## Credits

This plugin is based on prior work from the community and has been refactored for Capacitor v8 and Swift Package Manager compatibility.

Original inspiration:

- [https://github.com/mchl18/Capacitor-SSL-Pinning](https://github.com/mchl18/Capacitor-SSL-Pinning)

---

## License

MIT
