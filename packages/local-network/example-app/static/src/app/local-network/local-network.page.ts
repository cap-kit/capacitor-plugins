/**
 * @file local-network.page.ts
 * Demo page for the Local Network plugin: current permission state, request
 * flow and a timestamped log of `permissionChange` events.
 */
import { ChangeDetectionStrategy, Component, OnDestroy, computed, signal } from '@angular/core';

import {
  ActionSheetController,
  IonBadge,
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonContent,
  IonIcon,
  IonItem,
  IonLabel,
  IonNote,
  IonSpinner,
  IonText,
  ToastController,
} from '@ionic/angular';

import type { PermissionState, PluginListenerHandle } from '@capacitor/core';

import { LocalNetwork } from '@cap-kit/local-network';

import { addIcons } from 'ionicons';
import {
  informationCircleOutline,
  refresh,
  settingsOutline,
  shieldCheckmarkOutline,
  syncOutline,
  terminalOutline,
  wifi,
} from 'ionicons/icons';

import { InfoCardComponent } from '../components/info-card.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { PageHeaderComponent } from '../components/page-header.component';

/** Maximum number of entries kept in the event log. */
const LOG_LIMIT = 50;

@Component({
  selector: 'app-local-network',
  standalone: true,
  templateUrl: './local-network.page.html',
  styleUrls: ['./local-network.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardContent,
    IonButton,
    IonIcon,
    IonSpinner,
    IonBadge,
    IonItem,
    IonLabel,
    IonText,
    IonNote,
    PageHeaderComponent,
    PageFooterComponent,
    InfoCardComponent,
  ],
})
export class LocalNetworkPage implements OnDestroy {
  readonly isSupported = signal(false);
  readonly pluginVersion = signal<string | undefined>(undefined);
  readonly inProgress = signal(false);

  readonly displayVersion = computed(() => this.pluginVersion() ?? 'Unknown');

  /** Last state read from `checkPermissions()` or pushed by an event. */
  readonly permissionState = signal<PermissionState | null>(null);

  /** State resolved by the last `requestPermissions()` call. */
  readonly lastRequestResult = signal<PermissionState | null>(null);

  readonly logs = signal<string[]>([]);

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/local-network';

  private listeners: PluginListenerHandle[] = [];

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!LocalNetwork);
    void this.initPluginVersion();
    void this.refreshPermission();
    void this.initEventListeners();

    addIcons({
      informationCircleOutline,
      refresh,
      settingsOutline,
      shieldCheckmarkOutline,
      syncOutline,
      terminalOutline,
      wifi,
    });
  }

  async ngOnDestroy(): Promise<void> {
    for (const listener of this.listeners) {
      await listener.remove();
    }
    this.listeners = [];
  }

  // ---------------------------------------------------------------------------
  // UI helpers
  // ---------------------------------------------------------------------------

  openOnGithub(): void {
    window.open(this.GH_URL, '_blank');
  }

  permissionLabel(state: PermissionState | null): string {
    switch (state) {
      case 'granted':
        return 'Granted';
      case 'denied':
        return 'Denied';
      case 'prompt':
        return 'Prompt';
      case 'prompt-with-rationale':
        return 'Prompt (rationale)';
      default:
        return 'Unknown';
    }
  }

  permissionColor(state: PermissionState | null): string {
    switch (state) {
      case 'granted':
        return 'success';
      case 'denied':
        return 'danger';
      case 'prompt':
      case 'prompt-with-rationale':
        return 'warning';
      default:
        return 'medium';
    }
  }

  clearLogs(): void {
    this.logs.set([]);
  }

  private addLog(message: string): void {
    const timestamp = new Date().toLocaleTimeString();
    this.logs.update((logs) => [`[${timestamp}] ${message}`, ...logs].slice(0, LOG_LIMIT));
  }

  private async showToast(message: string, color: string = 'success'): Promise<void> {
    const toast = await this.toastCtrl.create({
      message,
      duration: 1500,
      color,
    });
    await toast.present();
  }

  private async presentError(error: any): Promise<void> {
    this.addLog(`ERROR: ${error?.message ?? String(error)}`);
    const sheet = await this.actionSheetCtrl.create({
      header: 'Plugin Error',
      subHeader: error?.code ?? 'UNKNOWN',
      buttons: [
        {
          text: error?.message ?? String(error),
          role: 'destructive',
        },
        { text: 'OK', role: 'cancel' },
      ],
    });
    await sheet.present();
  }

  // ---------------------------------------------------------------------------
  // Plugin actions
  // ---------------------------------------------------------------------------

  private async initPluginVersion(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const result = await LocalNetwork.getPluginVersion();
      this.pluginVersion.set(result.version);
    } catch {
      this.pluginVersion.set('Error');
    }
  }

  private async initEventListeners(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const handle = await LocalNetwork.addListener('permissionChange', (event) => {
        this.permissionState.set(event.localNetwork);
        this.addLog(`permissionChange: ${event.localNetwork}`);
      });
      this.listeners.push(handle);
      this.addLog('permissionChange listener registered');
    } catch (error) {
      console.error('Failed to register listeners', error);
      this.addLog('ERROR: failed to register permissionChange listener');
    }
  }

  async refreshPermission(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    try {
      const { localNetwork } = await LocalNetwork.checkPermissions();
      this.permissionState.set(localNetwork);
      this.addLog(`checkPermissions resolved: ${localNetwork}`);
    } catch (error) {
      await this.presentError(error);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onRequestPermission(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    this.lastRequestResult.set(null);
    try {
      const { localNetwork } = await LocalNetwork.requestPermissions();
      this.permissionState.set(localNetwork);
      this.lastRequestResult.set(localNetwork);
      this.addLog(`requestPermissions resolved: ${localNetwork}`);
      await this.showToast(
        `Local network permission: ${this.permissionLabel(localNetwork)}`,
        localNetwork === 'granted' ? 'success' : 'warning',
      );
    } catch (error) {
      await this.presentError(error);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onGetVersion(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    try {
      const { version } = await LocalNetwork.getPluginVersion();
      this.pluginVersion.set(version);
      this.addLog(`Plugin version: ${version}`);
    } catch (error) {
      await this.presentError(error);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onRemoveAllListeners(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      await LocalNetwork.removeAllListeners();
      this.listeners = [];
      this.addLog('Listeners removed via removeAllListeners');
      await this.initEventListeners();
      await this.showToast('Listeners reset and re-registered');
    } catch (error) {
      await this.presentError(error);
    }
  }
}
