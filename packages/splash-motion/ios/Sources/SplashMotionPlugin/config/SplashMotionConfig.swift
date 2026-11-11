import Foundation
import Capacitor

/**
 * Plugin configuration container for the SplashMotion plugin.
 *
 * This struct is responsible for reading and exposing static configuration
 * values defined under the `SplashMotion` key in `capacitor.config.ts`.
 *
 * Holds the full official `@capacitor/splash-screen` static configuration
 * plus the SplashMotion extensions (Lottie, haptics, image layers,
 * entrance animation, gradient background).
 *
 * Architectural rules:
 * - Read once during plugin initialization (`load()`).
 * - Treated as immutable runtime input.
 * - Consumed only by native code (never by JavaScript).
 */
public struct SplashMotionConfig {

    // MARK: - Configuration Keys

    /**
     * Internal structure to maintain consistent configuration keys.
     */
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let launchShowDuration = "launchShowDuration"
        static let launchAutoHide = "launchAutoHide"
        static let launchFadeOutDuration = "launchFadeOutDuration"
        static let backgroundColor = "backgroundColor"
        static let backgroundColorDark = "backgroundColorDark"
        static let showSpinner = "showSpinner"
        static let iosSpinnerStyle = "iosSpinnerStyle"
        static let spinnerColor = "spinnerColor"
        static let launchLottie = "launchLottie"
        static let launchHaptic = "launchHaptic"
        static let imageResourceName = "imageResourceName"
        static let imageResourceNameDark = "imageResourceNameDark"
        static let imageGravity = "imageGravity"
        static let androidSplashResourceName = "androidSplashResourceName"
        static let androidScaleType = "androidScaleType"
        static let backgroundImage = "backgroundImage"
        static let brandingImage = "brandingImage"
        static let entrance = "entrance"
        static let backgroundGradient = "backgroundGradient"
    }

    // MARK: - Public Properties

    /**
     * Enables verbose native logging via SplashMotionLogger.
     *
     * When enabled, additional debug information is printed to the Xcode console.
     * Default: false
     */
    public let verboseLogging: Bool

    /**
     * How long to show the launch splash when autoHide is enabled (in ms).
     * Default: 500
     */
    public let launchShowDuration: Int

    /**
     * Whether to auto hide the splash after launchShowDuration.
     * Default: true
     */
    public let launchAutoHide: Bool

    /**
     * Duration for the fade out animation of the launch splash (in ms).
     * Default: 200
     */
    public let launchFadeOutDuration: Int

    /**
     * Background color of the splash surface in hex format (#RRGGBB or #RRGGBBAA).
     * Default: nil (falls back to the system background)
     */
    public let backgroundColor: String?

    /**
     * Background color of the splash surface in dark mode, in hex format.
     * When the system is in dark mode and this is set, it overrides
     * backgroundColor. Default: nil
     */
    public let backgroundColorDark: String?

    /**
     * Whether a loading spinner should be shown. Default: false
     */
    public let showSpinner: Bool

    /**
     * Style of the iOS spinner ("large" or "small"; other values fall back
     * to the platform default, mirroring the official plugin mapping).
     * Default: nil (platform default style)
     */
    public let iosSpinnerStyle: String?

    /**
     * Color of the spinner in hex format.
     * Default: nil
     */
    public let spinnerColor: String?

    /**
     * Lottie animation shown during launch, when configured.
     * Default: nil
     */
    public let launchLottie: LottieOptions?

    /**
     * Haptic feedback fired when the launch splash appears.
     * Default: nil
     */
    public let launchHaptic: HapticOptions?

    /**
     * Name of the resource used as the static splash image, resolved in
     * order: `imageResourceName`, official `androidSplashResourceName`,
     * "splash".
     */
    public let imageResourceName: String

    /**
     * Whether a static launch image was explicitly configured
     * (`imageResourceName` / `androidSplashResourceName` keys present in
     * the plugin config). Drives the load-time guard rail: when
     * `launchLottie` is also configured it takes precedence and the static
     * image is ignored, so the conflicting configuration is reported.
     */
    public let launchImageExplicitlyConfigured: Bool

    /**
     * Name of the resource used as the static splash image in dark mode.
     * When nil, the light resource is used for both modes.
     */
    public let imageResourceNameDark: String?

    /**
     * Raw splash image gravity name (validated by SplashMotionValidators).
     * When nil, the image view fills the surface (official behavior).
     */
    public let imageGravity: String?

    /**
     * Raw legacy scale type name applied to the static splash image
     * (official `androidScaleType` key).
     */
    public let androidScaleType: String?

    /**
     * Background image layer rendered behind the splash content.
     * Default: nil
     */
    public let backgroundImage: ImageOptions?

    /**
     * Small branding image anchored to an edge of the splash surface.
     * Default: nil
     */
    public let brandingImage: ImageOptions?

    /**
     * Entrance animation applied to the splash content when it appears.
     * Default: nil
     */
    public let entrance: EntranceOptions?

    /**
     * Vertical gradient background of the splash surface.
     * Default: nil
     */
    public let backgroundGradient: BackgroundGradientOptions?

    // MARK: - Private Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultLaunchShowDuration: Int = 500
    private static let defaultLaunchAutoHide: Bool = true
    private static let defaultLaunchFadeOutDuration: Int = 200

    // MARK: - Initialization

    /**
     * Initializes the configuration by reading values from the Capacitor bridge.
     *
     * - Parameter plugin: The CAPPlugin instance used to access typed configuration via `getConfig()`.
     */
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Extract values with fallback to defaults
        self.verboseLogging = config.getBoolean(
            Keys.verboseLogging,
            Self.defaultVerboseLogging
        )
        self.launchShowDuration = config.getInt(
            Keys.launchShowDuration,
            Self.defaultLaunchShowDuration
        )
        self.launchAutoHide = config.getBoolean(
            Keys.launchAutoHide,
            Self.defaultLaunchAutoHide
        )
        self.launchFadeOutDuration = config.getInt(
            Keys.launchFadeOutDuration,
            Self.defaultLaunchFadeOutDuration
        )
        self.backgroundColor = config.getString(Keys.backgroundColor)
        self.backgroundColorDark = config.getString(Keys.backgroundColorDark)
        self.showSpinner = config.getBoolean(Keys.showSpinner, false)
        self.iosSpinnerStyle = config.getString(Keys.iosSpinnerStyle)
        self.spinnerColor = config.getString(Keys.spinnerColor)

        if let lottieConfig = config.getObject(Keys.launchLottie), let source = lottieConfig["source"] as? String {
            self.launchLottie = LottieOptions(
                source: source,
                darkSource: lottieConfig["darkSource"] as? String,
                loop: lottieConfig["loop"] as? Bool,
                speed: lottieConfig["speed"] as? Double
            )
        } else {
            self.launchLottie = nil
        }

        if let hapticConfig = config.getObject(Keys.launchHaptic) {
            self.launchHaptic = HapticOptions(style: hapticConfig["style"] as? String)
        } else {
            self.launchHaptic = nil
        }

        self.imageResourceName = config.getString(Keys.imageResourceName)
            ?? config.getString(Keys.androidSplashResourceName)
            // iOS default mirrors the official plugin: the "Splash" asset
            // (asset-catalog lookup, not a bundle file), while Android keeps
            // its "splash" drawable name on its own side.
            ?? "Splash"
        // Explicit (non-default) static launch image configuration, used by
        // the load-time guard rail: when launchLottie is also configured it
        // takes precedence and the static image is ignored.
        self.launchImageExplicitlyConfigured = config.getString(Keys.imageResourceName) != nil
            || config.getString(Keys.androidSplashResourceName) != nil
        self.imageResourceNameDark = config.getString(Keys.imageResourceNameDark)
        self.imageGravity = config.getString(Keys.imageGravity)
        self.androidScaleType = config.getString(Keys.androidScaleType)
        self.backgroundImage = ImageOptions.parse(config.getObject(Keys.backgroundImage))
        self.brandingImage = ImageOptions.parse(config.getObject(Keys.brandingImage))
        self.entrance = EntranceOptions.parse(config.getObject(Keys.entrance))
        self.backgroundGradient = BackgroundGradientOptions.parse(config.getObject(Keys.backgroundGradient))
    }
}

/**
 * Lottie animation options.
 *
 * Mirrors the TypeScript `LottieOptions` interface.
 */
public struct LottieOptions {
    /// Path to the Lottie JSON file (.json) or dotLottie archive (.lottie):
    /// local asset or remote HTTPS URL.
    public let source: String
    /// Alternative Lottie resource rendered when the system is in dark mode.
    /// Same accepted forms as source. When nil, source is used for both modes.
    public let darkSource: String?
    /// Whether the animation loops while the splash is visible. Default: true
    public let loop: Bool?
    /// Playback speed multiplier. Default: 1
    public let speed: Double?

    init(source: String, darkSource: String?, loop: Bool?, speed: Double?) {
        self.source = source
        self.darkSource = darkSource
        self.loop = loop
        self.speed = speed
    }
}

/**
 * Haptic feedback options fired on splash visibility transitions.
 *
 * Mirrors the TypeScript `HapticOptions` interface.
 */
public struct HapticOptions {
    /// Impact strength of the haptic feedback. Default: "medium"
    public let style: String?

    init(style: String?) {
        self.style = style
    }
}
