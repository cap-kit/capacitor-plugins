import Foundation

/**
 * Options for showing the splash screen.
 *
 * Mirrors the TypeScript `ShowOptions` interface, extending the official
 * `@capacitor/splash-screen` shape with Lottie and haptic controls.
 */
public struct ShowOptions {
    /// Whether to auto hide the splash after showDuration. Default: true
    public var autoHide: Bool?
    /// How long (in ms) to fade in. Default: 200
    public var fadeInDuration: Int?
    /// How long (in ms) to fade out. Default: 200
    public var fadeOutDuration: Int?
    /// How long to show the splash screen when autoHide is enabled (in ms). Default: 3000
    public var showDuration: Int?
    /// Lottie animation to render while the splash is visible.
    public var lottie: LottieOptions?
    /// Static image to render while the splash is visible. Overrides the
    /// static image configuration for this show call.
    public var image: ImageOptions?
    /// Background image layer rendered behind the splash content. Overrides
    /// the static background image configuration for this show call.
    public var backgroundImage: ImageOptions?
    /// Small branding image anchored to an edge of the splash surface.
    /// Overrides the static branding image configuration for this show call.
    public var brandingImage: ImageOptions?
    /// Entrance animation applied to the splash content when it appears.
    /// Overrides the static entrance configuration for this show call.
    public var entrance: EntranceOptions?
    /// Vertical gradient background of the splash surface. Overrides the
    /// static gradient configuration for this show call.
    public var backgroundGradient: BackgroundGradientOptions?
    /// Loading spinner rendered centered on top of the splash content.
    /// Overrides the static spinner configuration for this show call.
    public var spinner: SpinnerOptions?
    /// Haptic feedback fired when the splash appears.
    public var haptic: HapticOptions?

    public init(
        autoHide: Bool? = nil,
        fadeInDuration: Int? = nil,
        fadeOutDuration: Int? = nil,
        showDuration: Int? = nil,
        lottie: LottieOptions? = nil,
        image: ImageOptions? = nil,
        backgroundImage: ImageOptions? = nil,
        brandingImage: ImageOptions? = nil,
        entrance: EntranceOptions? = nil,
        backgroundGradient: BackgroundGradientOptions? = nil,
        spinner: SpinnerOptions? = nil,
        haptic: HapticOptions? = nil
    ) {
        self.autoHide = autoHide
        self.fadeInDuration = fadeInDuration
        self.fadeOutDuration = fadeOutDuration
        self.showDuration = showDuration
        self.lottie = lottie
        self.image = image
        self.backgroundImage = backgroundImage
        self.brandingImage = brandingImage
        self.entrance = entrance
        self.backgroundGradient = backgroundGradient
        self.spinner = spinner
        self.haptic = haptic
    }
}

/**
 * Options for hiding the splash screen.
 *
 * Mirrors the TypeScript `HideOptions` interface.
 */
public struct HideOptions {
    /// How long (in ms) to fade out. Default: 200
    public var fadeOutDuration: Int?
    /// Haptic feedback fired when the splash disappears.
    public var haptic: HapticOptions?

    public init(fadeOutDuration: Int? = nil, haptic: HapticOptions? = nil) {
        self.fadeOutDuration = fadeOutDuration
        self.haptic = haptic
    }
}

/**
 * Options describing a splash image layer (background layer, branding
 * image, or per-call static image).
 *
 * Mirrors the TypeScript `ImageOptions` interface.
 */
public struct ImageOptions {
    /// Image resource: an asset catalog or app bundle name on iOS, a
    /// drawable resource name on Android. A value containing "/" is
    /// resolved as a web asset path relative to the web asset root.
    public let source: String
    /// Alternative image rendered when the system is in dark mode.
    /// Same accepted forms as source. When nil, source is used for both.
    public let darkSource: String?
    /// Placement of the image inside the splash surface. Default: "center"
    public let gravity: String?
    /// Scaling applied to the bitmap inside its view. Default: "CENTER_CROP"
    public let scaleType: String?
    /// Opacity of the image, from 0 (transparent) to 1 (opaque). Default: 1
    public let alpha: Double?
    /// Margin between the image and the surface edge, in points. Default: 16
    public let margin: Double?

