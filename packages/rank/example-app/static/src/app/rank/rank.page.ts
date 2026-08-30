import { ChangeDetectionStrategy, Component, signal, computed } from '@angular/core';
import { FormGroup, FormControl, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  ActionSheetController,
  ToastController,
  IonContent,
  IonButton,
  IonItem,
  IonList,
  IonLabel,
  IonListHeader,
  IonBadge,
  IonNote,
  IonToggle,
} from '@ionic/angular';
import { PluginVersionResult, Rank } from '@cap-kit/rank';
import { addIcons } from 'ionicons';
import { home, trash, addCircle } from 'ionicons/icons';
import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

type ResultState = 'idle' | 'match' | 'no-match' | 'error';

@Component({
  selector: 'app-rank',
  templateUrl: './rank.page.html',
  styleUrls: ['./rank.page.scss'],
  standalone: true,
  imports: [
    IonToggle,
    IonNote,
    IonListHeader,
    IonLabel,
    IonItem,
    IonContent,
    IonButton,
    IonBadge,
    ReactiveFormsModule,
    PageHeaderComponent,
    InfoCardComponent,
    PageFooterComponent,
    IonList,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RankPage {
  // ---------------------------------------------------------------------------
  // Plugin state
  // ---------------------------------------------------------------------------

  public readonly isSupported = signal(false);
  public readonly availability = signal<boolean | undefined>(undefined);
  public readonly fireAndForget = signal<boolean>(false);
  public readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);

  // ---------------------------------------------------------------------------
  // Android review environment diagnostics
  // ---------------------------------------------------------------------------

  public readonly reviewEnvChecked = signal(false);
  public readonly canRequestReview = signal<boolean | undefined>(undefined);
  public readonly reviewEnvReason = signal<string | undefined>(undefined);

  public readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/rank';

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

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!Rank);
    this.initPluginVersion();
    addIcons({
      home,
      trash,
      addCircle,
    });
  }

  // ---------------------------------------------------------------------------
  // Getters
  // ---------------------------------------------------------------------------

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
      const result: PluginVersionResult = await Rank.getPluginVersion();
      this.pluginVersion.set(result);
    } catch (e) {
      console.error('Error getting plugin version', e);
      this.pluginVersion.set('Error');
    }
  }

  // ---------------------------------------------------------------------------
  // Rank actions
  // ---------------------------------------------------------------------------

  public async checkAvailability(): Promise<void> {
    try {
      const { value } = await Rank.isAvailable();
      this.availability.set(value);
      this.singleOkState.set(value ? 'match' : 'no-match');
    } catch (e) {
      this.singleOkState.set('error');
      await this.presentError(e);
    }
  }

  private async checkReviewEnvironmentOnce(): Promise<void> {
    // Avoid repeating the diagnostic check
    if (this.reviewEnvChecked()) return;

    try {
      const env = await Rank.checkReviewEnvironment();

      this.canRequestReview.set(env.canRequestReview);
      this.reviewEnvReason.set(env.reason);
    } catch (e) {
      // Defensive fallback: treat as unavailable
      this.canRequestReview.set(false);
      this.reviewEnvReason.set('UNKNOWN');
    } finally {
      this.reviewEnvChecked.set(true);
    }
  }

  public async requestReview(): Promise<void> {
    try {
      await Rank.requestReview({
        fireAndForget: this.fireAndForget(),
      });

      // Perform environment diagnostic AFTER the first attempt
      await this.checkReviewEnvironmentOnce();

      if (this.canRequestReview() === false) {
        this.singleOkState.set('no-match');
      } else {
        this.singleOkState.set('match');
      }
      this.singleOkState.set('match');
    } catch (e) {
      this.singleOkState.set('error');
      await this.presentError(e);
    }
  }

  public async openStore(): Promise<void> {
    try {
      await Rank.openStore();
      this.singleOkState.set('match');
    } catch (e) {
      this.singleOkState.set('error');
      await this.presentError(e);
    }
  }
}
