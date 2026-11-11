/// <reference types="@capacitor/cli" />

declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * These config values are available:
     */
    SplashMotion?: {
      /**
       * Enables verbose native logging.
       *
       * @since 8.0.0
       * @default false
       */
      verboseLogging?: boolean;

      /**
       * How long to show the launch splash screen when autoHide is enabled (in ms)
       *
       * When `launchAutoHide` is false this value acts as the MINIMUM hold,
       * clamped to `max(launchShowDuration, lottie duration)`: a loaded
       * launch lottie always plays to completion before the splash is
       * released, and a shorter animation never shortens the configured
       * minimum.
       *
       * @since 8.0.0
       * @default 500
       * @example 3000
       */
      launchShowDuration?: number;

      /**
       * Whether to auto hide the splash after launchShowDuration.
       *
       * @since 8.0.0
       * @default true
       * @example true
       */
      launchAutoHide?: boolean;

      /**
       * Duration for the fade out animation of the launch splash screen (in ms)
       *
       * Only available for Android, when using the Android 12 Splash Screen API.
       *
       * @since 8.0.0
       * @default 200
       * @example 3000
       */
      launchFadeOutDuration?: number;

      /**
       * Color of the background of the Splash Screen in hex format, #RRGGBB or #RRGGBBAA.
       * Doesn't work if `useDialog` is true or on launch when using the Android 12 API.
       *
       * @since 8.0.0
       * @example "#ffffffff"
       */
      backgroundColor?: string;

      /**
       * Color of the background of the Splash Screen in dark mode, in hex
       * format, #RRGGBB or #RRGGBBAA. When the system is in dark mode and
       * this key is set, it overrides `backgroundColor`.
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       * @example "#000000"
       */
      backgroundColorDark?: string;

      /**
       * Name of the resource to be used as Splash Screen.
       *
       * Doesn't work on launch when using the Android 12 API.
       *
       * Only available on Android.
       *
       * @since 8.0.0
       * @default splash
       * @example "splash"
       */
      androidSplashResourceName?: string;

      /**
       * The [ImageView.ScaleType](https://developer.android.com/reference/android/widget/ImageView.ScaleType) used to scale
       * the Splash Screen image.
       * Doesn't work if `useDialog` is true or on launch when using the Android 12 API.
       *
       * Only available on Android.
       *
       * @since 8.0.0
       * @default FIT_XY
       * @example "CENTER_CROP"
       */
      androidScaleType?:
        'CENTER' | 'CENTER_CROP' | 'CENTER_INSIDE' | 'FIT_CENTER' | 'FIT_END' | 'FIT_START' | 'FIT_XY' | 'MATRIX';

      /**
       * Show a loading spinner centered on the splash surface. The spinner
       * renders as the topmost content layer, so the entrance animation
       * covers it and it disappears with the splash on hide.
       *
       * @since 8.0.0
       * @default false
       * @example true
       */
      showSpinner?: boolean;

      /**
       * Style of the Android spinner. Unknown values fall back to `large`.
       *
       * Only available on Android.
       *
       * @since 8.0.0
       * @default large
       * @example "large"
       */
      androidSpinnerStyle?: 'horizontal' | 'small' | 'large' | 'inverse' | 'smallInverse' | 'largeInverse';

      /**
       * Style of the iOS spinner. Only `small` and `large` are
       * differentiated; any other value falls back to the platform default.
       *
       * Only available on iOS.
       *
       * @since 8.0.0
       * @default small
       * @example "small"
       */
      iosSpinnerStyle?: 'large' | 'small';

      /**
       * Color of the spinner in hex format, #RRGGBB or #RRGGBBAA.
       * An invalid value keeps the platform default spinner color.
       *
       * @since 8.0.0
       * @example "#999999"
       */
      spinnerColor?: string;

      /**
       * Hide the status bar on the Splash Screen.
       *
       * Doesn't work on launch when using the Android 12 API.
       *
       * Only available on Android.
       *
       * @since 8.0.0
       * @example true
       */
      splashFullScreen?: boolean;

      /**
       * Hide the status bar and the software navigation buttons on the Splash Screen.
       *
       * Doesn't work on launch when using the Android 12 API.
       *
       * Only available on Android.
       *
       * @since 8.0.0
       * @example true
       */
      splashImmersive?: boolean;

      /**
       * If `useDialog` is set to true, configure the Dialog layout.
       * If `useDialog` is not set or false, use a layout instead of the ImageView.
       *
       * Doesn't work on launch when using the Android 12 API.
       *
       * Only available on Android.
       *
       * @since 8.0.0
       * @example "launch_screen"
       */
      layoutName?: string;

      /**
       * Use a Dialog instead of an ImageView.
       * If `layoutName` is not configured, it will use
       * a layout that uses the splash image as background.
       *
       * Doesn't work on launch when using the Android 12 API.
       *
       * Only available on Android.
       *
       * @since 8.0.0
       * @example true
       */
      useDialog?: boolean;

      /**
       * Name of the resource used as the static splash image.
       *
       * On Android this is a drawable resource name; on iOS it resolves
       * from the asset catalog or the app bundle. When unset, the official
       * `androidSplashResourceName` key is honored (default: `splash`).
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       * @default splash
       * @example "splash"
       */
      imageResourceName?: string;

      /**
       * Name of the resource used as the static splash image in dark mode.
       * When the system is in dark mode and this key is set, it overrides
       * `imageResourceName`.
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       * @example "splash_dark"
       */
      imageResourceNameDark?: string;

      /**
       * Placement of the static splash image inside the splash surface.
       * When set, the image view is sized to its intrinsic bitmap size and
       * positioned by gravity; `androidScaleType` still scales the bitmap
       * inside that view. When unset, the image view fills the surface
       * (official behavior).
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       * @default center
       * @example "bottom"
       */
      imageGravity?: SplashGravity;

      /**
       * Background image layer rendered behind the splash content.
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       */
      backgroundImage?: ImageOptions;

      /**
       * Small branding image anchored to an edge of the splash surface
       * (co-branding logo, Android 12 style).
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       */
      brandingImage?: ImageOptions;

      /**
       * Entrance animation applied to the splash content when it appears.
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       */
      entrance?: EntranceOptions;

      /**
       * Vertical gradient background of the splash surface. When set, it
       * overrides `backgroundColor` / `backgroundColorDark`.
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       */
      backgroundGradient?: BackgroundGradientOptions;

      /**
       * Lottie animation shown on top of (or instead of) the static splash
       * image during launch.
       *
       * On Android and iOS it is played by the native Lottie runtimes. On
       * web, the launch overlay plays it via `@lottiefiles/dotlottie-web`
       * (lazy-loaded); the web launch defaults are supplied to
       * `SplashMotionWeb.setConfig()` because web cannot read the static
       * plugin config. The web player requires the app to serve
       * `dotlottie-player.wasm` as a local asset and pass its URL as
       * `wasmUrl` in the web config.
       *
       * @since 8.0.0
       */
      launchLottie?: LottieOptions;

      /**
       * Haptic feedback fired when the launch splash screen appears.
       *
       * Only available on Android and iOS.
       *
       * @since 8.0.0
       */
      launchHaptic?: HapticOptions;
    };
  }
}

