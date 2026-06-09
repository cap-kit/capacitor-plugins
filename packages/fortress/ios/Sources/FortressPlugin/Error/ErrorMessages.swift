import Foundation

/**
 Canonical error messages shared across platforms.

 These strings should remain byte-identical on iOS and Android
 whenever they represent the same failure condition.
 */
enum ErrorMessages {
    static let unavailable = "Feature is unavailable on this device or configuration."
    static let cancelled = "Operation was cancelled by the user."
    static let permissionDenied = "Required permission is denied."
    static let initFailed = "Native initialization failed."
    static let invalidInput = "Invalid input provided."
    static let notFound = "Requested resource not found."
    static let conflict = "Operation conflicts with current vault state."
    static let timeout = "Operation timed out."
    static let securityViolation = "Security validation failed."
    static let vaultLocked = "Vault is locked."
    static let internalError = "Internal error."
    static let unexpectedNativeError = "Unexpected native error."
}
