import Foundation

/**
 Canonical error messages shared across platforms.

 These strings should remain byte-identical on iOS and Android
 whenever they represent the same failure condition.
 */
enum ErrorMessages {
    static let unavailable = "Feature is unavailable on this device or configuration."
    static let permissionDenied = "Required permission is denied or not granted."
    static let initFailed = "Native initialization failed."
    static let unknownType = "Unsupported or invalid input type."

    static let timeout = "Timeout"
    static let networkError = "Network error"
    static let internalError = "Internal error"
    static let unexpectedNativeError = "Unexpected native error during integrity check."
    static let viewControllerUnavailable = "View controller not available"
}
