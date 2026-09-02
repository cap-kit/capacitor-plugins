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
  IonSegment,
  IonSegmentButton,
  IonItemDivider,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonChip,
  IonNote,
} from '@ionic/angular';

// import { Fortress, DeviceSecurityStatus, FortressSession, SecureValue } from '@cap-kit/fortress';

import { PluginListenerHandle } from '@capacitor/core';

import { addIcons } from 'ionicons';
import {
  lockClosed,
  lockOpen,
  key,
  trash,
  addCircle,
  refresh,
  fingerPrint,
  checkmarkCircle,
  alertCircle,
  shieldCheckmarkOutline,
  copyOutline,
  eyeOffOutline,
  eyeOutline,
  analyticsOutline,
  constructOutline,
  keyOutline,
  settingsOutline,
  terminalOutline,
} from 'ionicons/icons';

import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

type ResultState = 'idle' | 'success' | 'failure' | 'error';

type FortressTab = 'storage' | 'session' | 'crypto' | 'config';
type FallbackStrategy = 'none' | 'deviceCredential' | 'systemDefault';
type OverlayTheme = 'system' | 'light' | 'dark';

type RuntimeConfigSnapshot = Partial<{
  verboseLogging: boolean;
  enablePrivacyScreen: boolean;
  privacyOverlayText: string;
  privacyOverlayImageName: string;
  privacyOverlayShowText: boolean;
  privacyOverlayShowImage: boolean;
  privacyOverlayTextColor: string;
  privacyOverlayBackgroundOpacity: number;
  privacyOverlayTheme: OverlayTheme;
  fallbackStrategy: FallbackStrategy;
  persistSessionState: boolean;
}>;

@Component({
  selector: 'app-fortress',
  standalone: true,
  templateUrl: './fortress.page.html',
  styleUrls: ['./fortress.page.scss'],
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
    IonSegment,
    IonSegmentButton,
    IonItemDivider,
    IonInput,
    IonSelect,
    IonSelectOption,
    IonChip,
    PageHeaderComponent,
    PageFooterComponent,
    InfoCardComponent,
    IonNote,
  ],
})
export class FortressPage implements OnDestroy {
  readonly isSupported = signal(false);
  readonly pluginVersion = signal<string | undefined>(undefined);
  readonly activeTab = signal<FortressTab>('storage');
  readonly inProgress = signal(false);

  readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return current ?? 'Unknown';
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/fortress';

  // readonly securityStatus = signal<DeviceSecurityStatus | null>(null);
  // readonly session = signal<FortressSession | null>(null);

  readonly storageKey = signal('');
  readonly storageValue = signal('');
  readonly storedKeys = signal<string[]>([]);
  readonly lastRetrievedValue = signal<string | null>(null);

  readonly unlockState = signal<ResultState>('idle');
  readonly lockState = signal<ResultState>('idle');
  readonly setValueState = signal<ResultState>('idle');
  readonly getValueState = signal<ResultState>('idle');
  readonly removeValueState = signal<ResultState>('idle');
  readonly clearAllState = signal<ResultState>('idle');

  readonly biometricKeysExist = signal<boolean | null>(null);
  readonly createKeysState = signal<ResultState>('idle');
  readonly deleteKeysState = signal<ResultState>('idle');
  readonly createSignatureState = signal<ResultState>('idle');
  readonly lastSignature = signal<string | null>(null);

  readonly showConfig = signal(false);
  readonly verboseLogging = signal(false);
  readonly privacyScreen = signal(true);
  readonly privacyOverlayText = signal('');
  readonly appliedPrivacyOverlayText = signal('');
  readonly isPrivacyOverlayTextDirty = computed(() => this.privacyOverlayText() !== this.appliedPrivacyOverlayText());
  readonly privacyOverlayImageName = signal('');
  readonly privacyOverlayShowText = signal(true);
  readonly privacyOverlayShowImage = signal(true);
  readonly privacyOverlayTextColor = signal('');
  readonly privacyOverlayBackgroundOpacity = signal('-1');
  readonly privacyOverlayTheme = signal<OverlayTheme>('system');
  readonly fallbackStrategy = signal<FallbackStrategy>('systemDefault');
  readonly persistSessionState = signal(false);

  readonly logs = signal<string[]>([]);

  private listeners: PluginListenerHandle[] = [];

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    // this.isSupported.set(!!Fortress);
    this.initPluginVersion();
    this.loadRuntimeConfig();
    this.initEventListeners();

