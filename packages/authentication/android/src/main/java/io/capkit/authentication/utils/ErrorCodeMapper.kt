package io.capkit.authentication.utils

import io.capkit.authentication.error.AuthenticationError

/**
 * Maps native [AuthenticationError] instances to the standardized JS-facing
 * `CustomError` code strings.
 *
 * This is the single source of truth for code parity across the three platforms:
 * the same underlying failure must report the same code on Web, iOS and Android
 * (oauth2-client / social-auth-facade specs). Pure and side-effect free.
 */
object ErrorCodeMapper {
  /**
   * Returns the standardized error code for a native error.
   *
   * @param error the native [AuthenticationError] thrown by the implementation.
   * @return the JS-facing code, including `USER_CANCELLED` for user cancellations.
   */
  fun mapCode(error: AuthenticationError): String =
    when (error) {
      is AuthenticationError.UserCancelled -> "USER_CANCELLED"
      is AuthenticationError.Unavailable -> "UNAVAILABLE"
      is AuthenticationError.Cancelled -> "CANCELLED"
      is AuthenticationError.PermissionDenied -> "PERMISSION_DENIED"
      is AuthenticationError.InitFailed -> "INIT_FAILED"
      is AuthenticationError.InvalidInput -> "INVALID_INPUT"
      is AuthenticationError.UnknownType -> "UNKNOWN_TYPE"
      is AuthenticationError.NotFound -> "NOT_FOUND"
      is AuthenticationError.Conflict -> "CONFLICT"
      is AuthenticationError.Timeout -> "TIMEOUT"
    }
}
