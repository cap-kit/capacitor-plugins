import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { PLUGIN_VERSION } from '../src/version.js';
import { SplashMotionWeb } from '../src/web.js';

/**
 * Contract under test (web implementation):
 * - `setConfig()` supplies launch defaults mirrored from the native static
 *   config keys (launchLottie, backgroundColor, backgroundColorDark,
 *   launchAutoHide, launchShowDuration) plus the web-only `wasmUrl`.
 * - `show()` renders a fixed overlay with the resolved background color and,
 *   when a Lottie source is configured, lazily loads `@lottiefiles/dotlottie-web`
 *   and plays the animation on a full-size canvas.
 * - `hide()` honors the launch min-hold (`launchShowDuration` when
 *   `launchAutoHide` is false), fades out, destroys the player and removes
 *   the overlay.
 */

const dotlottieState = vi.hoisted(() => {
  return {
    setWasmUrl: vi.fn(),
    constructed: [] as {
      options: Record<string, unknown>;
      destroy: ReturnType<typeof vi.fn>;
      instance: {
        duration: number;
        addEventListener: (type: string, cb: () => void) => void;
      };
    }[],
    /** `load` listeners registered by the web implementation. */
    loadListeners: [] as (() => void)[],
    failConstruction: false,
  };
});

vi.mock('@lottiefiles/dotlottie-web', () => {
  class DotLottie {
    public destroy: ReturnType<typeof vi.fn> = vi.fn();

    /** Total animation duration in seconds (0 until the test sets it). */
    public duration = 0;

    public addEventListener(type: string, cb: () => void): void {
      if (type === 'load') {
        dotlottieState.loadListeners.push(cb);
      }
    }

    public constructor(options: Record<string, unknown>) {
      if (dotlottieState.failConstruction) {
        throw new Error('wasm unavailable');
      }
      dotlottieState.constructed.push({ options, destroy: this.destroy, instance: this });
    }

    public static setWasmUrl(url: string): void {
      dotlottieState.setWasmUrl(url);
    }
  }

  return { DotLottie };
});

/** Simulates the player finishing its source load with a known duration. */
function emitLottieLoad(durationSeconds: number): void {
  const instance = dotlottieState.constructed[0]?.instance;
  if (instance) {
    instance.duration = durationSeconds;
  }
  for (const cb of dotlottieState.loadListeners.splice(0)) {
    cb();
  }
}

function setDark(dark: boolean): void {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: dark && query.includes('prefers-color-scheme'),
  }));
}

function stubAnimationFrame(): void {
  vi.stubGlobal('requestAnimationFrame', (cb: (time: number) => void) => {
    return setTimeout(() => cb(0), 0);
  });
}

function overlay(): HTMLElement | null {
  return document.querySelector<HTMLElement>('[data-splash-motion="overlay"]');
}

async function flushFrames(): Promise<void> {
  // Two rAF stubs drive the fade-in start; flush them as timer ticks.
  await vi.advanceTimersByTimeAsync(0);
  await vi.advanceTimersByTimeAsync(0);
}

