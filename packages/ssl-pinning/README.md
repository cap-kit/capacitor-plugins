<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">SSL Pinning</h3>

<p align="center">
  <strong>
    <code>@cap-kit/ssl-pinning</code>
  </strong>
</p>

<p align="center">Runtime SSL certificate verification for Capacitor applications.</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/ssl-pinning">
    <img src="https://img.shields.io/npm/v/@cap-kit/ssl-pinning?style=flat-square" />
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/ssl-pinning">
    <img src="https://img.shields.io/npm/dm/@cap-kit/ssl-pinning?style=flat-square" />
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/ssl-pinning">
    <img src="https://img.shields.io/npm/l/@cap-kit/ssl-pinning?style=flat-square" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" />
</p>

<br />

## Overview

`@cap-kit/ssl-pinning` is a Capacitor plugin that performs **runtime SSL certificate
fingerprint verification** on **iOS and Android** by validating the
**SHA-256 fingerprint** of a server certificate during the TLS handshake.

The plugin is designed to:

- validate a single fingerprint using `checkCertificate`
- validate multiple fingerprints using `checkCertificates`
- support configuration-based defaults via `capacitor.config.ts`
- keep the JavaScript API platform-agnostic and stable

By enforcing explicit trust decisions, SSL pinning helps protect against
**man-in-the-middle attacks**.

This plugin performs **certificate pinning only**.
It does **not** expose full X.509 certificate inspection APIs at runtime,
does **not replace TLS**, and does **not guarantee absolute security**.

SSL pinning is a **defense-in-depth mechanism** and must be maintained correctly
over time.

---

## Platform Support

| Platform | Supported |
| -------- | --------- |
| iOS      | ✅        |
| Android  | ✅        |
| Web      | ❌        |

On the Web, SSL certificate inspection is not possible due to browser security
restrictions. All methods will reject with an `Unimplemented` error.

---

## Permissions

### Android

The plugin declares the `INTERNET` permission in its manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

This is a **normal permission** (no runtime prompt required). The app will
automatically be granted this permission at install time. No user interaction
or runtime permission request is needed.

---

## Install

```bash
pnpm add @cap-kit/ssl-pinning
# or
npm install @cap-kit/ssl-pinning
# or
yarn add @cap-kit/ssl-pinning
# then run:
npx cap sync
```

---

## 🔑 Obtaining SSL Certificate Fingerprints

To use SSL pinning, you must obtain the **SHA-256 fingerprint**
of the SSL certificate you intend to trust.

A fingerprint is a cryptographic hash of the server certificate and is used
to uniquely identify it during the TLS handshake.

Fingerprints can be obtained using **standard tools** or via the
**built-in CLI utility provided by this project**.

---

### Method 1 — Using a Web Browser

1. Open the target website in a modern browser.
2. View the website certificate details.
3. Export or copy the certificate (public key).
4. Use a fingerprint generator, for example:
   https://www.samltool.com/fingerprint.php
5. Select **SHA-256** as the algorithm.
6. Copy the resulting fingerprint.

---

### Method 2 — Using OpenSSL

If you have access to the certificate file (`.pem`, `.cer`, `.crt`):

```bash
openssl x509 -noout -fingerprint -sha256 -inform pem -in /path/to/cert.pem
```

Example output:

```bash
SHA256 Fingerprint=EF:BA:26:D8:C1:CE:37:79:AC:77:63:0A:90:F8:21:63:A3:D6:89:2E:D6:AF:EE:40:86:72:CF:19:EB:A7:A3:62
```

> **Canonical format**: The plugin normalizes fingerprints to lowercase hex with no separators.
> For example, `EF:BA:26:...` becomes `efba26...`. Use this format when configuring fingerprints.

---

### Method 3 — Using the Built-in CLI Tool

This project includes a **command-line utility** that can retrieve
SSL certificates from remote servers and generate SHA-256 fingerprints.

The CLI is intended to be used **at development time**.
It **does not perform SSL pinning** and does **not make trust decisions at runtime**.

Its purpose is to:

- inspect server certificates
- extract SHA-256 fingerprints
- assist in preparing configuration values for the runtime plugin

#### Basic usage

> Note  
> The CLI is exposed via the `cap-kit-ssl-pinning` command.  
> Any internal script names shown in the help output (e.g. `fingerprint.js`)
> are implementation details and should not be invoked directly.

```bash
npx cap-kit-ssl-pinning example.com
```

#### Multiple domains

```bash
npx cap-kit-ssl-pinning example.com api.example.com
```

#### Modes

The `--mode` flag controls how fingerprints are grouped:

