package io.capkit.settings

/**
 * Native error model for the Settings plugin (Android).
 *
 * IMPORTANT:
 * - Extends Throwable to allow usage with Result.failure(...)
 * - Must NOT reference Capacitor or JavaScript
 * - Mapping to JS error codes happens ONLY in the Plugin layer
 */
sealed class SettingsError(
  message: String,
) : Throwable(message) {
  /** Feature or capability is not available on this device */
  class Unavailable(message: String) : SettingsError(message)

  /** Permission was denied by the user or system */
  class PermissionDenied(message: String) : SettingsError(message)

  /** Plugin failed to initialize correctly */
  class InitFailed(message: String) : SettingsError(message)

  /** Unsupported or unknown type/value was provided */
  class UnknownType(message: String) : SettingsError(message)
}
