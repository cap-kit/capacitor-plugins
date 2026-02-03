package io.capkit.sslpinning

/**
 * Native error model for the SSLPinning plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs
 * - Must NOT reference JavaScript
 * - Must be throwable from the Impl layer
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
sealed class SSLPinningError(
  message: String,
) : Throwable(message) {
  /**
   * Feature or capability is not available
   * due to device or configuration limitations.
   */
  class Unavailable(message: String) :
    SSLPinningError(message)

  /**
   * Required permission was denied or not granted.
   */
  class PermissionDenied(message: String) :
    SSLPinningError(message)

  /**
   * Plugin failed to initialize or perform
   * a required operation.
   */
  class InitFailed(message: String) :
    SSLPinningError(message)

  /**
   * Invalid or unsupported input was provided.
   */
  class UnknownType(message: String) :
    SSLPinningError(message)
}
