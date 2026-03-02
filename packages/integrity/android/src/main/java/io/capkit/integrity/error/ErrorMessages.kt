package io.capkit.integrity.error

/**
 * Canonical error messages shared across platforms.
 * These strings should remain byte-identical on iOS and Android
 * whenever they represent the same failure condition.
 */
object ErrorMessages {
  const val UNAVAILABLE = "Feature is unavailable on this device or configuration."
  const val PERMISSION_DENIED = "Required permission is denied or not granted."
  const val INIT_FAILED = "Native initialization failed."
  const val UNKNOWN_TYPE = "Unsupported or invalid input type."

  const val TIMEOUT = "Timeout"
  const val NETWORK_ERROR = "Network error"
  const val INTERNAL_ERROR = "Internal error"
  const val UNEXPECTED_NATIVE_ERROR = "Unexpected native error during integrity check."
}
