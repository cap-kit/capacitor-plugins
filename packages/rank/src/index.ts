/**
 * @file index.ts
 * Main entry point for the Rank Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { RankPlugin } from './definitions';

/**
 * The Rank plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all rank functionality.
 */
const Rank = registerPlugin<RankPlugin>('Rank', {
  web: () => import('./web').then((m) => new m.RankWeb()),
});

export * from './definitions';
export { Rank };
