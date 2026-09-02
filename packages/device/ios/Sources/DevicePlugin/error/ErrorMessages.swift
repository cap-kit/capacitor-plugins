import Foundation

/**
 Canonical error messages shared across platforms.

 These strings should remain byte-identical on iOS and Android
 whenever they represent the same failure condition.
 */
enum ErrorMessages {
    static let unavailable = "Feature is unavailable on this device or configuration."
    static let cancelled = "Operation was cancelled."
    static let permissionDenied = "Required permission is denied or not granted."
    static let initFailed = "Native initialization failed."
    static let invalidInput = "Invalid or missing input parameters."
    static let unknownType = "Unsupported or invalid input type."
    static let notFound = "Requested resource not found."
    static let conflict = "Operation conflict with current state."
    static let timeout = "Operation timed out."
    static let internalError = "Internal error."
    static let unexpectedNativeError = "Unexpected native error in device operation."
}
