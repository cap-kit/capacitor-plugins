import { WebPlugin } from '@capacitor/core';

import { HideOptions, LottieOptions, PluginVersionResult, ShowOptions, SplashMotionPlugin } from './definitions.js';
import { PLUGIN_VERSION } from './version.js';

/**
 * Web-only configuration for the animated splash overlay.
 *
 * The launch-related keys mirror the native static config
 * (`plugins.SplashMotion` in capacitor.config.ts) so both platforms can be
 * fed from the same values. `wasmUrl` is web-only: the dotLottie WebAssembly
 * player is NOT fetched from a public CDN by default, so apps must serve
 * `dotlottie-player.wasm` (shipped inside this package) as a local asset and
 * point `wasmUrl` at it — required for offline/air-gapped deployments.
 */
export interface SplashMotionWebConfig {
  /**
   * Lottie animation rendered by the web launch splash. Mirrors the native
   * `launchLottie` static config key; `source`/`darkSource` accept local
   * asset paths or HTTPS URLs (`.json` and `.lottie`).
   */
  launchLottie?: LottieOptions;

  /**
   * Splash background color in light mode (`#RRGGBB` / `#RRGGBBAA`).
   * Mirrors the native `backgroundColor` static config key.
   */
  backgroundColor?: string;

  /**
   * Splash background color in dark mode. Mirrors the native
   * `backgroundColorDark` static config key; falls back to
   * `backgroundColor`.
   */
  backgroundColorDark?: string;

  /**
   * Whether the launch splash auto-hides after `launchShowDuration`.
   * Mirrors the native `launchAutoHide` static config key.
   *
   * @default true
   */
  launchAutoHide?: boolean;

  /**
   * With `launchAutoHide: true`: how long the launch splash stays visible
   * (ms) when no explicit `showDuration` is passed to `show()`.
   * With `launchAutoHide: false`: minimum time (ms) the launch splash stays
   * visible even if `hide()` is called earlier. Mirrors the native
   * `launchShowDuration` static config key.
   */
  launchShowDuration?: number;

  /**
   * URL of the dotLottie WebAssembly player (`dotlottie-player.wasm`).
   * Web-only. When set, `DotLottie.setWasmUrl()` is called with it before
   * any player is constructed. When unset, the player falls back to the
   * library's public CDN default — avoid in offline deployments.
   */
  wasmUrl?: string;
}

/**
 * Minimal structural typing of `@lottiefiles/dotlottie-web`, kept loose so
 * this plugin does not couple its compiled output to the player's internal
 * option types. The module is loaded lazily (dynamic import) only when a
 * Lottie source is actually configured.
 */
interface DotLottieInstance {
  destroy(): void;
  /**
   * Total animation duration in seconds. Only meaningful once the player
   * has loaded the source; absent on mocked/older players.
   */
  duration?: number;
  /** Loose listener surface: the real player emits `load` when ready. */
  addEventListener?(type: string, listener: () => void): void;
}

interface DotLottieModule {
  DotLottie: {
    setWasmUrl(url: string): void;
    new (options: {
      canvas: HTMLCanvasElement;
      src?: string;
      autoplay?: boolean;
      loop?: boolean;
      speed?: number;
    }): DotLottieInstance;
  };
}

const OVERLAY_ATTRIBUTE = 'data-splash-motion';
const OVERLAY_VALUE = 'overlay';
const MAX_Z_INDEX = '2147483647';

let webConfig: SplashMotionWebConfig = {};
let overlayEl: HTMLElement | null = null;
let player: DotLottieInstance | null = null;
let shownAt = 0;
let minHoldMs = 0;
/**
 * Duration (ms) of the currently playing launch lottie, captured from the
 * player once the source loads. When the min-hold rule is active
 * (`launchAutoHide: false`), the effective hold is clamped to at least this
 * duration so the splash is never released mid-animation — even when the
 * configured `launchShowDuration` is shorter than the animation.
 */
