import Foundation

/**
 Native error model for the Test plugin (iOS).

 This enum represents ALL possible error categories
 that the native implementation may produce.

 IMPORTANT:
 - Must NOT reference Capacitor
 - Must NOT reference JavaScript or TestErrorCode
 - Mapping to JS error codes happens ONLY in the Plugin layer
 */
enum TestError: Error {
    case unavailable(String)
    case permissionDenied(String)
    case initFailed(String)
    case unknownType(String)

    /// Human-readable error message
    var message: String {
        switch self {
        case .unavailable(let msg),
             .permissionDenied(let msg),
             .initFailed(let msg),
             .unknownType(let msg):
            return msg
        }
    }
}
