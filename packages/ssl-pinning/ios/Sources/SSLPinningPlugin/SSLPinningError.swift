import Foundation

/**
 Native error model for the SSLPinning plugin (iOS).

 This enum represents all error categories that can be
 produced by the native implementation layer.

 Architectural rules:
 - Must NOT reference Capacitor
 - Must NOT reference JavaScript
 - Must be throwable from the Impl layer
 - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
enum SSLPinningError: Error {

    /// Feature or capability is not available on this device or configuration
    case unavailable(String)

    /// Required permission was denied or not granted
    case permissionDenied(String)

    /// Plugin failed to initialize or perform a required operation
    case initFailed(String)

    /// Invalid or unsupported input was provided
    case unknownType(String)

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
        }
    }
}
