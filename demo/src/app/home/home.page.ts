import { Component, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  IonContent,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonText,
  IonHeader,
  IonToolbar,
  IonTitle,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import {
  logoIonic,
  flask,
  cog,
  shieldCheckmark,
  shieldHalf,
  trophy,
  card,
  person,
  key,
} from 'ionicons/icons';

import { APP_VERSION } from '../version';

import { PLUGINS } from '../plugins-list';
import { PageFooterComponent } from '../components/page-footer.component';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    IonTitle,
    IonToolbar,
    IonHeader,
    IonText,
    IonListHeader,
    IonContent,
    IonList,
    IonItem,
    IonLabel,
    IonIcon,
    RouterLink,
    PageFooterComponent,
  ],
})
export class HomePage {
  public plugins = computed(() => PLUGINS.filter((p) => p.isVisible).sort((a, b) => a.id - b.id));
  public readonly appVersion = signal<string | undefined>(APP_VERSION);

  constructor() {
    addIcons({ logoIonic, flask, cog, shieldCheckmark, shieldHalf, trophy, card, person, key });
  }
}
