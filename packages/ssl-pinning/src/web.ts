import { WebPlugin } from '@capacitor/core';

import { PluginVersionResult, SSLPinningPlugin, SSLPinningResult } from './definitions';
import { PLUGIN_VERSION } from './version';

/**
 * Web implementation of the SSLPinning plugin.
 *
 * SSL certificate inspection is not supported in browsers,
 * therefore all SSL pinning methods are unimplemented.
 */
export class SSLPinningWeb extends WebPlugin implements SSLPinningPlugin {
  constructor() {
    super();
  }

  /**
   * Checks a single SSL certificate against the expected fingerprint.
   * @return A promise that resolves to the result of the certificate check.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificate(): Promise<SSLPinningResult> {
    throw this.unimplemented();
  }

  /**
   * Checks multiple SSL certificates against their expected fingerprints.
   * @return A promise that resolves to an array of results for each certificate check.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificates(): Promise<SSLPinningResult> {
    throw this.unimplemented();
  }

  // -----------------------------------------------------------------------------
  // Plugin Info
  // -----------------------------------------------------------------------------

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
