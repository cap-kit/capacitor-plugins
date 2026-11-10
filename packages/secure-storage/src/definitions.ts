/// <reference types="@capacitor/cli" />

/**
 * Static configuration options for the SecureStorage plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the SecureStorage plugin.
     *
     * The key MUST match the plugin's registered name (`SecureStorage`,
     * the cap-kit convention: no CapKit prefix), which Capacitor uses to
     * resolve static plugin config on both platforms (Android:
     * `@CapacitorPlugin(name=...)`; iOS: `CAPPlugin.getConfig()` ->
     * `pluginName` -> `jsName`).
     */
    SecureStorage?: {
      /**
       * Enables verbose native logging.
       *
       * @default false
       * @since 8.0.0
       */
      verboseLogging?: boolean;
      /**
       * Enables iCloud Keychain synchronization for stored entries.
       *
       * iOS only; ignored on Android and Web.
       *
       * @default false
       * @since 8.0.0
       */
      enableICloudKeychainSync?: boolean;
      /**
       * Base64-encodes stored key names (both tiers) on top of any prefix.
       *
       * Obfuscation is not encryption: it hides key names from casual
       * inspection, nothing more. Reads transparently accept both forms,
       * so toggling never orphans existing entries.
       *
       * @default false
       * @since 8.0.0
       */
      obfuscateKeys?: boolean;
      /**
       * Prefix prepended to stored key names before optional encoding.
       *
       * @default ''
       * @since 8.0.0
       */
      obfuscationPrefix?: string;
    };
  }
}

/**
 * iOS Keychain accessibility level for stored items.
 *
 * Mirrors the platform `kSecAttrAccessible` constants. Applies to iOS
 * only; ignored on Android and Web.
 *
 * @since 8.0.0
 */
export type KeychainAccess =
  | 'whenUnlocked'
  | 'whenUnlockedThisDeviceOnly'
  | 'afterFirstUnlock'
  | 'afterFirstUnlockThisDeviceOnly'
  | 'whenPasscodeSetThisDeviceOnly';

/**
 * Key/value payload for writes.
 *
 * @since 8.0.0
 */
export interface SetOptions {
  key: string;
  value: string;
  /**
   * iOS Keychain accessibility for this item, overriding the default set
   * via `setDefaultKeychainAccess()`. Ignored on Android and Web.
   *
   * @since 8.0.0
   */
  access?: KeychainAccess;
}

/**
 * Key payload for reads, removals, and existence checks.
 *
 * @since 8.0.0
 */
export interface KeyOptions {
  key: string;
}

/**
 * Read result. `value` is `null` when the key does not exist.
 *
 * @since 8.0.0
 */
export interface GetResult {
  value: string | null;
}

/**
 * Removal result. `existed` reports whether an entry was present.
 *
 * @since 8.0.0
 */
export interface RemoveResult {
  existed: boolean;
}

/**
 * Existence-check result.
 *
 * @since 8.0.0
 */
export interface HasResult {
  exists: boolean;
}

/**
 * Key-enumeration result.
 *
 * @since 8.0.0
 */
export interface KeysResult {
  keys: string[];
}

/**
 * Batch-read input.
 *
 * @since 8.0.0
 */
export interface GetManyOptions {
  keys: string[];
}

/**
 * Batch-read result. Missing keys map to `null`.
 *
 * @since 8.0.0
 */
export interface GetManyResult {
  values: Record<string, string | null>;
}

/**
 * Synchronization-toggle input.
 *
 * @since 8.0.0
 */
export interface SynchronizeOptions {
  synchronize: boolean;
}

/**
 * Synchronization-state result.
 *
 * @since 8.0.0
 */
export interface SynchronizeResult {
  synchronize: boolean;
}

/**
 * Default-accessibility input.
 *
 * @since 8.0.0
 */
export interface DefaultAccessOptions {
  access: KeychainAccess;
}

/**
 * Runtime group configuration for the insecure tier.
 *
 * Groups namespace insecure key/value pairs, exactly like the official
 * Preferences plugin. The default group is `CapacitorStorage`.
 *
 * @since 8.0.0
 */
export interface ConfigureOptions {
  group?: string;
  /**
   * Base64-encodes stored key names (both tiers) at runtime, overriding
   * the static `obfuscateKeys` value for this session.
   *
   * @since 8.0.0
   */
  obfuscateKeys?: boolean;
  /**
   * Prefix prepended to stored key names at runtime, overriding the
   * static `obfuscationPrefix` value for this session.
   *
   * @since 8.0.0
   */
  obfuscationPrefix?: string;
}

