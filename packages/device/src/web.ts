import { WebPlugin, PluginListenerHandle } from '@capacitor/core';

import {
  BatteryChargingStateChangeListener,
  BatteryExtras,
  BatteryInfo,
  DeviceConfiguration,
  DeviceId,
  DeviceInfo,
  DevicePlugin,
  DisplayInfo,
  GetLanguageCodeResult,
  LanguageTag,
  MemoryInfo,
  PluginVersionResult,
  PowerState,
  StorageInfo,
  SystemUptime,
  AppVersion,
} from './definitions';
import { PLUGIN_VERSION } from './version';

declare global {
  interface Navigator {
    getBattery: any;
    oscpu: any;
  }

  interface Window {
    InstallTrigger?: any;
    ApplePaySession?: any;
    chrome?: any;
    MSStream?: any;
  }
}

/**
 * Web implementation of the Device plugin.
 *
 * This implementation exists primarily to satisfy Capacitor's
 * multi-platform contract and to allow usage in browser-based
 * environments. Device data is derived from browser APIs such as
 * user agent parsing and the Battery Status API.
 *
 * Native-only features like hardware and OS identifiers are unavailable on Web.
 */
export class DeviceWeb extends WebPlugin implements DevicePlugin {
  private batteryApi: any = null;
  private batteryListenersAttached = false;
  private readonly uidCache = new Map<string, string>();

  // -----------------------------------------------------------------------------
  // Battery Charging State Events
  // -----------------------------------------------------------------------------

  private readonly handleBatteryChargingChange = (): void => {
    if (!this.batteryApi) {
      return;
    }
    this.notifyListeners('batteryChargingStateChange', {
      batteryLevel: this.batteryApi.level,
      isCharging: this.batteryApi.charging,
    });
  };

  private async attachBatteryListeners(): Promise<void> {
    if (this.batteryListenersAttached || typeof navigator === 'undefined' || !navigator.getBattery) {
      return;
    }
    try {
      this.batteryApi = await navigator.getBattery();
      this.batteryApi.addEventListener('chargingchange', this.handleBatteryChargingChange);
      this.batteryListenersAttached = true;
    } catch {
      // Battery Status API unavailable or denied
    }
  }

  private detachBatteryListeners(): void {
    if (this.batteryApi && this.batteryListenersAttached) {
      this.batteryApi.removeEventListener('chargingchange', this.handleBatteryChargingChange);
      this.batteryListenersAttached = false;
      this.batteryApi = null;
    }
  }

  // -----------------------------------------------------------------------------
  // Event Listener Overrides
  // -----------------------------------------------------------------------------

  async addListener(
    eventName: 'batteryChargingStateChange',
    listenerFunc: BatteryChargingStateChangeListener,
  ): Promise<PluginListenerHandle> {
    if (eventName === 'batteryChargingStateChange') {
      void this.attachBatteryListeners();
    }
    return super.addListener(eventName, listenerFunc);
  }

