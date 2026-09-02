package io.capkit.device.error

/**
 * Canonical error messages shared across platforms.
 * Keep these strings identical on iOS and Android.
 */
object ErrorMessages {
  const val UNAVAILABLE = "Feature is unavailable on this device or configuration."
  const val CANCELLED = "Operation was cancelled."
  const val PERMISSION_DENIED = "Required permission is denied or not granted."
  const val INIT_FAILED = "Native initialization failed."
  const val INVALID_INPUT = "Invalid or missing input parameters."
  const val UNKNOWN_TYPE = "Unsupported or invalid input type."
  const val NOT_FOUND = "Requested resource not found."
  const val CONFLICT = "Operation conflict with current state."
  const val TIMEOUT = "Operation timed out."
  const val INTERNAL_ERROR = "Internal error."
  const val UNEXPECTED_NATIVE_ERROR = "Unexpected native error in device operation."
}