- `single`
  Generates a single `fingerprint` value.

- `multi`
  Generates a `fingerprints[]` array (useful for certificate rotation).

```bash
npx cap-kit-ssl-pinning example.com api.example.com --mode multi
```

> ⚠️ Note
>
> When using `--mode multi` with multiple domains, each domain is processed independently.
>
> If a certificate cannot be retrieved for a specific domain (e.g. DNS error or TLS failure),
> the error is reported in the output, but the CLI continues processing the remaining domains.
>
> The resulting `fingerprints` array will include **only successfully retrieved certificates**.

#### Output formats

The `--format` flag controls the output structure:

| Format             | Description                                    |
| ------------------ | ---------------------------------------------- |
| `json`             | Full certificate information (default)         |
| `fingerprints`     | JavaScript array of fingerprints               |
| `capacitor`        | Full `plugins` block for `capacitor.config.ts` |
| `capacitor-plugin` | `SSLPinning` config block only                 |
| `capacitor-json`   | JSON-compatible Capacitor configuration        |

Example:

```bash
npx cap-kit-ssl-pinning example.com \
  --mode multi \
  --format capacitor
```

Output:

```ts
plugins: {
  SSLPinning: {
    fingerprints: ['AA:BB:CC:DD:...', '11:22:33:44:...'];
  }
}
```

Example (plugin block only):

```bash
npx cap-kit-ssl-pinning example.com api.example.com \
  --mode multi \
  --format capacitor-plugin
```

Output:

```ts
SSLPinning: {
  fingerprints: ['AA:BB:CC:DD:...', '11:22:33:44:...'];
}
```

#### Insecure mode

By default, the CLI allows retrieving certificates even if the TLS
chain is invalid.

To enforce certificate validation:

```bash
npx cap-kit-ssl-pinning example.com --insecure=false
```

---

### Notes

- Fingerprints are normalized internally by the runtime plugin.
- Uppercase/lowercase differences are ignored.
- Colon separators (`:`) are optional.

The CLI **generates configuration**.
The runtime plugin **consumes it**.

---

### Important

SSL pinning requires **active maintenance**.

If a certificate expires or is rotated and the fingerprint is not updated,
network requests will fail until the configuration is corrected.

Always verify fingerprints in a controlled environment before releasing
to production.

---

## Configuration

Static configuration can be provided in `capacitor.config.ts` under
`plugins.SSLPinning`.

These values are:

- read natively at build/runtime
- not accessible from JavaScript at runtime
- treated as read-only fallback configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the SSLPinning plugin.

| Prop                  | Type                  | Description                                                                                                                 | Default            | Since  |
| --------------------- | --------------------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------ | ------ |
| **`verboseLogging`**  | <code>boolean</code>  | Enables verbose native logging (Logcat / Xcode console).                                                                    | <code>false</code> | 0.0.15 |
| **`fingerprint`**     | <code>string</code>   | Default fingerprint used by `checkCertificate()` when `options.fingerprint` is not provided at runtime.                     |                    | 0.0.14 |
| **`fingerprints`**    | <code>string[]</code> | Default fingerprints used by `checkCertificates()` when `options.fingerprints` is not provided at runtime.                  |                    | 0.0.15 |
| **`certs`**           | <code>string[]</code> | Local certificate filenames (e.g., ["mycert.cer"]). Files must be in 'assets/certs' (Android) or main bundle 'certs' (iOS). |                    | 8.0.3  |
| **`excludedDomains`** | <code>string[]</code> | Domains to bypass. Matches exact domain or subdomains. Do not include schemes or paths.                                     |                    | 8.0.3  |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "SSLPinning": {
      "verboseLogging": false,
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
/// <reference types="@cap-kit/ssl-pinning" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    SSLPinning: {
      verboseLogging: false,
      fingerprint: '50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26',
      fingerprints: ['50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26'],
    },
  },
};

export default config;
```

</docgen-config>

### Configuration precedence

For each method call, fingerprint resolution follows this order:

1. Runtime options passed from JavaScript
2. Static configuration from `capacitor.config.ts`

If no fingerprint is available from either source, the call will fail.

### Excluded Domains

When a domain is listed in `excludedDomains`, the plugin:

- **Bypasses SSL pinning** for that domain
- **Performs a real TLS handshake** using the device's system trust store
- **Returns a resolved result** with:
  - `fingerprintMatched: true`
  - `excludedDomain: true`
  - `mode: "excluded"`
  - `actualFingerprint: "..."` (the server's real certificate fingerprint)
  - `errorCode: "EXCLUDED_DOMAIN"`
  - `error: "Excluded domain"`

This allows trusted system CAs to validate the connection while still logging the actual certificate in use.

Example:

```ts
const result = await SSLPinning.checkCertificate({
  url: 'https://analytics.example.com',
});