/**
 * Options for the Lottie animation rendered by the splash screen.
 *
 * The `source` (and `darkSource`) accept:
 * - a path relative to the web asset root (e.g. `'public/lottie/splash.json'`)
 *   for `.json` Lottie files or `.lottie` dotLottie archives
 * - a full remote HTTPS URL (e.g. `'https://example.com/splash.lottie'`)
 *
 * When the system is in dark mode and `darkSource` is set, `darkSource`
 * is rendered instead of `source`.
 *
 * @since 8.0.0
 */
export interface LottieOptions {
  /**
   * Path to the Lottie JSON file (`.json`) or dotLottie archive
   * (`.lottie`): local asset or remote HTTPS URL.
   *
   * @since 8.0.0
   */
  source: string;

  /**
   * Alternative Lottie resource rendered when the system is in dark
   * mode. Same accepted forms as `source`. When unset, `source` is
   * used for both modes.
   *
   * @since 8.0.0
   */
  darkSource?: string;

  /**
   * Whether the animation loops while the splash is visible.
   *
   * @default true
   * @since 8.0.0
   */
  loop?: boolean;

  /**
   * Playback speed multiplier.
   *
   * @default 1
   * @since 8.0.0
   */
  speed?: number;
}

/**
 * Haptic feedback options fired on splash visibility transitions.
 *
 * @since 8.0.0
 */
export interface HapticOptions {
  /**
   * Impact strength of the haptic feedback.
   *
   * @default 'medium'
   * @since 8.0.0
   */
  style?: 'light' | 'medium' | 'heavy';
}

