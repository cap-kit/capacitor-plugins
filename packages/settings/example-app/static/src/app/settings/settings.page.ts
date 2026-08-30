import { ChangeDetectionStrategy, Component, signal, computed } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActionSheetController, ToastController, IonContent, IonButton, IonItem, IonList } from '@ionic/angular';
import { Settings, IOSSettings, AndroidSettings, PluginVersionResult } from '@cap-kit/settings';
import { addIcons } from 'ionicons';
import { home } from 'ionicons/icons';
import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.page.html',
  styleUrls: ['./settings.page.scss'],
  standalone: true,
  imports: [
    IonItem,
    IonList,
    IonContent,
    IonButton,
    ReactiveFormsModule,
    PageHeaderComponent,
    InfoCardComponent,
    PageFooterComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPage {
  public readonly isSupported = signal(false);
  public readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);

  public readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/settings';

  constructor(
    private actionSheetCtrl: ActionSheetController,
    private toastCtrl: ToastController,
  ) {
    this.isSupported.set(!!Settings);
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
      const result: PluginVersionResult = await Settings.getPluginVersion();
      this.pluginVersion.set(result);
    } catch (e) {
      console.error('Error getting plugin version', e);
      this.pluginVersion.set('Error');
    }
  }

  async openAppSettings() {
    try {
      const result = await Settings.open({
        optionIOS: IOSSettings.App,
        optionAndroid: AndroidSettings.ApplicationDetails,
      });
      await this.presentSuccess(`${result}`);
    } catch (e) {
      await this.presentError(true);
    }
  }

  async openNotificationSettings() {
    try {
      const result = await Settings.open({
        optionIOS: IOSSettings.AppNotification,
        optionAndroid: AndroidSettings.AppNotification,
      });
      await this.presentSuccess(`${result}`);
    } catch (e) {
      await this.presentError(e);
    }
  }

  async openWifiSettingsAndroid() {
    try {
      const result = await Settings.openAndroid({
        option: AndroidSettings.Wifi,
      });
      await this.presentSuccess(`${result}`);
    } catch (e) {
      await this.presentError(e);
    }
  }

  async openBluetoothSettingsIOS() {
    try {
      const result = await Settings.openIOS({
        option: IOSSettings.Bluetooth,
      });
      await this.presentSuccess(`${result}`);
    } catch (e) {
      await this.presentError(e);
    }
  }
}
