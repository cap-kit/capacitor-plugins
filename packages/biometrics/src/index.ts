/**
 * @file index.ts
 * Main entry point for the Biometrics Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { BiometricsPlugin } from './definitions.js';

/**
 * The Biometrics plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all biometric functionality.
 */
const Biometrics = registerPlugin<BiometricsPlugin>('Biometrics', {
  web: () => import('./web.js').then((m) => new m.BiometricsWeb()),
});

export * from './definitions.js';
export { Biometrics };