    addIcons({
      lockClosed,
      lockOpen,
      key,
      trash,
      addCircle,
      refresh,
      fingerPrint,
      checkmarkCircle,
      alertCircle,
      shieldCheckmarkOutline,
      copyOutline,
      eyeOffOutline,
      eyeOutline,
      analyticsOutline,
      constructOutline,
      keyOutline,
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
    // if (!this.isSupported()) return;
    // try {
    //   const result = await Fortress.getPluginVersion();
    //   this.pluginVersion.set(result.version);
    // } catch {
    //   this.pluginVersion.set('Error');
    // }
  }

  private async initEventListeners(): Promise<void> {
    // if (!this.isSupported()) return;
    // try {
    //   const lockStatusHandle = await Fortress.addListener('onLockStatusChanged', (state) => {
    //     this.addLog(`Lock status changed: ${state.isLocked ? 'LOCKED' : 'UNLOCKED'}`);
    //     this.refreshSession();
    //   });
    //   this.listeners.push(lockStatusHandle);
    //   const vaultInvalidatedHandle = await Fortress.addListener('onVaultInvalidated', (event) => {
    //     this.addLog(`Vault invalidated: ${event.reason}`);
    //   });
    //   this.listeners.push(vaultInvalidatedHandle);
    //   const securityStateHandle = await Fortress.addListener('onSecurityStateChanged', (status) => {
    //     this.addLog('Security state changed');
    //     this.securityStatus.set(status);
    //   });
    //   this.listeners.push(securityStateHandle);
    //   const appResumeHandle = await Fortress.addListener('onAppResume', () => {
    //     this.addLog('App resumed');
    //     this.refreshSession();
    //   });
    //   this.listeners.push(appResumeHandle);
    //   this.addLog('Event listeners registered');
    // } catch (e) {
    //   console.error('Failed to register listeners', e);
    // }
  }

  private applyRuntimeConfig(config: RuntimeConfigSnapshot): void {
    // if (typeof config.verboseLogging === 'boolean') {
    //   this.verboseLogging.set(config.verboseLogging);
    // }
    // if (typeof config.enablePrivacyScreen === 'boolean') {
    //   this.privacyScreen.set(config.enablePrivacyScreen);
    // }
    // if (typeof config.privacyOverlayText === 'string') {
    //   this.privacyOverlayText.set(config.privacyOverlayText);
    //   this.appliedPrivacyOverlayText.set(config.privacyOverlayText);
    // }
    // if (typeof config.privacyOverlayImageName === 'string') {
    //   this.privacyOverlayImageName.set(config.privacyOverlayImageName);
    // }
    // if (typeof config.privacyOverlayShowText === 'boolean') {
    //   this.privacyOverlayShowText.set(config.privacyOverlayShowText);
    // }
    // if (typeof config.privacyOverlayShowImage === 'boolean') {
    //   this.privacyOverlayShowImage.set(config.privacyOverlayShowImage);
    // }
    // if (typeof config.privacyOverlayTextColor === 'string') {
    //   this.privacyOverlayTextColor.set(config.privacyOverlayTextColor);
    // }
    // if (typeof config.privacyOverlayBackgroundOpacity === 'number') {
    //   this.privacyOverlayBackgroundOpacity.set(String(config.privacyOverlayBackgroundOpacity));
    // }
    // if (config.privacyOverlayTheme) {
    //   this.privacyOverlayTheme.set(config.privacyOverlayTheme);
    // }
    // if (config.fallbackStrategy) {
    //   this.fallbackStrategy.set(config.fallbackStrategy);
    // }
    // if (typeof config.persistSessionState === 'boolean') {
    //   this.persistSessionState.set(config.persistSessionState);
    // }
  }

  private async loadRuntimeConfig(): Promise<void> {
    // if (!this.isSupported()) return;
    // try {
    //   const plugin = Fortress as unknown as {
    //     getRuntimeConfig?: () => Promise<RuntimeConfigSnapshot>;
    //   };
    //   if (plugin.getRuntimeConfig) {
    //     const runtimeConfig = await plugin.getRuntimeConfig();
    //     this.applyRuntimeConfig(runtimeConfig);
    //   }
    // } catch (e) {
    //   console.warn('Failed to load Fortress runtime config', e);
    // }
  }

  openOnGithub(): void {
    window.open(this.GH_URL, '_blank');
  }

  onTabChange(event: CustomEvent): void {
    this.activeTab.set(event.detail.value);
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
    // const timestamp = new Date().toLocaleTimeString();
    // this.logs.update((logs) => [`[${timestamp}] ${message}`, ...logs].slice(0, 50));
  }

  private async presentError(error: any): Promise<void> {
    // this.addLog(`ERROR: ${error?.message ?? String(error)}`);
    // const sheet = await this.actionSheetCtrl.create({
    //   header: 'Plugin Error',
    //   subHeader: error?.code ?? 'UNKNOWN',
    //   buttons: [
    //     {
    //       text: error?.message ?? String(error),
    //       role: 'destructive',
    //     },
    //     { text: 'OK', role: 'cancel' },
    //   ],
    // });
    // await sheet.present();
  }

  private async showToast(message: string, color: string = 'success'): Promise<void> {
    // const toast = await this.toastCtrl.create({
    //   message,
    //   duration: 1500,
    //   color,
    // });
    // await toast.present();
  }

  async checkStatus(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // try {
    //   const status = await Fortress.checkStatus();
    //   this.securityStatus.set(status);
    //   this.addLog(
    //     `Status checked: biometrics=${status.isBiometricsAvailable}, type=${status.biometryType}`,
    //   );
    // } catch (e) {
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async refreshSession(): Promise<void> {
    // if (!this.isSupported()) return;
    // try {
    //   const session = await Fortress.getSession();
    //   this.session.set(session);
    //   this.addLog(`Session: locked=${session.isLocked}`);
    // } catch (e) {
    //   console.error('Failed to get session', e);
    // }
  }

  async onUnlock(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // this.unlockState.set('idle');
    // try {
    //   await Fortress.unlock();
    //   this.unlockState.set('success');
    //   this.addLog('Vault unlocked');
    //   await this.refreshSession();
    // } catch (e: any) {
    //   this.unlockState.set('error');
    //   if (e.code !== 'CANCELLED') {
    //     await this.presentError(e);
    //   }
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onLock(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // this.lockState.set('idle');
    // try {
    //   await Fortress.lock();
    //   this.lockState.set('success');
    //   this.addLog('Vault locked');
    //   await this.refreshSession();
    // } catch (e) {
    //   this.lockState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onSetValue(): Promise<void> {
    // if (!this.isSupported()) return;
    // const key = this.storageKey().trim();
    // const value = this.storageValue().trim();
    // if (!key || !value) {
    //   await this.showToast('Key and value are required', 'warning');
    //   return;
    // }
    // this.inProgress.set(true);
    // this.setValueState.set('idle');
    // try {
    //   await Fortress.setValue({ key, value });
    //   this.setValueState.set('success');
    //   this.addLog(`Set: ${key}`);
    //   this.storageValue.set('');
    //   await this.loadStoredKeys();
    // } catch (e: any) {
    //   this.setValueState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onGetValue(): Promise<void> {
    // if (!this.isSupported()) return;
    // const key = this.storageKey().trim();
    // if (!key) {
    //   await this.showToast('Key is required', 'warning');
    //   return;
    // }
    // this.inProgress.set(true);
    // this.getValueState.set('idle');
    // try {
    //   const result = await Fortress.getValue({ key });
    //   this.lastRetrievedValue.set(result.value);
    //   this.getValueState.set('success');
    //   this.addLog(`Get: ${key} = ${result.value ?? '(null)'}`);
    // } catch (e: any) {
    //   this.getValueState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onRemoveValue(): Promise<void> {
    // if (!this.isSupported()) return;
    // const key = this.storageKey().trim();
    // if (!key) {
    //   await this.showToast('Key is required', 'warning');
    //   return;
    // }
    // this.inProgress.set(true);
    // this.removeValueState.set('idle');
    // try {
    //   await Fortress.removeValue({ key });
    //   this.removeValueState.set('success');
    //   this.addLog(`Removed: ${key}`);
    //   this.lastRetrievedValue.set(null);
    //   await this.loadStoredKeys();
    // } catch (e: any) {
    //   this.removeValueState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onClearAll(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // this.clearAllState.set('idle');
    // try {
    //   await Fortress.clearAll();
    //   this.clearAllState.set('success');
    //   this.addLog('Cleared all values');
    //   this.lastRetrievedValue.set(null);
    //   this.storedKeys.set([]);
    // } catch (e: any) {
    //   this.clearAllState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async loadStoredKeys(): Promise<void> {
    // if (!this.isSupported()) return;
    // const demoKeys = ['auth_token', 'refresh_token', 'user_id', 'settings', 'cache'];
    // this.storedKeys.set(demoKeys);
  }

  async onCheckBiometricKeysExist(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // try {
    //   const result = await Fortress.biometricKeysExist();
    //   this.biometricKeysExist.set(result.keysExist);
    //   this.addLog(`Keys exist: ${result.keysExist}`);
    // } catch (e) {
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onCreateKeys(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // this.createKeysState.set('idle');
    // try {
    //   const result = await Fortress.createKeys();
    //   this.createKeysState.set('success');
    //   this.biometricKeysExist.set(true);
    //   this.addLog(`Keys created: ${result.publicKey.substring(0, 20)}...`);
    // } catch (e: any) {
    //   this.createKeysState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onDeleteKeys(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // this.deleteKeysState.set('idle');
    // try {
    //   await Fortress.deleteKeys();
    //   this.deleteKeysState.set('success');
    //   this.biometricKeysExist.set(false);
    //   this.addLog('Keys deleted');
    // } catch (e: any) {
    //   this.deleteKeysState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onCreateSignature(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // this.createSignatureState.set('idle');
    // try {
    //   const payload = `challenge_${Date.now()}`;
    //   const result = await Fortress.createSignature({
    //     payload,
    //     promptMessage: 'Authenticate to sign',
    //   });
    //   this.lastSignature.set(result.signature);
    //   this.createSignatureState.set('success');
    //   this.addLog(`Signature created: ${result.signature.substring(0, 20)}...`);
    // } catch (e: any) {
    //   this.createSignatureState.set('error');
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onConfigure(): Promise<void> {
    // if (!this.isSupported()) return;
    // try {
    //   const parsedOpacity = Number(this.privacyOverlayBackgroundOpacity());
    //   const clampedOpacity = Number.isFinite(parsedOpacity)
    //     ? Math.max(0, Math.min(1, parsedOpacity))
    //     : 0.8;
    //   const runtimeConfig: any = {
    //     verboseLogging: this.verboseLogging(),
    //     enablePrivacyScreen: this.privacyScreen(),
    //     privacyOverlayText: this.privacyOverlayText(),
    //     privacyOverlayImageName: this.privacyOverlayImageName(),
    //     privacyOverlayShowText: this.privacyOverlayShowText(),
    //     privacyOverlayShowImage: this.privacyOverlayShowImage(),
    //     privacyOverlayTextColor: this.privacyOverlayTextColor(),
    //     privacyOverlayBackgroundOpacity: clampedOpacity,
    //     privacyOverlayTheme: this.privacyOverlayTheme(),
    //     fallbackStrategy: this.fallbackStrategy(),
    //     persistSessionState: this.persistSessionState(),
    //   };
    //   await Fortress.configure(runtimeConfig);
    //   this.appliedPrivacyOverlayText.set(this.privacyOverlayText());
    //   this.addLog('Configuration applied');
    //   await this.showToast('Configuration applied');
    // } catch (e) {
    //   await this.presentError(e);
    // }
  }

  async onResetSession(): Promise<void> {
    // if (!this.isSupported()) return;
    // this.inProgress.set(true);
    // try {
    //   await Fortress.resetSession();
    //   this.addLog('Session reset');
    //   await this.refreshSession();
    // } catch (e) {
    //   await this.presentError(e);
    // } finally {
    //   this.inProgress.set(false);
    // }
  }

  async onTouchSession(): Promise<void> {
    // if (!this.isSupported()) return;
    // try {
    //   await Fortress.touchSession();
    //   this.addLog('Session touched');
    //   await this.refreshSession();
    // } catch (e) {
    //   await this.presentError(e);
    // }
  }

  biometryTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      none: 'None',
      touchId: 'Touch ID',
      faceId: 'Face ID',
      fingerprint: 'Fingerprint',
      iris: 'Iris',
    };
    return labels[type] ?? type;
  }

  selectKey(key: string): void {
    this.storageKey.set(key);
  }

  clearLogs(): void {
    this.logs.set([]);
  }

  async copyValue(): Promise<void> {
    const value = this.lastRetrievedValue();
    if (!value) return;
    await navigator.clipboard.writeText(value);
    await this.showToast('Copied to clipboard');
  }
}
