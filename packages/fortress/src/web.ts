import { WebPlugin } from '@capacitor/core';

import { FortressPlugin, PluginVersionResult } from './definitions';
import { PLUGIN_VERSION } from './version';

/**
 * Web implementation of the Fortress plugin.
 *
 * This implementation exists primarily to satisfy Capacitor's
 * multi-platform contract and to allow usage in browser-based
 * environments.
 *
 * Native-only features may be unavailable on Web.
 */
export class FortressWeb extends WebPlugin implements FortressPlugin {
  constructor() {
    super();
  }

  // --- Plugin Info ---

  /**
   * Returns the plugin version.
   *
   * On the Web, this value represents the JavaScript package version
   * rather than a native implementation.
   */
  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: PLUGIN_VERSION };
  }
}
