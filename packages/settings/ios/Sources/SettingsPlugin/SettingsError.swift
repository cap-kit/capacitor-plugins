import Foundation

/**
 Native error model for the Settings plugin (iOS).

 This enum represents all possible error categories
 produced by the native implementation.

 IMPORTANT:
 - Must NOT reference Capacitor
 - Must NOT reference JavaScript
 - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
enum SettingsError: Error {
    case unavailable(String)
    case permissionDenied(String)
    case initFailed(String)
    case unknownType(String)

    /// Human-readable error message
    var message: String {
        switch self {
        case .unavailable(let message),
             .permissionDenied(let message),
             .initFailed(let message),
             .unknownType(let message):
            return message
        }
    }
}
