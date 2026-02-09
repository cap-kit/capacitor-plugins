/**
 * @file index.ts
 * This file exports the Rank and registers it with Capacitor.
 * It acts as the main entry point for accessing the plugin's functionality
 * across different platforms, including web.
 */

import { registerPlugin } from '@capacitor/core';

import { RankPlugin } from './definitions';

/**
 * Main entry point for the Rank Capacitor plugin.
 *
 * This file registers the plugin with Capacitor and exports
 * both the runtime instance and the public TypeScript types.
 */
const Rank = registerPlugin<RankPlugin>('Rank', {
  web: () => import('./web').then((m) => new m.RankWeb()),
});

export * from './definitions';
export { Rank };
