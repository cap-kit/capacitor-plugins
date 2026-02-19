package io.capkit.sslpinning.error

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
  class Unavailable(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Required permission was denied or not granted.
   */
  class PermissionDenied(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Plugin failed to initialize or perform
   * a required operation.
   */
  class InitFailed(
    message: String,
  ) : SSLPinningError(message)

  /**
   * Invalid or unsupported input was provided.
   */
  class UnknownType(
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
}
