/**
 * @file index.ts
 * Main entry point for the SecureStorage Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { SecureStoragePlugin } from './definitions.js';

/**
 * The SecureStorage plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all secure storage functionality.
 */
const SecureStorage = registerPlugin<SecureStoragePlugin>('SecureStorage', {
  web: () => import('./web').then((m) => new m.SecureStorageWeb()),
});

export * from './definitions.js';
export { SecureStorage };
