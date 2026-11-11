package io.capkit.splashmotion.config

import android.content.Context
import com.getcapacitor.Plugin
import io.capkit.splashmotion.model.BackgroundGradientOptionsData
import io.capkit.splashmotion.model.EntranceOptionsData
import io.capkit.splashmotion.model.HapticOptionsData
import io.capkit.splashmotion.model.ImageOptionsData
import io.capkit.splashmotion.model.LottieOptionsData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 *
 * Holds the full official `@capacitor/splash-screen` static configuration
 * plus the SplashMotion extensions (`launchLottie`, `launchHaptic`).
 */
@Serializable
private data class SplashMotionConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("launchShowDuration")
  val launchShowDuration: Long = 500,
  @SerialName("launchAutoHide")
  val launchAutoHide: Boolean = true,
  @SerialName("launchFadeOutDuration")
  val launchFadeOutDuration: Long = 200,
  @SerialName("backgroundColor")
  val backgroundColor: String? = null,
  @SerialName("backgroundColorDark")
  val backgroundColorDark: String? = null,
  @SerialName("androidSplashResourceName")
  val androidSplashResourceName: String? = null,
  @SerialName("androidScaleType")
  val androidScaleType: String? = null,
  @SerialName("showSpinner")
  val showSpinner: Boolean = false,
  @SerialName("androidSpinnerStyle")
  val androidSpinnerStyle: String? = null,
  @SerialName("iosSpinnerStyle")
  val iosSpinnerStyle: String? = null,
  @SerialName("spinnerColor")
  val spinnerColor: String? = null,
  @SerialName("splashFullScreen")
  val splashFullScreen: Boolean = false,
  @SerialName("splashImmersive")
  val splashImmersive: Boolean = false,
  @SerialName("layoutName")
  val layoutName: String? = null,
  @SerialName("useDialog")
  val useDialog: Boolean = false,
  @SerialName("launchLottie")
  val launchLottie: LottieOptionsData? = null,
  @SerialName("launchHaptic")
  val launchHaptic: HapticOptionsData? = null,
  @SerialName("imageResourceName")
  val imageResourceName: String? = null,
  @SerialName("imageResourceNameDark")
  val imageResourceNameDark: String? = null,
  @SerialName("imageGravity")
  val imageGravity: String? = null,
  @SerialName("backgroundImage")
  val backgroundImage: ImageOptionsData? = null,
  @SerialName("brandingImage")
  val brandingImage: ImageOptionsData? = null,
  @SerialName("entrance")
  val entrance: EntranceOptionsData? = null,
  @SerialName("backgroundGradient")
  val backgroundGradient: BackgroundGradientOptionsData? = null,
)

/**
 * Plugin configuration holder for the SplashMotion plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.SplashMotion` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization in the load() phase.
 * - Configuration values are read-only at runtime.
 * - Consumed only by native code.
 */
