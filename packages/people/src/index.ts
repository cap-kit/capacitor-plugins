/**
 * @file index.ts
 * Main entry point for the People Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { PeoplePlugin } from './definitions';

/**
 * The People plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all people functionality.
 */
const People = registerPlugin<PeoplePlugin>('People', {
  web: () => import('./web').then((m) => new m.PeopleWeb()),
});

export * from './definitions';
export { People };