let lottieDurationMs: number | null = null;
let pendingFadeOutMs = 200;
let autoHideTimer: ReturnType<typeof setTimeout> | undefined;
let generation = 0;

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function prefersDarkMode(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/**
 * Starts the CSS fade-in: the element must first be painted with opacity 0,
 * then transition to 1 on a later frame. Uses double requestAnimationFrame
 * when available, with a double setTimeout(0) fallback.
 */
function startFadeIn(el: HTMLElement, durationMs: number): void {
  el.style.transition = `opacity ${durationMs}ms ease-out`;
  el.style.opacity = '0';
  const frame = (cb: () => void): void => {
    if (typeof window.requestAnimationFrame === 'function') {
      window.requestAnimationFrame(() => cb());
    } else {
      setTimeout(cb, 0);
    }
  };
  frame(() => {
    frame(() => {
      el.style.opacity = '1';
    });
  });
}

function teardown(): void {
  if (autoHideTimer !== undefined) {
    clearTimeout(autoHideTimer);
    autoHideTimer = undefined;
  }
  lottieDurationMs = null;
  if (player !== null) {
    try {
      player.destroy();
    } catch {
      // Destroying an already-broken player must never throw.
    }
    player = null;
  }
  if (overlayEl !== null) {
    overlayEl.remove();
    overlayEl = null;
  }
  generation += 1;
}

/**
 * Web implementation of the SplashMotion plugin.
 *
 * The web platform has no native splash surface, so this implementation
 * renders a full-viewport DOM overlay on top of the page: background color
 * resolved against `prefers-color-scheme`, plus an optional Lottie animation
 * played by `@lottiefiles/dotlottie-web` (lazy-loaded, WASM-based, plays both
 * `.json` and `.lottie` sources).
 *
 * Native-only features are ignored on web: static images, background image /
 * gradient, entrance animation, spinner and haptics. The overlay also cannot
 * cover the pre-bundle launch gap (before the app JS executes) — that gap
 * requires a static splash in the host app's `index.html`.
 */
export class SplashMotionWeb extends WebPlugin implements SplashMotionPlugin {
  /**
   * Sets the web launch defaults. Call once at app startup (before the first
   * `show()`) with the same values used in the native static config.
   */
  public static setConfig(config: SplashMotionWebConfig): void {
    webConfig = { ...config };
  }

  public async show(options?: ShowOptions): Promise<void> {
    // A new show replaces any visible splash synchronously (no fade).
    teardown();

    const lottie = options?.lottie ?? webConfig.launchLottie;
    const fadeInMs = options?.fadeInDuration ?? 200;
    const fadeOutMs = options?.fadeOutDuration ?? 200;
    const autoHide = options?.autoHide ?? webConfig.launchAutoHide ?? true;
    const showDurationMs =
      options?.showDuration ?? (options?.autoHide === undefined ? (webConfig.launchShowDuration ?? 3000) : 3000);
    minHoldMs = autoHide ? 0 : (webConfig.launchShowDuration ?? 0);
    pendingFadeOutMs = fadeOutMs;

    const dark = prefersDarkMode();
    const overlay = document.createElement('div');
    overlay.setAttribute(OVERLAY_ATTRIBUTE, OVERLAY_VALUE);
    overlay.setAttribute('aria-hidden', 'true');
    overlay.style.cssText = [
      'position:fixed',
      'inset:0',
      `z-index:${MAX_Z_INDEX}`,
      'display:flex',
      'align-items:center',
      'justify-content:center',
      `background-color:${dark ? (webConfig.backgroundColorDark ?? webConfig.backgroundColor) : webConfig.backgroundColor}`,
    ].join(';');

    const canvas = document.createElement('canvas');
    canvas.style.cssText = ['width:100%', 'height:100%', 'display:block'].join(';');
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    overlay.appendChild(canvas);
    document.body.appendChild(overlay);
    overlayEl = overlay;

    const currentGeneration = ++generation;
    shownAt = Date.now();
    startFadeIn(overlay, fadeInMs);

    if (lottie !== undefined) {
      void this.startPlayer(canvas, lottie, dark, currentGeneration);
    }
    if (autoHide) {
      autoHideTimer = setTimeout(() => {
        autoHideTimer = undefined;
        void this.hide();
      }, showDurationMs);
    }
  }

  public async hide(options?: HideOptions): Promise<void> {
    if (overlayEl === null) {
      return;
    }
    if (autoHideTimer !== undefined) {
      clearTimeout(autoHideTimer);
      autoHideTimer = undefined;
    }

    const fadeOutMs = options?.fadeOutDuration ?? pendingFadeOutMs;
    const startGeneration = generation;
    // Launch min-hold clamp: when the minimum-hold rule is active, the
    // splash must also stay for the whole lottie duration (captured from
    // the player at load time) so it is never released mid-animation. The
    // clamp never shortens a hold and never applies to auto-hide mode.
    const effectiveMinHold = minHoldMs > 0 ? Math.max(minHoldMs, lottieDurationMs ?? 0) : 0;
    const remainingMs = Math.max(0, effectiveMinHold - (Date.now() - shownAt));

    if (remainingMs > 0) {
      await delay(remainingMs);
      if (generation !== startGeneration) {
        // A newer show() took ownership of the overlay; this hide is stale.
        return;
      }
    }

    const el = overlayEl;
    if (el === null) {
      return;
    }
    el.style.transition = `opacity ${fadeOutMs}ms ease-in`;
    el.style.opacity = '0';
    await delay(fadeOutMs);
    if (generation !== startGeneration) {
      return;
    }
    teardown();
  }

  /**
   * Returns the plugin version.
   *
   * On the Web, this value represents the JavaScript package version
   * rather than a native implementation.
   */
  public async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: PLUGIN_VERSION };
  }

  private async startPlayer(
    canvas: HTMLCanvasElement,
    lottie: LottieOptions,
    dark: boolean,
    startGeneration: number,
  ): Promise<void> {
    try {
      const { DotLottie } = (await import('@lottiefiles/dotlottie-web')) as unknown as DotLottieModule;
      if (webConfig.wasmUrl !== undefined) {
        DotLottie.setWasmUrl(webConfig.wasmUrl);
      }
      if (generation !== startGeneration) {
        // The splash was hidden or replaced while the module was loading.
        return;
      }
      const source = dark ? (lottie.darkSource ?? lottie.source) : lottie.source;
      player = new DotLottie({
        canvas,
        src: source,
        autoplay: true,
        loop: lottie.loop ?? true,
        speed: lottie.speed ?? 1,
      });
      // Capture the animation duration as soon as the source loads so the
      // launch min-hold can clamp to it (see hide()). Defensive listeners:
      // a player without the expected surface must never break the splash.
      const instance = player;
      if (typeof instance.addEventListener === 'function') {
        instance.addEventListener('load', () => {
          if (generation !== startGeneration || overlayEl === null) {
            return;
          }
          const seconds = instance.duration;
          if (typeof seconds === 'number' && seconds > 0) {
            lottieDurationMs = seconds * 1000;
          }
        });
      }
    } catch (error: unknown) {
      // The overlay (background color) stays visible; only the animation is
      // missing. Never let a player failure break the splash lifecycle.
      console.warn('[SplashMotion] web Lottie player failed to load:', error);
    }
  }
}
