import { Injectable, signal } from '@angular/core';
import type { IntegritySignal, IntegrityReport } from '@cap-kit/integrity';

/**
 * Global store for integrity signals and reports.
 *
 * Responsibilities:
 * - Keep track of real-time integrity signals
 * - Expose enable/disable flag for listener reactions
 * - Hold the latest integrity report (from manual checks or derived updates)
 *
 * NOTE:
 * - This store is intentionally simple (signals-based).
 * - No side effects (no plugin calls, no UI).
 */
@Injectable({ providedIn: 'root' })
export class IntegritySignalStore {
  /**
   * Whether integrity signal reactions are enabled.
   *
   * The listener may still be registered at native level,
   * but when disabled, incoming signals are ignored by the app.
   */
  readonly enabled = signal<boolean>(true);

  /**
   * Accumulated real-time integrity signals.
   *
   * Signals are observational and appended as they arrive.
   */
  readonly signals = signal<IntegritySignal[]>([]);

  /**
   *
   */
  readonly silentMode = signal<boolean>(false);

  /**
   * Last integrity report.
   *
   * This may be set by:
   * - manual Integrity.check()
   * - future derived logic from signals
   */
  readonly lastReport = signal<IntegrityReport | null>(null);

  constructor() {
    const stored = sessionStorage.getItem('integrity.signals');
    if (stored) {
      try {
        this.signals.set(JSON.parse(stored));
      } catch {
        // ignore corrupted data
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Mutators (explicit API)
  // ---------------------------------------------------------------------------

  addSignal(signal: IntegritySignal): void {
    this.signals.update((current) => {
      const next = [...current, signal];
      sessionStorage.setItem('integrity.signals', JSON.stringify(next));
      return next;
    });
  }

  clearSignals(): void {
    this.signals.set([]);
    sessionStorage.removeItem('integrity.signals');
  }

  setLastReport(report: IntegrityReport | null): void {
    this.lastReport.set(report);
  }

  setEnabled(value: boolean): void {
    this.enabled.set(value);
  }

  setSilentMode(value: boolean): void {
    this.silentMode.set(value);
  }
}
