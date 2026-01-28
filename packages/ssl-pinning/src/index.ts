/**
 * Import the `registerPlugin` method from the Capacitor core library.
 * This method is used to register a custom plugin.
 */
import { registerPlugin } from '@capacitor/core';

/**
 * @file index.ts
 * Main entry point for the SSLPinning Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { SSLPinningPlugin } from './definitions';

/**
 * The SSLPinning plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all ssl pinning functionality.
 */
const SSLPinning = registerPlugin<SSLPinningPlugin>('SSLPinning', {
  web: () => import('./web').then((m) => new m.SSLPinningWeb()),
});

export * from './definitions';
export { SSLPinning };
