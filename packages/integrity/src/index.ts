/**
 * Import the `registerPlugin` method from the Capacitor core library.
 * This method is used to register a custom plugin.
 */
import { registerPlugin } from '@capacitor/core';

/**
 * @file index.ts
 * Main entry point for the Integrity Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { IntegrityPlugin } from './definitions';

/**
 * The Integrity plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all ssl pinning functionality.
 */
const Integrity = registerPlugin<IntegrityPlugin>('Integrity', {
  web: () => import('./web').then((m) => new m.IntegrityWeb()),
});

export * from './definitions';
export { Integrity };
