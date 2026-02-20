package io.capkit.sslpinning.error

/**
 * Native error model for the Rank plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs.
 * - Must NOT reference JavaScript directly.
 * - Must be throwable from the Implementation (Impl) layer.
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer.
 */
sealed class SSLPinningError(
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
  ) : SSLPinningError(message)

  /**
   * The user cancelled an interactive flow.
   * Maps to the 'CANCELLED' error code in JavaScript.
   */
  class Cancelled(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Required permission was denied or not granted by the user.
   * Maps to the 'PERMISSION_DENIED' error code in JavaScript.
   */
  class PermissionDenied(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Plugin failed to initialize or perform a required native operation.
   * Maps to the 'INIT_FAILED' error code in JavaScript.
   */
  class InitFailed(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Invalid or malformed input was provided by the caller.
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidInput(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Invalid or unsupported input type was provided to the native implementation.
   * Maps to the 'UNKNOWN_TYPE' error code in JavaScript.
   */
  class UnknownType(
    message: String,
  ) : SSLPinningError(message)

  /**
   * The requested resource does not exist.
   * Maps to the 'NOT_FOUND' error code in JavaScript.
   */
  class NotFound(
    message: String,
  ) : SSLPinningError(message)

  /**
   * The operation conflicts with the current state.
   * Maps to the 'CONFLICT' error code in JavaScript.
   */
  class Conflict(
    message: String,
  ) : SSLPinningError(message)

  /**
   * The operation did not complete within the expected time.
   * Maps to the 'TIMEOUT' error code in JavaScript.
   */
  class Timeout(
    message: String,
  ) : SSLPinningError(message)

  /**
   * No runtime fingerprints, no config fingerprints,
   * and no certificates were configured.
   */
  class NoPinningConfig(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Certificate-based pinning was selected, but no valid
   * certificate files were found or loaded.
   */
  class CertNotFound(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Certificate-based trust evaluation failed.
   * The server certificate chain could not be validated
   * against the pinned certificates.
   */
  class TrustEvaluationFailed(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Network connectivity or TLS handshake error.
   * Maps to the 'NETWORK_ERROR' error code in JavaScript.
   */
  class NetworkError(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Invalid or malformed configuration.
   * Maps to the 'INVALID_INPUT' error code in JavaScript.
   */
  class InvalidConfig(
    message: String,
  ) : SSLPinningError(message)
}
