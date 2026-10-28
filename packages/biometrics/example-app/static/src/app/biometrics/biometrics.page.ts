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
  IonLabel,
  IonToggle,
  IonInput,
  IonNote,
} from '@ionic/angular';

import { Biometrics } from '@cap-kit/biometrics';

import { addIcons } from 'ionicons';
import {
  refresh,
  checkmarkCircle,
  alertCircle,
  shieldCheckmarkOutline,
  fingerPrint,
  eyeOutline,
  keyOutline,
  settingsOutline,
  terminalOutline,
} from 'ionicons/icons';

import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

type ResultState = 'idle' | 'success' | 'failure' | 'error';

@Component({
  selector: 'app-biometrics',
  standalone: true,
  templateUrl: './biometrics.page.html',
  styleUrls: ['./biometrics.page.scss'],
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
    IonLabel,
    IonToggle,
    IonInput,
    PageHeaderComponent,
    PageFooterComponent,
    InfoCardComponent,
    IonNote,
  ],
})
export class BiometricsPage implements OnDestroy {
  readonly isSupported = signal(false);
  readonly pluginVersion = signal<string | undefined>(undefined);
  readonly inProgress = signal(false);

  readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return current ?? 'Unknown';
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/biometrics';

  readonly isAvailable = signal<boolean | null>(null);
  readonly isEnrolled = signal<boolean | null>(null);
  readonly biometryType = signal<string | null>(null);
  readonly biometryTypes = signal<string[]>([]);
  readonly hasDeviceCredential = signal<boolean | null>(null);
  readonly strengthLevel = signal<string | null>(null);
  readonly lastAuthenticationType = signal<string | null>(null);

  readonly promptTitle = signal('Authentication Required');
  readonly promptSubtitle = signal('Please authenticate to continue');
  readonly promptReason = signal('');
  readonly allowDeviceCredential = signal(true);

  readonly authenticateState = signal<ResultState>('idle');

  readonly logs = signal<string[]>([]);

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!Biometrics);
    this.initPluginVersion();
    this.refreshCapabilities();

    addIcons({
      refresh,
      checkmarkCircle,
      alertCircle,
      shieldCheckmarkOutline,
      fingerPrint,
      eyeOutline,
      keyOutline,
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
      const result = await Biometrics.getPluginVersion();
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

  async refreshCapabilities(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      const [available, enrolled, type, types, credential, strength] = await Promise.all([
        Biometrics.isAvailable(),
        Biometrics.isEnrolled(),
        Biometrics.getBiometricType(),
        Biometrics.getBiometricTypes(),
        Biometrics.hasDeviceCredential(),
        Biometrics.getBiometricStrengthLevel(),
      ]);
      this.isAvailable.set(available.isAvailable);
      this.isEnrolled.set(enrolled.isEnrolled);
      this.biometryType.set(type.biometryType);
      this.biometryTypes.set(types.biometryTypes);
      this.hasDeviceCredential.set(credential.hasDeviceCredential);
      this.strengthLevel.set(strength.strengthLevel);
      this.addLog(
        `Capabilities: available=${available.isAvailable}, enrolled=${enrolled.isEnrolled}, type=${type.biometryType}`,
      );
    } catch (e) {
      console.error('Failed to refresh capabilities', e);
    }
  }

  async onAuthenticate(): Promise<void> {
    if (!this.isSupported()) return;
    this.inProgress.set(true);
    this.authenticateState.set('idle');
    try {
      const reason = this.promptReason().trim();
      await Biometrics.authenticate({
        title: this.promptTitle().trim() || undefined,
        subtitle: this.promptSubtitle().trim() || undefined,
        ...(reason ? { reason } : {}),
        allowDeviceCredential: this.allowDeviceCredential(),
      });
      this.authenticateState.set('success');
      this.addLog('Authentication succeeded');
      await this.showToast('Authenticated');
      const { authenticationType } = await Biometrics.getAuthenticationType();
      this.lastAuthenticationType.set(authenticationType);
      this.addLog(`Authentication type: ${authenticationType}`);
    } catch (e: any) {
      this.authenticateState.set('error');
      if (e?.code !== 'CANCELLED' && e?.message !== 'Biometric authentication was cancelled.') {
        await this.presentError(e);
      } else {
        this.addLog('Authentication cancelled by user');
      }
    } finally {
      this.inProgress.set(false);
    }
  }

  async onCancelAuthentication(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      await Biometrics.cancelAuthentication();
      this.addLog('Cancel requested (no-op when idle)');
    } catch (e) {
      await this.presentError(e);
    }
  }

  async onEnroll(): Promise<void> {
    if (!this.isSupported()) return;
    try {
      await Biometrics.enroll();
      this.addLog('Enrollment screen opened (Android)');
      await this.showToast('Enrollment opened');
    } catch (e) {
      await this.presentError(e);
    }
  }

  clearLogs(): void {
    this.logs.set([]);
  }
}
