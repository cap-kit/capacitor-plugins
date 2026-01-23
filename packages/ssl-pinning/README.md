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
verification** by validating the **SHA-256 fingerprint** of a server certificate.

The plugin works on **iOS and Android** and is designed to:

- validate a single fingerprint (`checkCertificate`)
- validate multiple fingerprints (`checkCertificates`)
- support configuration-based defaults via `capacitor.config.ts`
- keep the JavaScript API platform-agnostic and stable

This plugin **does not replace TLS** and **does not guarantee absolute security**.
It enforces explicit trust decisions on top of standard TLS.
It is a defense-in-depth mechanism that must be maintained correctly.

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

Fingerprints can be generated using standard tools or via the
companion CLI utility provided by this project.

---

### Method 1 — Using a Web Browser

1. Open the target website in a modern browser.
2. View the website certificate details.
3. Export or copy the certificate (public key).
4. Use an online fingerprint generator, for example:
   https://www.samltool.com/fingerprint.php
5. Select **SHA-256** as the algorithm.
6. Copy the resulting fingerprint (colon-separated format is acceptable).

---

### Method 2 — Using the Command Line (OpenSSL)

If you have access to the certificate file (`.pem`, `.cer`, `.crt`):

```bash
openssl x509 -noout -fingerprint -sha256 -inform pem -in /path/to/cert.pem
```

Example output:

```bash
SHA256 Fingerprint=EF:BA:26:D8:C1:CE:37:79:AC:77:63:0A:90:F8:21:63:A3:D6:89:2E:D6:AF:EE:40:86:72:CF:19:EB:A7:A3:62
```

---

### Method 3 — Using the Built-in CLI Tool

This project includes a CLI utility that can retrieve and display
SSL certificate information for one or more domains.

The CLI is intended as a **development and configuration helper**.
It **does not perform SSL pinning** and does **not enforce trust decisions**
at runtime.

Its purpose is to:

- inspect server certificates
- extract SHA-256 fingerprints
- assist in preparing configuration values for the runtime plugin

---

#### Installation & Usage

You don't need to install the tool globally. You can run it directly using your package manager if the plugin is installed in your project.

**Using pnpm (Recommended):**

```bash
pnpm exec ssl-fingerprint example.com
```

**Using npx:**

```bash
npx ssl-fingerprint example.com
```

If you prefer a global installation:

```bash
npm install -g @cap-kit/ssl-pinning
# or
yarn global add @cap-kit/ssl-pinning
# then run:
ssl-fingerprint example.com
```

---

#### Usage

```bash
# Using npx
npx ssl-fingerprint example.com

# Multiple domains
npx ssl-fingerprint example.com example.org example.net

# If installed globally
ssl-fingerprint example.com
```

---

#### Modes

The `--mode` flag controls how fingerprints are organized:

- `--mode single`
  Generates a single `fingerprint` value.

- `--mode multi`
  Generates a `fingerprints[]` array (useful for certificate rotation).

Example:

```bash
npx ssl-fingerprint example.com api.example.com --mode multi
```

---

You can also save the output to a file:

```bash
ssl-fingerprint example.com --out certs.json
ssl-fingerprint example.com example.org example.net --out certs.json
```

Generate output in TypeScript format (useful for configuration files):

```bash
ssl-fingerprint example.com --out fingerprints.ts --format fingerprints
```

---

#### Formats

The `--format` flag controls the output structure:

- `json`
  Raw certificate information (default)

- `fingerprints`
  TypeScript array of fingerprints

- `capacitor`
  Full `plugins` block for `capacitor.config.ts`

- `capacitor-plugin`
  Only the `SSLPinning` plugin block

- `capacitor-json`
  JSON equivalent of the Capacitor configuration

Example:

