/**
 * @file index.ts
 * This file exports the Settings and registers it with Capacitor.
 * It acts as the main entry point for accessing the plugin's functionality
 * across different platforms, including web.
 */

import { registerPlugin } from '@capacitor/core';

import type { SettingsPlugin } from './definitions';

/**
 * Main entry point for the Settings Capacitor plugin.
 *
 * This file registers the plugin with Capacitor and exports
 * both the runtime instance and the public TypeScript types.
 */
const Settings = registerPlugin<SettingsPlugin>('Settings', {
  web: () => import('./web').then((m) => new m.SettingsWeb()),
});

export * from './definitions';
export { Settings };
