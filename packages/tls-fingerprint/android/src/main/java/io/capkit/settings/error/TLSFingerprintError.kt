package io.capkit.tlsfingerprint.error

/**
 * Native error model for the TLSFingerprint plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs.
 * - Must NOT reference JavaScript directly.
 * - Must be throwable from the Implementation (Impl) layer.
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer.
 *
 * Errors are intentionally NOT @Serializable: they are conveyed to JavaScript
 * via call.reject(message, code) in the Plugin layer, never serialized as JSON.
 */
sealed class TLSFingerprintError(
  val errorMessage: String,
) : Throwable(errorMessage) {
  // -----------------------------------------------------------------------------
  // Specific Error Types
  // -----------------------------------------------------------------------------

  /**
   * Feature or capability is not available due to device or configuration limitations.
   * Maps to the 'UNAVAILABLE' error code in JavaScript.
   */
  class Unavailable(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * The user cancelled an interactive flow.
   * Maps to the 'CANCELLED' error code in JavaScript.
   */
  class Cancelled(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * Required permission was denied or not granted by the user.
   * Maps to the 'PERMISSION_DENIED' error code in JavaScript.
   */
  class PermissionDenied(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * Plugin failed to initialize or perform a required native operation.
   * Maps to the 'INIT_FAILED' error code in JavaScript.
   */
  class InitFailed(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * Invalid or malformed input was provided by the caller.
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidInput(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * Invalid or unsupported input type was provided to the native implementation.
   * Maps to the 'UNKNOWN_TYPE' error code in JavaScript.
   */
  class UnknownType(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * The requested resource does not exist.
   * Maps to the 'NOT_FOUND' error code in JavaScript.
   */
  class NotFound(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * The operation conflicts with the current state.
   * Maps to the 'CONFLICT' error code in JavaScript.
   */
  class Conflict(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * The operation did not complete within the expected time.
   * Maps to the 'TIMEOUT' error code in JavaScript.
   */
  class Timeout(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * Network connectivity or TLS handshake error.
   * Maps to the 'NETWORK_ERROR' error code in JavaScript.
   */
  class NetworkError(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * Invalid or malformed configuration.
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidConfig(
    val msg: String,
  ) : TLSFingerprintError(msg)

  /**
   * SSL/TLS specific error (certificate expired, handshake failure, etc.).
   * Maps to the 'SSL_ERROR' error code in JavaScript.
   */
  class SslError(
    val msg: String,
  ) : TLSFingerprintError(msg)
}
