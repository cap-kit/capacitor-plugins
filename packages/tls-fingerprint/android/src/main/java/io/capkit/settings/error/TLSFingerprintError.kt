package io.capkit.tlsfingerprint.error

/**
 * Native error model for the TLSFingerprint plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs.
 * - Must NOT reference JavaScript directly.
 * - Must be throwable from the Implementation (Impl) layer.
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer.
 */
sealed class TLSFingerprintError(
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
  ) : TLSFingerprintError(message)

  /**
   * The user cancelled an interactive flow.
   * Maps to the 'CANCELLED' error code in JavaScript.
   */
  class Cancelled(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * Required permission was denied or not granted by the user.
   * Maps to the 'PERMISSION_DENIED' error code in JavaScript.
   */
  class PermissionDenied(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * Plugin failed to initialize or perform a required native operation.
   * Maps to the 'INIT_FAILED' error code in JavaScript.
   */
  class InitFailed(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * Invalid or malformed input was provided by the caller.
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidInput(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * Invalid or unsupported input type was provided to the native implementation.
   * Maps to the 'UNKNOWN_TYPE' error code in JavaScript.
   */
  class UnknownType(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * The requested resource does not exist.
   * Maps to the 'NOT_FOUND' error code in JavaScript.
   */
  class NotFound(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * The operation conflicts with the current state.
   * Maps to the 'CONFLICT' error code in JavaScript.
   */
  class Conflict(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * The operation did not complete within the expected time.
   * Maps to the 'TIMEOUT' error code in JavaScript.
   */
  class Timeout(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * Network connectivity or TLS handshake error.
   * Maps to the 'NETWORK_ERROR' error code in JavaScript.
   */
  class NetworkError(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * Invalid or malformed configuration.
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidConfig(
    message: String,
  ) : TLSFingerprintError(message)

  /**
   * SSL/TLS specific error (certificate expired, handshake failure, etc.).
   * Maps to the 'SSL_ERROR' error code in JavaScript.
   */
  class SslError(
    message: String,
  ) : TLSFingerprintError(message)
}
