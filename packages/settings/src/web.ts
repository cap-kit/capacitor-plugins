import { WebPlugin } from '@capacitor/core';

import { SettingsErrorCode, SettingsPlugin, SettingsResult, PluginVersionResult } from './definitions';

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
  async open(): Promise<SettingsResult> {
    return {
      success: false,
      error: 'Opening system settings, is not supported on the Web.',
      code: SettingsErrorCode.UNAVAILABLE,
    };
  }

  /**
   * Attempts to open an iOS settings screen.
   *
   * This operation is not supported on the Web.
   */
  async openIOS(): Promise<SettingsResult> {
    return {
      success: false,
      error: 'iOS settings are not available in web environments.',
      code: SettingsErrorCode.UNAVAILABLE,
    };
  }

  /**
   * Attempts to open an Android settings screen.
   *
   * This operation is not supported on the Web.
   */
  async openAndroid(): Promise<SettingsResult> {
    return {
      success: false,
      error: 'Android settings are not available in web environments.',
      code: SettingsErrorCode.UNAVAILABLE,
    };
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
