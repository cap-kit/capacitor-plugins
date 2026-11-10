# Biometrics — Usage Guide

## Quick start

```bash
pnpm add @cap-kit/biometrics
npx cap sync
```

```ts
import { Biometrics } from '@cap-kit/biometrics';

const { isAvailable } = await Biometrics.isAvailable();
const { isEnrolled } = await Biometrics.isEnrolled();
if (isAvailable && isEnrolled) {
  await Biometrics.authenticate({ title: 'Authentication Required' });
}
```

## Capability checks first

Never prompt blindly. Query the device before showing UI:

```ts
const { isAvailable } = await Biometrics.isAvailable(); // hardware usable?
const { isEnrolled } = await Biometrics.isEnrolled(); // user enrolled?
const { biometryType } = await Biometrics.getBiometricType(); // display only
const { biometryTypes } = await Biometrics.getBiometricTypes(); // hardware set
const { hasDeviceCredential } = await Biometrics.hasDeviceCredential();
const { isDeviceSecure } = await Biometrics.isDeviceSecure(); // any lock-screen credential set?
const { strengthLevel } = await Biometrics.getBiometricStrengthLevel(); // strong | weak | none
```

Decide with `isAvailable`, never with the type: the type is a display
helper, availability is the gate.

## Authentication flows

```ts
try {
  await Biometrics.authenticate({
    title: 'Authentication Required',
    subtitle: 'Please authenticate to continue',
    cancelButtonText: 'Cancel',
    iosFallbackButtonText: 'Use Passcode',
    allowDeviceCredential: true,
  });
  // Identity proven.
} catch (error) {
  // CANCELLED (user dismissed) or UNAVAILABLE (no usable authenticator).
}

// Dismiss an in-flight prompt, e.g. on navigation away.
await Biometrics.cancelAuthentication();
```

After a success, `getAuthenticationType()` reports which credential
satisfied the ceremony (`biometric`, `deviceCredential`, or `unknown`
when the platform does not disclose it).

## Enrollment

```ts
// Android only (API 30+): opens the system enrollment screen.
await Biometrics.enroll();
```

iOS and Web reject with `UNAVAILABLE` — guide those users to system
settings instead of retrying.

## Web platform

Browsers run the ceremony through WebAuthn against a locally created
platform credential. Availability reflects the platform authenticator;
`enroll()` is unavailable and the authentication type is always
`unknown`. Suitable for development and low-risk flows, not as a
hardware-backed guarantee.

## Demo app

A working demo lives in `example-app/static/src/app/biometrics/`:
capability cards with refresh, an authenticate card with live prompt
options, enrollment, version readout, and an event log. Apply it to the
shared demo via `pnpm run app:demo:prepare` from the package root.