/**
 * Result object returned by the `getPluginVersion()` method.
 */
export interface PluginVersionResult {
  /**
   * The native plugin version string.
   */
  version: string;
}

/**
 * Public JavaScript API for the SecureStorage Capacitor plugin.
 *
 * Transparent encrypted key/value storage: hardware-backed on Android
 * (Keystore) and iOS (Keychain), with iCloud sync and Keychain access
 * levels on iOS. No vault, no lock state, no authentication — reads are
 * always available, exactly like the community secure-storage contract.
 */
export interface SecureStoragePlugin {
  /**
   * Stores a value under a key, overwriting any previous entry.
   *
   * @param options - Key, value, and optional iOS accessibility.
   *
   * @example
   * ```ts
   * await SecureStorage.set({ key: 'theme', value: 'dark' });
   * ```
   *
   * @since 8.0.0
   */
  set(options: SetOptions): Promise<void>;

  /**
   * Reads the value for a key, or `null` when absent.
   *
   * @since 8.0.0
   */
  get(options: KeyOptions): Promise<GetResult>;

  /**
   * Removes the entry for a key.
   *
   * @returns Whether an entry was present.
   *
   * @since 8.0.0
   */
  remove(options: KeyOptions): Promise<RemoveResult>;

  /**
   * Checks whether an entry exists without reading it.
   *
   * @since 8.0.0
   */
  has(options: KeyOptions): Promise<HasResult>;

  /**
   * Lists every stored key.
   *
   * @since 8.0.0
   */
  keys(): Promise<KeysResult>;

  /**
   * Reads several keys in one call. Missing keys map to `null`.
   *
   * @since 8.0.0
   */
  getMany(options: GetManyOptions): Promise<GetManyResult>;

  /**
   * Removes every stored entry.
   *
   * On Android this also rotates the Keystore keys, so previously
   * captured ciphertext can never be decrypted again.
   *
   * @since 8.0.0
   */
  clear(): Promise<void>;

  /**
   * Configures the insecure tier at runtime.
   *
   * Only `group` is supported; `undefined` values leave the current
   * setting untouched. Mirrors the official Preferences contract.
   *
   * @since 8.0.0
   */
  configure(options: ConfigureOptions): Promise<void>;

  /**
   * Stores a plaintext value in the insecure tier under the current group.
   *
   * For non-sensitive data only (UI preferences, flags). Values are NOT
   * encrypted; keys follow the same prefix/obfuscation rules as the
   * secure tier.
   *
   * @since 8.0.0
   */
  setInsecure(options: SetOptions): Promise<void>;

  /**
   * Reads a plaintext value from the insecure tier, or `null` when absent.
   *
   * @since 8.0.0
   */
  getInsecure(options: KeyOptions): Promise<GetResult>;

  /**
   * Removes an insecure entry.
   *
   * @returns Whether an entry was present.
   *
   * @since 8.0.0
   */
  removeInsecure(options: KeyOptions): Promise<RemoveResult>;

  /**
   * Checks insecure existence without reading.
   *
   * @since 8.0.0
   */
  hasInsecure(options: KeyOptions): Promise<HasResult>;

  /**
   * Lists insecure keys of the current group.
   *
   * @since 8.0.0
   */
  keysInsecure(): Promise<KeysResult>;

  /**
   * Removes every insecure entry of the current group.
   *
   * @since 8.0.0
   */
  clearInsecure(): Promise<void>;

  /**
   * Enables or disables iCloud Keychain synchronization at runtime.
   *
   * iOS only; a no-op on Android and Web.
   *
   * @since 8.0.0
   */
  setSynchronize(options: SynchronizeOptions): Promise<void>;

  /**
   * Reports whether iCloud Keychain synchronization is active.
   *
   * iOS only; always `false` elsewhere.
   *
   * @since 8.0.0
   */
  getSynchronize(): Promise<SynchronizeResult>;

  /**
   * Sets the default iOS Keychain accessibility for subsequently
   * stored items. Per-item `access` in `set()` wins.
   *
   * iOS only; a no-op on Android and Web.
   *
   * @since 8.0.0
   */
  setDefaultKeychainAccess(options: DefaultAccessOptions): Promise<void>;

  /**
   * Returns the native plugin version.
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
