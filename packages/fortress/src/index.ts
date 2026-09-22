/**
 * @file index.ts
 * This file exports the Fortress and registers it with Capacitor.
 * It acts as the main entry point for accessing the plugin's functionality
 * across different platforms, including web.
 */

import { registerPlugin } from '@capacitor/core';

import { FortressPlugin } from './definitions';

/**
 * Main entry point for the Fortress Capacitor plugin.
 *
 * This file registers the plugin with Capacitor and exports
 * both the runtime instance and the public TypeScript types.
 */
const Fortress = registerPlugin<FortressPlugin>('Fortress', {
  web: () => import('./web').then((m) => new m.FortressWeb()),
});

export * from './definitions';
export { Fortress };
