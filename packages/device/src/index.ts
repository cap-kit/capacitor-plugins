/**
 * @file index.ts
 * Main entry point for the Device Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { DevicePlugin } from './definitions';

/**
 * The Device plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to access all device functionality.
 */
const Device = registerPlugin<DevicePlugin>('Device', {
  web: () => import('./web').then((m) => new m.DeviceWeb()),
});

export * from './definitions';
export { Device };
