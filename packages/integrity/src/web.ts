import { WebPlugin } from '@capacitor/core';

import { IntegrityPlugin, IntegrityReport, PresentBlockPageResult, PluginVersionResult } from './definitions';

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

  // -----------------------------------------------------------------------------
  // Check
  // -----------------------------------------------------------------------------

  /**
   * Executes a runtime integrity check.
   *
   * On Web, this feature is not available.
   */
  async check(): Promise<IntegrityReport> {
    throw this.unimplemented('Integrity checks are not implemented on web.');
  }

  // -----------------------------------------------------------------------------
  // Present Block Page
  // -----------------------------------------------------------------------------

  /**
   * Presents the integrity block page.
   *
   * On Web, this feature is not available.
   */
  async presentBlockPage(): Promise<PresentBlockPageResult> {
    // Web platform cannot present native block pages.
    // Throw to satisfy TypeScript return flow analysis.
    throw this.unavailable('Integrity block page is not available on the Web platform.');
  }

  // -----------------------------------------------------------------------------
  // Plugin Info
  // -----------------------------------------------------------------------------

  /**
   * Returns the plugin version.
   *
   * On Web, this represents the JavaScript package version.
   */
  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: 'web' };
  }
}
