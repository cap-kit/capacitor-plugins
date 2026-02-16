/**
 * This module provides a web implementation of the SSLPinningPlugin.
 * The functionality is limited in a web context due to the lack of SSL certificate inspection capabilities in browsers.
 *
 * The implementation adheres to the SSLPinningPlugin interface but provides fallback behavior
 * because browsers do not allow direct inspection of SSL certificate details.
 */

import { WebPlugin } from '@capacitor/core';

import { PluginVersionResult, SSLPinningPlugin, SSLPinningResult } from './definitions';

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
