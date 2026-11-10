import Foundation
import UIKit

/**
 Utility helpers for the PrivacyScreen plugin.

 This type is intentionally minimal and serves as a placeholder
 for future shared helper functions.

 Keeping a dedicated utilities file helps maintain a clean
 separation between core logic and helper code.
 */
struct PrivacyScreenUtils {

    /**
     Maps a validated `blurEffect` session value to the system blur style
     used by the app-switcher cover.

     Pure string-to-enum mapping with no UIKit side effects:
     - `light` -> systemMaterialLight
     - `dark`  -> systemMaterialDark
     - anything else (including `none` and the internal `system` default)
       -> systemMaterial (the neutral system style)

     Callers must only pass values accepted by
     `PrivacyScreenValidators.isValidBlurEffect` (or the internal default).
     */
    static func blurStyle(for effect: String) -> UIBlurEffect.Style {
        switch effect {
        case "light":
            return .systemMaterialLight
        case "dark":
            return .systemMaterialDark
        default:
            return .systemMaterial
        }
    }

    /**
     Builds a `UIColor` from a validated hex color string.

     Accepts the same shapes as `PrivacyScreenValidators.isValidHexColor`
     (`#RRGGBB`, `#AARRGGBB`, with or without `#`); 6-digit input is treated
     as fully opaque. Returns nil for invalid input so callers can fall back
     to the default blur cover.

     Pure string parsing with no UIKit side effects.
     */
    static func color(fromHex hex: String) -> UIColor? {
        guard PrivacyScreenValidators.isValidHexColor(hex) else { return nil }
        var digits = hex
        if digits.hasPrefix("#") {
            digits.removeFirst()
        }
        if digits.count == 6 {
            digits = "FF" + digits
        }
        var argb: UInt64 = 0
        Scanner(string: digits).scanHexInt64(&argb)
        return UIColor(
            red: CGFloat((argb >> 16) & 0xFF) / 255.0,
            green: CGFloat((argb >> 8) & 0xFF) / 255.0,
            blue: CGFloat(argb & 0xFF) / 255.0,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255.0
        )
    }
}
