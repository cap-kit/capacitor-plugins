package io.capkit.fortress.error

/**
 * Native error model for the Fortress plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs
 * - Must NOT reference JavaScript
 * - Must be throwable from the Impl layer
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
sealed class NativeError(
  message: String,
  val errorCode: String,
) : Throwable(message) {
  // -----------------------------------------------------------------------------
  // Error Code Constants
  // -----------------------------------------------------------------------------

  companion object {
    const val UNAVAILABLE = "UNAVAILABLE"
    const val CANCELLED = "CANCELLED"
    const val PERMISSION_DENIED = "PERMISSION_DENIED"
    const val INIT_FAILED = "INIT_FAILED"
    const val INVALID_INPUT = "INVALID_INPUT"
    const val NOT_FOUND = "NOT_FOUND"
    const val CONFLICT = "CONFLICT"
    const val TIMEOUT = "TIMEOUT"
    const val SECURITY_VIOLATION = "SECURITY_VIOLATION"
    const val VAULT_LOCKED = "VAULT_LOCKED"
  }

  // -----------------------------------------------------------------------------
  // Specific Error Types
  // -----------------------------------------------------------------------------

  /**
   * Feature or capability is not available due to device or configuration limitations.
   * Maps to the 'UNAVAILABLE' error code in JavaScript.
   */
  class Unavailable(
    message: String,
  ) : NativeError(message, UNAVAILABLE)

  /**
   * The user cancelled an interactive flow.
   * Maps to the 'CANCELLED' error code in JavaScript.
   */
  class Cancelled(
    message: String,
  ) : NativeError(message, CANCELLED)

  /**
   * Required permission was denied or not granted by the user.
   * Maps to the 'PERMISSION_DENIED' error code in JavaScript.
   */
  class PermissionDenied(
    message: String,
  ) : NativeError(message, PERMISSION_DENIED)

  /**
   * Plugin failed to initialize or perform
   * a required operation.
   */
  class InitFailed(
    message: String,
  ) : NativeError(message, INIT_FAILED)

  /**
   * Invalid input provided (e.g., exceeds max length).
   */
  class InvalidInput(
    message: String,
  ) : NativeError(message, INVALID_INPUT)

  /**
   * The requested resource does not exist.
   * Maps to the 'NOT_FOUND' error code in JavaScript.
   */
  class NotFound(
    message: String,
  ) : NativeError(message, NOT_FOUND)

  /**
   * The operation conflicts with the current state.
   * Maps to the 'CONFLICT' error code in JavaScript.
   */
  class Conflict(
    message: String,
  ) : NativeError(message, CONFLICT)

  /**
   * The operation did not complete within the expected time.
   * Maps to the 'TIMEOUT' error code in JavaScript.
   */
  class Timeout(
    message: String,
  ) : NativeError(message, TIMEOUT)

  /**
   * Security validation failed in cryptographic or integrity checks.
   */
  class SecurityViolation(
    message: String,
  ) : NativeError(message, SECURITY_VIOLATION)

  /**
   * Secure operation requested while vault is locked.
   */
  class VaultLocked(
    message: String,
  ) : NativeError(message, VAULT_LOCKED)
}
