import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardSubtitle,
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
  IonRange,
  IonChip,
  IonGrid,
  IonRow,
  IonCol,
  IonNote,
} from '@ionic/angular';

import { Integrity, IntegrityReport, PluginVersionResult, IntegrityBlockReason } from '@cap-kit/integrity';

import { addIcons } from 'ionicons';
import {
  home,
  shieldCheckmarkOutline,
  logInOutline,
  checkmarkCircle,
  alertCircle,
  cubeOutline,
  copyOutline,
  playOutline,
  analyticsOutline,
  pulseOutline,
  eyeOffOutline,
  eyeOutline,
} from 'ionicons/icons';

import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

import { IntegritySignalStore } from './integrity-signal.store';

/* -------------------------------------------------------------------------- */
/* Types                                                                      */
/* -------------------------------------------------------------------------- */

type ResultState = 'idle' | 'success' | 'failure' | 'error';
type Confidence = 'all' | 'low' | 'medium' | 'high';

type Category = 'all' | 'root' | 'jailbreak' | 'emulator' | 'emulator' | 'debug' | 'hook' | 'tamper' | 'environment';

type IntegrityTab = 'actions' | 'report' | 'signals';

/* -------------------------------------------------------------------------- */
/* Component                                                                  */
/* -------------------------------------------------------------------------- */

@Component({
  selector: 'app-integrity',
  standalone: true,
  templateUrl: './integrity.page.html',
  styleUrls: ['./integrity.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardSubtitle,
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
    IonRange,
    IonChip,
    IonGrid,
    IonRow,
    IonCol,
    PageHeaderComponent,
    PageFooterComponent,
    InfoCardComponent,
    IonNote,
  ],
})
export class IntegrityPage {
  /* ------------------------------------------------------------------------ */
  /* Core UI State                                                            */
  /* ------------------------------------------------------------------------ */

  /** Whether the plugin is available on the current platform */
  readonly isSupported = signal<boolean>(false);

