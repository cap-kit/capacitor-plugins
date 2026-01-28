/**
 * This module provides a web implementation of the SSLPinningPlugin.
 * The functionality is limited in a web context due to the lack of SSL certificate inspection capabilities in browsers.
 *
 * The implementation adheres to the SSLPinningPlugin interface but provides fallback behavior
 * because browsers do not allow direct inspection of SSL certificate details.
 */

import { CapacitorException, ExceptionCode, WebPlugin } from '@capacitor/core';

import {
  PluginVersionResult,
  SSLPinningMultiOptions,
  SSLPinningOptions,
  SSLPinningPlugin,
  SSLPinningResult,
} from './definitions';

/**
 * Web implementation of the SSLPinningPlugin interface.
 *
 * This class is intended to be used in a browser environment and handles scenarios where SSL certificate
 * checking is unsupported. It implements the methods defined by the SSLPinningPlugin
 * interface but returns standardized error responses to indicate the lack of functionality in web contexts.
 */
export class SSLPinningWeb extends WebPlugin implements SSLPinningPlugin {
  /**
   * Checks a single SSL certificate against the expected fingerprint.
   * @param options - The options for checking the certificate.
   * @returns A promise that resolves to the result of the certificate check.
   */
  async checkCertificate(): Promise<SSLPinningResult>;

  /**
   * Checks a single SSL certificate against the expected fingerprint.
   * @param options - The options for checking the certificate.
   * @returns A promise that resolves to the result of the certificate check.
   */
  async checkCertificate(_: SSLPinningOptions): Promise<SSLPinningResult>;

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
  async checkCertificates(): Promise<SSLPinningResult[]>;

  /**
   * Checks multiple SSL certificates against their expected fingerprints.
   * @param options - The options for checking multiple certificates.
   * @return A promise that resolves to an array of results for each certificate check.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificates(_: SSLPinningMultiOptions[]): Promise<SSLPinningResult[]>;

  /**
   * Checks multiple SSL certificates against their expected fingerprints.
   * @return A promise that resolves to an array of results for each certificate check.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificates(): Promise<SSLPinningResult[]> {
    throw this.createUnimplementedError();
  }

  // --- Plugin Info ---

  /**
   * Returns the plugin version.
   *
   * @returns The current plugin version.
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
