import { WebPlugin } from '@capacitor/core';

import type { TestPlugin } from './definitions';

/**
 * Web implementation of the Test plugin.
 *
 * This implementation exists primarily to satisfy Capacitor's
 * multi-platform contract and to allow usage in browser-based
 * environments.
 *
 * Native-only features may be unavailable on Web.
 */
export class TestWeb extends WebPlugin implements TestPlugin {
  constructor() {
    super();
  }

  // --- Echo Method ---

  /**
   * Echoes a value back for the web platform.
   * This method is a basic implementation example, primarily for testing
   * or validating communication with the plugin.
   *
   * @param options - An object containing a `value` property to be echoed back.
   * @returns A promise resolving to an object containing the echoed `value`.
   */
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    // Note: On the web, reading 'capacitor.config.ts' requires specific build setups.
    // We pass the value through as-is for parity, or you can implement logic to read
    // from a global config object if your app exposes one.
    return options;
  }

  // --- App Settings ---

  /**
   * Opens the app settings page.
   * On Web, this is not applicable.
   *
   * @returns A promise that resolves when the operation is complete.
   */
  async openAppSettings(): Promise<void> {
    console.warn('Test: openAppSettings is not available on Web.');
    return this.unimplemented('Not implemented on Web.') as never;
  }

  // --- Plugin Info ---

  /**
   * Returns the plugin version.
   *
   * @returns The current plugin version.
   */
  async getPluginVersion(): Promise<{ version: string }> {
    return { version: 'web-1.0.0' };
  }
}
