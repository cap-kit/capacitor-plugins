package io.capkit.integrity

/**
 * Native error model for the Integrity plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs
 * - Must NOT reference JavaScript
 * - Must be throwable from the Impl layer
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
sealed class IntegrityError(
  message: String,
) : Throwable(message) {
  /**
   * Feature or capability is not available
   * due to device or configuration limitations.
   */
  class Unavailable(message: String) :
    IntegrityError(message)

  /**
   * Required permission was denied or not granted.
   */
  class PermissionDenied(message: String) :
    IntegrityError(message)

  /**
   * Plugin failed to initialize or perform
   * a required operation.
   */
  class InitFailed(message: String) :
    IntegrityError(message)

  /**
   * Invalid or unsupported input was provided.
   */
  class UnknownType(message: String) :
    IntegrityError(message)
}
