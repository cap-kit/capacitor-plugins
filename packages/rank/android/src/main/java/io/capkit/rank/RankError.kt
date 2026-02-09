package io.capkit.rank

/**
 * Native error model for the Rank plugin (Android).
 *
 * Architectural rules:
 * - Must NOT reference Capacitor APIs
 * - Must NOT reference JavaScript
 * - Must be throwable from the Impl layer
 * - Mapping to JS-facing error codes happens ONLY in the Plugin layer
 */
sealed class RankError(
  message: String,
) : Throwable(message) {
  /**
   * Feature or capability is not available
   * due to device or configuration limitations.
   */
  class Unavailable(message: String) :
    RankError(message)

  /**
   * Required permission was denied or not granted.
   */
  class PermissionDenied(message: String) :
    RankError(message)

  /**
   * Plugin failed to initialize or perform
   * a required operation.
   */
  class InitFailed(message: String) :
    RankError(message)

  /**
   * Invalid or unsupported input was provided.
   */
  class UnknownType(message: String) :
    RankError(message)
}
