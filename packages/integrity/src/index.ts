import { registerPlugin } from '@capacitor/core';

/**
 * @file index.ts
 * Entry point for the Integrity Capacitor plugin.
 * Registers the plugin with Capacitor and re-exports public types.
 */

import { IntegrityPlugin } from './definitions';

/**
 * The Integrity plugin instance.
 * It lazy-loads the Web implementation when running in a browser.
 */
const Integrity = registerPlugin<IntegrityPlugin>('Integrity', {
  web: () => import('./web').then((m) => new m.IntegrityWeb()),
});

export * from './definitions';
export { Integrity };
