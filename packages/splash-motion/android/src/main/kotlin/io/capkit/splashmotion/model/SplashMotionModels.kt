package io.capkit.splashmotion.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lottie animation options.
 *
 * Mirrors the TypeScript `LottieOptions` interface.
 */
@Serializable
data class LottieOptionsData(
  /**
   * Path to the Lottie JSON file (.json) or dotLottie archive (.lottie):
   * local asset or remote HTTPS URL.
   */
  @SerialName("source")
  val source: String,
  /**
   * Alternative Lottie resource rendered when the system is in dark mode.
   * Same accepted forms as source. When null, source is used for both modes.
   */
  @SerialName("darkSource")
  val darkSource: String? = null,
  /**
   * Whether the animation loops while the splash is visible.
   * Default: true
   */
  @SerialName("loop")
  val loop: Boolean? = null,
  /**
   * Playback speed multiplier.
   * Default: 1
   */
  @SerialName("speed")
  val speed: Double? = null,
)

/**
 * Haptic feedback options fired on splash visibility transitions.
 *
 * Mirrors the TypeScript `HapticOptions` interface.
 */
@Serializable
data class HapticOptionsData(
  /**
   * Impact strength of the haptic feedback.
   * Default: 'medium'
   */
  @SerialName("style")
  val style: String? = null,
)

/**
 * Options describing a splash image layer (background layer, branding
 * image, or per-call static image).
 *
 * Mirrors the TypeScript `ImageOptions` interface.
 */
@Serializable
data class ImageOptionsData(
  /**
   * Image resource: a drawable resource name on Android, an asset catalog
   * or app bundle name on iOS. A value containing `/` is resolved as a
   * web asset path relative to the web asset root.
   */
  @SerialName("source")
  val source: String,
  /**
   * Alternative image rendered when the system is in dark mode. Same
   * accepted forms as source. When null, source is used for both modes.
   */
  @SerialName("darkSource")
  val darkSource: String? = null,
  /**
   * Placement of the image inside the splash surface.
   * Default: 'center'.
   */
  @SerialName("gravity")
  val gravity: String? = null,
  /**
   * Scaling applied to the bitmap inside its view.
   * Default: 'CENTER_CROP'.
   */
  @SerialName("scaleType")
  val scaleType: String? = null,
  /**
   * Opacity of the image, from 0 (transparent) to 1 (opaque).
   * Default: 1.
   */
  @SerialName("alpha")
  val alpha: Double? = null,
  /**
   * Margin between the image and the splash surface edge, in dp.
   * Only applies when the image is anchored to an edge.
   * Default: 16.
   */
  @SerialName("margin")
  val margin: Double? = null,
)

/**
 * Entrance animation applied to the splash content when it appears.
 *
 * Mirrors the TypeScript `EntranceOptions` interface.
 */
@Serializable
data class EntranceOptionsData(
  /**
   * Animation type applied to the whole splash content.
   * Default: 'none'.
   */
  @SerialName("type")
  val type: String? = null,
  /**
   * Duration of the entrance animation, in ms. Clamped to 0-2000.
   * Default: 300.
   */
  @SerialName("durationMs")
  val durationMs: Long? = null,
)

/**
 * Loading spinner rendered centered on top of the splash content.
 *
 * Mirrors the TypeScript `SpinnerOptions` interface.
 */
@Serializable
data class SpinnerOptionsData(
  /**
   * Whether the spinner is shown.
   * Default: false
   */
  @SerialName("show")
  val show: Boolean? = null,
  /**
   * Style of the native spinner.
   * Default: 'large'.
   */
  @SerialName("style")
  val style: String? = null,
  /**
   * Color of the spinner in hex format (#RRGGBB or #RRGGBBAA).
   * An invalid value keeps the platform default spinner color.
   */
  @SerialName("color")
  val color: String? = null,
)

