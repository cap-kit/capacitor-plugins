import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
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
  IonItem,
  IonLabel,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonToggle,
  IonNote,
} from '@ionic/angular';

import { SplashMotion } from '@cap-kit/splash-motion';

import { addIcons } from 'ionicons';
import { playOutline, pauseOutline, checkmarkCircle, alertCircle, terminalOutline } from 'ionicons/icons';

import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

type ResultState = 'idle' | 'success' | 'failure' | 'error';

@Component({
  selector: 'app-splash-motion',
  templateUrl: './splash-motion.page.html',
  styleUrls: ['./splash-motion.page.scss'],
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
    IonItem,
    IonLabel,
    IonInput,
    IonSelect,
    IonSelectOption,
    IonToggle,
    IonNote,
    PageHeaderComponent,
    PageFooterComponent,
    InfoCardComponent,
  ],
})
export class SplashMotionPage {
  readonly version = signal<string>('…');
  readonly lastResult = signal<ResultState>('idle');
  readonly lastAction = signal<string>('');

  // Live option controls (plain fields so ngModel can two-way bind).
  autoHide = true;
  showDuration = 3000;
  fadeInDuration = 200;
  fadeOutDuration = 200;
  useLottie = false;
  lottieSource = 'public/lottie/splash.json';
  lottieDarkSource = '';
  lottieLoop = true;
  lottieSpeed = 1;
  hapticStyle: 'light' | 'medium' | 'heavy' = 'medium';
  entranceType: 'none' | 'fade' | 'scale' = 'none';
  useGradient = false;
  useBranding = false;
  useSpinner = false;
  spinnerStyle: 'small' | 'large' | 'horizontal' | 'inverse' | 'smallInverse' | 'largeInverse' = 'large';
  spinnerColor = '#999999';

  constructor() {
    addIcons({
      playOutline,
      pauseOutline,
      checkmarkCircle,
      alertCircle,
      terminalOutline,
    });
    void this.loadVersion();
  }

  private async loadVersion(): Promise<void> {
    try {
      const result = await SplashMotion.getPluginVersion();
      this.version.set(result.version);
      this.lastResult.set('success');
      this.lastAction.set('getPluginVersion');
    } catch {
      this.version.set('unavailable');
      this.lastResult.set('error');
      this.lastAction.set('getPluginVersion');
    }
  }

  async onShow(): Promise<void> {
    try {
      await SplashMotion.show({
        autoHide: this.autoHide,
        showDuration: this.showDuration,
        fadeInDuration: this.fadeInDuration,
        fadeOutDuration: this.fadeOutDuration,
        lottie: this.useLottie
          ? {
              source: this.lottieSource,
              ...(this.lottieDarkSource.trim() !== '' ? { darkSource: this.lottieDarkSource } : {}),
              loop: this.lottieLoop,
              speed: this.lottieSpeed,
            }
          : undefined,
        entrance: this.entranceType !== 'none' ? { type: this.entranceType, durationMs: 400 } : undefined,
        backgroundGradient: this.useGradient ? { top: '#1a2a6c', bottom: '#b21f1f' } : undefined,
        brandingImage: this.useBranding
          ? { source: 'public/images/branding.png', gravity: 'bottomEnd', margin: 16 }
          : undefined,
        spinner: this.useSpinner
          ? {
              show: true,
              style: this.spinnerStyle,
              ...(this.spinnerColor.trim() !== '' ? { color: this.spinnerColor } : {}),
            }
          : undefined,
        haptic: { style: this.hapticStyle },
      });
      this.lastResult.set('success');
      this.lastAction.set('show');
    } catch {
      this.lastResult.set('error');
      this.lastAction.set('show');
    }
  }

  async onHide(): Promise<void> {
    try {
      await SplashMotion.hide({
        fadeOutDuration: this.fadeOutDuration,
        haptic: { style: this.hapticStyle },
      });
      this.lastResult.set('success');
      this.lastAction.set('hide');
    } catch {
      this.lastResult.set('error');
      this.lastAction.set('hide');
    }
  }
}