```bash
npx ssl-fingerprint example.com \
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

---

#### Notes

- Fingerprints are normalized internally by the runtime plugin.
- Uppercase/lowercase differences are ignored.
- Colons (`:`) are optional.

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

The plugin supports static configuration via `capacitor.config.ts`.
Configuration values are read **natively at runtime** and are not accessed from JavaScript.

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the SSLPinning plugin.

| Prop                 | Type                  | Description                                                                                                                | Default                | Since  |
| -------------------- | --------------------- | -------------------------------------------------------------------------------------------------------------------------- | ---------------------- | ------ |
| **`verboseLogging`** | <code>boolean</code>  | Enables detailed logging in the native console (Logcat/Xcode). Useful for debugging sensor data flow and lifecycle events. | <code>false</code>     | 0.0.15 |
| **`fingerprint`**    | <code>string</code>   | Default fingerprint used by checkCertificate() if no arguments are provided.                                               | <code>undefined</code> | 0.0.14 |
| **`fingerprints`**   | <code>string[]</code> | Default fingerprints used by checkCertificates() if no arguments are provided.                                             | <code>undefined</code> | 0.0.15 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "SSLPinning": {
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
/// <reference types="@cap-kit/ssl-pinning" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    SSLPinning: {
      verboseLogging: true,
      fingerprint: '50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26',
      fingerprints: ['50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26'],
    },
  },
};

export default config;
```

</docgen-config>

---

## 🔐 Pinning Mode (Runtime Behavior)

This version of the plugin supports **fingerprint-based SSL pinning only**.

### How it works

At runtime, the plugin:

1. Opens a TLS connection to the target HTTPS endpoint
2. Extracts the **leaf SSL certificate** presented by the server
3. Computes its **SHA-256 fingerprint**
4. Compares it against the expected fingerprint(s)

The certificate is considered trusted if **any fingerprint matches**.

---

### Supported pinning modes

| Mode                | Supported |
| ------------------- | --------- |
| Fingerprint pinning | ✅ Yes    |
| Certificate pinning | ❌ No     |
| Excluded domains    | ❌ No     |

---

## 🛡️ Security Model

This plugin mitigates **man-in-the-middle (MITM) attacks**
by enforcing explicit trust decisions at the TLS layer
using certificate fingerprint validation.

### Security guarantees

When correctly configured:

- The connection is accepted **only if** the server certificate
  fingerprint matches one of the expected values.
- Trust decisions are performed **natively at runtime**,
  outside of JavaScript control.
- Certificate validation fails **closed** by default.

---

### Limitations

This plugin intentionally:

- Validates **only the leaf certificate**
- Does **not** evaluate the full trust chain
- Does **not** check certificate revocation (CRL / OCSP)
- Does **not** protect against compromised application binaries

It is a **defense-in-depth control** and must be combined
with proper TLS configuration and backend security practices.

---

### Operational requirements

Applications using this plugin must:

- Monitor certificate expiration dates
- Update fingerprints **before certificate rotation**
- Test pinning behavior before production releases

Incorrect configuration may result in **loss of connectivity** by design.

---

### Priority rules

Fingerprint values are selected in the following order:

1. Fingerprints passed at runtime (`checkCertificate` / `checkCertificates`)
2. Fingerprints defined in `capacitor.config.ts`

If no fingerprints are available, the operation fails.

There is **no automatic fallback**.

---

## 🔧 Error Handling Model (Important)

> ⚠️ **Read this before using `checkCertificate()` or `checkCertificates()`**

This plugin uses a **state-based error model**, not exception-based errors.

### Why?

- On **iOS (Capacitor 8 + Swift Package Manager)**, native Promise rejection
  is not reliably available.
- To guarantee **cross-platform consistency**, the plugin always
  **resolves Promises** and returns an explicit result object.

### What this means

- Promises are **never rejected** by the plugin.
  This behavior applies to **all platforms** to ensure API consistency.
- Errors are reported as part of the resolved result.
- Consumers must **always inspect the returned object**, not rely on `catch()`.

### Error fields

When an error occurs, the result object may include:

