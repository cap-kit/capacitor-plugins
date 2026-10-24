import { ChangeDetectionStrategy, Component, OnDestroy, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonButton,
  IonIcon,
  IonSpinner,
  ActionSheetController,
  ToastController,
  IonBadge,
  IonItem,
  IonText,
  IonLabel,
  IonToggle,
  IonItemDivider,
  IonSelect,
  IonSelectOption,
  IonNote,
} from '@ionic/angular';

import { PrivacyScreen } from '@cap-kit/privacy-screen';

import type { PluginListenerHandle } from '@capacitor/core';

import { addIcons } from 'ionicons';
import {
  lockClosed,
  lockOpen,
  refresh,
  checkmarkCircle,
  alertCircle,
  shieldCheckmarkOutline,
  eyeOffOutline,
  eyeOutline,
  settingsOutline,
  terminalOutline,
} from 'ionicons/icons';

import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

type ResultState = 'idle' | 'success' | 'failure' | 'error';
type BlurEffect = 'none' | 'light' | 'dark';
type HiddenMode = 'none' | 'dim' | 'splash';

@Component({
  selector: 'app-privacy-screen',
  standalone: true,
  templateUrl: './privacy-screen.page.html',
  styleUrls: ['./privacy-screen.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
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
    IonText,
    IonLabel,
    IonToggle,
    IonItemDivider,
    IonSelect,
    IonSelectOption,
    PageHeaderComponent,
    PageFooterComponent,
    InfoCardComponent,
    IonNote,
  ],
})
export class PrivacyScreenPage implements OnDestroy {
  readonly isSupported = signal(false);
  readonly pluginVersion = signal<string | undefined>(undefined);
  readonly inProgress = signal(false);

  readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return current ?? 'Unknown';
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/privacy-screen';

  readonly protectionEnabled = signal<boolean | null>(null);
  readonly enableState = signal<ResultState>('idle');
  readonly disableState = signal<ResultState>('idle');

  readonly dimBackground = signal(false);
  readonly blurEffect = signal<BlurEffect>('none');
  readonly hiddenMode = signal<HiddenMode>('none');

  readonly logs = signal<string[]>([]);

  private listeners: PluginListenerHandle[] = [];

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!PrivacyScreen);
    this.initPluginVersion();
    this.refreshStatus();
    this.initEventListeners();

    addIcons({
      lockClosed,
      lockOpen,
      refresh,
      checkmarkCircle,
      alertCircle,
      shieldCheckmarkOutline,
      eyeOffOutline,
      eyeOutline,
      settingsOutline,
      terminalOutline,
    });
  }

  async ngOnDestroy(): Promise<void> {
    for (const listener of this.listeners) {
      await listener.remove();
    }
  }

  private async initPluginVersion(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const result = await PrivacyScreen.getPluginVersion();
      this.pluginVersion.set(result.version);
    } catch {
      this.pluginVersion.set('Error');
    }
  }

  private async initEventListeners(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const screenshotHandle = await PrivacyScreen.addListener('screenshotTaken', () => {
        this.addLog('Screenshot taken (observed after the fact)');
        void this.showToast('Screenshot detected', 'warning');
      });
      this.listeners.push(screenshotHandle);
      this.addLog('Event listeners registered');
    } catch (e) {
      console.error('Failed to register listeners', e);
    }
  }

  openOnGithub(): void {
    window.open(this.GH_URL, '_blank');
  }

  badgeColor(state: ResultState): string {
    switch (state) {
      case 'success':
        return 'success';
      case 'failure':
        return 'warning';
      case 'error':
        return 'danger';
      default:
        return 'medium';
    }
  }

  badgeLabel(state: ResultState): string {
    return state.toUpperCase();
  }

  private addLog(message: string): void {
    const timestamp = new Date().toLocaleTimeString();
    this.logs.update((logs) => [`[${timestamp}] ${message}`, ...logs].slice(0, 50));
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

  private async showToast(message: string, color: string = 'success'): Promise<void> {
    const toast = await this.toastCtrl.create({
      message,
      duration: 1500,
      color,
    });
    await toast.present();
  }

  async refreshStatus(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const { enabled } = await PrivacyScreen.isEnabled();
      this.protectionEnabled.set(enabled);
      this.addLog(`Status refreshed: enabled=${enabled}`);
    } catch (e) {
      console.error('Failed to refresh status', e);
    }
  }

  async onEnable(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    this.enableState.set('idle');
    try {
      const { success } = await PrivacyScreen.enable({
        android: {
          dimBackground: this.dimBackground(),
          privacyModeOnActivityHidden: this.hiddenMode(),
        },
        ios: {
          blurEffect: this.blurEffect(),
        },
      });
      this.enableState.set(success ? 'success' : 'failure');
      this.addLog(
        `Protection enabled (dim=${this.dimBackground()}, blur=${this.blurEffect()}, hidden=${this.hiddenMode()})`,
      );
      await this.showToast('Privacy protection enabled');
      await this.refreshStatus();
    } catch (e: any) {
      this.enableState.set('error');
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onDisable(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    this.disableState.set('idle');
    try {
      const { success } = await PrivacyScreen.disable();
      this.disableState.set(success ? 'success' : 'failure');
      this.addLog('Protection disabled');
      await this.showToast('Privacy protection disabled', 'warning');
      await this.refreshStatus();
    } catch (e: any) {
      this.disableState.set('error');
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onGetVersion(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const { version } = await PrivacyScreen.getPluginVersion();
      this.pluginVersion.set(version);
      this.addLog(`Plugin version: ${version}`);
    } catch (e) {
      await this.presentError(e);
    }
  }

  clearLogs(): void {
    this.logs.set([]);
  }

  async onRemoveAllListeners(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      await PrivacyScreen.removeAllListeners();
      this.listeners = [];
      this.addLog('Listeners cleared via removeAllListeners — watcher re-registered');
      await this.initEventListeners();
    } catch (e) {
      console.error('Failed to remove listeners', e);
    }
  }
}
