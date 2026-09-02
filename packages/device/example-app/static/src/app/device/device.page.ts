import { ChangeDetectionStrategy, Component, signal, computed } from '@angular/core';
import { PluginListenerHandle } from '@capacitor/core';
import {
  ActionSheetController,
  ToastController,
  IonContent,
  IonItem,
  IonLabel,
  IonText,
  IonList,
  IonButton,
  IonIcon,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
} from '@ionic/angular';
import {
  Device,
  PluginVersionResult,
  DeviceId,
  DeviceInfo,
  BatteryInfo,
  BatteryExtras,
  DisplayInfo,
  DeviceConfiguration,
  PowerState,
  MemoryInfo,
  StorageInfo,
  SystemUptime,
  AppVersion,
  GetLanguageCodeResult,
  LanguageTag,
} from '@cap-kit/device';
import { addIcons } from 'ionicons';
import {
  home,
  refresh,
  phonePortrait,
  batteryHalf,
  language,
  hardwareChip,
  snow,
  speedometer,
  disc,
  timer,
  settings,
} from 'ionicons/icons';
import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

interface Row {
  key: string;
  value: string;
}

/**
 * Demo page that exercises the read-only getters exposed by the
 * `@cap-kit/device` plugin, rendering each result as a collapsible card
 * with human-readable key/value rows.
 */
