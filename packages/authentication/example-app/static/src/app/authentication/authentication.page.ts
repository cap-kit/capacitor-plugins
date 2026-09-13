/**
 * @file authentication.page.ts
 * Functional demo page for the `@cap-kit/authentication` plugin.
 *
 * Exercises the provider-keyed facade against its real API surface:
 * signIn / logout / getCurrentAccessToken / refreshToken / getPluginVersion.
 * Tracks per-provider session state, renders token-presence badges (never
 * full token values) and surfaces structured `AuthenticationError`s (the
 * canonical 10-code set) in a shared error panel. Provider scopes are
 * deliberately omitted so the plugin applies its documented defaults.
 */

import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import {
  IonBadge,
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonChip,
  IonContent,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonSegment,
  IonSegmentButton,
  ToastController,
} from '@ionic/angular';
import {
  Authentication,
  AuthProvider,
  PluginVersionResult,
  SocialAuthResult,
} from '@cap-kit/authentication';
import { addIcons } from 'ionicons';
import {
  home,
  keyOutline,
  logoApple,
  logoFacebook,
  logoGoogle,
  logOutOutline,
  refreshOutline,
} from 'ionicons/icons';
import { PageFooterComponent } from '../components/page-footer.component';
import { PageHeaderComponent } from '../components/page-header.component';
import { InfoCardComponent } from '../components/info-card.component';

/**
 * Per-provider session snapshot kept in a single signal record.
 */
interface ProviderSession {
  busy: boolean;
  signedIn: boolean;
  result?: SocialAuthResult;
  accessToken?: string;
}

/**
 * Shape rendered by the shared error panel.
 */
interface ErrorDisplay {
  provider: AuthProvider;
  action: string;
  code: string;
  message: string;
  structured: boolean;
}

const EMPTY_SESSION: ProviderSession = { busy: false, signedIn: false };

