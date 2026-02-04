/**
 * This module provides a web implementation of the SSLPinningPlugin.
 * The functionality is limited in a web context due to the lack of SSL certificate inspection capabilities in browsers.
 *
 * The implementation adheres to the SSLPinningPlugin interface but provides fallback behavior
 * because browsers do not allow direct inspection of SSL certificate details.
 */

import { CapacitorException, ExceptionCode, WebPlugin } from '@capacitor/core';

import { PluginVersionResult, SSLPinningPlugin, SSLPinningResult } from './definitions';

/**
 * Web implementation of the SSLPinning plugin.
 *
 * SSL certificate inspection is not supported in browsers,
 * therefore all SSL pinning methods are unimplemented.
 */
export class SSLPinningWeb extends WebPlugin implements SSLPinningPlugin {
  /**
   * Checks a single SSL certificate against the expected fingerprint.
   * @return A promise that resolves to the result of the certificate check.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificate(): Promise<SSLPinningResult> {
    throw this.createUnimplementedError();
  }

  /**
   * Checks multiple SSL certificates against their expected fingerprints.
   * @return A promise that resolves to an array of results for each certificate check.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificates(): Promise<SSLPinningResult> {
    throw this.createUnimplementedError();
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

  /**
   * Creates a standardized exception for unimplemented methods.
   *
   * This utility method centralizes the creation of exceptions for functionality that is not supported
   * on the current platform, ensuring consistency in error reporting.
   *
   * @returns {CapacitorException} An exception with the code `Unimplemented` and a descriptive message.
   */
  private createUnimplementedError(): CapacitorException {
    return new CapacitorException(
      'This plugin method is not implemented on this platform.',
      ExceptionCode.Unimplemented,
    );
  }
}
