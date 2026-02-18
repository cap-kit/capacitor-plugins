import Foundation

/**
 Native error model for the People plugin (iOS).

 This enum represents all error categories that can be
 produced by the native implementation layer.

 Architectural rules:
 - Must NOT reference Capacitor
 - Must NOT reference JavaScript
 - Must be throwable from the Impl layer
 - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
enum PeopleError: Error {

    /// Feature or capability is not available on this device or configuration
    case unavailable(String)

    /// The user cancelled an interactive flow (e.g., contact picker)
    case cancelled(String)

    /// Required permission was denied or not granted
    case permissionDenied(String)

    /// Plugin failed to initialize or perform a required operation
    case initFailed(String)

    /// The input provided to the plugin method is invalid or malformed
    case invalidInput(String)

    /// Invalid or unsupported input was provided
    case unknownType(String)

    /// The requested resource does not exist (e.g., contact or group not found)
    case notFound(String)

    /// The operation conflicts with the current state (e.g., read-only group)
    case conflict(String)

    /// The operation did not complete within the expected time
    case timeout(String)

    // MARK: - Human-readable message

    /**
     Human-readable error message.

     This message is intended to be passed verbatim
     to JavaScript via `call.reject(message, code)`.
     */
    var message: String {
        switch self {
        case .unavailable(let message):
            return message
        case .cancelled(let message):
            return message
        case .permissionDenied(let message):
            return message
        case .initFailed(let message):
            return message
        case .invalidInput(let message):
            return message
        case .unknownType(let message):
            return message
        case .notFound(let message):
            return message
        case .conflict(let message):
            return message
        case .timeout(let message):
            return message
        }
    }

    /// Standardized error code string for JS rejection.
    var errorCode: String {
        switch self {
        case .unavailable: return "UNAVAILABLE"
        case .cancelled: return "CANCELLED"
        case .permissionDenied: return "PERMISSION_DENIED"
        case .initFailed: return "INIT_FAILED"
        case .invalidInput: return "INVALID_INPUT"
        case .unknownType: return "UNKNOWN_TYPE"
        case .notFound: return "NOT_FOUND"
        case .conflict: return "CONFLICT"
        case .timeout: return "TIMEOUT"
        }
    }
}