@Component({
  selector: 'app-device',
  templateUrl: './device.page.html',
  styleUrls: ['./device.page.scss'],
  standalone: true,
  imports: [
    IonIcon,
    IonButton,
    IonItem,
    IonLabel,
    IonText,
    IonList,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardContent,
    PageHeaderComponent,
    InfoCardComponent,
    PageFooterComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DevicePage {
  public readonly isSupported = signal(false);
  public readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);
  public readonly isLoading = signal(false);

  // Identity
  public readonly deviceId = signal<DeviceId | undefined>(undefined);
  public readonly appVersion = signal<AppVersion | undefined>(undefined);

  // Device
  public readonly deviceInfo = signal<DeviceInfo | undefined>(undefined);

  // Battery
  public readonly batteryInfo = signal<BatteryInfo | undefined>(undefined);
  public readonly batteryExtras = signal<BatteryExtras | undefined>(undefined);

  // Language
  public readonly languageCode = signal<GetLanguageCodeResult | undefined>(undefined);
  public readonly languageTag = signal<LanguageTag | undefined>(undefined);

  // Display & configuration
  public readonly displayInfo = signal<DisplayInfo | undefined>(undefined);
  public readonly configuration = signal<DeviceConfiguration | undefined>(undefined);

  // Power & resources
  public readonly powerState = signal<PowerState | undefined>(undefined);
  public readonly memoryInfo = signal<MemoryInfo | undefined>(undefined);
  public readonly storageInfo = signal<StorageInfo | undefined>(undefined);

  // System
  public readonly systemUptime = signal<SystemUptime | undefined>(undefined);

  // Events
  public readonly isObservingBattery = signal(false);
  private batteryListener?: PluginListenerHandle;

  public readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  public readonly countErrors = computed(() => this.errors().length);
  public readonly errors = signal<string[]>([]);

  public readonly deviceInfoRows = computed(() =>
    this.toRows(this.deviceInfo() as unknown as Record<string, unknown> | undefined),
  );
  public readonly batteryInfoRows = computed(() =>
    this.toRows(this.batteryInfo() as unknown as Record<string, unknown> | undefined),
  );
  public readonly batteryExtrasRows = computed(() =>
    this.toRows(this.batteryExtras() as unknown as Record<string, unknown> | undefined),
  );
  public readonly displayRows = computed(() =>
    this.toRows(this.displayInfo() as unknown as Record<string, unknown> | undefined),
  );
  public readonly configRows = computed(() =>
    this.toRows(this.configuration() as unknown as Record<string, unknown> | undefined),
  );
  public readonly powerRows = computed(() =>
    this.toRows(this.powerState() as unknown as Record<string, unknown> | undefined),
  );
  public readonly memoryRows = computed(() =>
    this.toRows(this.memoryInfo() as unknown as Record<string, unknown> | undefined),
  );
  public readonly storageRows = computed(() =>
    this.toRows(this.storageInfo() as unknown as Record<string, unknown> | undefined),
  );

  public readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/device';

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!Device);
    this.initPluginVersion();
    addIcons({
      home,
      refresh,
      phonePortrait,
      batteryHalf,
      language,
      hardwareChip,
      snow,
      speedometer,
      disc,
      timer,
      settings,
    });

    if (this.isSupported()) {
      this.loadAll();
    }
  }

  // ---------------------------------------------------------------------------
  // Formatting helpers
  // ---------------------------------------------------------------------------

  /** Pretty-print a bytes count into a human readable unit string. */
  public formatBytes(bytes: number | undefined): string {
    if (bytes == null || typeof bytes !== 'number' || !Number.isFinite(bytes)) return 'n/a';
    if (bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
    return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
  }

  /** Pretty-print a percentage value. */
  public formatPercent(value: number | undefined | null): string {
    if (value == null || !Number.isFinite(value)) return 'n/a';
    return `${value.toFixed(1)}%`;
  }

  /** Pretty-print seconds into a human readable duration string. */
  public formatUptime(seconds: number | undefined): string {
    if (seconds == null || !Number.isFinite(seconds)) return 'n/a';
    if (seconds < 60) return `${Math.round(seconds)}s`;
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    if (days > 0) return `${days}d ${hours}h ${mins}m`;
    if (hours > 0) return `${hours}h ${mins}m`;
    return `${mins}m`;
  }

  /**
   * Convert a result object into an ordered list of key/value rows, applying
   * specialized formatting for common numeric fields (bytes, percent, uptime).
   */
  private toRows(obj: Record<string, unknown> | undefined): Row[] {
    if (!obj) return [];
    return Object.entries(obj).map(([key, value]) => ({
      key: this.prettifyKey(key),
      value: this.formatValue(key, value),
    }));
  }

  private prettifyKey(key: string): string {
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (c) => c.toUpperCase())
      .trim();
  }

  private formatValue(key: string, value: unknown): string {
    if (value == null) return 'n/a';
    if (typeof value === 'boolean') return value ? 'Yes' : 'No';
    if (typeof value === 'number') {
      if (key.toLowerCase().includes('bytes')) return this.formatBytes(value);
      if (key.toLowerCase().includes('percent')) return this.formatPercent(value);
      if (key.toLowerCase().includes('uptime')) return this.formatUptime(value);
      return String(value);
    }
    if (typeof value === 'object') return JSON.stringify(value);
    return String(value);
  }

  // ---------------------------------------------------------------------------
  // UI helpers
  // ---------------------------------------------------------------------------

  private async presentSuccess(message: string): Promise<void> {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2000,
      position: 'bottom',
    });
    await toast.present();
  }

  private async presentError(error: any): Promise<void> {
    const sheet = await this.actionSheetCtrl.create({
      header: 'Plugin Error',
      subHeader: error?.code ?? 'UNKNOWN',
      buttons: [
        {
          text: (error?.message ?? typeof error === 'object') ? JSON.stringify(error) : String(error),
          role: 'destructive',
          cssClass: 'action-sheet-message',
          handler: () => {},
        },
        { text: 'OK', role: 'cancel' },
      ],
    });
    await sheet.present();
  }

  private trackError(label: string, error: unknown): void {
    const message = error instanceof Error ? error.message : JSON.stringify(error);
    this.errors.update((list) => [...list, `${label}: ${message}`]);
  }

  // ---------------------------------------------------------------------------
  // Actions
  // ---------------------------------------------------------------------------

  public openOnGithub(): void {
    window.open(this.GH_URL, '_blank');
  }

  private async initPluginVersion(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const result: PluginVersionResult = await Device.getPluginVersion();
      this.pluginVersion.set(result);
    } catch (e) {
      console.error('Error getting plugin version', e);
      this.pluginVersion.set('Error');
    }
  }

  /** Loads every read-only getter, collapsing per-section failures. */
  public async loadAll(): Promise<void> {
    this.isLoading.set(true);
    this.errors.set([]);

    const tasks: Array<Promise<void>> = [
      this.safeLoad(
        'id',
        () => Device.getId(),
        (r) => this.deviceId.set(r),
      ),
      this.safeLoad(
        'appVersion',
        () => Device.getAppVersion(),
        (r) => this.appVersion.set(r),
      ),
      this.safeLoad(
        'info',
        () => Device.getInfo(),
        (r) => this.deviceInfo.set(r),
      ),
      this.safeLoad(
        'batteryInfo',
        () => Device.getBatteryInfo(),
        (r) => this.batteryInfo.set(r),
      ),
      this.safeLoad(
        'batteryExtras',
        () => Device.getBatteryExtras(),
        (r) => this.batteryExtras.set(r),
      ),
      this.safeLoad(
        'languageCode',
        () => Device.getLanguageCode(),
        (r) => this.languageCode.set(r),
      ),
      this.safeLoad(
        'languageTag',
        () => Device.getLanguageTag(),
        (r) => this.languageTag.set(r),
      ),
      this.safeLoad(
        'displayInfo',
        () => Device.getDisplayInfo(),
        (r) => this.displayInfo.set(r),
      ),
      this.safeLoad(
        'configuration',
        () => Device.getConfiguration(),
        (r) => this.configuration.set(r),
      ),
      this.safeLoad(
        'powerState',
        () => Device.getPowerState(),
        (r) => this.powerState.set(r),
      ),
      this.safeLoad(
        'memoryInfo',
        () => Device.getMemoryInfo(),
        (r) => this.memoryInfo.set(r),
      ),
      this.safeLoad(
        'storageInfo',
        () => Device.getStorageInfo(),
        (r) => this.storageInfo.set(r),
      ),
      this.safeLoad(
        'systemUptime',
        () => Device.getSystemUptime(),
        (r) => this.systemUptime.set(r),
      ),
    ];

    await Promise.all(tasks);
    this.isLoading.set(false);
  }

  /**
   * Wrap a single getter so failures are recorded but never block the other
   * sections from loading.
   */
  private async safeLoad<T>(label: string, fn: () => Promise<T>, setter: (value: T) => void): Promise<void> {
    try {
      setter(await fn());
    } catch (e: any) {
      this.trackError(label, e);
    }
  }

  /** Reloads data; used by the refresh button and useful for delta-based CPU. */
  public async onRefresh(): Promise<void> {
    await this.loadAll();
    const cpu = this.memoryInfo()?.cpuUsagePercent;
    if (cpu == null) {
      await this.presentSuccess('Refreshed. Call again for CPU usage (delta-based).');
    } else {
      await this.presentSuccess(`Refreshed. CPU usage: ${cpu.toFixed(1)}%`);
    }
  }

  // ---------------------------------------------------------------------------
  // Event listener demo (batteryChargingStateChange)
  // ---------------------------------------------------------------------------

  public async onToggleBatteryListener(): Promise<void> {
    if (this.isObservingBattery()) {
      await this.batteryListener?.remove();
      this.isObservingBattery.set(false);
      await this.presentSuccess('Battery listener removed');
      return;
    }

    try {
      this.batteryListener = await Device.addListener('batteryChargingStateChange', (info) => {
        const percent = info.batteryLevel != null ? Math.round(info.batteryLevel * 100) : 'n/a';
        this.presentSuccess(`Charging changed: ${info.isCharging ? 'charging' : 'on battery'} (${percent}%)`).catch(
          () => {},
        );
        this.refreshBatteryInfo();
      });
      this.isObservingBattery.set(true);
      await this.presentSuccess('Battery listener active');
    } catch (e) {
      await this.presentError(e);
    }
  }

  private async refreshBatteryInfo(): Promise<void> {
    try {
      this.batteryInfo.set(await Device.getBatteryInfo());
    } catch {
      /* ignore – listener toast already informs the user */
    }
  }
}
