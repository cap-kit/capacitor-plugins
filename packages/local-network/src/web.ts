/**
 * @file web.ts
 * Web implementation of the Local Network plugin.
 * Browsers do not gate local-network access behind a runtime permission, so
 * the state is always reported as `granted`, mirroring iOS.
 */
import { WebPlugin } from '@capacitor/core';

import { LocalNetworkPermissions, LocalNetworkPlugin, PluginVersionResult } from './definitions.js';
import { PLUGIN_VERSION } from './version.js';

export class LocalNetworkWeb extends WebPlugin implements LocalNetworkPlugin {
  private lastPermissionState = 'granted' as const;

  constructor() {
    super();
    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
    }
    if (typeof window !== 'undefined') {
      window.addEventListener('focus', this.handleFocus.bind(this));
    }
  }

  private handleVisibilityChange(): void {
    if (document.visibilityState === 'visible') {
      this.checkAndNotifyPermissionChange();
    }
  }

  private handleFocus(): void {
    this.checkAndNotifyPermissionChange();
  }

  private checkAndNotifyPermissionChange(): void {
    const currentState = 'granted';
    if (this.lastPermissionState !== currentState) {
      this.lastPermissionState = currentState;
      this.notifyListeners('permissionChange', { localNetwork: currentState });
    } else {
      this.lastPermissionState = currentState;
    }
  }

  // -----------------------------------------------------------------------------
  // Permissions
  // -----------------------------------------------------------------------------

  /**
   * Checks the permission status for the plugin.
   *
   * @returns A promise resolving to an object containing the permission states.
   */
  async checkPermissions(): Promise<LocalNetworkPermissions> {
    return { localNetwork: 'granted' };
  }

  /**
   * Requests the necessary permissions for the plugin.
   *
   * @returns A promise resolving to an object containing the updated permission states.
   */
  async requestPermissions(): Promise<LocalNetworkPermissions> {
    return { localNetwork: 'granted' };
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

  // -----------------------------------------------------------------------------
  // Listener Handling
  // -----------------------------------------------------------------------------

  /**
   * Cleanup all listeners
   *
   * @returns A promise that resolves when all listeners are removed.
   */
  async removeAllListeners(): Promise<void> {
    super.removeAllListeners();
  }
}
