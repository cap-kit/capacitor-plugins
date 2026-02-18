package io.capkit.people.error

/**
 * Native error model for the People plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs.
 * - Must NOT reference JavaScript directly.
 * - Must be throwable from the Implementation (Impl) layer.
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer.
 */
sealed class PeopleError(
  message: String,
) : Throwable(message) {
  // -----------------------------------------------------------------------------
  // Specific Error Types
  // -----------------------------------------------------------------------------

  /**
   * Feature or capability is not available due to device or configuration limitations.
   * Maps to the 'UNAVAILABLE' error code in JavaScript.
   */
  class Unavailable(
    message: String,
  ) : PeopleError(message)

  /**
   * The user cancelled an interactive flow (e.g., contact picker).
   * Maps to the 'CANCELLED' error code in JavaScript.
   */
  class Cancelled(
    message: String,
  ) : PeopleError(message)

  /**
   * Required permission was denied or not granted by the user.
   * Maps to the 'PERMISSION_DENIED' error code in JavaScript.
   */
  class PermissionDenied(
    message: String,
  ) : PeopleError(message)

  /**
   * Plugin failed to initialize or perform a required native operation.
   * Maps to the 'INIT_FAILED' error code in JavaScript.
   */
  class InitFailed(
    message: String,
  ) : PeopleError(message)

  /**
   * Invalid or malformed input was provided by the caller.
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidInput(
    message: String,
  ) : PeopleError(message)

  /**
   * Invalid or unsupported input type was provided to the native implementation.
   * Maps to the 'UNKNOWN_TYPE' error code in JavaScript.
   */
  class UnknownType(
    message: String,
  ) : PeopleError(message)

  /**
   * The requested resource does not exist (e.g., contact or group not found).
   * Maps to the 'NOT_FOUND' error code in JavaScript.
   */
  class NotFound(
    message: String,
  ) : PeopleError(message)

  /**
   * The operation conflicts with the current state (e.g., read-only group).
   * Maps to the 'CONFLICT' error code in JavaScript.
   */
  class Conflict(
    message: String,
  ) : PeopleError(message)

  /**
   * The operation did not complete within the expected time.
   * Maps to the 'TIMEOUT' error code in JavaScript.
   */
  class Timeout(
    message: String,
  ) : PeopleError(message)
}
