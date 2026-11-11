import Foundation
import UIKit

/**
 Collection of input validation helpers for the SplashMotion plugin.

 These functions are pure and side-effect free.
 They MUST NOT depend on Capacitor APIs.
 */
enum SplashMotionValidators {

    /**
     Normalizes a haptic style name to one of the supported values.

     - Returns: "light", "medium" or "heavy". Any other value (including nil)
     falls back to "medium".
     */
    static func normalizeHapticStyle(_ style: String?) -> String {
        switch style?.lowercased() {
        case "light":
            return "light"
        case "heavy":
            return "heavy"
        default:
            return "medium"
        }
    }

    /**
     Parses a hex color string (#RRGGBB or #RRGGBBAA) into a UIColor.

     - Returns: the parsed color, or nil when the input is nil or invalid.
     */
    static func parseColor(_ value: String?) -> UIColor? {
        guard let value, !value.isEmpty else { return nil }

        var hex = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if hex.hasPrefix("#") {
            hex.removeFirst()
        }

        var rgba: UInt64 = 0
        guard Scanner(string: hex).scanHexInt64(&rgba) else { return nil }

        switch hex.count {
        case 6:
            return UIColor(
                red: CGFloat((rgba >> 16) & 0xFF) / 255.0,
                green: CGFloat((rgba >> 8) & 0xFF) / 255.0,
                blue: CGFloat(rgba & 0xFF) / 255.0,
                alpha: 1.0
            )
        case 8:
            return UIColor(
                red: CGFloat((rgba >> 24) & 0xFF) / 255.0,
                green: CGFloat((rgba >> 16) & 0xFF) / 255.0,
                blue: CGFloat((rgba >> 8) & 0xFF) / 255.0,
                alpha: CGFloat(rgba & 0xFF) / 255.0
            )
        default:
            return nil
        }
    }

    /**
     Maps a spinner style name to the matching iOS activity indicator style,
     mirroring the official plugin mapping: only "small" and "large"
     (case-insensitive) are differentiated; any other value falls back to
     the platform default (.medium).

     - Returns: the mapped UIActivityIndicatorView style.
     */
    static func parseSpinnerStyle(_ value: String?) -> UIActivityIndicatorView.Style {
        switch value?.lowercased() {
        case "large":
            return .large
        default:
            return .medium
        }
    }

    /**
     Clamps a duration in milliseconds to a sane non-negative range.

     - Returns: the clamped duration, or `fallback` when the input is nil.
     */
    static func clampDuration(_ value: Int?, fallback: Int) -> Int {
        guard let value else { return fallback }
        return min(max(value, 0), 60_000)
    }

    // MARK: - Image layers

    /**
     Horizontal anchor of a splash gravity value, following the reading
     direction of the layout ("start"/"end" map to leading/trailing).
     */
    enum GravityHorizontal {
        case leading
        case center
        case trailing
    }

    /**
     Vertical anchor of a splash gravity value.
     */
    enum GravityVertical {
        case top
        case center
        case bottom
    }

    /**
     Parsed splash gravity: the horizontal and vertical anchors used to
     build Auto Layout constraints.
     */
    struct GravityAnchor {
        let horizontal: GravityHorizontal
        let vertical: GravityVertical
    }

    /**
     Parses a splash gravity name into its anchors.

     Accepted values: topStart, top, topEnd, start, center, end,
     bottomStart, bottom, bottomEnd (case-insensitive).

     - Returns: the parsed anchors, or centered anchors for nil/unknown
     values.
     */
    static func parseGravity(_ value: String?) -> GravityAnchor {
        switch value?.lowercased() {
        case "topstart":
            return GravityAnchor(horizontal: .leading, vertical: .top)
        case "top":
            return GravityAnchor(horizontal: .center, vertical: .top)
        case "topend":
            return GravityAnchor(horizontal: .trailing, vertical: .top)
        case "start":
            return GravityAnchor(horizontal: .leading, vertical: .center)
        case "end":
            return GravityAnchor(horizontal: .trailing, vertical: .center)
        case "bottomstart":
            return GravityAnchor(horizontal: .leading, vertical: .bottom)
        case "bottom":
            return GravityAnchor(horizontal: .center, vertical: .bottom)
        case "bottomend":
            return GravityAnchor(horizontal: .trailing, vertical: .bottom)
        default:
            return GravityAnchor(horizontal: .center, vertical: .center)
        }
    }

    /**
     Maps a splash scale type name (official ImageView.ScaleType names) to
     the matching UIView content mode.

     - Returns: the mapped content mode, or `.scaleAspectFill` for nil and
     unknown values (matching the Android default CENTER_CROP).
     */
    static func parseContentMode(_ value: String?) -> UIView.ContentMode {
        switch value?.uppercased() {
        case "CENTER":
            return .center
        case "CENTER_CROP":
            return .scaleAspectFill
        case "CENTER_INSIDE":
            return .scaleAspectFit
        case "FIT_CENTER":
            return .scaleAspectFit
        case "FIT_END":
            return .scaleAspectFit
        case "FIT_START":
            return .scaleAspectFit
        case "FIT_XY":
            return .scaleToFill
        case "MATRIX":
            return .center
        default:
            return .scaleAspectFill
        }
    }

    /**
     Normalizes an image opacity value to the 0...1 range.

     - Returns: the clamped opacity, or 1 when the input is nil.
     */
    static func normalizeAlpha(_ value: Double?) -> CGFloat {
        CGFloat(min(max(value ?? 1.0, 0.0), 1.0))
    }

    /**
     Normalizes an image margin value in points to the documented 0-200
     range.

     - Returns: the clamped margin, or `fallback` when the input is nil.
     */
    static func normalizeMargin(_ value: Double?, fallback: Double) -> CGFloat {
        CGFloat(min(max(value ?? fallback, 0.0), 200.0))
    }

    // MARK: - Entrance animation

    /**
     Normalizes an entrance animation type name to one of the supported
     values.

     - Returns: "none", "fade" or "scale". Any other value (including nil)
     falls back to "none".
     */
    static func normalizeEntranceType(_ type: String?) -> String {
        switch type?.lowercased() {
        case "fade":
            return "fade"
        case "scale":
            return "scale"
        default:
            return "none"
        }
    }

    /**
     Clamps an entrance animation duration in milliseconds to the
     documented 0-2000 range.

     - Returns: the clamped duration, or 300 when the input is nil.
     */
    static func clampEntranceDuration(_ value: Int?) -> Int {
        guard let value else { return 300 }
        return min(max(value, 0), 2_000)
    }

    // MARK: - Gradient

    /**
     Resolves the splash gradient colors honoring dark mode.

     When `isDark` is true and both dark variants are valid colors, the
     dark pair wins; otherwise the light pair is used. Lenient: an invalid
     dark pair silently falls back to the light pair.

     - Returns: the top/bottom color pair, or nil when no valid pair is
     available (the gradient is then ignored and the flat background color
     applies).
     */
    static func resolveGradient(
        top: String?,
        bottom: String?,
        topDark: String?,
        bottomDark: String?,
        isDark: Bool
    ) -> (UIColor, UIColor)? {
        if isDark,
           let darkTop = parseColor(topDark),
           let darkBottom = parseColor(bottomDark) {
            return (darkTop, darkBottom)
        }
        guard let lightTop = parseColor(top), let lightBottom = parseColor(bottom) else {
            return nil
        }
        return (lightTop, lightBottom)
    }
}
