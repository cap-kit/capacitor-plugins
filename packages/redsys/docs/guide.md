# Redsys — Usage Guide

This guide covers the payment flows, HMAC signature generation, error handling, and response structure of the Redsys plugin.

> **Note:** This plugin requires official Redsys InApp SDK credentials and configuration.

## What the plugin provides

`@cap-kit/redsys` is a Capacitor v8 plugin that bridges the official Redsys InApp SDK for iOS and Android.

It provides:

- A **platform-agnostic JavaScript API**
- **Native SDK orchestration**
- **Unified response structure** across platforms
- **Standardized error handling**
- **Built-in HMAC signature utilities**
- Optional UI customization via `capacitor.config.ts`

The plugin follows a strict layered architecture:

- **Bridge Layer** → Handles Capacitor calls
- **Implementation Layer** → Orchestrates native SDK
- **Utils Layer** → Mapping & crypto utilities
- **Config Layer** → Immutable runtime configuration

## Supported Flows

### Direct Payment

Fully native card entry flow handled by the Redsys SDK.

```ts
await Redsys.doDirectPayment({
  order: 'ORDER123',
  amount: 25.5,
  transactionType: 'normal',
});
```

### Web Payment Workflow (3-D Secure)

The WebView payment flow requires a server-side signature to ensure transaction integrity. The process follows these steps:

1. **Initialize**: Call `initializeWebPayment()` to get the `base64Data`.
2. **Sign**: Send `base64Data` to your backend. Your server signs it using your **Merchant Secret Key**.
3. **Process**: Call `processWebPayment({ signature: '...' })` with the signature from your server.

## Signature Generation (HMAC)

The signature must be an **HMAC-SHA256** (or SHA512) hash.

> **⚠️ Security Warning**
>
> **Never store your Merchant Secret Key inside the mobile application.** Doing so exposes your credentials to decompilation. Always generate signatures on a secure backend.

### Using `computeHash` (Testing Only)

For development and rapid prototyping, the plugin provides a native helper to generate signatures locally:

```ts
import { Redsys, RedsysTransactionType } from '@cap-kit/redsys';

// 1. Get transaction data
const { base64Data } = await Redsys.initializeWebPayment({
  order: 'ORDER123',
  amount: 25.5,
  transactionType: RedsysTransactionType.Normal,
});

// 2. Generate hash locally (ONLY FOR TESTING)
const { signature } = await Redsys.computeHash({
  data: base64Data,
  keyBase64: 'YOUR_TEST_SECRET_KEY', // Base64 encoded key
  algorithm: 'HMAC_SHA256_V1',
});

// 3. Open the secure WebView
const result = await Redsys.processWebPayment({ signature });
```

### Production Implementation (Recommended)

In production, your backend should perform the signature calculation following the official Redsys logic:

1. Base64 decode your Merchant Key.
2. Generate a derived key using **3DES** with the `order` number.
3. Compute the **HMAC-SHA256** of the `base64Data` using that derived key.
4. Base64 encode the final result.

## Error Handling

The plugin uses standardized error codes across platforms:

| Code                | Description                         |
| ------------------- | ----------------------------------- |
| `UNAVAILABLE`       | Native SDK unavailable              |
| `PERMISSION_DENIED` | Required permission missing         |
| `INIT_FAILED`       | SDK initialization failure          |
| `UNKNOWN_TYPE`      | Invalid transaction type            |
| `CRYPTO_ERROR`      | Signature computation failure       |
| `SDK_ERROR`         | Native Redsys SDK returned an error |

SDK errors include additional metadata when available.

## Response Structure

All successful payment responses return a normalized structure:

```ts
{
  code: number,
  amount: string,
  currency: string,
  order: string,
  merchantCode: string,
  terminal: string,
  responseCode: string,
  authorisationCode: string,
  transactionType: string,
  securePayment: string,
  signature: string,
  cardNumber: string,
  cardBrand: string,
  cardCountry: string,
  cardType: string,
  expiryDate: string,
  merchantIdentifier: string,
  consumerLanguage: string,
  date: string,
  hour: string,
  merchantData: string
}
```

Card numbers are automatically masked before being returned to JavaScript.

## Platform Requirements

- Capacitor v8
- iOS: Xcode 26 (Swift Package Manager)
- Android: Manual integration of Redsys InApp SDK 2.4.5 via local Maven repository (persistent)