@Component({
  selector: 'app-authentication',
  templateUrl: './authentication.page.html',
  styleUrls: ['./authentication.page.scss'],
  standalone: true,
  imports: [
    IonBadge,
    IonButton,
    IonCard,
    IonCardContent,
    IonCardHeader,
    IonCardSubtitle,
    IonCardTitle,
    IonChip,
    IonContent,
    IonIcon,
    IonItem,
    IonLabel,
    IonList,
    IonSegment,
    IonSegmentButton,
    PageHeaderComponent,
    InfoCardComponent,
    PageFooterComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthenticationPage {
  public readonly providers: AuthProvider[] = ['google', 'apple', 'facebook'];

  public readonly pluginVersion = signal<PluginVersionResult | string | undefined>(undefined);

  public readonly displayVersion = computed(() => {
    const current = this.pluginVersion();
    return typeof current === 'object' ? current.version : current;
  });

  public readonly selectedProvider = signal<AuthProvider>('google');

  public readonly state = signal<Record<AuthProvider, ProviderSession>>({
    google: { ...EMPTY_SESSION },
    apple: { ...EMPTY_SESSION },
    facebook: { ...EMPTY_SESSION },
  });

  public readonly currentError = signal<ErrorDisplay | null>(null);

  public readonly selectedSession = computed(() => this.state()[this.selectedProvider()]);

  public readonly selectedTokens = computed(() => this.selectedSession().result?.tokens);

  public readonly selectedUser = computed(() => this.selectedTokens()?.user);

  private readonly PROVIDER_LABELS: Record<AuthProvider, string> = {
    google: 'Google',
    apple: 'Apple',
    facebook: 'Facebook',
  };

  private readonly GH_URL =
    'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/authentication';

  constructor(private toastCtrl: ToastController) {
    addIcons({
      home,
      'logo-google': logoGoogle,
      'logo-apple': logoApple,
      'logo-facebook': logoFacebook,
      'log-out-outline': logOutOutline,
      'refresh-outline': refreshOutline,
      'key-outline': keyOutline,
    });
    void this.initPluginVersion();
  }

  // ---------------------------------------------------------------------------
  // UI helpers
  // ---------------------------------------------------------------------------

  private async presentToast(message: string): Promise<void> {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2500,
      position: 'bottom',
    });
    await toast.present();
  }

  public providerLabel(provider: AuthProvider): string {
    return this.PROVIDER_LABELS[provider];
  }

  public isBusy(provider: AuthProvider): boolean {
    return this.state()[provider].busy;
  }

  /** Masks token values: only the last 4 characters are ever shown. */
  public mask(token: string | undefined): string {
    if (!token) return '';
    return token.length > 4 ? `••••${token.slice(-4)}` : '••••';
  }

  public formatName(name: { firstName?: string; lastName?: string } | string | undefined): string {
    if (!name) return 'not shared';
    if (typeof name === 'string') return name;
    const parts = [name.firstName, name.lastName].filter((part): part is string => Boolean(part));
    return parts.length > 0 ? parts.join(' ') : 'not shared';
  }

  public permissionsLabel(permissions: string[] | undefined): string {
    return permissions && permissions.length > 0 ? permissions.join(', ') : '—';
  }

  public formatExpiry(expiresAt: number | undefined): string {
    if (expiresAt === undefined) return '—';
    return `${expiresAt} · ${new Date(expiresAt * 1000).toLocaleString()}`;
  }

  // ---------------------------------------------------------------------------
  // Actions
  // ---------------------------------------------------------------------------

  public async signIn(provider: AuthProvider): Promise<void> {
    if (this.isBusy(provider)) return;
    this.patchProvider(provider, { busy: true });
    try {
      // Scopes intentionally omitted: the plugin applies provider defaults
      // (google: openid+email+profile, apple: name+email, facebook: public_profile+email).
      const result = await Authentication.signIn({ provider });
      this.patchProvider(provider, { result, busy: false });
      await this.refreshAccessState(provider);
      await this.presentToast(`Signed in with ${this.providerLabel(provider)}`);
    } catch (error) {
      this.patchProvider(provider, { busy: false });
      this.handleError(provider, 'signIn', error);
    }
  }

  public async signOut(): Promise<void> {
    const provider = this.selectedProvider();
    if (this.isBusy(provider)) return;
    this.patchProvider(provider, { busy: true });
    try {
      await Authentication.logout({ provider });
      this.patchProvider(provider, { result: undefined });
      await this.refreshAccessState(provider);
      await this.presentToast(`Signed out of ${this.providerLabel(provider)}`);
    } catch (error) {
      this.patchProvider(provider, { busy: false });
      this.handleError(provider, 'signOut', error);
    }
  }

  public async currentAccessToken(): Promise<void> {
    const provider = this.selectedProvider();
    if (this.isBusy(provider)) return;
    this.patchProvider(provider, { busy: true });
    await this.refreshAccessState(provider);
    const session = this.state()[provider];
    await this.presentToast(
      session.accessToken
        ? `Access token present (${this.mask(session.accessToken)})`
        : `No access token stored for ${this.providerLabel(provider)}`,
    );
  }

  public async refresh(): Promise<void> {
    const provider = this.selectedProvider();
    if (this.isBusy(provider)) return;
    this.patchProvider(provider, { busy: true });
    try {
      // apple/facebook reject with INVALID_INPUT by design; the structured
      // error lands in the shared panel below instead of crashing the page.
      const tokens = await Authentication.refreshToken({ provider });
      this.patchProvider(provider, { result: { provider, tokens }, busy: false });
      await this.presentToast('Token refreshed');
    } catch (error) {
      this.patchProvider(provider, { busy: false });
      this.handleError(provider, 'refreshToken', error);
    }
  }

  public selectProvider(provider: AuthProvider): void {
    this.selectedProvider.set(provider);
  }

  public onSegmentChange(event: Event): void {
    const value = (event as CustomEvent<{ value?: string }>).detail.value;
    if (value === 'google' || value === 'apple' || value === 'facebook') {
      this.selectedProvider.set(value);
    }
  }

  public dismissError(): void {
    this.currentError.set(null);
  }

  public openOnGithub(): void {
    window.open(this.GH_URL, '_blank');
  }

  // ---------------------------------------------------------------------------
  // Internals
  // ---------------------------------------------------------------------------

  private async initPluginVersion(): Promise<void> {
    try {
      const result: PluginVersionResult = await Authentication.getPluginVersion();
      this.pluginVersion.set(result);
    } catch (error) {
      console.error('Error getting plugin version', error);
      this.pluginVersion.set('Error');
    }
  }

  /** Re-reads the persisted access token and derives the signed-in state. */
  private async refreshAccessState(provider: AuthProvider): Promise<void> {
    try {
      const { accessToken } = await Authentication.getCurrentAccessToken({ provider });
      this.patchProvider(provider, { accessToken, signedIn: Boolean(accessToken), busy: false });
    } catch (error) {
      this.patchProvider(provider, { busy: false });
      this.handleError(provider, 'getCurrentAccessToken', error);
    }
  }

  private patchProvider(provider: AuthProvider, patch: Partial<ProviderSession>): void {
    this.state.update((s) => ({ ...s, [provider]: { ...s[provider], ...patch } }));
  }

  /**
   * Routes rejections to the error panel. USER_CANCELLED / CANCELLED are
   * consent dismissals reported identically on all platforms — neutral toast,
   * never an error panel.
   */
  private handleError(provider: AuthProvider, action: string, error: unknown): void {
    const parsed = this.parseError(error);
    if (parsed.structured && (parsed.code === 'USER_CANCELLED' || parsed.code === 'CANCELLED')) {
      this.currentError.set(null);
      void this.presentToast('Sign-in cancelled');
      return;
    }
    this.currentError.set({
      provider,
      action,
      code: parsed.structured ? parsed.code : 'UNKNOWN',
      message: parsed.message,
      structured: parsed.structured,
    });
    console.error(`[@cap-kit/authentication demo] ${action} failed`, error);
  }

  private parseError(error: unknown): { structured: boolean; code: string; message: string } {
    if (typeof error === 'object' && error !== null) {
      const candidate = error as { code?: unknown; message?: unknown };
      if (typeof candidate.code === 'string' && typeof candidate.message === 'string') {
        return { structured: true, code: candidate.code, message: candidate.message };
      }
      if (typeof candidate.message === 'string') {
        return { structured: false, code: '', message: candidate.message };
      }
      const raw = JSON.stringify(error);
      return { structured: false, code: '', message: raw || 'Unknown error' };
    }
    return { structured: false, code: '', message: String(error ?? 'Unknown error') };
  }
}
