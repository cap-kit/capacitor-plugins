/**
 * @file index.ts
 * This file exports the TestPlugin and registers it with Capacitor.
 * It acts as the main entry point for accessing the plugin's functionality
 * across different platforms, including web.
 */

import { registerPlugin } from '@capacitor/core';

import type { TestPlugin } from './definitions';

/**
 * Main entry point for the Test Capacitor plugin.
 *
 * This file registers the plugin with Capacitor and exports
 * both the runtime instance and the public TypeScript types.
 */
const Test = registerPlugin<TestPlugin>('Test', {
  web: () => import('./web').then((m) => new m.TestWeb()),
});

export * from './definitions';
export { Test };
