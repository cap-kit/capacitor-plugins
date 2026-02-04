import { WebPlugin } from '@capacitor/core';

import {
  IntegrityPlugin,
  IntegrityReport,
  PresentBlockPageResult,
  PluginVersionResult,
  IntegrityErrorCode,
} from './definitions';

/**
 * Web implementation of the Integrity plugin.
 *
 * This implementation exists to preserve API parity
 * across all platforms.
 *
 * The Web platform does NOT provide native integrity signals.
 * Therefore, most methods are explicitly unavailable.
 */
export class IntegrityWeb extends WebPlugin implements IntegrityPlugin {
  constructor() {
    super();
  }

  // ---------------------------------------------------------------------------
  // Check
  // ---------------------------------------------------------------------------

  /**
   * Executes a runtime integrity check.
   *
   * On Web, this feature is not available.
   */
  async check(): Promise<IntegrityReport> {
    return Promise.reject({
      message: 'Integrity checks are not available on the Web platform.',
      code: IntegrityErrorCode.UNAVAILABLE,
    });
  }

  // ---------------------------------------------------------------------------
  // PresentBlockPage
  // ---------------------------------------------------------------------------

  /**
   * Presents the integrity block page.
   *
   * On Web, this feature is not available.
   */
  async presentBlockPage(): Promise<PresentBlockPageResult> {
    return Promise.reject({
      message: 'Integrity block page is not available on the Web platform.',
      code: IntegrityErrorCode.UNAVAILABLE,
    });
  }

  // ---------------------------------------------------------------------------
  // Plugin info
  // ---------------------------------------------------------------------------

  /**
   * Returns the plugin version.
   *
   * On Web, this represents the JavaScript package version.
   */
  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: 'web' };
  }
}
