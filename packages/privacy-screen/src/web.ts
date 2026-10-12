import { WebPlugin, PluginListenerHandle, ListenerCallback  } from '@capacitor/core';

import {
  PluginVersionResult,
  PrivacyScreenActionResult,
  PrivacyScreenConfig,
  PrivacyScreenPlugin,
  PrivacyScreenStatus,
} from './definitions';
import { PLUGIN_VERSION } from './version';

/**
 * Web implementation of the PrivacyScreen plugin.
 *
 * The web platform has no OS-level snapshot protection, so this
 * implementation tracks the enabled state for API parity. The
 * `screenshotTaken` event never fires on Web.
 * iOS screen recording events are also not available on Web.
 */
export class PrivacyScreenWeb extends WebPlugin implements PrivacyScreenPlugin {
  private enabled = false;

  constructor() {
    super();
  }

  async enable(_config?: PrivacyScreenConfig): Promise<PrivacyScreenActionResult> {
    this.enabled = true;
    return { success: true };
  }

  async disable(): Promise<PrivacyScreenActionResult> {
    this.enabled = false;
    return { success: true };
  }

  async isEnabled(): Promise<PrivacyScreenStatus> {
    return { enabled: this.enabled };
  }

  /**
   * Returns the plugin version.
   *
   * On the Web, this value represents the JavaScript package version
   * rather than a native implementation.
   */
  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: PLUGIN_VERSION };
  }

  async addListener(
    _eventName: string,
    _listenerFunc: ListenerCallback,
  ): Promise<PluginListenerHandle> {
    throw this.unimplemented('Not implemented on web.');
  }

  async removeAllListeners(): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }
}
