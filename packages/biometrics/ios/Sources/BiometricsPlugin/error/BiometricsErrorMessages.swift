import Foundation

/// Canonical error messages shared across platforms.
/// Keep these strings identical on iOS and Android.
enum BiometricsErrorMessages {
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
    static let unexpectedNativeError = "Unexpected native error in plugin operation."

    // MARK: - Secure storage (biometry-bound store)

    /// No value exists for the requested key in the secure store.
    static let secureDataNotFound = "No secure data found for the given key."

    /// The stored payload exists but could not be decoded.
    static let secureDataInvalid = "Stored secure data could not be decoded."
}
