import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import {
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardSubtitle,
  IonCardContent,
  IonText,
  IonRow,
  IonCol,
  IonIcon,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import { logoGithub, chevronUpOutline, chevronDownOutline } from 'ionicons/icons';

@Component({
  selector: 'app-info-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardSubtitle,
    IonCardContent,
    IonText,
    IonRow,
    IonCol,
    IonIcon,
  ],
  template: `
    <ion-card>
      <ion-card-header>
        <ion-row class="ion-align-items-center">
          <ion-col>
            <ion-card-title>{{ title() }}</ion-card-title>
            @if (subtitle()) {
              <ion-card-subtitle>{{ subtitle() }}</ion-card-subtitle>
            }
          </ion-col>

          <!-- Collapse toggle -->
          <ion-col size="auto">
            <ion-icon
              class="toggle-icon"
              [name]="expanded() ? 'chevron-up-outline' : 'chevron-down-outline'"
              (click)="toggle()"
            />
          </ion-col>
        </ion-row>
      </ion-card-header>

      <!-- Collapsible content -->
      @if (expanded()) {
        <ion-card-content>
          <ion-text>
            {{ content() }}
          </ion-text>

          @if (showGithubIcon()) {
            <ion-row class="ion-no-padding ion-justify-content-end">
              <ion-icon
                name="logo-github"
                size="large"
                class="github-icon"
                (click)="openGithub()"
              />
            </ion-row>
          }
        </ion-card-content>
      }
    </ion-card>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .toggle-icon {
        cursor: pointer;
        font-size: 22px;
        opacity: 0.7;
      }
      .toggle-icon:hover {
        opacity: 1;
      }
      .github-icon {
        cursor: pointer;
        padding: 8px;
      }
    `,
  ],
})
export class InfoCardComponent {
  // Inputs
  title = input.required<string>();
  subtitle = input<string | undefined>(undefined);
  content = input.required<string>();

  // Optional GitHub link
  githubUrl = input<string | undefined>(undefined);

  // Explicit visibility control for the GitHub icon
  showGithubIcon = input<boolean>(true);

  // Output event
  githubClick = output<void>();

  // State: expanded by default
  readonly expanded = signal(true);

  toggle(): void {
    this.expanded.update((v) => !v);
  }

  constructor() {
    addIcons({
      'logo-github': logoGithub,
      'chevron-up-outline': chevronUpOutline,
      'chevron-down-outline': chevronDownOutline,
    });
  }

  openGithub(): void {
    const url = this.githubUrl();
    if (!url) return;

    // Emit event for external listeners (optional)
    this.githubClick.emit();

    // Default behavior: open link
    window.open(url, '_blank');
  }
}