  /** Native plugin version */
  readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);

  /** Last integrity report produced by a manual check */
  readonly lastReport = signal<IntegrityReport | null>(null);

  /** Active UI tab */
  readonly activeTab = signal<IntegrityTab>('actions');

  /** Whether a request is currently in progress */
  readonly inProgress = signal<boolean>(false);

  /** Raw JSON inspection toggle */
  readonly showRawJson = signal<boolean>(false);

  /* ------------------------------------------------------------------------ */
  /* Integrity Check Controls                                                 */
  /* ------------------------------------------------------------------------ */

  /**
   * Integrity strictness index:
   * 0 → basic
   * 1 → standard
   * 2 → strict
   */
  readonly checkLevelIndex = signal<number>(0);

  private readonly checkLevels: Array<'basic' | 'standard' | 'strict'> = ['basic', 'standard', 'strict'];

  /** Include diagnostic descriptions and metadata */
  readonly includeDebugInfo = signal<boolean>(true);

  /** UI state for last check */
  readonly checkState = signal<ResultState>('idle');

  /** UI state for block page actions */
  readonly presentBlockPageState = signal<ResultState>('idle');
  readonly presentBlockPageDemoState = signal<ResultState>('idle');
  readonly presentBlockPageContextState = signal<ResultState>('idle');

  /* ------------------------------------------------------------------------ */
  /* Signal Filters (UI only)                                                  */
  /* ------------------------------------------------------------------------ */

  readonly selectedCategory = signal<Category>('all');
  readonly selectedConfidence = signal<Confidence>('all');

  readonly categories = ['all', 'root', 'emulator', 'debug', 'hook', 'tamper', 'environment'] as const;

  readonly confidences = ['all', 'high', 'medium', 'low'] as const;

  /* ------------------------------------------------------------------------ */
  /* Derived State                                                            */
  /* ------------------------------------------------------------------------ */

  /** Whether real-time integrity listener reactions are enabled */
  readonly realtimeListenerEnabled = computed(() => {
    return this.integrityStore.enabled();
  });

  /** */
  readonly isRealtimeLive = computed(() => {
    return this.integrityStore.enabled();
  });

  /** Whether silent mode is enabled (UI facade) */
  readonly silentModeEnabled = computed(() => {
    return this.integrityStore.silentMode();
  });

  /** Current integrity level label */
  readonly selectedCheckLevelLabel = computed(() => {
    const level = this.checkLevels[this.checkLevelIndex()] ?? 'basic';
    return level.toUpperCase();
  });

  /** Environment summary extracted from the last report */
  readonly environmentSummary = computed(() => {
    const env = this.lastReport()?.environment;
    if (!env) return null;

    return {
      platform: env.platform,
      isEmulator: env.isEmulator,
      isDebugBuild: env.isDebugBuild,
    };
  });

  /** Policy preview (illustrative only) */
  readonly policyPreview = computed<'ALLOW' | 'WARN' | 'BLOCK'>(() => {
    const signals = this.lastReport()?.signals ?? [];

    if (signals.some((s) => s.confidence === 'high')) return 'BLOCK';
    if (signals.some((s) => s.confidence === 'medium')) return 'WARN';
    return 'ALLOW';
  });

  /** Policy preview derived from real-time signals (demo-only) */
  readonly realtimePolicyPreview = computed<'ALLOW' | 'WARN' | 'BLOCK'>(() => {
    const signals = this.integrityStore.signals();

    if (signals.some((s) => s.confidence === 'high')) {
      return 'BLOCK';
    }

    if (signals.some((s) => s.confidence === 'medium')) {
      return 'WARN';
    }

    return 'ALLOW';
  });

  /** */
  readonly policyColor = computed(() => {
    switch (this.policyPreview()) {
      case 'ALLOW':
        return 'success';
      case 'WARN':
        return 'warning';
      case 'BLOCK':
        return 'danger';
      default:
        return 'medium';
    }
  });

  /** Pretty printed raw JSON */
  readonly rawJsonResponse = computed(() => {
    const report = this.lastReport();
    return report ? JSON.stringify(report, null, 2) : '';
  });

  /** */
  readonly realtimeSummary = computed(() => {
    const signals = this.integrityStore.signals();

    if (signals.length === 0) {
      return {
        count: 0,
        high: 0,
        medium: 0,
        low: 0,
      };
    }

    return {
      count: signals.length,
      high: signals.filter((s) => s.confidence === 'high').length,
      medium: signals.filter((s) => s.confidence === 'medium').length,
      low: signals.filter((s) => s.confidence === 'low').length,
    };
  });

  /** Signals filtered by UI controls */
  readonly filteredSignals = computed(() => {
    const report = this.lastReport();
    if (!report) return [];

    return report.signals.filter((signal) => {
      const categoryOk = this.selectedCategory() === 'all' || signal.category === this.selectedCategory();

      const confidenceOk = this.selectedConfidence() === 'all' || signal.confidence === this.selectedConfidence();

      return categoryOk && confidenceOk;
    });
  });

  /** Last executed check level label (derived) */
  readonly lastCheckLevelLabel = computed(() => {
    if (!this.lastReport()) return null;
    const level = this.checkLevels[this.checkLevelIndex()] ?? 'basic';
    return level.toUpperCase();
  });

  /** Plugin version string for footer */
  readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  /* ------------------------------------------------------------------------ */
  /* Constants                                                                */
  /* ------------------------------------------------------------------------ */

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/integrity';

  /* ------------------------------------------------------------------------ */
  /* Constructor & Init                                                       */
  /* ------------------------------------------------------------------------ */

  constructor(
    private readonly actionSheetCtrl: ActionSheetController,
    private readonly toastCtrl: ToastController,
    private readonly integrityStore: IntegritySignalStore,
  ) {
    this.isSupported.set(!!Integrity);
    this.initPluginVersion();

    addIcons({
      playOutline,
      analyticsOutline,
      pulseOutline,
      logInOutline,
      home,
      shieldCheckmarkOutline,
      checkmarkCircle,
      alertCircle,
      cubeOutline,
      copyOutline,
      eyeOffOutline,
      eyeOutline,
    });
  }

  /* ------------------------------------------------------------------------ */
  /* Navigation & UI Helpers                                                  */
  /* ------------------------------------------------------------------------ */

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

  toggleRawJson(): void {
    this.showRawJson.update((v) => !v);
  }

  clearRealtimeSignals(): void {
    this.integrityStore.clearSignals();
  }

  /* ------------------------------------------------------------------------ */
  /* Event Handlers (UI)                                                      */
  /* ------------------------------------------------------------------------ */

  onToggleRealtimeListener(enabled: boolean): void {
    this.integrityStore.setEnabled(enabled);
  }

  onToggleSilentMode(enabled: boolean): void {
    this.integrityStore.setSilentMode(enabled);
  }

  onTabChange(event: CustomEvent): void {
    this.activeTab.set(event.detail.value);
  }

  onCheckLevelChange(event: CustomEvent): void {
    if (typeof event.detail.value === 'number') {
      this.checkLevelIndex.set(event.detail.value);
    }
  }

  setCategory(cat: Category): void {
    this.selectedCategory.set(cat);
  }

  setConfidence(conf: Confidence): void {
    this.selectedConfidence.set(conf);
  }

  /* ------------------------------------------------------------------------ */
  /* Plugin Calls                                                             */
  /* ------------------------------------------------------------------------ */

  async onRunCheck(): Promise<void> {
    const level = this.checkLevels[this.checkLevelIndex()] ?? 'basic';
    await this.runCheck(level);
  }

  private async runCheck(level: 'basic' | 'standard' | 'strict'): Promise<void> {
    this.inProgress.set(true);

    try {
      const report = await Integrity.check({
        level,
        includeDebugInfo: this.includeDebugInfo(),
      });

      this.lastReport.set(report);
      this.integrityStore.setLastReport(report);

      this.checkState.set(report.compromised ? 'failure' : 'success');
    } catch (err: any) {
      this.checkState.set('error');
      await this.presentError(err);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onPresentBlockPage(): Promise<void> {
    this.inProgress.set(true);
    try {
      const res = await Integrity.presentBlockPage({
        reason: IntegrityBlockReason.COMPROMISED_ENVIRONMENT,
      });
      this.presentBlockPageState.set(res.presented ? 'success' : 'failure');
    } catch (err: any) {
      this.presentBlockPageState.set('error');
      await this.presentError(err);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onPresentBlockPageDemo(): Promise<void> {
    this.inProgress.set(true);
    try {
      const res = await Integrity.presentBlockPage({
        reason: IntegrityBlockReason.INTEGRITY_FAILED,
        dismissible: true,
      });
      this.presentBlockPageDemoState.set(res.presented ? 'success' : 'failure');
    } catch (err: any) {
      this.presentBlockPageDemoState.set('error');
      await this.presentError(err);
    } finally {
      this.inProgress.set(false);
    }
  }

  async onPresentBlockPageWithContext(): Promise<void> {
    this.inProgress.set(true);
    try {
      const report = this.lastReport();
      const res = await Integrity.presentBlockPage({
        reason: IntegrityBlockReason.INTEGRITY_FAILED,
        dismissible: true,
        customUrl: 'public/integrity-block.html',
        context: {
          score: report?.score ?? 0,
          compromised: report?.compromised ?? false,
          signalCount: report?.signals.length ?? 0,
          timestamp: Date.now(),
        },
      });
      this.presentBlockPageContextState.set(res.presented ? 'success' : 'failure');
    } catch (err: any) {
      this.presentBlockPageContextState.set('error');
      await this.presentError(err);
    } finally {
      this.inProgress.set(false);
    }
  }

  /* ------------------------------------------------------------------------ */
  /* Utilities                                                                */
  /* ------------------------------------------------------------------------ */

  async copyReport(): Promise<void> {
    const report = this.lastReport();
    if (!report) return;

    await navigator.clipboard.writeText(JSON.stringify(report, null, 2));

    const toast = await this.toastCtrl.create({
      message: 'Integrity report copied to clipboard',
      duration: 1500,
      color: 'success',
    });

    await toast.present();
  }

  async exportSignalsJson(): Promise<void> {
    const signals = this.integrityStore.signals();
    if (!signals.length) return;

    const blob = new Blob([JSON.stringify(signals, null, 2)], { type: 'application/json' });

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'integrity-signals.json';
    a.click();

    URL.revokeObjectURL(url);
  }

  formatMetadata(metadata: any): string {
    if (!metadata) return '';
    return Object.entries(metadata)
      .map(([k, v]) => `${k}: ${v}`)
      .join(', ');
  }

  private async presentError(error: any): Promise<void> {
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

  private async initPluginVersion(): Promise<void> {
    if (!this.isSupported()) return;

    try {
      const result = await Integrity.getPluginVersion();
      this.pluginVersion.set(result);
    } catch {
      this.pluginVersion.set('Error');
    }
  }
}
