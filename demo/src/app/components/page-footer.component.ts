import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IonFooter, IonToolbar, IonNote } from '@ionic/angular';

@Component({
  selector: 'app-page-footer',
  template: `
    <ion-footer>
      <ion-toolbar>
        <ion-note slot="start" style="padding-left: 10px;">
          @if (version()) {
            <span>{{ label() }}: {{ version() }}</span>
          }
        </ion-note>
      </ion-toolbar>
    </ion-footer>
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
  imports: [IonFooter, IonToolbar, IonNote],
})
export class PageFooterComponent {
  version = input<string | undefined>();
  label = input<string>('Version');
}
