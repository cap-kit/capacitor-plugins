import { ChangeDetectionStrategy, Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import {
  ActionSheetController,
  ToastController,
  IonContent,
  IonButton,
  IonItem,
  IonList,
  IonItemDivider,
  IonLabel,
  IonSelect,
  IonSelectOption,
  IonModal,
} from '@ionic/angular';
import {
  Redsys,
  PluginVersionResult,
  RedsysTransactionType,
  RedsysLanguage,
  RedsysPaymentMethod,
} from '@cap-kit/redsys';
import { addIcons } from 'ionicons';
import { home } from 'ionicons/icons';
import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

@Component({
  selector: 'app-redsys',
  templateUrl: './redsys.page.html',
  styleUrls: ['./redsys.page.scss'],
  standalone: true,
  imports: [
    IonModal,
    CommonModule,
    IonLabel,
    IonItemDivider,
    IonItem,
    IonList,
    IonContent,
    IonButton,
    ReactiveFormsModule,
    PageHeaderComponent,
    InfoCardComponent,
    PageFooterComponent,
    IonSelect,
    IonSelectOption,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RedsysPage {
  // Expose the Enum to the template
  public readonly RedsysLanguages = RedsysLanguage;

  public readonly isSupported = signal(false);
  public readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);

  public readonly RedsysTransactionTypes = RedsysTransactionType;

  /**
   * Signal to manage UI localization based on the official SDK manual.
   * Default is set to Spanish ('1') as per Redsys guidelines.
   */
  public readonly selectedLanguage = signal<RedsysLanguage>(RedsysLanguage.Spanish);

  /**
   *
   */
  public readonly selectedTransactionType = signal<RedsysTransactionType>(RedsysTransactionType.Normal);

  /**
   * Signal for testing dynamic theme/color variations.
   * Maps to backgroundColor in our standardized RedsysUIOptions.
   */
  public readonly customColor = signal<string>('#ff5722');

  // ---------------------------------------------------------------------------
  // Result Modal State
  // ---------------------------------------------------------------------------

  public readonly resultOpen = signal(false);
  public readonly resultData = signal<any>(null);
  public readonly resultIsError = signal(false);

  public readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/redsys';

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!Redsys);
    this.initPluginVersion();
    addIcons({ home });
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
    const actionSheet = await this.actionSheetCtrl.create({
      header: 'Plugin Error',
      subHeader: typeof error === 'object' ? JSON.stringify(error, null, 2) : String(error),
      buttons: [
        {
          text: 'OK',
          role: 'cancel',
        },
      ],
    });
    await actionSheet.present();
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
      const result: PluginVersionResult = await Redsys.getPluginVersion();
      this.pluginVersion.set(result);
    } catch (e) {
      console.error('Error getting plugin version', e);
      this.pluginVersion.set('Error');
    }
  }

  // ---------------------------------------------------------------------------
  // Utilities
  // ---------------------------------------------------------------------------

  private generateOrderCode(): string {
    return 'CAP' + Date.now().toString().substring(5);
  }

  // ---------------------------------------------------------------------------
  // Modal Controls
  // ---------------------------------------------------------------------------

  private openResult(data: any, isError: boolean): void {
    this.resultData.set(data);
    this.resultIsError.set(isError);
    this.resultOpen.set(true);
  }

  public closeResult(): void {
    this.resultOpen.set(false);
  }

  // ---------------------------------------------------------------------------
  // Result Handlers
  // ---------------------------------------------------------------------------

  private async showResult(result: any): Promise<void> {
    this.openResult(result, false);
  }

  private async showError(error: any): Promise<void> {
    this.openResult(error, true);
  }

  // ---------------------------------------------------------------------------
  // DIRECT PAYMENT VARIANTS
  // ---------------------------------------------------------------------------

  public async payDirectDefault(): Promise<void> {
    try {
      const res = await Redsys.doDirectPayment({
        order: this.generateOrderCode(),
        amount: 10.5,
        transactionType: this.selectedTransactionType(),
      });
      await this.showResult(res);
    } catch (e) {
      await this.showError(e);
    }
  }

  public async payDirectToken(): Promise<void> {
    try {
      const res = await Redsys.doDirectPayment({
        order: this.generateOrderCode(),
        amount: 15,
        transactionType: this.selectedTransactionType(),
        identifier: 'REQUEST_REFERENCE',
      });
      await this.showResult(res);
    } catch (e) {
      await this.showError(e);
    }
  }

  public async payDirectExtra(): Promise<void> {
    try {
      const res = await Redsys.doDirectPayment({
        order: this.generateOrderCode(),
        amount: 20,
        transactionType: this.selectedTransactionType(),
        extraParams: {
          demoKey: 'demoValue',
          timestamp: Date.now().toString(),
        },
      });
      await this.showResult(res);
    } catch (e) {
      await this.showError(e);
    }
  }

  public async payDirectAggressiveUI(): Promise<void> {
    try {
      const res = await Redsys.doDirectPayment({
        order: this.generateOrderCode(),
        amount: 25,
        transactionType: this.selectedTransactionType(),
        uiOptions: {
          backgroundColor: '#000000',
          confirmButtonText: 'OVERRIDE',
          labelTextColor: '#FFFFFF',
          cardNumberLabel: 'Card No.',
          expirationLabel: 'Expiry',
          cvvLabel: 'Security Code',
        },
      });
      await this.showResult(res);
    } catch (e) {
      await this.showError(e);
    }
  }

  // ---------------------------------------------------------------------------
  // WEB PAYMENT VARIANTS
  // ---------------------------------------------------------------------------

  public async payWebSHA256(): Promise<void> {
    try {
      const init = await Redsys.initializeWebPayment({
        order: this.generateOrderCode(),
        amount: 1.5,
        transactionType: this.selectedTransactionType(),
      });

      const hash = await Redsys.computeHash({
        data: init.base64Data,
        keyBase64: '3Xe1uoMGqqFPSrsqK4xo',
        algorithm: 'HMAC_SHA256_V1',
      });

      const result = await Redsys.processWebPayment({
        signature: hash.signature,
        signatureVersion: 'HMAC_SHA256_V1',
      });

      await this.showResult(result);
    } catch (e) {
      await this.showError(e);
    }
  }

  public async payWebSHA512(): Promise<void> {
    try {
      const init = await Redsys.initializeWebPayment({
        order: this.generateOrderCode(),
        amount: 2,
        transactionType: this.selectedTransactionType(),
      });

      const hash = await Redsys.computeHash({
        data: init.base64Data,
        keyBase64: '3Xe1uoMGqqFPSrsqK4xo',
        algorithm: 'HMAC_SHA512_V1',
      });

      const result = await Redsys.processWebPayment({
        signature: hash.signature,
        signatureVersion: 'HMAC_SHA512_V1',
      });

      await this.showResult(result);
    } catch (e) {
      await this.showError(e);
    }
  }

  public async payWebConfigSignature(): Promise<void> {
    try {
      const result = await Redsys.processWebPayment({});
      await this.showResult(result);
    } catch (e) {
      await this.showError(e);
    }
  }

  // ---------------------------------------------------------------------------
  // ERROR SIMULATION
  // ---------------------------------------------------------------------------

  public async testInvalidSignature(): Promise<void> {
    try {
      await Redsys.processWebPayment({
        signature: 'INVALID_SIGNATURE',
        signatureVersion: 'HMAC_SHA256_V1',
      });
    } catch (e) {
      await this.showError(e);
    }
  }
}
