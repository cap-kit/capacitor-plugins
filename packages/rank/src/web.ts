import { WebPlugin } from '@capacitor/core';

import { RankPlugin, PluginVersionResult, StoreOptions, ReviewEnvironmentResult, RankErrorCode } from './definitions';
import { PLUGIN_VERSION } from './version';

/**
 * Web implementation of the Rank plugin.
 *
 * This implementation exists primarily to satisfy Capacitor's
 * multi-platform contract and to allow usage in browser-based
 * environments.
 *
 * Native-only features like the In-App Review prompt are unavailable on Web.
 */
export class RankWeb extends WebPlugin implements RankPlugin {
  constructor() {
    super();
  }

  // -----------------------------------------------------------------------------
  // Availability
  // -----------------------------------------------------------------------------

  /**
   * On Web, native In-App Review is never available.
   */
  async isAvailable(): Promise<{ value: boolean }> {
    return { value: false };
  }

  /**
   * On Web, the review environment is not available, so this method returns a default result.
   */
  async checkReviewEnvironment(): Promise<ReviewEnvironmentResult> {
    return {
      canRequestReview: false,
    };
  }

  // -----------------------------------------------------------------------------
  // In-App Review
  // -----------------------------------------------------------------------------

  /**
   * Requests the display of the native review popup.
   *
   * On Web, this feature is not available.
   */
  async requestReview(): Promise<void> {
    this.rejectWithCode(RankErrorCode.UNAVAILABLE, 'In-App Review is not available on Web.');
  }

  // -----------------------------------------------------------------------------
  // Product Page Navigation
  // -----------------------------------------------------------------------------

  /**
   * On Web, redirects to the store URL as an internal overlay is not possible.
   */
  async presentProductPage(options?: StoreOptions): Promise<void> {
    return this.openStore(options);
  }

  // -----------------------------------------------------------------------------
  // Store Navigation
  // -----------------------------------------------------------------------------

  /**
   * Opens the app's page in the App Store or Play Store via URL redirect.
   * @description Detects the user's platform to provide the correct store link.
   * @param options Store identification overrides including appId for iOS or packageName for Android.
   */
  async openStore(options?: StoreOptions): Promise<void> {
    if (!options?.appId && !options?.packageName) {
      this.rejectWithCode(RankErrorCode.INVALID_INPUT, 'Invalid or missing appId/packageName.');
    }

    const url = this.getStoreUrl(options);

    if (!url) {
      // URL construction failed due to missing or invalid identifiers
      return;
    }

    window.open(url, '_blank');
  }

  /**
   * Opens the App Store listing page for a specific app via URL redirect.
   * @param options Store identification options.
   */
  async openStoreListing(options?: { appId?: string }): Promise<void> {
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
    if (isIOS && options?.appId) {
      window.open(RankStoreUtils.appStoreUrl(options.appId), '_blank');
    } else {
      this.rejectWithCode(RankErrorCode.INVALID_INPUT, 'Invalid or missing appId.');
    }
  }

  /**
   * Redirects to the developer's page in the store.
   * @param options.devId The developer ID to navigate to.
   */
  async openDevPage(options: { devId: string }): Promise<void> {
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
    const url = isIOS
      ? RankStoreUtils.appStoreSearchUrl(options.devId) // iOS uses search for dev pages often
      : RankStoreUtils.playStoreDevUrl(options.devId);

    window.open(url, '_blank');
  }

  /**
   * On Web, collection navigation is not supported, so this method logs a warning.
   * @param options Collection identification (not used on Web).
   * @description This method is a no-op on Web and serves as a placeholder for potential future functionality.
   */
  async openCollection(): Promise<void> {
    throw this.unavailable('openCollection is not available on Web.');
  }

  /**
   * On Web, search navigation is not supported, so this method logs a warning and opens a generic search URL.
   * @param options Search terms for finding the app in the store (not used on Web).
   * @description Redirects to a generic search page on the respective store as a fallback.
   */
  async search(options: { terms: string }): Promise<void> {
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
    const url = isIOS
      ? RankStoreUtils.appStoreSearchUrl(options.terms)
      : RankStoreUtils.playStoreSearchUrl(options.terms);
    window.open(url, '_blank');
  }

  // -----------------------------------------------------------------------------
  // Internal Helpers
  // -----------------------------------------------------------------------------

  /**
   * Generates the appropriate store URL based on user agent and provided options.
   * * @param options Store options containing identifiers.
   * @returns A string representing the full store URL.
   */
  private getStoreUrl(options: StoreOptions): string {
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);

    if (isIOS && options.appId) {
      return RankStoreUtils.appStoreUrl(options.appId);
    }

    return RankStoreUtils.playStoreUrl(options.packageName);
  }

  private rejectWithCode(code: RankErrorCode, message: string): never {
    throw { message, code };
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

/**
 * Internal utility helpers for Store URL construction.
 * Not part of the public API.
 */
class RankStoreUtils {
  static appStoreUrl(appId: string): string {
    return `https://apps.apple.com/app/id${appId}`;
  }

  static appStoreSearchUrl(terms: string): string {
    return `https://apps.apple.com/search?term=${encodeURIComponent(terms)}`;
  }

  static playStoreUrl(packageName?: string): string {
    if (!packageName || packageName.trim().length === 0) {
      console.warn('Rank: Missing Android packageName. Cannot construct Play Store URL.');
      return '';
    }
    return `https://play.google.com/store/apps/details?id=${packageName}`;
  }

  static playStoreDevUrl(devId: string): string {
    return `https://play.google.com/store/apps/dev?id=${devId}`;
  }

  static playStoreSearchUrl(terms: string): string {
    return `https://play.google.com/store/search?q=${encodeURIComponent(terms)}&c=apps`;
  }
}
