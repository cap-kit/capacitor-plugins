import { Injectable, computed, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import type { PermissionState, PluginListenerHandle } from '@capacitor/core';
import { LocalNetwork } from '@cap-kit/local-network';
import type { LocalNetworkPermissionChangeEvent } from '@cap-kit/local-network';

/** A `permissionChange` payload with the local time it was received. */
export interface PermissionChangeLogEntry {
  readonly state: PermissionState;
  readonly receivedAt: string;
}

/** Number of permission change events kept in the in-memory log. */
const MAX_LOG_ENTRIES = 20;

/**
 * Thin Angular wrapper around the LocalNetwork plugin.
 *
 * Owns the listener lifecycle so the page never has to keep a
 * `PluginListenerHandle` around, and exposes every plugin call as a signal.
 */
@Injectable({ providedIn: 'root' })
export class LocalNetworkService {
  private readonly platformState = signal(Capacitor.getPlatform());
  private readonly permissionState = signal<PermissionState | 'unknown'>('unknown');
  private readonly versionState = signal<string | undefined>(undefined);
  private readonly listeningState = signal(false);
  private readonly eventsState = signal<readonly PermissionChangeLogEntry[]>([]);
  private readonly errorState = signal<string | undefined>(undefined);
  private readonly busyState = signal(false);

  private handle: PluginListenerHandle | undefined;

  readonly platform = this.platformState.asReadonly();
  readonly permission = this.permissionState.asReadonly();
  readonly version = this.versionState.asReadonly();
  readonly listening = this.listeningState.asReadonly();
  readonly events = this.eventsState.asReadonly();
  readonly error = this.errorState.asReadonly();
  readonly busy = this.busyState.asReadonly();

  readonly isNative = computed(() => Capacitor.isNativePlatform());
  readonly permissionGranted = computed(() => this.permissionState() === 'granted');

  /** Reads the current permission state. */
  async check(): Promise<void> {
    await this.run(async () => {
      const result = await LocalNetwork.checkPermissions();
      this.permissionState.set(result.localNetwork);
    });
  }

  /**
   * Shows the system prompt (Android 17+) or resolves immediately with the
   * current state on platforms where the permission is not enforced.
   */
  async request(): Promise<void> {
    await this.run(async () => {
      const result = await LocalNetwork.requestPermissions();
      this.permissionState.set(result.localNetwork);
    });
  }

  /** Reads the native plugin version (the JS package version on the web). */
  async loadVersion(): Promise<void> {
    await this.run(async () => {
      const result = await LocalNetwork.getPluginVersion();
      this.versionState.set(result.version);
    });
  }

  /** Subscribes to `permissionChange`; no-op when already listening. */
  async startListening(): Promise<void> {
    if (this.handle) {
      return;
    }

    await this.run(async () => {
      this.handle = await LocalNetwork.addListener('permissionChange', (event: LocalNetworkPermissionChangeEvent) =>
        this.onPermissionChange(event),
      );
      this.listeningState.set(true);
    });
  }

  /** Removes the listener registered by {@link startListening}. */
  async stopListening(): Promise<void> {
    const handle = this.handle;
    this.handle = undefined;
    this.listeningState.set(false);

    await this.run(async () => {
      await handle?.remove();
    });
  }

  /** Removes every listener for this plugin, including ones not owned here. */
  async removeAllListeners(): Promise<void> {
    this.handle = undefined;
    this.listeningState.set(false);

    await this.run(async () => {
      await LocalNetwork.removeAllListeners();
    });
  }

  /** Clears the in-memory event log. */
  clearEvents(): void {
    this.eventsState.set([]);
  }

  /** Clears the last error. */
  clearError(): void {
    this.errorState.set(undefined);
  }

  private onPermissionChange(event: LocalNetworkPermissionChangeEvent): void {
    this.permissionState.set(event.localNetwork);
    this.eventsState.update((entries) =>
      [{ state: event.localNetwork, receivedAt: new Date().toLocaleTimeString() }, ...entries].slice(
        0,
        MAX_LOG_ENTRIES,
      ),
    );
  }

  /**
   * Runs a plugin call, tracking the busy state and normalizing any rejection
   * into a user facing message.
   */
  private async run(action: () => Promise<void>): Promise<void> {
    this.busyState.set(true);
    this.errorState.set(undefined);

    try {
      await action();
    } catch (error: unknown) {
      this.errorState.set(this.describeError(error));
    } finally {
      this.busyState.set(false);
    }
  }

  private describeError(error: unknown): string {
    if (typeof error === 'object' && error !== null) {
      const candidate = error as { code?: unknown; message?: unknown };
      const code = typeof candidate.code === 'string' ? candidate.code : undefined;
      const message = typeof candidate.message === 'string' ? candidate.message : undefined;

      if (code && message) {
        return `${code}: ${message}`;
      }
      if (message) {
        return message;
      }
    }

    return String(error);
  }
}
