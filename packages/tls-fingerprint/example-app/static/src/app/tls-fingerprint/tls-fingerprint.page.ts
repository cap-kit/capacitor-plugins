import { ChangeDetectionStrategy, Component, signal, computed } from '@angular/core';
import { FormGroup, FormControl, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  ActionSheetController,
  ToastController,
  IonContent,
  IonButton,
  IonItem,
  IonInput,
  IonList,
  IonLabel,
  IonListHeader,
  IonBadge,
  IonIcon,
  IonNote,
} from '@ionic/angular';
import { TLSFingerprint, PluginVersionResult } from '@cap-kit/tls-fingerprint';
import { addIcons } from 'ionicons';
import { home, trash, addCircle } from 'ionicons/icons';
import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

type ResultState = 'idle' | 'match' | 'no-match' | 'error';

@Component({
  selector: 'app-tls-fingerprint',
  templateUrl: './tls-fingerprint.page.html',
  styleUrls: ['./tls-fingerprint.page.scss'],
  standalone: true,
  imports: [
    IonNote,
    IonIcon,
    IonListHeader,
    IonLabel,
    IonItem,
    IonContent,
    IonButton,
    IonInput,
    IonBadge,
    ReactiveFormsModule,
    IonInput,
    PageHeaderComponent,
    InfoCardComponent,
    PageFooterComponent,
    IonList,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TLSFingerprintPage {
  public readonly isSupported = signal(false);
  public readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);

  public readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/tls-fingerprint';

  // ---------------------------------------------------------------------------
  // Result state (badges)
  // ---------------------------------------------------------------------------

  singleOkState = signal<ResultState>('idle');
  singleKoState = signal<ResultState>('idle');
  multiOkState = signal<ResultState>('idle');
  multiKoState = signal<ResultState>('idle');

  // ---------------------------------------------------------------------------
  // Forms
  // ---------------------------------------------------------------------------

  // Single – OK (config fallback)
  singleOkForm = new FormGroup({
    url: new FormControl({ value: 'https://capacitorjs.com', disabled: true }, Validators.required),
    fingerprint: new FormControl(''),
  });

  // Single – KO
  singleKoForm = new FormGroup({
    url: new FormControl('https://capacitorjs.com', Validators.required),
    fingerprint: new FormControl('00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF', Validators.required),
  });

  // Multi – OK (config fallback)
  multiOkForm = new FormGroup({
    url: new FormControl({ value: 'https://ionic.io', disabled: true }, Validators.required),
    fingerprints: new FormArray([]),
  });

  // Multi – KO (runtime override)
  multiKoForm = new FormGroup({
    url: new FormControl('https://ionic.io', Validators.required),
    fingerprints: new FormArray([
      new FormControl('00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF', Validators.required),
    ]),
  });

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!TLSFingerprint);
    this.initPluginVersion();
    addIcons({
      trash,
      addCircle,
      home,
    });
  }

  // ---------------------------------------------------------------------------
  // Getters
  // ---------------------------------------------------------------------------

  get multiOkFingerprints(): FormArray {
    return this.multiOkForm.get('fingerprints') as FormArray;
  }

  get multiKoFingerprints(): FormArray {
    return this.multiKoForm.get('fingerprints') as FormArray;
  }

  // ---------------------------------------------------------------------------
  // UI helpers
  // ---------------------------------------------------------------------------

  badgeColor(state: ResultState): string {
    switch (state) {
      case 'match':
        return 'success';
      case 'no-match':
        return 'warning';
      case 'error':
        return 'danger';
      default:
        return 'medium';
    }
  }

  badgeLabel(state: ResultState): string {
    switch (state) {
      case 'match':
        return 'MATCH';
      case 'no-match':
        return 'NO MATCH';
      case 'error':
        return 'ERROR';
      default:
        return 'IDLE';
    }
  }

  private async presentError(error: any): Promise<void> {
    const actionSheet = await this.actionSheetCtrl.create({
      header: 'Plugin Error',
      subHeader: error?.code ?? 'UNKNOWN',
      buttons: [
        {
          text: error?.message ?? String(error),
          role: 'destructive',
          cssClass: 'action-sheet-message',
          handler: () => {},
        },
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
      const result: PluginVersionResult = await TLSFingerprint.getPluginVersion();
      this.pluginVersion.set(result);
    } catch (e) {
      console.error('Error getting plugin version', e);
      this.pluginVersion.set('Error');
    }
  }

  addFingerprint(form: FormGroup) {
    (form.get('fingerprints') as FormArray).push(new FormControl('', Validators.required));
  }

  removeFingerprint(form: FormGroup, index: number) {
    (form.get('fingerprints') as FormArray).removeAt(index);
  }

  async runSingle(form: FormGroup, state: typeof this.singleOkState): Promise<void> {
    try {
      const { url, fingerprint } = form.getRawValue() as any;

      const res = await TLSFingerprint.checkCertificate({
        url,
        ...(fingerprint ? { fingerprint } : {}),
      });

      state.set(res.fingerprintMatched ? 'match' : 'no-match');
    } catch (e) {
      state.set('error');
      await this.presentError(e);
    }
  }

  async runMulti(form: FormGroup, state: typeof this.multiOkState): Promise<void> {
    try {
      const { url, fingerprints } = form.getRawValue() as any;

      const list = (fingerprints as string[]).filter(Boolean);

      const res = await TLSFingerprint.checkCertificates({
        url,
        ...(list.length ? { fingerprints: list } : {}),
      });

      state.set(res.fingerprintMatched ? 'match' : 'no-match');
    } catch (e) {
      state.set('error');
      await this.presentError(e);
    }
  }
}