class SplashMotionConfig(
  plugin: Plugin,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Android application context.
   * Accessible for native implementation components that require system services.
   */
  val context: Context = plugin.context

  /**
   * Enables verbose native logging via SplashMotionLogger.
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * How long to show the launch splash when autoHide is enabled (in ms).
   * Default: 500
   */
  val launchShowDuration: Long

  /**
   * Whether to auto hide the splash after launchShowDuration.
   * Default: true
   */
  val launchAutoHide: Boolean

  /**
   * Duration for the fade out animation of the launch splash (in ms).
   * Default: 200
   */
  val launchFadeOutDuration: Long

  /**
   * Background color of the splash surface in hex format (#RRGGBB or #RRGGBBAA).
   * Default: null (falls back to the theme window background)
   */
  val backgroundColor: String?

  /**
   * Background color of the splash surface in dark mode, in hex format.
   * When the system is in dark mode and this is set, it overrides
   * backgroundColor. Default: null
   */
  val backgroundColorDark: String?

  /**
   * Name of the drawable resource used as the static splash image.
   * Default: "splash"
   */
  val androidSplashResourceName: String

  /**
   * ImageView.ScaleType name used to scale the static splash image.
   * Default: "FIT_XY"
   */
  val androidScaleType: String

  /**
   * Whether a loading spinner should be shown.
   * Default: false
   */
  val showSpinner: Boolean

  /**
   * Style name of the Android loading spinner (validated by
   * SplashMotionValidators).
   * Default: null (falls back to the official default style, "large")
   */
  val androidSpinnerStyle: String?

  /**
   * Color of the loading spinner in hex format (#RRGGBB or #RRGGBBAA).
   * Default: null (platform default spinner color)
   */
  val spinnerColor: String?

  /**
   * Hide the status bar on the splash surface.
   * Default: false
   */
  val splashFullScreen: Boolean

  /**
   * Hide the status bar and software navigation buttons on the splash surface.
   * Default: false
   */
  val splashImmersive: Boolean

  /**
   * Lottie animation shown during launch, when configured.
   * Default: null
   */
  val launchLottie: LottieOptionsData?

  /**
   * Haptic feedback fired when the launch splash appears.
   * Default: null
   */
  val launchHaptic: HapticOptionsData?

  /**
   * Name of the resource used as the static splash image, resolved in
   * order: `imageResourceName`, official `androidSplashResourceName`,
   * "splash".
   */
  val imageResourceName: String

  /**
   * Whether a static launch image was explicitly configured
   * (`imageResourceName` / `androidSplashResourceName` keys present in
   * the plugin config). Drives the load-time guard rail: when
   * `launchLottie` is also configured it takes precedence and the static
   * image is ignored, so the conflicting configuration is reported.
   */
  val launchImageExplicitlyConfigured: Boolean

  /**
   * Name of the resource used as the static splash image in dark mode.
   * When null, the light resource is used for both modes.
   */
  val imageResourceNameDark: String?

  /**
   * Raw splash image gravity name (validated by SplashMotionValidators).
   * When null, the image view fills the surface (official behavior).
   */
  val imageGravity: String?

  /**
   * Background image layer rendered behind the splash content.
   * Default: null
   */
  val backgroundImage: ImageOptionsData?

  /**
   * Small branding image anchored to an edge of the splash surface.
   * Default: null
   */
  val brandingImage: ImageOptionsData?

  /**
   * Entrance animation applied to the splash content when it appears.
   * Default: null
   */
  val entrance: EntranceOptionsData?

  /**
   * Vertical gradient background of the splash surface.
   * Default: null
   */
  val backgroundGradient: BackgroundGradientOptionsData?

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val parsedConfig = parseConfig(plugin)

    verboseLogging = parsedConfig.verboseLogging
    launchShowDuration = parsedConfig.launchShowDuration
    launchAutoHide = parsedConfig.launchAutoHide
    launchFadeOutDuration = parsedConfig.launchFadeOutDuration
    backgroundColor = parsedConfig.backgroundColor
    backgroundColorDark = parsedConfig.backgroundColorDark
    androidSplashResourceName = parsedConfig.androidSplashResourceName ?: "splash"
    androidScaleType = parsedConfig.androidScaleType ?: "FIT_XY"
    showSpinner = parsedConfig.showSpinner
    androidSpinnerStyle = parsedConfig.androidSpinnerStyle
    spinnerColor = parsedConfig.spinnerColor
    splashFullScreen = parsedConfig.splashFullScreen
    splashImmersive = parsedConfig.splashImmersive
    launchLottie = parsedConfig.launchLottie
    launchHaptic = parsedConfig.launchHaptic
    imageResourceName =
      parsedConfig.imageResourceName
        ?: parsedConfig.androidSplashResourceName
        ?: "splash"
    // Explicit (non-default) static launch image configuration, used by
    // the load-time guard rail: when launchLottie is also configured it
    // takes precedence and the static image is ignored.
    launchImageExplicitlyConfigured =
      parsedConfig.imageResourceName != null ||
      parsedConfig.androidSplashResourceName != null
    imageResourceNameDark = parsedConfig.imageResourceNameDark
    imageGravity = parsedConfig.imageGravity
    backgroundImage = parsedConfig.backgroundImage
    brandingImage = parsedConfig.brandingImage
    entrance = parsedConfig.entrance
    backgroundGradient = parsedConfig.backgroundGradient
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(plugin: Plugin): SplashMotionConfigData =
      try {
        val configJsonObject = plugin.config.getConfigJSON()
        if (configJsonObject != null) {
          json.decodeFromString<SplashMotionConfigData>(configJsonObject.toString())
        } else {
          SplashMotionConfigData()
        }
      } catch (_: Exception) {
        SplashMotionConfigData()
      }
  }
}
