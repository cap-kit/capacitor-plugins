package io.capkit.fortress.error

/**
 * Canonical error messages shared across platforms.
 * These strings should remain byte-identical on iOS and Android
 * whenever they represent the same failure condition.
 */
object ErrorMessages {
  const val UNAVAILABLE = "Feature is unavailable on this device or configuration."
  const val CANCELLED = "Operation was cancelled by the user."
  const val PERMISSION_DENIED = "Required permission is denied."
  const val INIT_FAILED = "Native initialization failed."
  const val INVALID_INPUT = "Invalid input provided."
  const val NOT_FOUND = "Requested resource not found."
  const val CONFLICT = "Operation conflicts with current vault state."
  const val TIMEOUT = "Operation timed out."
  const val SECURITY_VIOLATION = "Security validation failed."
  const val VAULT_LOCKED = "Vault is locked."
  const val INTERNAL_ERROR = "Internal error."
  const val UNEXPECTED_NATIVE_ERROR = "Unexpected native error."
}
