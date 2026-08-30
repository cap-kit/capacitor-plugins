import { ChangeDetectionStrategy, Component, signal, computed } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import {
  ActionSheetController,
  ToastController,
  IonContent,
  IonList,
  IonItemDivider,
  IonLabel,
  IonIcon,
  IonItem,
  IonBadge,
} from '@ionic/angular';
import {
  People,
  PeopleCapabilities,
  PeopleErrorCode,
  PluginVersionResult,
  UnifiedContact,
  Group,
  PeopleChangeEvent,
} from '@cap-kit/people';
import { addIcons } from 'ionicons';
import {
  home,
  trash,
  addCircle,
  person,
  search,
  shieldCheckmark,
  list,
  add,
  peopleCircle,
  eye,
  eyeOff,
  grid,
  refresh,
} from 'ionicons/icons';
import { PageHeaderComponent } from '../components/page-header.component';
import { PageFooterComponent } from '../components/page-footer.component';
import { InfoCardComponent } from '../components/info-card.component';
import { PluginListenerHandle } from '@capacitor/core';

type ResultState = 'idle' | 'match' | 'no-match' | 'error';

@Component({
  selector: 'app-people',
  templateUrl: './people.page.html',
  styleUrls: ['./people.page.scss'],
  standalone: true,
  imports: [
    IonBadge,
    IonItem,
    IonIcon,
    IonLabel,
    IonItemDivider,
    IonContent,
    ReactiveFormsModule,
    PageHeaderComponent,
    InfoCardComponent,
    PageFooterComponent,
    IonList,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PeoplePage {
  // ---------------------------------------------------------------------------
  // Plugin state
  // ---------------------------------------------------------------------------

  public readonly isSupported = signal(false);
  public readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);

  public readonly selectedContact = signal<UnifiedContact | undefined>(undefined);
  public readonly contactsList = signal<UnifiedContact[]>([]);
  public readonly capabilities = signal<PeopleCapabilities | undefined>(undefined);
  public readonly groups = signal<Group[]>([]);
  public readonly isObserving = signal(false);
  private changeListener?: PluginListenerHandle;

  // ---------------------------------------------------------------------------
  // Android review environment diagnostics
  // ---------------------------------------------------------------------------

  public readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  private readonly GH_URL = 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/people';

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
    this.isSupported.set(!!People);
    this.initPluginVersion();
    addIcons({
      home,
      trash,
      addCircle,
      person,
      search,
      list,
      shieldCheckmark,
      add,
      peopleCircle,
      eye,
      eyeOff,
      grid,
      refresh,
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
      const result: PluginVersionResult = await People.getPluginVersion();
      this.pluginVersion.set(result);
    } catch (e) {
      console.error('Error getting plugin version', e);
      this.pluginVersion.set('Error');
    }
  }

  // ---------------------------------------------------------------------------
  // People actions
  // ---------------------------------------------------------------------------

  /** [ZERO-PERMISSION] Opens the system picker */
  async pickContact() {
    try {
      const result = await People.pickContact({
        projection: ['name', 'phones', 'emails'],
      });
      this.selectedContact.set(result.contact);
      this.showToast('Contact selected successfully');
    } catch (e: any) {
      if (e.code !== PeopleErrorCode.CANCELLED) {
        this.presentError(e);
      }
    }
  }

  /** Requests permissions and retrieves capabilities */
  async checkAndRequestPermissions() {
    try {
      await People.requestPermissions();
      const caps = await People.getCapabilities();
      this.capabilities.set(caps);
      this.showToast(`Reading: ${caps.canRead ? 'YES' : 'NO'}, Writing: ${caps.canWrite ? 'YES' : 'NO'}`);
    } catch (e) {
      this.presentError(e);
    }
  }

  /** [SYSTEMIC-ACCESS] Load first 10 contacts (Requires permissions) */
  async loadContacts() {
    try {
      const result = await People.getContacts({
        projection: ['name', 'phones'],
        limit: 10,
        offset: 0,
      });
      this.contactsList.set(result.contacts);
      this.showToast(`Loaded ${result.contacts.length} contacts`);
    } catch (e) {
      this.presentError(e);
    }
  }

  /** Create an "App Owned" test contact */
  async createTestContact() {
    try {
      const { contact } = await People.createContact({
        contact: {
          name: {
            given: 'Cap',
            family: 'Tester',
            display: '',
          },
          phones: [{ number: '555-0123', label: 'work' }],
          emails: [{ address: 'test@capkit.io', label: 'work' }],
        },
      });
      this.selectedContact.set(contact);
      this.showToast('Test contact created!');
    } catch (e) {
      this.presentError(e);
    }
  }

  // --- OBSERVER MANAGEMENT (Events) ---

  async toggleObservation() {
    if (this.isObserving()) {
      await this.changeListener?.remove();
      this.isObserving.set(false);
      this.showToast('Observation interrupted');
    } else {
      try {
        // Register the listener for changes in the contact database
        this.changeListener = await People.addListener('peopleChange', (event: PeopleChangeEvent) => {
          console.log('Change detected:', event);
          this.showToast(`Change detected! Affected IDs: ${event.ids.length || 'General'}`);
          // Optional: Reload the list if displayed
          if (this.contactsList().length > 0) this.loadContacts();
        });

        this.isObserving.set(true);
        this.showToast('Active observation: Edit a contact in the system to test');
      } catch (e) {
        this.presentError(e);
      }
    }
  }

  // --- GROUP MANAGEMENT ---

  async loadGroups() {
    try {
      const result = await People.listGroups();
      this.groups.set(result.groups);
      this.showToast(`${result.groups.length} groups found`);
    } catch (e) {
      this.presentError(e);
    }
  }

  async createGroup() {
    try {
      const { group } = await People.createGroup({ name: 'Test CapKit ' + new Date().getSeconds() });
      this.groups.update((g) => [...g, group]);
      this.showToast(`Group "${group.name}" created`);
    } catch (e) {
      this.presentError(e);
    }
  }

  private async showToast(message: string) {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2000,
      position: 'bottom',
    });
    await toast.present();
  }
}
