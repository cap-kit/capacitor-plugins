/**
 * @file index.ts
 * Main entry point for the PrivacyScreen Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { PrivacyScreenPlugin } from './definitions';

/**
 * The PrivacyScreen plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to protect app content in the app switcher and block screenshots.
 */
const PrivacyScreen = registerPlugin<PrivacyScreenPlugin>('PrivacyScreen', {
  web: () => import('./web').then((m) => new m.PrivacyScreenWeb()),
});

export * from './definitions';
export { PrivacyScreen };
