import { registerPlugin } from '@capacitor/core';

/**
 * @file index.ts
 * Entry point for the SSLPinning Capacitor plugin.
 * Registers the plugin and re-exports public API types.
 */

import { SSLPinningPlugin } from './definitions';

/**
 * The SSLPinning plugin instance.
 * It lazily loads the Web implementation when running in a browser.
 */
const SSLPinning = registerPlugin<SSLPinningPlugin>('SSLPinning', {
  web: () => import('./web').then((m) => new m.SSLPinningWeb()),
});

export * from './definitions';
export { SSLPinning };
