import { registerPlugin } from '@capacitor/core';

/**
 * @file index.ts
 * Entry point for the TLSFingerprint Capacitor plugin.
 * Registers the plugin and re-exports public API types.
 */

import { TLSFingerprintPlugin } from './definitions';

/**
 * The TLSFingerprint plugin instance.
 * It lazily loads the Web implementation when running in a browser.
 */
const TLSFingerprint = registerPlugin<TLSFingerprintPlugin>('TLSFingerprint', {
  web: () => import('./web').then((m) => new m.TLSFingerprintWeb()),
});

export * from './definitions';
export { TLSFingerprint };