/**
 * Placement of a splash layer inside the splash surface.
 *
 * Nine named positions combining a vertical (top/center/bottom) and a
 * horizontal (start/center/end) anchor. `start`/`end` follow the reading
 * direction of the current layout direction (left in LTR layouts).
 *
 * @since 8.0.0
 */
export type SplashGravity =
  'topStart' | 'top' | 'topEnd' | 'start' | 'center' | 'end' | 'bottomStart' | 'bottom' | 'bottomEnd';

/**
 * Scaling applied to a splash image bitmap inside its view.
 *
 * Uses the official [ImageView.ScaleType](https://developer.android.com/reference/android/widget/ImageView.ScaleType)
 * names so the value is shared with the legacy `androidScaleType` key.
 * On iOS, `MATRIX` falls back to `CENTER`.
 *
 * @since 8.0.0
 */
export type SplashScaleType =
  'CENTER' | 'CENTER_CROP' | 'CENTER_INSIDE' | 'FIT_CENTER' | 'FIT_END' | 'FIT_START' | 'FIT_XY' | 'MATRIX';

/**
 * Options describing a splash image layer (background layer, branding
 * image, or per-call static image).
 *
 * @since 8.0.0
 */
export interface ImageOptions {
  /**
   * Image resource: a drawable resource name on Android, an asset catalog
   * or app bundle name on iOS. A value containing `/` is resolved as a web
   * asset path relative to the web asset root.
   *
   * @since 8.0.0
   */
  source: string;

  /**
   * Alternative image rendered when the system is in dark mode. Same
   * accepted forms as `source`. When unset, `source` is used for both
   * modes.
   *
   * @since 8.0.0
   */
  darkSource?: string;

  /**
   * Placement of the image inside the splash surface.
   *
   * @default center
   * @since 8.0.0
   */
  gravity?: SplashGravity;

  /**
   * Scaling applied to the bitmap inside its view.
   *
   * @default CENTER_CROP
   * @since 8.0.0
   */
  scaleType?: SplashScaleType;

  /**
   * Opacity of the image, from 0 (transparent) to 1 (opaque).
   *
   * @default 1
   * @since 8.0.0
   */
  alpha?: number;

  /**
   * Margin between the image and the splash surface edge, in dp (Android)
   * or points (iOS). Only applies when the image is anchored to an edge.
   *
   * @default 16
   * @since 8.0.0
   */
  margin?: number;
}

/**
 * Style of the native loading spinner rendered on the splash surface.
 *
 * Android supports all six values (official `ProgressBar` styles). iOS
 * only differentiates small and large: `small` maps to the medium
 * activity indicator style, `large` to the large style, and the inverse
 * values fall back to the platform default.
 *
 * @since 8.0.0
 */
export type SpinnerStyle = 'horizontal' | 'small' | 'large' | 'inverse' | 'smallInverse' | 'largeInverse';

/**
 * Loading spinner rendered centered on top of the splash content.
 *
 * @since 8.0.0
 */
export interface SpinnerOptions {
  /**
   * Whether the spinner is shown.
   *
   * @default false
   * @since 8.0.0
   */
  show?: boolean;

  /**
   * Style of the native spinner.
   *
   * @default large
   * @since 8.0.0
   */
  style?: SpinnerStyle;

  /**
   * Color of the spinner in hex format, #RRGGBB or #RRGGBBAA.
   * An invalid value keeps the platform default spinner color.
   *
   * @since 8.0.0
   */
  color?: string;
}

/**
 * Entrance animation applied to the splash content when it appears.
 *
 * @since 8.0.0
 */
export interface EntranceOptions {
  /**
   * Animation type applied to the whole content of the splash surface
   * (all layers together). The surface fade in/out is unchanged.
   *
   * @default none
   * @since 8.0.0
   */
  type?: 'none' | 'fade' | 'scale';

  /**
   * Duration of the entrance animation, in ms. Clamped to 0–2000.
   *
   * @default 300
   * @since 8.0.0
   */
  durationMs?: number;
}

/**
 * Vertical gradient background of the splash surface.
 *
 * Colors use hex format, #RRGGBB or #RRGGBBAA. When the system is in
 * dark mode, the dark variants win when set; otherwise the light colors
 * are used for both modes. When this option is set it overrides
 * `backgroundColor` / `backgroundColorDark`.
 *
 * @since 8.0.0
 */
export interface BackgroundGradientOptions {
  /**
   * Gradient color at the top of the splash surface.
   *
   * @since 8.0.0
   */
  top: string;

