import { WebPlugin } from '@capacitor/core';

import { SettingsPlugin, PluginVersionResult } from './definitions';

/**
 * Web implementation of the Settings plugin.
 *
 * This implementation exists to satisfy Capacitor's multi-platform contract.
 * Opening native system settings is not supported in web browsers.
 *
 * All methods follow the same state-based result model used on
 * Android and iOS:
 * - operations never throw
 * - Promise rejection is not used
 * - failures are reported via structured result objects
 */
export class SettingsWeb extends WebPlugin implements SettingsPlugin {
  constructor() {
    super();
  }

  // --- Open Settings ---

  /**
   * Attempts to open a platform-specific settings screen.
   *
   * This operation is not supported on the Web, as browsers do not
   * provide APIs to open operating system settings.
   */
  async open(): Promise<void> {
    this.unavailable('Opening system settings is not supported on the Web.');
  }

  /**
   * Attempts to open an iOS settings screen.
   *
   * This operation is not supported on the Web.
   */
  async openIOS(): Promise<void> {
    this.unavailable('iOS settings are not available in web environments.');
  }

  /**
   * Attempts to open an Android settings screen.
   *
   * This operation is not supported on the Web.
   */
  async openAndroid(): Promise<void> {
    this.unavailable('Android settings are not available in web environments.');
  }

  // --- Plugin Info ---

  /**
   * Returns the plugin version.
   *
   * On the Web, this value represents the JavaScript package version
   * rather than a native implementation.
   */
  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: 'web' };
  }
}
