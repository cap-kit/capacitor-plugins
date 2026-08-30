import { Integrity } from '@cap-kit/integrity';
import { ToastController } from '@ionic/angular';
import { IntegritySignalStore } from './integrity-signal.store';

/**
 * Initializes the global integrity signal listener.
 *
 * CONTRACT:
 * - Called exactly once at app startup
 * - Listener remains active for the entire app lifetime
 * - Reactions can be enabled/disabled via the store
 *
 * WARNING:
 * - This function MUST NOT be called from a page/component.
 */
export async function initIntegrityListener(store: IntegritySignalStore, toastCtrl: ToastController): Promise<void> {
  try {
    await Integrity.addListener('integritySignal', async (event) => {
      // Listener is globally registered, but reactions are conditional
      if (!store.enabled()) return;

      // Normalize payload
      const signals = Array.isArray((event as any).signals) ? (event as any).signals : [event];

      for (const signal of signals) {
        if (!signal?.id) continue;

        store.addSignal(signal);

        if (store.silentMode()) continue;

        // Show global toast notification
        const toast = await toastCtrl.create({
          message: `Integrity signal detected: ${signal.id}`,
          duration: 3000,
          color: signal.confidence === 'high' ? 'danger' : signal.confidence === 'medium' ? 'warning' : 'medium',
          position: 'top',
        });

        await toast.present();
      }
    });
  } catch (err) {
    // Listener setup failure should not crash the app
    // but is useful during development
    // eslint-disable-next-line no-console
    console.error('[Integrity] Failed to register integritySignal listener', err);
  }
}
