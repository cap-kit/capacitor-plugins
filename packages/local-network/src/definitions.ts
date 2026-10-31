/// <reference types="@capacitor/cli" />

/**
 * @file definitions.ts
 * Public API surface of the Local Network plugin: configuration, results,
 * events, error codes and the plugin interface.
 */
import { PermissionState, PluginListenerHandle } from '@capacitor/core';

/**
 * Extension of the Capacitor CLI configuration to include specific settings for LocalNetwork.
 * This allows users to configure the plugin via capacitor.config.ts or capacitor.config.json.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the LocalNetwork plugin.
     */
    LocalNetwork?: LocalNetworkConfig;
  }
}

/**
 * Static configuration options for the LocalNetwork plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface LocalNetworkConfig {
  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information is printed
   * to the native console (Logcat on Android, Xcode on iOS).
   *
   * This option affects native logging behavior only and
   * has no impact on the JavaScript API.
   *
   * @default false
   * @example true
   *
   * @since 8.0.0
   */
  verboseLogging?: boolean;
}

/**
 * Result object returned by the `getPluginVersion()` method.
 */
export interface PluginVersionResult {
  /** The native version string of the plugin. */
  version: string;
}

// -----------------------------------------------------------------------------
// EVENTS
// -----------------------------------------------------------------------------

/**
 * Event emitted when the local-network permission state changes.
 * Useful when the user returns from system settings after changing the permission.
 */
export interface LocalNetworkPermissionChangeEvent {
  /** Current state of the local-network permission */
  localNetwork: PermissionState;
}

/**
 * Standardized error codes used by the LocalNetwork plugin.
 *
 * These codes are returned as part of structured error objects
 * and allow consumers to implement programmatic error handling.
 *
 * Defined as a readonly object literal rather than an enum to satisfy
 * strict TypeScript rules (no enums; use `as const` objects).
 */
export const LocalNetworkErrorCode = {
  /** The capability is not available on this device or platform. */
  UNAVAILABLE: 'UNAVAILABLE',
  /** The user cancelled an interactive flow. */
  CANCELLED: 'CANCELLED',
  /** The user denied the permission or the feature is disabled by the OS. */
  PERMISSION_DENIED: 'PERMISSION_DENIED',
  /** The plugin failed to initialize or perform an operation. */
  INIT_FAILED: 'INIT_FAILED',
  /** The input provided to the plugin method is invalid, missing, or malformed. */
  INVALID_INPUT: 'INVALID_INPUT',
  /** The requested type is not valid or supported. */
  UNKNOWN_TYPE: 'UNKNOWN_TYPE',
  /** The requested resource does not exist. */
  NOT_FOUND: 'NOT_FOUND',
  /** The operation conflicts with the current state. */
  CONFLICT: 'CONFLICT',
  /** The operation did not complete within the expected time. */
  TIMEOUT: 'TIMEOUT',
} as const;

/**
 * Union type derived from the `LocalNetworkErrorCode` object.
 */
export type LocalNetworkErrorCode = (typeof LocalNetworkErrorCode)[keyof typeof LocalNetworkErrorCode];

/**
 * Result of the LocalNetwork permission flow.
 *
 * The property is declared explicitly (not via an index signature) so that
 * consumers can use dot access (`result.localNetwork`) even with the
 * `noPropertyAccessFromIndexSignature` compiler option enabled.
 */
export interface LocalNetworkPermissions {
  /**
   * State of the local-network (`ACCESS_LOCAL_NETWORK`) permission.
   *
   * On Android 17+ (API 37) this reflects the runtime permission state.
   * On older Android versions, iOS and web the permission does not gate
   * local-network access and is always reported as `granted`.
   */
  localNetwork: PermissionState;
}

export interface LocalNetworkPlugin {
  /**
   * Checks the current state of the local-network permission.
   *
   * Android 17+ (API 37) reports the real runtime permission state
   * (`granted`, `denied`, `prompt` or `prompt-with-rationale`).
   *
   * On older Android versions, iOS and web the permission is not enforced
   * and is always reported as `granted`, so calling this method never
   * triggers a system prompt.
   *
   * @since 8.0.0
   */
  checkPermissions(): Promise<LocalNetworkPermissions>;

  /**
   * Requests the local-network permission.
   *
   * Android 17+ (API 37) shows the system permission prompt when the
   * permission has not been decided yet, then resolves with the resulting
   * state. A denied decision is reported as state, not as a rejection.
   *
   * On older Android versions, iOS and web the permission is not enforced
   * and the call resolves immediately with `granted`.
   *
   * @since 8.0.0
   */
  requestPermissions(): Promise<LocalNetworkPermissions>;

  /**
   * Listen for changes to the local-network permission state.
   * Emits when the permission state changes (e.g. after returning from system settings).
   * On platforms where the permission is not enforced, this will typically remain `granted`.
   *
   * @returns A promise that resolves to a handle to remove the listener.
   *
   * @example
   * ```typescript
   * import { LocalNetwork } from '@cap-kit/local-network';
   *
   * const handle = await LocalNetwork.addListener('permissionChange', (event) => {
   *   console.log('Local network permission changed:', event.localNetwork);
   * });
   *
   * // To remove the listener later:
   * // await handle.remove();
   * ```
   *
   * @since 8.0.0
   */
  addListener(
    eventName: 'permissionChange',
    listenerFunc: (event: LocalNetworkPermissionChangeEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Registers a listener for plugin events using a generic event name.
   *
   * Prefer the typed `permissionChange` overload for full payload type safety.
   *
   * @since 8.0.0
   */
  addListener(eventName: string, listenerFunc: (...args: unknown[]) => void): Promise<PluginListenerHandle>;

  /**
   * Removes all registered listeners for this plugin.
   * @returns Promise that resolves when all listeners have been removed.
   *
   * @example
   * ```typescript
   * import { LocalNetwork } from '@cap-kit/local-network';
   *
   * await LocalNetwork.removeAllListeners();
   * ```
   *
   * @since 8.0.0
   */
  removeAllListeners(): Promise<void>;

  /**
   * Returns the native plugin version.
   *
   * The returned version corresponds to the native implementation
   * bundled with the application.
   *
   * @returns A promise resolving to the plugin version.
   *
   * @example
   * ```typescript
   * import { LocalNetwork } from '@cap-kit/local-network';
   *
   * const { version } = await LocalNetwork.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
