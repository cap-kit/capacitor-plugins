package io.capkit.test

/**
 * Native error model for the Test plugin (Android).
 *
 * IMPORTANT:
 * - Extends Throwable to allow usage with Result.failure(...)
 * - Must NOT reference Capacitor or JavaScript
 * - Mapping to JS error codes happens ONLY in the Plugin layer
 */
sealed class TestError(
  message: String,
) : Throwable(message) {
  /** Feature or capability is not available on this device */
  class Unavailable(message: String) : TestError(message)

  /** Permission was denied by the user or system */
  class PermissionDenied(message: String) : TestError(message)

  /** Plugin failed to initialize correctly */
  class InitFailed(message: String) : TestError(message)

  /** Unsupported or unknown type/value was provided */
  class UnknownType(message: String) : TestError(message)
}
