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
  IonItemDivider,
  IonText,
  IonLabel,
  IonToggle,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonChip,
  IonNote,
} from '@ionic/angular';

import { SecureStorage } from '@cap-kit/secure-storage';

import { addIcons } from 'ionicons';
import {
  refresh,
  checkmarkCircle,
  alertCircle,
  shieldCheckmarkOutline,
  eyeOutline,
  keyOutline,
  trash,
  addCircle,
  settingsOutline,
  terminalOutline,
} from 'ionicons/icons';

import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

type ResultState = 'idle' | 'success' | 'failure' | 'error';
type KeychainAccess = 'whenUnlocked' | 'whenUnlockedThisDeviceOnly' | 'afterFirstUnlock' | 'afterFirstUnlockThisDeviceOnly' | 'whenPasscodeSetThisDeviceOnly';

@Component({
  selector: 'app-secure-storage',
  standalone: true,
  templateUrl: './secure-storage.page.html',
  styleUrls: ['./secure-storage.page.scss'],
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
    IonItemDivider,
    IonText,
    IonLabel,
    IonToggle,
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
export class SecureStoragePage implements OnDestroy {
  readonly isSupported = signal(false);
  readonly pluginVersion = signal<string | undefined>(undefined);
  readonly inProgress = signal(false);

  readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return current ?? 'Unknown';
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/secure-storage';

  readonly storageKey = signal('');
  readonly storageValue = signal('');
  readonly storedKeys = signal<string[]>([]);
  readonly lastRetrievedValue = signal<string | null>(null);

  readonly setState = signal<ResultState>('idle');
  readonly getState = signal<ResultState>('idle');
  readonly removeState = signal<ResultState>('idle');

  readonly synchronize = signal(false);
  readonly defaultAccess = signal<KeychainAccess>('afterFirstUnlockThisDeviceOnly');

  readonly logs = signal<string[]>([]);

  readonly insecureGroup = signal('CapacitorStorage');
  readonly insecureKey = signal('');
  readonly insecureValue = signal('');
  readonly storedInsecureKeys = signal<string[]>([]);
  readonly lastInsecureValue = signal<string | null>(null);
  readonly obfuscateKeys = signal(false);

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!SecureStorage);
    this.initPluginVersion();
    this.loadStoredKeys();
    this.loadInsecureKeys();
    this.loadSynchronize();

    addIcons({
      refresh,
      checkmarkCircle,
      alertCircle,
      shieldCheckmarkOutline,
      eyeOutline,
      keyOutline,
      trash,
      addCircle,
      settingsOutline,
      terminalOutline,
    });
  }

  async ngOnDestroy(): Promise<void> {
    // No persistent listeners in this demo.
  }

  private async initPluginVersion(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const result = await SecureStorage.getPluginVersion();
      this.pluginVersion.set(result.version);
    } catch {
      this.pluginVersion.set('Error');
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

  async onSetValue(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.storageKey().trim();
    const value = this.storageValue().trim();
    if (!key || !value) {
      await this.showToast('Key and value are required', 'warning');
      return;
    }
    this.inProgress.set(true);
    this.setState.set('idle');
    try {
      await SecureStorage.set({ key, value });
      this.setState.set('success');
      this.addLog(`Set: ${key}`);
      this.storageValue.set('');
      await this.loadStoredKeys();
    } catch (e: any) {
      this.setState.set('error');
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onGetValue(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.storageKey().trim();
    if (!key) {
      await this.showToast('Key is required', 'warning');
      return;
    }
    this.inProgress.set(true);
    this.getState.set('idle');
    try {
      const result = await SecureStorage.get({ key });
      this.lastRetrievedValue.set(result.value);
      this.getState.set('success');
      this.addLog(`Get: ${key} = ${result.value ?? '(null)'}`);
    } catch (e: any) {
      this.getState.set('error');
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onRemoveValue(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.storageKey().trim();
    if (!key) {
      await this.showToast('Key is required', 'warning');
      return;
    }
    this.inProgress.set(true);
    this.removeState.set('idle');
    try {
      const { existed } = await SecureStorage.remove({ key });
      this.removeState.set('success');
      this.addLog(`Removed: ${key} (existed=${existed})`);
      this.lastRetrievedValue.set(null);
      await this.loadStoredKeys();
    } catch (e: any) {
      this.removeState.set('error');
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onClearAll(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    try {
      await SecureStorage.clear();
      this.addLog('Cleared all values (master key rotated on Android)');
      this.lastRetrievedValue.set(null);
      await this.loadStoredKeys();
      await this.showToast('Storage cleared');
    } catch (e) {
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onHasValue(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.storageKey().trim();
    if (!key) {
      await this.showToast('Key is required', 'warning');
      return;
    }
    try {
      const { exists } = await SecureStorage.has({ key });
      this.addLog(`Has: ${key} = ${exists}`);
      await this.showToast(exists ? 'Key exists' : 'Key missing', exists ? 'success' : 'medium');
    } catch (e: any) {
      await this.presentError(e);
    }
  }

  async onHasInsecure(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.insecureKey().trim();
    if (!key) {
      await this.showToast('Key is required', 'warning');
      return;
    }
    try {
      const { exists } = await SecureStorage.hasInsecure({ key });
      this.addLog(`Insecure has: ${key} = ${exists}`);
      await this.showToast(exists ? 'Key exists' : 'Key missing', exists ? 'success' : 'medium');
    } catch (e: any) {
      await this.presentError(e);
    }
  }

  async onClearInsecure(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    try {
      await SecureStorage.clearInsecure();
      this.addLog('Insecure group cleared');
      this.lastInsecureValue.set(null);
      await this.loadInsecureKeys();
      await this.showToast('Group cleared');
    } catch (e) {
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async loadStoredKeys(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const { keys } = await SecureStorage.keys();
      this.storedKeys.set(keys);
    } catch (e) {
      console.error('Failed to list keys', e);
    }
  }

  async onGetMany(): Promise<void> {
    if (!this.isSupported()) return;
    const keys = this.storedKeys();
    if (keys.length === 0) {
      await this.showToast('No keys stored yet', 'warning');
      return;
    }
    try {
      const { values } = await SecureStorage.getMany({ keys });
      const shown = Object.entries(values)
        .map(([key, value]) => `${key}=${value ?? '(null)'}`)
        .join(', ');
      this.addLog(`GetMany: ${shown}`);
      await this.showToast(`${keys.length} values read`);
    } catch (e) {
      await this.presentError(e);
    }
  }

  async loadSynchronize(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const { synchronize } = await SecureStorage.getSynchronize();
      this.synchronize.set(synchronize);
    } catch (e) {
      console.error('Failed to read sync state', e);
    }
  }

  async onSynchronizeChange(enabled: boolean): Promise<void> {
    if (!this.isSupported()) return;
    try {
      await SecureStorage.setSynchronize({ synchronize: enabled });
      this.synchronize.set(enabled);
      this.addLog(`iCloud sync ${enabled ? 'enabled' : 'disabled'} (iOS only)`);
    } catch (e) {
      await this.presentError(e);
    }
  }

  async onDefaultAccessChange(access: KeychainAccess): Promise<void> {
    if (!this.isSupported()) return;
    try {
      await SecureStorage.setDefaultKeychainAccess({ access });
      this.defaultAccess.set(access);
      this.addLog(`Default Keychain access: ${access} (iOS only)`);
    } catch (e) {
      await this.presentError(e);
    }
  }

  selectKey(key: string): void {
    this.storageKey.set(key);
  }

  clearLogs(): void {
    this.logs.set([]);
  }

  async onApplyGroup(): Promise<void> {
    if (!this.isSupported()) return;
    const group = this.insecureGroup().trim();
    if (!group) {
      await this.showToast('Group is required', 'warning');
      return;
    }
    try {
      await SecureStorage.configure({ group });
      this.addLog(`Insecure group set: ${group}`);
      await this.loadInsecureKeys();
      await this.showToast('Group applied');
    } catch (e) {
      await this.presentError(e);
    }
  }

  async onToggleObfuscation(enabled: boolean): Promise<void> {
    if (!this.isSupported()) return;
    try {
      await SecureStorage.configure({ obfuscateKeys: enabled });
      this.obfuscateKeys.set(enabled);
      this.addLog(`Key obfuscation ${enabled ? 'enabled' : 'disabled'} (applies to new writes)`);
      await this.loadStoredKeys();
      await this.loadInsecureKeys();
    } catch (e) {
      await this.presentError(e);
    }
  }

  async onSetInsecure(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.insecureKey().trim();
    const value = this.insecureValue().trim();
    if (!key || !value) {
      await this.showToast('Key and value are required', 'warning');
      return;
    }
    this.inProgress.set(true);
    try {
      await SecureStorage.setInsecure({ key, value });
      this.addLog(`Insecure set: ${key}`);
      this.insecureValue.set('');
      await this.loadInsecureKeys();
    } catch (e: any) {
      await this.presentError(e);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onGetInsecure(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.insecureKey().trim();
    if (!key) {
      await this.showToast('Key is required', 'warning');
      return;
    }
    try {
      const result = await SecureStorage.getInsecure({ key });
      this.lastInsecureValue.set(result.value);
      this.addLog(`Insecure get: ${key} = ${result.value ?? '(null)'}`);
    } catch (e: any) {
      await this.presentError(e);
    }
  }

  async onRemoveInsecure(): Promise<void> {
    if (!this.isSupported()) return;
    const key = this.insecureKey().trim();
    if (!key) {
      await this.showToast('Key is required', 'warning');
      return;
    }
    try {
      const { existed } = await SecureStorage.removeInsecure({ key });
      this.addLog(`Insecure removed: ${key} (existed=${existed})`);
      this.lastInsecureValue.set(null);
      await this.loadInsecureKeys();
    } catch (e: any) {
      await this.presentError(e);
    }
  }

  async loadInsecureKeys(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const { keys } = await SecureStorage.keysInsecure();
      this.storedInsecureKeys.set(keys);
    } catch (e) {
      console.error('Failed to list insecure keys', e);
    }
  }

  selectInsecureKey(key: string): void {
    this.insecureKey.set(key);
  }
}
