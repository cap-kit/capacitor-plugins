import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { IntegritySignalStore } from './integrity-signal.store';

describe('IntegritySignalStore', () => {
  let store: IntegritySignalStore;

  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  describe('initial state', () => {
    it('should have enabled signal set to true by default', () => {
      store = new IntegritySignalStore();
      expect(store.enabled()).toBe(true);
    });

    it('should have signals array empty by default', () => {
      store = new IntegritySignalStore();
      expect(store.signals()).toEqual([]);
    });

    it('should have silentMode set to false by default', () => {
      store = new IntegritySignalStore();
      expect(store.silentMode()).toBe(false);
    });

    it('should have lastReport set to null by default', () => {
      store = new IntegritySignalStore();
      expect(store.lastReport()).toBeNull();
    });
  });

  describe('addSignal', () => {
    it('should add a signal to the signals array', () => {
      store = new IntegritySignalStore();
      const mockSignal = { type: 'debug', message: 'test' } as never;

      store.addSignal(mockSignal);

      expect(store.signals().length).toBe(1);
    });

    it('should append multiple signals in order', () => {
      store = new IntegritySignalStore();
      const signal1 = { type: 'debug', message: 'first' } as never;
      const signal2 = { type: 'info', message: 'second' } as never;

      store.addSignal(signal1);
      store.addSignal(signal2);

      expect(store.signals().length).toBe(2);
    });

    it('should persist signals to sessionStorage', () => {
      store = new IntegritySignalStore();
      const mockSignal = { type: 'debug', message: 'test' } as never;

      store.addSignal(mockSignal);

      const stored = sessionStorage.getItem('integrity.signals');
      expect(stored).toContain('test');
    });
  });

  describe('clearSignals', () => {
    it('should clear all signals', () => {
      store = new IntegritySignalStore();
      const mockSignal = { type: 'debug', message: 'test' } as never;
      store.addSignal(mockSignal);

      store.clearSignals();

      expect(store.signals()).toEqual([]);
    });

    it('should remove signals from sessionStorage', () => {
      store = new IntegritySignalStore();
      const mockSignal = { type: 'debug', message: 'test' } as never;
      store.addSignal(mockSignal);

      store.clearSignals();

      expect(sessionStorage.getItem('integrity.signals')).toBeNull();
    });
  });

  describe('setLastReport', () => {
    it('should set the lastReport signal', () => {
      store = new IntegritySignalStore();
      const report = { valid: true, details: {} } as never;

      store.setLastReport(report);

      expect(store.lastReport()).toBe(report);
    });

    it('should allow setting lastReport to null', () => {
      store = new IntegritySignalStore();
      const report = { valid: true, details: {} } as never;
      store.setLastReport(report);

      store.setLastReport(null);

      expect(store.lastReport()).toBeNull();
    });
  });

  describe('setEnabled', () => {
    it('should set enabled to false', () => {
      store = new IntegritySignalStore();

      store.setEnabled(false);

      expect(store.enabled()).toBe(false);
    });

    it('should set enabled back to true', () => {
      store = new IntegritySignalStore();
      store.setEnabled(false);

      store.setEnabled(true);

      expect(store.enabled()).toBe(true);
    });
  });

  describe('setSilentMode', () => {
    it('should set silentMode to true', () => {
      store = new IntegritySignalStore();

      store.setSilentMode(true);

      expect(store.silentMode()).toBe(true);
    });

    it('should toggle silentMode', () => {
      store = new IntegritySignalStore();
      store.setSilentMode(true);

      store.setSilentMode(false);

      expect(store.silentMode()).toBe(false);
    });
  });

  describe('sessionStorage recovery', () => {
    it('should load signals from sessionStorage on initialization', () => {
      const storedSignals = [{ type: 'debug', message: 'stored' }];
      sessionStorage.setItem('integrity.signals', JSON.stringify(storedSignals));

      const newStore = new IntegritySignalStore();

      expect(newStore.signals()).toEqual(storedSignals);
    });

    it('should handle corrupted sessionStorage data gracefully', () => {
      sessionStorage.setItem('integrity.signals', 'invalid-json');

      const newStore = new IntegritySignalStore();

      expect(newStore.signals()).toEqual([]);
    });
  });
});
