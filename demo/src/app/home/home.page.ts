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

import { APP_VERSION } from '../version';

import { PLUGINS, PluginConfig, registerPluginIcons } from '../plugins-list';
import { PageFooterComponent } from '../components/page-footer.component';
import { AboutCardComponent } from '../components/about-card.component';

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
    AboutCardComponent,
  ],
})
export class HomePage {
  /**
   * Raw plugin registry, seeded from `plugins-list.ts`.
   *
   * A signal keeps the derived state reactive and lets tests provide their own
   * list without mocking the module.
   */
  public readonly pluginList = signal<PluginConfig[]>(PLUGINS);

  public readonly plugins = computed(() =>
    this.pluginList()
      .filter((plugin) => plugin.isVisible)
      .sort((a, b) => a.id - b.id),
  );

  /**
   * The only visible plugin when the demo hosts exactly one.
   *
   * The about card describes a single plugin, so it is rendered only for
   * single-plugin demos and hidden as soon as a second plugin is present.
   */
  public readonly singlePlugin = computed<PluginConfig | undefined>(() => {
    const plugins = this.plugins();

    return plugins.length === 1 ? plugins[0] : undefined;
  });

  public readonly appVersion = signal<string | undefined>(APP_VERSION);

  public trackByPluginId(_index: number, plugin: { id: number }): number {
    return plugin.id;
  }

  constructor() {
    // Every plugin entry owns its icon, so the demo only registers what
    // `plugins-list.ts` declares. This runs once, before the first render.
    registerPluginIcons(PLUGINS);
  }
}
