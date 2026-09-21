/**
 * @file index.ts
 * This file exports the Redsys and registers it with Capacitor.
 * It acts as the main entry point for accessing the plugin's functionality
 * across different platforms, including web.
 */

import { registerPlugin } from '@capacitor/core';

import { RedsysPlugin } from './definitions';

/**
 * Main entry point for the Redsys Capacitor plugin.
 *
 * This file registers the plugin with Capacitor and exports
 * both the runtime instance and the public TypeScript types.
 */
const Redsys = registerPlugin<RedsysPlugin>('Redsys', {
  web: () => import('./web').then((m) => new m.RedsysWeb()),
});

export * from './definitions';
export { Redsys };