  /**
   * Gradient color at the bottom of the splash surface.
   *
   * @since 8.0.0
   */
  bottom: string;

  /**
   * Gradient color at the top of the splash surface in dark mode.
   *
   * @since 8.0.0
   */
  topDark?: string;

  /**
   * Gradient color at the bottom of the splash surface in dark mode.
   *
   * @since 8.0.0
   */
  bottomDark?: string;
}

/**
 * Options for showing the splash screen.
 *
 * Extends the official `@capacitor/splash-screen` `ShowOptions` with
 * Lottie and haptic feedback controls, keeping full API compatibility.
 *
 * @since 8.0.0
 */
export interface ShowOptions {
  /**
   * Whether to auto hide the splash after showDuration.
   *
   * @since 8.0.0
   */
  autoHide?: boolean;

  /**
   * How long (in ms) to fade in.
   *
   * @default 200
   * @since 8.0.0
   */
  fadeInDuration?: number;

  /**
   * How long (in ms) to fade out.
   *
   * @default 200
   * @since 8.0.0
   */
  fadeOutDuration?: number;

  /**
   * How long to show the splash screen when autoHide is enabled (in ms).
   *
   * @default 3000
   * @since 8.0.0
   */
  showDuration?: number;

  /**
   * Lottie animation to render while the splash is visible.
   * Overrides the static `launchLottie` configuration for this show call.
   *
   * Available on Android, iOS and web. On web, `darkSource` is resolved
   * against `prefers-color-scheme` and the WASM player URL comes from the
   * web config (`SplashMotionWeb.setConfig()`), not from this option.
   *
   * @since 8.0.0
   */
  lottie?: LottieOptions;

  /**
   * Static image to render while the splash is visible.
   * Overrides the static `imageResourceName` configuration for this
   * show call.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  image?: ImageOptions;

  /**
   * Background image layer rendered behind the splash content.
   * Overrides the static `backgroundImage` configuration for this show
   * call.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  backgroundImage?: ImageOptions;

  /**
   * Small branding image anchored to an edge of the splash surface.
   * Overrides the static `brandingImage` configuration for this show call.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  brandingImage?: ImageOptions;

  /**
   * Entrance animation applied to the splash content when it appears.
   * Overrides the static `entrance` configuration for this show call.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  entrance?: EntranceOptions;

  /**
   * Vertical gradient background of the splash surface.
   * Overrides the static `backgroundGradient` configuration for this show
   * call.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  backgroundGradient?: BackgroundGradientOptions;

  /**
   * Loading spinner rendered centered on top of the splash content.
   * Overrides the static `showSpinner` / `androidSpinnerStyle` /
   * `iosSpinnerStyle` / `spinnerColor` configuration for this show call.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  spinner?: SpinnerOptions;

  /**
   * Haptic feedback fired when the splash appears.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  haptic?: HapticOptions;
}

/**
 * Options for hiding the splash screen.
 *
 * @since 8.0.0
 */
export interface HideOptions {
  /**
   * How long (in ms) to fade out.
   *
   * On Android, if using the Android 12 Splash Screen API, it's not being used.
   * Use launchFadeOutDuration configuration option instead.
   *
   * @default 200
   * @since 8.0.0
   */
  fadeOutDuration?: number;

  /**
   * Haptic feedback fired when the splash disappears.
   *
   * Only available on Android and iOS.
   *
   * @since 8.0.0
   */
  haptic?: HapticOptions;
}

/**
 * Result object returned by the `getPluginVersion()` method.
 */
export interface PluginVersionResult {
  /** The native version string of the plugin. */
  version: string;
}

/**
 * Public JavaScript API for the SplashMotion Capacitor plugin.
 *
 * The method surface is API-compatible with the official
 * `@capacitor/splash-screen` plugin, so it works as a drop-in
 * replacement, with Lottie animation and haptic feedback extensions.
 */
export interface SplashMotionPlugin {
  /**
   * Show the splash screen.
   *
   * @since 8.0.0
   */
  show(options?: ShowOptions): Promise<void>;

  /**
   * Hide the splash screen.
   *
   * @since 8.0.0
   */
  hide(options?: HideOptions): Promise<void>;

  /**
   * Get the plugin version.
   *
   * @returns A promise resolving to the plugin version.
   *
   * @example
   * ```typescript
   * import { SplashMotion } from '@cap-kit/splash-motion';
   *
   * const { version } = await SplashMotion.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
