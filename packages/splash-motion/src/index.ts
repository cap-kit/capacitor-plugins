/**
 * @file index.ts
 * Main entry point for the SplashMotion Capacitor Plugin.
 * This file handles the registration of the plugin with the Capacitor core runtime
 * and exports all necessary types for consumers.
 */

import { registerPlugin } from '@capacitor/core';

import { SplashMotionPlugin } from './definitions.js';

/**
 * The SplashMotion plugin instance.
 * It automatically lazy-loads the web implementation if running in a browser environment.
 * Use this instance to show a native splash screen with Lottie animation and haptic feedback.
 */
const SplashMotion = registerPlugin<SplashMotionPlugin>('SplashMotion', {
  web: () => import('./web.js').then((m) => new m.SplashMotionWeb()),
});

export * from './definitions.js';
export { SplashMotion };
// Re-exported so apps can call `SplashMotionWeb.setConfig()` to supply the
// web launch defaults (web cannot read the static plugin config). The heavy
// dotlottie-web dependency inside web.ts stays lazy (dynamic import).
export { SplashMotionWeb } from './web.js';
export type { SplashMotionWebConfig } from './web.js';
