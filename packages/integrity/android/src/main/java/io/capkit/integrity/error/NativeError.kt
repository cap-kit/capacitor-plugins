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
  message: String,
  val errorCode: String,
) : Throwable(message) {
  /**
   * Feature or capability is not available
   * due to device or configuration limitations.
   */
  class Unavailable(
    message: String,
  ) : NativeError(message, "UNAVAILABLE")

  /**
   * Required permission was denied or not granted.
   */
  class PermissionDenied(
    message: String,
  ) : NativeError(message, "PERMISSION_DENIED")

  /**
   * Plugin failed to initialize or perform
   * a required operation.
   */
  class InitFailed(
    message: String,
  ) : NativeError(message, "INIT_FAILED")

  /**
   * Invalid or unsupported input was provided.
   */
  class UnknownType(
    message: String,
  ) : NativeError(message, "UNKNOWN_TYPE")
}