if (result.excludedDomain) {
  console.log('Domain excluded, using system trust');
  console.log('Server fingerprint:', result.actualFingerprint);
}
```

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

SSL Pinning Capacitor Plugin interface.

### checkCertificate(...)

```typescript
checkCertificate(options: SSLPinningOptions) => Promise<SSLPinningResult>
```

Checks the SSL certificate of a server using a single fingerprint.

| Param         | Type                                                            |
| ------------- | --------------------------------------------------------------- |
| **`options`** | <code><a href="#sslpinningoptions">SSLPinningOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#sslpinningresult">SSLPinningResult</a>&gt;</code>

**Since:** 0.0.14

---

### checkCertificates(...)

```typescript
checkCertificates(options: SSLPinningMultiOptions) => Promise<SSLPinningResult>
```

Checks the SSL certificate of a server using multiple allowed fingerprints.

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code><a href="#sslpinningmultioptions">SSLPinningMultiOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#sslpinningresult">SSLPinningResult</a>&gt;</code>

**Since:** 0.0.15

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 0.0.15

#### Example

```ts
const { version } = await SSLPinning.getPluginVersion();
```

---

### Interfaces

#### SSLPinningResult

Result returned by an SSL pinning operation.

This object is returned for ALL outcomes:

- Success: `fingerprintMatched: true`
- Mismatch: `fingerprintMatched: false` with error info (RESOLVED, not rejected)

Only operation failures (invalid input, config missing, network errors,
timeout, internal errors) reject the Promise.

| Prop                     | Type                                                                | Description                                                                                              |
| ------------------------ | ------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| **`actualFingerprint`**  | <code>string</code>                                                 | The actual SHA-256 fingerprint of the server certificate. Present in fingerprint mode.                   |
| **`fingerprintMatched`** | <code>boolean</code>                                                | Indicates whether the certificate validation succeeded. - true → Pinning passed - false → Pinning failed |
| **`matchedFingerprint`** | <code>string</code>                                                 | The fingerprint that successfully matched, if any.                                                       |
| **`excludedDomain`**     | <code>boolean</code>                                                | Indicates that SSL pinning was skipped because the request host matched an excluded domain.              |
| **`mode`**               | <code>'fingerprint' \| 'cert' \| 'excluded'</code>                  | Indicates which pinning mode was used. - "fingerprint" - "cert" - "excluded"                             |
| **`error`**              | <code>string</code>                                                 | Human-readable error message when pinning fails. Present when `fingerprintMatched: false`.               |
| **`errorCode`**          | <code><a href="#sslpinningerrorcode">SSLPinningErrorCode</a></code> | Standardized error code aligned with <a href="#sslpinningerrorcode">SSLPinningErrorCode</a>.             |

#### SSLPinningOptions

Options for checking a single SSL certificate.

| Prop              | Type                | Description                                                                                                                                                                                                                                |
| ----------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **`url`**         | <code>string</code> | HTTPS URL of the server whose SSL certificate must be checked. This value is REQUIRED and cannot be provided via configuration.                                                                                                            |
| **`fingerprint`** | <code>string</code> | Expected SHA-256 fingerprint of the certificate. Resolution order: 1. `options.fingerprint` (runtime) 2. `plugins.SSLPinning.fingerprint` (config) If neither is provided, the Promise is rejected with `SSLPinningErrorCode.UNAVAILABLE`. |

#### SSLPinningMultiOptions

Options for checking an SSL certificate using multiple allowed fingerprints.

| Prop               | Type                  | Description                                                                                                                                                                                                                                   |
| ------------------ | --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`url`**          | <code>string</code>   | HTTPS URL of the server whose SSL certificate must be checked. This value is REQUIRED and cannot be provided via configuration.                                                                                                               |
| **`fingerprints`** | <code>string[]</code> | Expected SHA-256 fingerprints of the certificate. Resolution order: 1. `options.fingerprints` (runtime) 2. `plugins.SSLPinning.fingerprints` (config) If neither is provided, the Promise is rejected with `SSLPinningErrorCode.UNAVAILABLE`. |

#### PluginVersionResult

Result returned by the getPluginVersion method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

### Enums

#### SSLPinningErrorCode

| Members                       | Value                                  | Description                                                                           |
| ----------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------- |
| **`UNAVAILABLE`**             | <code>'UNAVAILABLE'</code>             | Required data is missing or the feature is not available.                             |
| **`CANCELLED`**               | <code>'CANCELLED'</code>               | The user cancelled an interactive flow.                                               |
| **`PERMISSION_DENIED`**       | <code>'PERMISSION_DENIED'</code>       | The user denied a required permission or the feature is disabled.                     |
| **`INIT_FAILED`**             | <code>'INIT_FAILED'</code>             | The SSL pinning operation failed due to a runtime or initialization error.            |
| **`INVALID_INPUT`**           | <code>'INVALID_INPUT'</code>           | The input provided to the plugin method is invalid, missing, or malformed.            |
| **`UNKNOWN_TYPE`**            | <code>'UNKNOWN_TYPE'</code>            | Invalid or unsupported input was provided.                                            |
| **`NOT_FOUND`**               | <code>'NOT_FOUND'</code>               | The requested resource does not exist.                                                |
| **`CONFLICT`**                | <code>'CONFLICT'</code>                | The operation conflicts with the current state.                                       |
| **`TIMEOUT`**                 | <code>'TIMEOUT'</code>                 | The operation did not complete within the expected time.                              |
| **`NO_PINNING_CONFIG`**       | <code>'NO_PINNING_CONFIG'</code>       | No runtime fingerprints, no config fingerprints, and no certificates were configured. |
| **`CERT_NOT_FOUND`**          | <code>'CERT_NOT_FOUND'</code>          | Certificate-based pinning was selected, but no valid certificate files were found.    |
| **`TRUST_EVALUATION_FAILED`** | <code>'TRUST_EVALUATION_FAILED'</code> | Certificate-based trust evaluation failed at the handshake level.                     |
| **`PINNING_FAILED`**          | <code>'PINNING_FAILED'</code>          | The server certificate fingerprint did not match any expected fingerprint.            |
| **`EXCLUDED_DOMAIN`**         | <code>'EXCLUDED_DOMAIN'</code>         | The request host matched an excluded domain.                                          |
| **`NETWORK_ERROR`**           | <code>'NETWORK_ERROR'</code>           | Network connectivity or TLS handshake error.                                          |

</docgen-api>

---

## Usage

### Single fingerprint check

```ts
import { SSLPinning } from '@cap-kit/ssl-pinning';

