# TLS Fingerprint — Usage Guide

This guide covers how to obtain server certificate fingerprints and how to use the plugin in your application.

> **Note:** All network operations have a 10-second timeout. If the server does not respond within this time, the Promise is rejected with `TLSFingerprintErrorCode.TIMEOUT`.

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
