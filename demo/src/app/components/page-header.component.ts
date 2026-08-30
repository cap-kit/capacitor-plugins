import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonBreadcrumbs,
  IonBreadcrumb,
  IonIcon,
} from '@ionic/angular';

@Component({
  selector: 'app-page-header',
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ title() }}</ion-title>
      </ion-toolbar>
      <ion-toolbar>
        <ion-breadcrumbs>
          <ion-breadcrumb routerLink="/">
            <ion-icon slot="start" name="home" />
            Home
          </ion-breadcrumb>
          <ion-breadcrumb>{{ title() }}</ion-breadcrumb>
        </ion-breadcrumbs>
      </ion-toolbar>
    </ion-header>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonBreadcrumbs, IonBreadcrumb, IonIcon, RouterLink],
})
export class PageHeaderComponent {
  title = input.required<string>();
}
