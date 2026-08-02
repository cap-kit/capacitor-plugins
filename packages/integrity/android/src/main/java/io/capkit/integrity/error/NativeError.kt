package io.capkit.integrity.error

/**
 * Native error model for the Integrity plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs
 * - Must NOT reference JavaScript
 * - Must be throwable from the Impl layer
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
sealed class NativeError(
  val errorMessage: String,
) : Throwable(errorMessage) {
  // -----------------------------------------------------------------------------
  // Specific Error Types
  // -----------------------------------------------------------------------------

  /**
   * Feature or capability is not available
   * due to device or configuration limitations.
   * Maps to the 'UNAVAILABLE' error code in JavaScript.
   */
  class Unavailable(
    val msg: String,
  ) : NativeError(msg)

  /**
   * Required permission was denied or not granted.
   * Maps to the 'PERMISSION_DENIED' error code in JavaScript.
   */
  class PermissionDenied(
    val msg: String,
  ) : NativeError(msg)

  /**
   * Plugin failed to initialize or perform
   * a required operation.
   * Maps to the 'INIT_FAILED' error code in JavaScript.
   */
  class InitFailed(
    val msg: String,
  ) : NativeError(msg)

  /**
   * Invalid or unsupported input was provided.
   * Maps to the 'UNKNOWN_TYPE' error code in JavaScript.
   */
  class UnknownType(
    val msg: String,
  ) : NativeError(msg)

  /**
   * Invalid input provided (e.g., exceeds max length).
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidInput(
    val msg: String,
  ) : NativeError(msg)
}
