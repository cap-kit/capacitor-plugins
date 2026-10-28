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
}