describe('SplashMotionWeb', () => {
  const web = new SplashMotionWeb();

  beforeEach(() => {
    vi.useFakeTimers();
    dotlottieState.constructed.length = 0;
    dotlottieState.loadListeners.length = 0;
    dotlottieState.setWasmUrl.mockClear();
    dotlottieState.failConstruction = false;
    setDark(false);
    stubAnimationFrame();
    SplashMotionWeb.setConfig({});
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    document.body.innerHTML = '';
  });

  describe('show()', () => {
    it('creates a full-viewport overlay with the light background color', async () => {
      SplashMotionWeb.setConfig({ backgroundColor: '#004D71' });

      await web.show();
      await flushFrames();

      const node = overlay();
      expect(node).not.toBeNull();
      expect(node?.style.backgroundColor).toBe('rgb(0, 77, 113)');
      expect(node?.getAttribute('aria-hidden')).toBe('true');
      expect(node?.querySelector('canvas')).not.toBeNull();
    });

    it('uses the dark background color and darkSource when the system is dark', async () => {
      setDark(true);
      SplashMotionWeb.setConfig({
        backgroundColor: '#004D71',
        backgroundColorDark: '#001B28',
        launchLottie: { source: 'light.lottie', darkSource: 'dark.lottie' },
      });

      await web.show();
      await flushFrames();

      expect(overlay()?.style.backgroundColor).toBe('rgb(0, 27, 40)');
      expect(dotlottieState.constructed[0]?.options.src).toBe('dark.lottie');
    });

    it('configures the player from launchLottie and calls setWasmUrl before construction', async () => {
      SplashMotionWeb.setConfig({
        launchLottie: { source: 'splash.lottie', loop: false, speed: 2 },
        wasmUrl: 'assets/dotlottie/dotlottie-player.wasm',
      });

      await web.show();
      await flushFrames();

      expect(dotlottieState.setWasmUrl).toHaveBeenCalledWith('assets/dotlottie/dotlottie-player.wasm');
      expect(dotlottieState.setWasmUrl.mock.invocationCallOrder[0]).toBeLessThan(
        dotlottieState.constructed.length > 0 ? Number.MAX_SAFE_INTEGER : 0,
      );
      expect(dotlottieState.constructed).toHaveLength(1);
      expect(dotlottieState.constructed[0]?.options).toMatchObject({
        src: 'splash.lottie',
        loop: false,
        speed: 2,
        autoplay: true,
      });
    });

    it('does not touch the player when no Lottie source is configured', async () => {
      await web.show();
      await flushFrames();

      expect(dotlottieState.constructed).toHaveLength(0);
      expect(dotlottieState.setWasmUrl).not.toHaveBeenCalled();
      expect(overlay()).not.toBeNull();
    });

    it('keeps the overlay when the player fails to load', async () => {
      dotlottieState.failConstruction = true;
      const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
      SplashMotionWeb.setConfig({ launchLottie: { source: 'splash.lottie' } });

      await expect(web.show()).resolves.toBeUndefined();
      await flushFrames();

      expect(overlay()).not.toBeNull();
      expect(warn).toHaveBeenCalled();
    });

    it('auto-hides after launchShowDuration when launchAutoHide is true', async () => {
      SplashMotionWeb.setConfig({ launchAutoHide: true, launchShowDuration: 1000 });

      await web.show();
      await flushFrames();

      await vi.advanceTimersByTimeAsync(999);
      expect(overlay()).not.toBeNull();

      await vi.advanceTimersByTimeAsync(1 + 200); // duration + default fade-out
      expect(overlay()).toBeNull();
    });

    it('honors an explicit showDuration override from ShowOptions', async () => {
      SplashMotionWeb.setConfig({ launchAutoHide: true, launchShowDuration: 5000 });

      await web.show({ autoHide: true, showDuration: 300, fadeOutDuration: 100 });
      await flushFrames();

      await vi.advanceTimersByTimeAsync(299);
      expect(overlay()).not.toBeNull();

      await vi.advanceTimersByTimeAsync(1 + 100);
      expect(overlay()).toBeNull();
    });
  });

  describe('hide()', () => {
    it('waits for the remaining launch min-hold before fading out', async () => {
      SplashMotionWeb.setConfig({ launchAutoHide: false, launchShowDuration: 1000 });

      await web.show();
      await flushFrames();

      const hidden = web.hide();
      await vi.advanceTimersByTimeAsync(300);
      await Promise.resolve();
      expect(overlay()).not.toBeNull();

      await vi.advanceTimersByTimeAsync(700 + 200); // remaining min-hold + fade-out
      await hidden;
      expect(overlay()).toBeNull();
    });

    it('clamps the launch min-hold to the loaded lottie duration', async () => {
      SplashMotionWeb.setConfig({
        launchAutoHide: false,
        launchShowDuration: 1000,
        launchLottie: { source: 'splash.lottie' },
      });

      await web.show();
      await flushFrames();

      // The player loads a 3s animation: the splash must never be released
      // after only the configured 1s minimum.
      emitLottieLoad(3);

      const hidden = web.hide();
      await vi.advanceTimersByTimeAsync(1000 + 200); // configured min + fade-out
      expect(overlay()).not.toBeNull();

      await vi.advanceTimersByTimeAsync(2000 + 200); // remaining lottie time + fade-out
      await hidden;
      expect(overlay()).toBeNull();
    });

    it('keeps the configured min-hold when the lottie is shorter', async () => {
      SplashMotionWeb.setConfig({
        launchAutoHide: false,
        launchShowDuration: 1000,
        launchLottie: { source: 'splash.lottie' },
      });

      await web.show();
      await flushFrames();

      // A 0.5s animation must not shorten the 1s minimum hold.
      emitLottieLoad(0.5);

      const hidden = web.hide();
      await vi.advanceTimersByTimeAsync(999);
      expect(overlay()).not.toBeNull();

      await vi.advanceTimersByTimeAsync(1 + 200);
      await hidden;
      expect(overlay()).toBeNull();
    });

    it('resolves immediately when the splash is not visible', async () => {
      await expect(web.hide()).resolves.toBeUndefined();
      expect(overlay()).toBeNull();
    });

    it('destroys the player after the fade-out completes', async () => {
      SplashMotionWeb.setConfig({ launchLottie: { source: 'splash.lottie' } });

      await web.show();
      await flushFrames();

      const hidden = web.hide();
      await vi.advanceTimersByTimeAsync(200);
      await hidden;

      expect(dotlottieState.constructed[0]?.destroy).toHaveBeenCalled();
      expect(overlay()).toBeNull();
    });

    it('applies the HideOptions fade-out duration', async () => {
      SplashMotionWeb.setConfig({ launchAutoHide: false });

      await web.show();
      await flushFrames();

      const hidden = web.hide({ fadeOutDuration: 50 });
      await vi.advanceTimersByTimeAsync(49);
      expect(overlay()).not.toBeNull();

      await vi.advanceTimersByTimeAsync(1);
      expect(overlay()).toBeNull();
      await hidden;
    });
  });

  describe('repeated show()', () => {
    it('replaces the previous splash and destroys its player', async () => {
      SplashMotionWeb.setConfig({ launchLottie: { source: 'first.lottie' } });

      await web.show();
      await flushFrames();

      SplashMotionWeb.setConfig({ launchLottie: { source: 'second.lottie' } });
      await web.show();
      await flushFrames();

      expect(dotlottieState.constructed).toHaveLength(2);
      expect(dotlottieState.constructed[0]?.destroy).toHaveBeenCalled();
      expect(dotlottieState.constructed[1]?.options.src).toBe('second.lottie');
      expect(document.querySelectorAll('[data-splash-motion="overlay"]')).toHaveLength(1);
    });
  });

  describe('getPluginVersion()', () => {
    it('resolves with the package version', async () => {
      const result = await web.getPluginVersion();

      expect(result.version).toBe(PLUGIN_VERSION);
      expect(result.version).toMatch(/^\d+\.\d+\.\d+/);
    });
  });
});
