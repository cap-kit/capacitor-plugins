# PrivacyScreen — Usage Guide

## Quick start

```bash
pnpm add @cap-kit/privacy-screen
npx cap sync
```

```ts
import { PrivacyScreen } from '@cap-kit/privacy-screen';

// Protect everything by default, then open holes only where needed.
await PrivacyScreen.enable();
```

## Basic control

```ts
// Enable with default settings
await PrivacyScreen.enable();

// Enable with platform-specific behavior
await PrivacyScreen.enable({
  android: {
    dimBackground: true,
    privacyModeOnActivityHidden: 'splash',
  },
  ios: {
    blurEffect: 'dark',
  },
});

// Disable for public screens
await PrivacyScreen.disable();

// Read the current state
const { enabled } = await PrivacyScreen.isEnabled();
```

## Per-screen protection

The recommended pattern: enable when entering a sensitive screen, disable
when leaving it. Re-enable on every navigation into secure content,
including back navigation — the plugin cannot know your router state.

```ts
async function navigateToSecureScreen() {
  await PrivacyScreen.enable({
    android: { dimBackground: true },
    ios: { blurEffect: 'dark' },
  });
  // Navigate to your secure screen
}

async function navigateToPublicScreen() {
  await PrivacyScreen.disable();
  // Navigate to your public screen
}
```

## Screenshot detection

```ts
const handle = await PrivacyScreen.addListener('screenshotTaken', () => {
  console.log('Screenshot taken (observed after the fact)');
});

// Later
await handle.remove();
// or remove everything at once
await PrivacyScreen.removeAllListeners();
```

The event fires _after_ the screenshot exists, so it can trigger incident
response (logging, session review) but never prevention. On iOS it is
delivered via the system screenshot notification; on Android it requires
API 34+ and active protection.

## Web platform

The web implementation tracks the enabled state for API parity but the
browser offers no OS-level snapshot protection, so `enable()` is a no-op
visually and `screenshotTaken` never fires. Do not rely on it for real
protection; gate sensitive web views behind authentication instead.

## Demo app

A working demo lives in `example-app/static/src/app/privacy-screen/`:
status badge with refresh, enable with live option controls, disable,
version readout, and an event log wired to `screenshotTaken`. Apply it to
the shared demo via `pnpm run app:demo:prepare` from the package root.
