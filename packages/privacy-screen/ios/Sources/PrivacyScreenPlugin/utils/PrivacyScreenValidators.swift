import Foundation

/**
 Collection of input validation helpers for the PrivacyScreen plugin.

 These functions are pure and side-effect free.
 They MUST NOT depend on Capacitor APIs.
 */
enum PrivacyScreenValidators {

    /**
     Returns whether the raw `ios.blurEffect` session option is one of the
     documented values (`light` / `dark` / `none`).

     Any other value (including nil and wrong casing) is rejected, so the
     implementation keeps the previously requested style. The comparison is
     case-sensitive on purpose: the JS contract sends exact lowercase values.
     */
    static func isValidBlurEffect(_ value: String?) -> Bool {
        guard let value else { return false }
        return ["light", "dark", "none"].contains(value)
    }

    /**
     Returns whether the raw static `color` option is a valid hex color.

     Accepted shapes: `#RRGGBB`, `#AARRGGBB` and the same shapes without the
     leading `#`. Any other value (including CSS color names and `rgba()`
     strings) is rejected so the cover keeps its default blur style. The
     comparison is case-insensitive on purpose: hex digits may be written in
     either case and both are unambiguous.
     */
    static func isValidHexColor(_ value: String?) -> Bool {
        guard let value else { return false }
        return value.range(of: "^#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$", options: .regularExpression) != nil
    }
}