const result = await SSLPinning.checkCertificate({
  url: 'https://example.com',
  fingerprint: 'AA:BB:CC:DD:...',
});

if (result.fingerprintMatched) {
  console.log('Certificate is trusted');
} else {
  // Fingerprint mismatch - Promise resolved with details
  console.log('Pinning failed:', result.errorCode, result.error);
}
```

### Multiple fingerprint check

```ts
import { SSLPinning } from '@cap-kit/ssl-pinning';

const result = await SSLPinning.checkCertificates({
  url: 'https://example.com',
  fingerprints: ['AA:BB:CC:DD:...', '11:22:33:44:...'],
});

if (result.fingerprintMatched) {
  console.log('Certificate matched:', result.matchedFingerprint);
} else {
  // Fingerprint mismatch - Promise resolved with details
  console.log('Pinning failed:', result.errorCode, result.error);
}
```

---

## Result Object

The Promise always resolves with an `SSLPinningResult` object:

```ts
interface SSLPinningResult {
  // Always present
  fingerprintMatched: boolean;

  // Present on success or mismatch
  actualFingerprint?: string;
  matchedFingerprint?: string;
  mode?: 'fingerprint' | 'cert' | 'excluded';

  // Present when fingerprintMatched is false
  error?: string;
  errorCode?: 'PINNING_FAILED' | 'TRUST_EVALUATION_FAILED';

  // Present when domain is excluded
  excludedDomain?: boolean;
}
```

---

## Error Handling Model (Important)

This plugin uses a **hybrid error model**:

- **Fingerprint mismatch**: Promise resolves with `fingerprintMatched: false` and error details
- **Operation failures**: Promise rejects with `CapacitorException`

### Fingerprint Mismatch (Resolved)

When the server certificate fingerprint does not match any expected fingerprint,
the Promise **resolves** (not rejects) with:

```ts
{
  fingerprintMatched: false,
  error: "Pinning failed",
  errorCode: "PINNING_FAILED",
  actualFingerprint: "...",
  mode: "fingerprint"
}
```

This allows you to handle the mismatch gracefully without try/catch:

```ts
const result = await SSLPinning.checkCertificate({
  url: 'https://example.com',
  fingerprint: 'AA:BB:CC:DD:...',
});

