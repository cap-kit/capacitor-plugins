import Foundation

/**
 Native error model for the Integrity plugin (iOS).

 This enum represents all error categories that can be
 produced by the native implementation layer.

 Architectural rules:
 - Must NOT reference Capacitor
 - Must NOT reference JavaScript
 - Must be throwable from the Impl layer
 - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
enum NativeError: Swift.Error {

    /// Feature or capability is not available on this device or configuration
    case unavailable(String)

    /// Required permission was denied or not granted
    case permissionDenied(String)

    /// Plugin failed to initialize or perform a required operation
    case initFailed(String)

    /// Invalid or unsupported input was provided
    case unknownType(String)

    /// Invalid input provided (e.g., exceeds max length)
    case invalidInput(String)

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
        case .permissionDenied(let message):
            return message
        case .initFailed(let message):
            return message
        case .unknownType(let message):
            return message
        case .invalidInput(let message):
            return message
        }
    }

    /// Standardized error code string for JS rejection.
    var errorCode: String {
        switch self {
        case .unavailable:
            return "UNAVAILABLE"
        case .permissionDenied:
            return "PERMISSION_DENIED"
        case .initFailed:
            return "INIT_FAILED"
        case .unknownType:
            return "UNKNOWN_TYPE"
        case .invalidInput:
            return "INVALID_INPUT"
        }
    }
}
