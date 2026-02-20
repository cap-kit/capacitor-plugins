import { WebPlugin } from '@capacitor/core';

import {
  RedsysPlugin,
  PluginVersionResult,
  RedsysPaymentResponseOK,
  RedsysWebPaymentInitResult,
  HashResult,
} from './definitions';
import { PLUGIN_VERSION } from './version';

/**
 * Web implementation of the Redsys plugin.
 * Native SDK functionality for Redsys is strictly platform-dependent (iOS/Android)
 * and cannot be executed in a standard browser environment.
 */
export class RedsysWeb extends WebPlugin implements RedsysPlugin {
  constructor() {
    super();
  }

  // --- Payment Methods ---

  /**
   * Native SDK UI for card entry is not available on the web platform.
   * Standard Redsys redirection should be used for web-based flows.
   */
  async doDirectPayment(): Promise<RedsysPaymentResponseOK> {
    throw this.unimplemented('Direct payment is not available on web.');
  }

  /**
   * Native initialization returns data specifically formatted for the mobile SDKs.
   */
  async initializeWebPayment(): Promise<RedsysWebPaymentInitResult> {
    throw this.unimplemented('Web payment initialization is not available on web.');
  }

  /**
   * The WebView flow is a native container provided by the Redsys SDK.
   * On web, the merchant should handle the redirection to the Redsys TPV portal.
   */
  async processWebPayment(): Promise<RedsysPaymentResponseOK> {
    throw this.unimplemented('Web payment processing is not available on web.');
  }

  /**
   * Cryptographic operations are performed using native system libraries
   * for security and consistency with the SDK requirements.
   */
  async computeHash(): Promise<HashResult> {
    throw this.unimplemented('Native hash computation is not available on web.');
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