    init(
        source: String,
        darkSource: String? = nil,
        gravity: String? = nil,
        scaleType: String? = nil,
        alpha: Double? = nil,
        margin: Double? = nil
    ) {
        self.source = source
        self.darkSource = darkSource
        self.gravity = gravity
        self.scaleType = scaleType
        self.alpha = alpha
        self.margin = margin
    }

    /**
     Parses an image layer from a JS-facing dictionary (per-call options
     or static config object). Returns nil when the source is missing.
     */
    static func parse(_ dict: [AnyHashable: Any]?) -> ImageOptions? {
        guard let dict, let source = dict["source"] as? String else {
            return nil
        }
        return ImageOptions(
            source: source,
            darkSource: dict["darkSource"] as? String,
            gravity: dict["gravity"] as? String,
            scaleType: dict["scaleType"] as? String,
            alpha: dict["alpha"] as? Double,
            margin: dict["margin"] as? Double
        )
    }
}

/**
 * Entrance animation applied to the splash content when it appears.
 *
 * Mirrors the TypeScript `EntranceOptions` interface.
 */
/**
 * Loading spinner rendered centered on top of the splash content.
 *
 * Mirrors the TypeScript `SpinnerOptions` interface.
 */
public struct SpinnerOptions {
    /// Whether the spinner is shown. Default: false
    public let show: Bool?
    /// Style of the native spinner ("small" or "large" on iOS).
    /// Default: nil (platform default style)
    public let style: String?
    /// Color of the spinner in hex format (#RRGGBB or #RRGGBBAA).
    /// An invalid value keeps the platform default spinner color.
    public let color: String?

    init(show: Bool?, style: String?, color: String?) {
        self.show = show
        self.style = style
        self.color = color
    }

    static func parse(_ dict: [AnyHashable: Any]?) -> SpinnerOptions? {
        guard let dict else {
            return nil
        }
        return SpinnerOptions(
            show: dict["show"] as? Bool,
            style: dict["style"] as? String,
            color: dict["color"] as? String
        )
    }
}

public struct EntranceOptions {
    /// Animation type applied to the whole splash content. Default: "none"
    public let type: String?
    /// Duration of the entrance animation, in ms. Clamped to 0-2000. Default: 300
    public let durationMs: Int?

    init(type: String?, durationMs: Int?) {
        self.type = type
        self.durationMs = durationMs
    }

    static func parse(_ dict: [AnyHashable: Any]?) -> EntranceOptions? {
        guard let dict else {
            return nil
        }
        return EntranceOptions(
            type: dict["type"] as? String,
            durationMs: dict["durationMs"] as? Int
        )
    }
}

/**
 * Vertical gradient background of the splash surface.
 *
 * Mirrors the TypeScript `BackgroundGradientOptions` interface.
 */
public struct BackgroundGradientOptions {
    /// Gradient color at the top of the splash surface (hex format).
    public let top: String
    /// Gradient color at the bottom of the splash surface (hex format).
    public let bottom: String
    /// Gradient color at the top of the splash surface in dark mode.
    public let topDark: String?
    /// Gradient color at the bottom of the splash surface in dark mode.
    public let bottomDark: String?

    init(top: String, bottom: String, topDark: String?, bottomDark: String?) {
        self.top = top
        self.bottom = bottom
        self.topDark = topDark
        self.bottomDark = bottomDark
    }

    static func parse(_ dict: [AnyHashable: Any]?) -> BackgroundGradientOptions? {
        guard let dict, let top = dict["top"] as? String, let bottom = dict["bottom"] as? String else {
            return nil
        }
        return BackgroundGradientOptions(
            top: top,
            bottom: bottom,
            topDark: dict["topDark"] as? String,
            bottomDark: dict["bottomDark"] as? String
        )
    }
}
