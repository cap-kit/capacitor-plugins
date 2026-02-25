import { WebPlugin } from '@capacitor/core';

import {
  TLSFingerprintPlugin,
  PluginVersionResult,
  TLSFingerprintOptions,
  TLSFingerprintMultiOptions,
  TLSFingerprintResult,
} from './definitions';
import { PLUGIN_VERSION } from './version';

/**
 * Web implementation of the TLSFingerprint plugin.
 *
 * This implementation exists to satisfy Capacitor's multi-platform contract.
 * TLS fingerprinting is not supported in web browsers.
 *
 * All methods follow the standard Promise rejection model:
 * - unsupported features reject with UNAVAILABLE
 */
export class TLSFingerprintWeb extends WebPlugin implements TLSFingerprintPlugin {
  constructor() {
    super();
  }

  /**
   * Checks a single SSL certificate against the expected fingerprint.
   * @param options - Unused on web platform.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificate(options: TLSFingerprintOptions): Promise<TLSFingerprintResult> {
    void options;
    throw this.unimplemented();
  }

  /**
   * Checks multiple SSL certificates against their expected fingerprints.
   * @param options - Unused on web platform.
   * @throws CapacitorException indicating unimplemented functionality.
   */
  async checkCertificates(options: TLSFingerprintMultiOptions): Promise<TLSFingerprintResult> {
    void options;
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