- `fingerprintMatched: false`
- `error`: a human-readable diagnostic message
- `errorCode`: a standardized error identifier

Example:

```ts
const result = await SSLPinning.checkCertificate();

if (!result.fingerprintMatched) {
  console.error(result.error, result.errorCode);
}
```

### Standardized error codes

The `errorCode` field (when present) is one of the following:

- `UNAVAILABLE` – the feature is not available at runtime
- `PERMISSION_DENIED` – access was denied by the system or configuration
- `INIT_FAILED` – initialization or runtime failure occurred
- `UNKNOWN_TYPE` – unsupported or invalid pinning configuration

These codes are intended for **programmatic handling**, not for user-facing messages.

---

## API

<docgen-index>

- [`checkCertificate()`](#checkcertificate)
- [`checkCertificate(...)`](#checkcertificate)
- [`checkCertificates()`](#checkcertificates)
- [`checkCertificates(...)`](#checkcertificates)
- [`getPluginVersion()`](#getpluginversion)
- [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Interface defining the structure of an SSL Certificate Checker Plugin.

Implementations of this interface should provide the logic for checking
the status and details of an SSL certificate based on the provided options.

### checkCertificate()

```typescript
checkCertificate() => Promise<SSLPinningResult>
```

Check the SSL certificate of a server.

**Returns:** <code>Promise&lt;<a href="#sslpinningresult">SSLPinningResult</a>&gt;</code>

**Since:** 0.0.14

#### Example

```typescript
import { SSLPinning } from '@cap-kit/ssl-pinning';

const result = await SSLPinning.checkCertificate({
  url: 'https://example.com',
  fingerprint: '50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26',
});

console.log('SSL Pinning Result:', result);
```

---

### checkCertificate(...)

```typescript
checkCertificate(options: SSLPinningOptions) => Promise<SSLPinningResult>
```

Check the SSL certificate of a server.

| Param         | Type                                                            | Description                                 |
| ------------- | --------------------------------------------------------------- | ------------------------------------------- |
| **`options`** | <code><a href="#sslpinningoptions">SSLPinningOptions</a></code> | - Options for checking the SSL certificate. |

**Returns:** <code>Promise&lt;<a href="#sslpinningresult">SSLPinningResult</a>&gt;</code>

**Since:** 0.0.14

#### Example

```typescript
import { SSLPinning } from '@cap-kit/ssl-pinning';

const result = await SSLPinning.checkCertificate({
  url: 'https://example.com',
  fingerprint: '50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26',
});

console.log('SSL Pinning Result:', result);
```

---

### checkCertificates()

```typescript
checkCertificates() => Promise<SSLPinningResult[]>
```

Check the SSL certificates of multiple servers.

**Returns:** <code>Promise&lt;SSLPinningResult[]&gt;</code>

**Since:** 0.0.15

#### Example

```typescript
import { SSLPinning } from '@cap-kit/ssl-pinning';

const results = await SSLPinning.checkCertificates();

results.forEach((result) => {
  console.log('SSL Pinning Result:', result);
});
```

---

### checkCertificates(...)

```typescript
checkCertificates(options: SSLPinningMultiOptions[]) => Promise<SSLPinningResult[]>
```

Check the SSL certificates of multiple servers.

| Param         | Type                                  | Description                                  |
| ------------- | ------------------------------------- | -------------------------------------------- |
| **`options`** | <code>SSLPinningMultiOptions[]</code> | - Options for checking the SSL certificates. |

**Returns:** <code>Promise&lt;SSLPinningResult[]&gt;</code>

**Since:** 0.0.15

#### Example

```typescript
import { SSLPinning } from '@cap-kit/ssl-pinning';

const results = await SSLPinning.checkCertificates([
  {
    url: 'https://example.com',
    fingerprints: ['50:4B:A1:B5:48:96:71:F3:9F:87:7E:0A:09:FD:3E:1B:C0:4F:AA:9F:FC:83:3E:A9:3A:00:78:88:F8:BA:60:26'],
  },
  {
    url: 'https://another-example.com',
    fingerprints: ['AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90'],
  },
]);

results.forEach((result) => {
  console.log('SSL Pinning Result:', result);
});
```

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

Result returned by the SSL certificate check.

NOTE:
On iOS (Swift Package Manager), errors are returned
as part of the resolved result object rather than
Promise rejections.

| Prop                      | Type                 | Description                                                                                                                                                           |
| ------------------------- | -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`subject`**             | <code>string</code>  | The subject of the certificate, representing the entity the certificate is issued to.                                                                                 |
| **`issuer`**              | <code>string</code>  | The issuer of the certificate, indicating the certificate authority that issued it. Results may vary slightly between iOS and Android platforms.                      |
| **`validFrom`**           | <code>string</code>  | The start date from which the certificate is valid. Format: ISO 8601 string or platform-specific date representation.                                                 |
| **`validTo`**             | <code>string</code>  | The end date until which the certificate is valid. Format: ISO 8601 string or platform-specific date representation.                                                  |
| **`expectedFingerprint`** | <code>string</code>  | The fingerprint that is expected to match the certificate's actual fingerprint. This is typically provided in the <a href="#sslpinningoptions">SSLPinningOptions</a>. |
| **`actualFingerprint`**   | <code>string</code>  | The actual fingerprint of the SSL certificate retrieved from the server.                                                                                              |
| **`fingerprintMatched`**  | <code>boolean</code> | Indicates whether the actual fingerprint matches the expected fingerprint. `true` if they match, `false` otherwise.                                                   |
| **`error`**               | <code>string</code>  | A descriptive error message if an issue occurred during the SSL certificate check.                                                                                    |

#### SSLPinningOptions

Options for checking a single SSL certificate.

| Prop              | Type                | Description                                                                                                           |
| ----------------- | ------------------- | --------------------------------------------------------------------------------------------------------------------- |
| **`url`**         | <code>string</code> | The URL of the server whose SSL certificate needs to be checked.                                                      |
| **`fingerprint`** | <code>string</code> | The expected fingerprint of the SSL certificate to validate against. This is typically a hash string such as SHA-256. |

#### SSLPinningMultiOptions

Options for checking multiple SSL certificates.

| Prop               | Type                  | Description                                                                                                                       |
| ------------------ | --------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **`url`**          | <code>string</code>   | The URL of the server whose SSL certificate needs to be checked.                                                                  |
| **`fingerprints`** | <code>string[]</code> | The expected fingerprints of the SSL certificate to validate against. This is typically an array of hash strings such as SHA-256. |

#### PluginVersionResult

Result returned by the getPluginVersion method.

| Prop          | Type                | Description                              |
| ------------- | ------------------- | ---------------------------------------- |
| **`version`** | <code>string</code> | The native version string of the plugin. |

</docgen-api>

---

## Usage

### Single fingerprint

```ts
import { SSLPinning } from '@cap-kit/ssl-pinning';

const result = await SSLPinning.checkCertificate({
  url: 'https://example.com',
  fingerprint: 'AA:BB:CC:DD:...',
});

if (result.fingerprintMatched) {
  // Certificate is trusted
}
```

### Multiple fingerprints

```ts
const result = await SSLPinning.checkCertificates({
  url: 'https://example.com',
  fingerprints: ['AA:BB:CC:DD:...', '11:22:33:44:...'],
});

if (result.fingerprintMatched) {
  // At least one fingerprint matched
}
```

When no fingerprints are passed at runtime, the plugin will use the values
defined in `capacitor.config.ts`.

---

## Security Notes

- SSL pinning **requires maintenance**. Expired or rotated certificates
  must be updated before they become invalid.
- Incorrect configuration can result in **loss of connectivity**.
- SSL pinning may interfere with debugging tools and traffic inspection.
- This plugin is provided **as-is**, without warranty.

Always test thoroughly in your target environment.

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