  async removeAllListeners(): Promise<void> {
    this.detachBatteryListeners();
    return super.removeAllListeners();
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
  // Public API Methods
  // -----------------------------------------------------------------------------

  async getId(): Promise<DeviceId> {
    return {
      identifier: this.getUid(),
    };
  }

  async getInfo(): Promise<DeviceInfo> {
    if (typeof navigator === 'undefined' || !navigator.userAgent) {
      throw this.unavailable('Device API not available in this browser');
    }

    const ua = navigator.userAgent;
    const uaFields = this.parseUa(ua);

    return {
      model: uaFields.model,
      platform: 'web' as const,
      operatingSystem: uaFields.operatingSystem,
      osVersion: uaFields.osVersion,
      manufacturer: navigator.vendor,
      isVirtual: false,
      webViewVersion: uaFields.browserVersion,
    };
  }

  async getBatteryInfo(): Promise<BatteryInfo> {
    if (typeof navigator === 'undefined' || !navigator.getBattery) {
      throw this.unavailable('Device API not available in this browser');
    }
    let battery: any = {};

    try {
      battery = await navigator.getBattery();
    } catch {
      // Let it fail, we don't care
    }

    return {
      batteryLevel: battery.level,
      isCharging: battery.charging,
    };
  }

  async getLanguageCode(): Promise<GetLanguageCodeResult> {
    return {
      value: navigator.language.split('-')[0].toLowerCase(),
    };
  }

  async getLanguageTag(): Promise<LanguageTag> {
    return {
      value: navigator.language,
    };
  }

  async getBatteryExtras(): Promise<BatteryExtras> {
    if (typeof navigator === 'undefined' || !navigator.getBattery) {
      throw this.unavailable('Battery extras are not available on web');
    }

    let battery: any = {};
    try {
      battery = await navigator.getBattery();
    } catch {
      // Battery Status API unavailable
    }

    const charging = battery.charging ?? false;
    const chargingTime = battery.chargingTime ?? Infinity;

    const detailedState: BatteryExtras['detailedState'] = (() => {
      if (charging && chargingTime === 0) return 'full';
      if (charging) return 'charging';
      return 'unplugged';
    })();

    const chargeSource: BatteryExtras['chargeSource'] = charging ? 'ac' : 'unknown';

    return {
      chargeSource,
      detailedState,
    };
  }

  async getDisplayInfo(): Promise<DisplayInfo> {
    if (typeof window === 'undefined') {
      throw this.unavailable('Display info not available in this environment');
    }

    return {
      widthPx: window.screen.width,
      heightPx: window.screen.height,
      densityDpi: window.devicePixelRatio * 160,
      scale: window.devicePixelRatio,
      refreshRateHz: 60,
    };
  }

  async getConfiguration(): Promise<DeviceConfiguration> {
    if (typeof window === 'undefined' || typeof navigator === 'undefined') {
      throw this.unavailable('Configuration not available in this environment');
    }

    const isDarkMode = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    const orientation = window.screen.orientation?.type?.includes('portrait') ? 'portrait' : 'landscape';

    return {
      orientation: orientation as 'portrait' | 'landscape',
      isDarkMode,
      fontScale: 1.0,
      idiom: 'phone',
      screenSize: 'normal',
    };
  }

  async getPowerState(): Promise<PowerState> {
    throw this.unavailable('Power state is not available on web');
  }

  async getMemoryInfo(): Promise<MemoryInfo> {
    const cpuCores = typeof navigator !== 'undefined' ? (navigator.hardwareConcurrency ?? 0) : 0;

    // navigator.deviceMemory returns GB as a number (Chrome/Opera only)
    const deviceMemoryGB = typeof navigator !== 'undefined' ? (navigator as any).deviceMemory : undefined;
    const physicalRam = deviceMemoryGB != null ? deviceMemoryGB * 1024 * 1024 * 1024 : 0;

    // performance.memory is Chrome-only (usedJSHeapSize, jsHeapSizeLimit)
    const perfMemory = typeof performance !== 'undefined' ? (performance as any).memory : undefined;
    const memoryClassMb = perfMemory?.jsHeapSizeLimit
      ? Math.round(perfMemory.jsHeapSizeLimit / (1024 * 1024))
      : Math.round(physicalRam / (1024 * 1024));

    return {
      physicalRam,
      cpuCores,
      memoryClassMb,
      isLowRamDevice: deviceMemoryGB != null && deviceMemoryGB <= 1,
      cpuUsagePercent: null,
      memoryPressure: 'unknown',
    };
  }

  async getStorageInfo(): Promise<StorageInfo> {
    if (typeof navigator === 'undefined' || !navigator.storage?.estimate) {
      throw this.unavailable('Storage info not available in this environment');
    }

    try {
      const estimate = await navigator.storage.estimate();
      const quota = estimate.quota ?? 0;
      const usage = estimate.usage ?? 0;
      const freeBytes = quota - usage;
      const usedPercent = quota > 0 ? (usage / quota) * 100.0 : 0.0;

      return {
        totalBytes: quota,
        freeBytes,
        usedBytes: usage,
        usedPercent,
      };
    } catch {
      return {
        totalBytes: 0,
        freeBytes: 0,
        usedBytes: 0,
        usedPercent: 0,
      };
    }
  }

  async getSystemUptime(): Promise<SystemUptime> {
    throw this.unavailable('System uptime is not available on web');
  }

  async getAppVersion(): Promise<AppVersion> {
    return {
      version: PLUGIN_VERSION,
      buildNumber: 0,
    };
  }

  // -----------------------------------------------------------------------------
  // Implementation Details
  // -----------------------------------------------------------------------------

  parseUa(ua: string): any {
    const uaFields: any = {};
    const start = ua.indexOf('(') + 1;
    let end = ua.indexOf(') AppleWebKit');
    if (ua.indexOf(') Gecko') !== -1) {
      end = ua.indexOf(') Gecko');
    }
    const fields = ua.substring(start, end);
    if (ua.indexOf('Android') !== -1) {
      const tmpFields = fields.replace('; wv', '').split('; ').pop();
      if (tmpFields) {
        uaFields.model = tmpFields.split(' Build')[0];
      }
      uaFields.osVersion = fields.split('; ')[1];
    } else {
      uaFields.model = fields.split('; ')[0];
      if (typeof navigator !== 'undefined' && navigator.oscpu) {
        uaFields.osVersion = navigator.oscpu;
      } else {
        if (ua.indexOf('Windows') !== -1) {
          uaFields.osVersion = fields;
        } else {
          const tmpFields = fields.split('; ').pop();
          if (tmpFields) {
            const lastParts = tmpFields.replace(' like Mac OS X', '').split(' ');
            uaFields.osVersion = lastParts[lastParts.length - 1].replace(/_/g, '.');
          }
        }
      }
    }

    if (/android/i.test(ua)) {
      uaFields.operatingSystem = 'android';
    } else if (/iPad|iPhone|iPod/.test(ua) && !window.MSStream) {
      uaFields.operatingSystem = 'ios';
    } else if (/Win/.test(ua)) {
      uaFields.operatingSystem = 'windows';
    } else if (/Mac/i.test(ua)) {
      uaFields.operatingSystem = 'mac';
    } else {
      uaFields.operatingSystem = 'unknown';
    }

    // Check for browsers based on non-standard javascript apis, only not user agent
    const isSafari = !!window.ApplePaySession;
    const isChrome = !!window.chrome;
    const isFirefox = /Firefox/.test(ua);
    const isEdge = /Edg/.test(ua);
    const isFirefoxIOS = /FxiOS/.test(ua);
    const isChromeIOS = /CriOS/.test(ua);
    const isEdgeIOS = /EdgiOS/.test(ua);

    // FF and Edge User Agents both end with "/MAJOR.MINOR"
    if (isSafari || (isChrome && !isEdge) || isFirefoxIOS || isChromeIOS || isEdgeIOS) {
      // Safari version comes as     "... Version/MAJOR.MINOR ..."
      // Chrome version comes as     "... Chrome/MAJOR.MINOR ..."
      // FirefoxIOS version comes as "... FxiOS/MAJOR.MINOR ..."
      // ChromeIOS version comes as  "... CriOS/MAJOR.MINOR ..."
      let searchWord: string;
      if (isFirefoxIOS) {
        searchWord = 'FxiOS';
      } else if (isChromeIOS) {
        searchWord = 'CriOS';
      } else if (isEdgeIOS) {
        searchWord = 'EdgiOS';
      } else if (isSafari) {
        searchWord = 'Version';
      } else {
        searchWord = 'Chrome';
      }

      const words = ua.split(' ');
      for (const word of words) {
        if (word.includes(searchWord)) {
          const version = word.split('/')[1];
          uaFields.browserVersion = version;
        }
      }
    } else if (isFirefox || isEdge) {
      const reverseUA = ua.split('').reverse().join('');
      const reverseVersion = reverseUA.split('/')[0];
      const version = reverseVersion.split('').reverse().join('');
      uaFields.browserVersion = version;
    }

    return uaFields;
  }

  private getUid(): string {
    const key = '_capuid';
    const cached = this.uidCache.get(key);
    if (cached) {
      return cached;
    }
    const uid = this.uuid4();
    this.uidCache.set(key, uid);
    return uid;
  }

  uuid4(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      const r = (Math.random() * 16) | 0,
        v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }
}