/**
 * Vertical gradient background of the splash surface.
 *
 * Mirrors the TypeScript `BackgroundGradientOptions` interface.
 */
@Serializable
data class BackgroundGradientOptionsData(
  /**
   * Gradient color at the top of the splash surface (hex format).
   */
  @SerialName("top")
  val top: String,
  /**
   * Gradient color at the bottom of the splash surface (hex format).
   */
  @SerialName("bottom")
  val bottom: String,
  /**
   * Gradient color at the top of the splash surface in dark mode.
   */
  @SerialName("topDark")
  val topDark: String? = null,
  /**
   * Gradient color at the bottom of the splash surface in dark mode.
   */
  @SerialName("bottomDark")
  val bottomDark: String? = null,
)

/**
 * Options for showing the splash screen.
 *
 * Mirrors the TypeScript `ShowOptions` interface, extending the official
 * `@capacitor/splash-screen` shape with Lottie and haptic controls.
 */
@Serializable
data class ShowOptionsData(
  /**
   * Whether to auto hide the splash after showDuration.
   * Default: true
   */
  @SerialName("autoHide")
  val autoHide: Boolean? = null,
  /**
   * How long (in ms) to fade in.
   * Default: 200
   */
  @SerialName("fadeInDuration")
  val fadeInDuration: Long? = null,
  /**
   * How long (in ms) to fade out.
   * Default: 200
   */
  @SerialName("fadeOutDuration")
  val fadeOutDuration: Long? = null,
  /**
   * How long to show the splash screen when autoHide is enabled (in ms).
   * Default: 3000
   */
  @SerialName("showDuration")
  val showDuration: Long? = null,
  /**
   * Lottie animation to render while the splash is visible.
   */
  @SerialName("lottie")
  val lottie: LottieOptionsData? = null,
  /**
   * Static image to render while the splash is visible. Overrides the
   * static image configuration for this show call.
   */
  @SerialName("image")
  val image: ImageOptionsData? = null,
  /**
   * Background image layer rendered behind the splash content. Overrides
   * the static background image configuration for this show call.
   */
  @SerialName("backgroundImage")
  val backgroundImage: ImageOptionsData? = null,
  /**
   * Small branding image anchored to an edge of the splash surface.
   * Overrides the static branding image configuration for this show call.
   */
  @SerialName("brandingImage")
  val brandingImage: ImageOptionsData? = null,
  /**
   * Entrance animation applied to the splash content when it appears.
   * Overrides the static entrance configuration for this show call.
   */
  @SerialName("entrance")
  val entrance: EntranceOptionsData? = null,
  /**
   * Vertical gradient background of the splash surface. Overrides the
   * static gradient configuration for this show call.
   */
  @SerialName("backgroundGradient")
  val backgroundGradient: BackgroundGradientOptionsData? = null,
  /**
   * Loading spinner rendered on top of the splash content. Overrides the
   * static spinner configuration for this show call.
   */
  @SerialName("spinner")
  val spinner: SpinnerOptionsData? = null,
  /**
   * Haptic feedback fired when the splash appears.
   */
  @SerialName("haptic")
  val haptic: HapticOptionsData? = null,
)

/**
 * Options for hiding the splash screen.
 *
 * Mirrors the TypeScript `HideOptions` interface.
 */
@Serializable
data class HideOptionsData(
  /**
   * How long (in ms) to fade out.
   * Default: 200
   */
  @SerialName("fadeOutDuration")
  val fadeOutDuration: Long? = null,
  /**
   * Haptic feedback fired when the splash disappears.
   */
  @SerialName("haptic")
  val haptic: HapticOptionsData? = null,
)

/**
 * Result of the `getPluginVersion()` method.
 *
 * Mirrors the TypeScript `PluginVersionResult` interface:
 * - version: native plugin version synchronized from package.json
 */
@Serializable
data class SplashMotionVersionResult(
  /**
   * The native plugin version string.
   */
  @SerialName("version")
  val version: String,
)