if (result.fingerprintMatched) {
  console.log('Certificate is trusted');
} else {
  console.log('Pinning failed:', result.errorCode, result.error);
}
```

### Operation Failures (Rejected)

Only the following scenarios **reject** the Promise:

- Missing URL
- Missing fingerprint (runtime and config)
- Invalid URL format
- Network/timeout errors
- Internal errors

```ts
import { SSLPinning, SSLPinningErrorCode } from '@cap-kit/ssl-pinning';

try {
  await SSLPinning.checkCertificate({
    url: 'https://example.com',
  });
} catch (err: any) {
  if (err.code === SSLPinningErrorCode.UNAVAILABLE) {
    console.error('No fingerprint provided');
  } else {
    console.error('SSL pinning operation failed', err);
  }
}
```

### Error codes

| Code                      | Description                                                                             |
| ------------------------- | --------------------------------------------------------------------------------------- |
| `PINNING_FAILED`          | Certificate fingerprint did not match any expected fingerprint (resolved, not rejected) |
| `EXCLUDED_DOMAIN`         | Request matched an excluded domain; pinning bypassed, system trust used (resolved)      |
| `TRUST_EVALUATION_FAILED` | Certificate-based trust evaluation failed                                               |
| `TIMEOUT`                 | Connection timed out                                                                    |
| `NETWORK_ERROR`           | Network connectivity or IO error                                                        |
| `UNAVAILABLE`             | No fingerprint provided (runtime or config)                                             |
| `PERMISSION_DENIED`       | Required permission denied                                                              |
| `INIT_FAILED`             | Runtime or initialization failure                                                       |
| `UNKNOWN_TYPE`            | Invalid or unsupported input                                                            |

---

## Security Notes

- Only HTTPS URLs are accepted.
- The system trust chain is **not** evaluated.
- Certificate acceptance is based **solely on fingerprint matching**.

- SSL pinning **requires ongoing maintenance**.
  Certificates that expire or are rotated **must be updated** before they become invalid.
- Incorrect configuration may result in **loss of network connectivity**.
- SSL pinning can interfere with debugging tools and HTTPS traffic inspection.
- This plugin is provided **as-is**, without warranty.

Always test thoroughly in your target environment.

---

## Cross-Platform Nominal Payload

This section documents the nominal payload that the bridges return on both Android and iOS after the Task 7 changes. It is intended for developers integrating the plugin from JavaScript and should remain free of autogenerated docgen blocks.

- Payload shape (fields present on both platforms):
- actualFingerprint: string | null
- fingerprintMatched: boolean
- matchedFingerprint: string | null
- excludedDomain: boolean | null
- mode: "fingerprint" | "cert" | "excluded" | null
- error: string | null
- errorCode: string | null

- Semantics:
- Excluded Domain: resolved result with excludedDomain = true and fingerprintMatched = true; errorCode = EXCLUDED_DOMAIN; error = "Excluded domain"; not rejected.
- Fingerprint/Cert: results return fingerprintMatched = true with actualFingerprint; errorCode/error empty on success, or PINNING_FAILED on mismatch.
- Failures: non-excluded errors are delivered via rejection with canonical error codes (TIMEOUT, NETWORK_ERROR, INVALID_INPUT, INIT_FAILED, TRUST_EVALUATION_FAILED, etc.).

- Examples:

```ts
// Successful fingerprint check
{
  actualFingerprint: "AB:CD:...",
  fingerprintMatched: true,
  mode: "fingerprint",
  error: "",
  errorCode: ""
}
```

```ts
// Excluded domain, system trust handshake
{
  actualFingerprint: "AB:CD:...",
  fingerprintMatched: true,
  excludedDomain: true,
  mode: "excluded",
  errorCode: "EXCLUDED_DOMAIN",
  error: "Excluded domain"
}
```

```ts
// Timeout
{
  fingerprintMatched: false,
  errorCode: "TIMEOUT",
  error: "Timeout"
}
```

- Implementation notes:
- Android bridge returns SSLPinningResultModel mapped to a JSObject with the fields above.
- iOS payload keys align with the same fields and canonical error messages.

---

## Contributing

Contributions are welcome.

Please read the [CONTRIBUTING.md](CONTRIBUTING.md) file before submitting
a pull request.

---

## Credits

This plugin is based on prior work from the Community and
has been refactored and modernized for **Capacitor v8** and
**Swift Package Manager** compatibility.

Original inspiration:

- [https://github.com/mchl18/Capacitor-SSL-Pinning](https://github.com/mchl18/Capacitor-SSL-Pinning)

---

## License

MIT
