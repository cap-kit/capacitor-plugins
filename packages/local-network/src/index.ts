/**
 * @file index.ts
 * Main entry point for the Local Network Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { LocalNetworkPlugin } from './definitions.js';

/**
 * The LocalNetwork plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all local-network permission functionality.
 */
const LocalNetwork = registerPlugin<LocalNetworkPlugin>('LocalNetwork', {
  web: () => import('./web.js').then((m) => new m.LocalNetworkWeb()),
});

export * from './definitions.js';
export { LocalNetwork };
