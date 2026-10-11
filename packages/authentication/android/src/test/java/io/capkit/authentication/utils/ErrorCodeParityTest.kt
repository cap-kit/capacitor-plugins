package io.capkit.authentication.utils

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @file ErrorCodeParityTest.kt
 * Locks the 10-code error contract shared by Web, iOS and Android (oauth2-client /
 * social-auth-facade specs): every native [AuthenticationError] subtype maps to the
 * exact JS-facing `CustomError` code, and no code is invented outside the set.
 */
class ErrorCodeParityTest {
  @Test
  fun `maps user cancellation to USER_CANCELLED identically across platforms`() {
    assertEquals("USER_CANCELLED", ErrorCodeMapper.mapCode(AuthenticationError.UserCancelled("nope")))
  }

  @Test
  fun `maps every native error type to its standard code`() {
    assertEquals("UNAVAILABLE", ErrorCodeMapper.mapCode(AuthenticationError.Unavailable("u")))
    assertEquals("CANCELLED", ErrorCodeMapper.mapCode(AuthenticationError.Cancelled("c")))
    assertEquals("PERMISSION_DENIED", ErrorCodeMapper.mapCode(AuthenticationError.PermissionDenied("p")))
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(AuthenticationError.InitFailed("i")))
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(AuthenticationError.InvalidInput("v")))
    assertEquals("UNKNOWN_TYPE", ErrorCodeMapper.mapCode(AuthenticationError.UnknownType("t")))
    assertEquals("NOT_FOUND", ErrorCodeMapper.mapCode(AuthenticationError.NotFound("n")))
    assertEquals("CONFLICT", ErrorCodeMapper.mapCode(AuthenticationError.Conflict("f")))
    assertEquals("TIMEOUT", ErrorCodeMapper.mapCode(AuthenticationError.Timeout("o")))
  }

  @Test
  fun `error codes are exactly the ten known codes`() {
    val knownCodes =
      setOf(
        "USER_CANCELLED",
        "UNAVAILABLE",
        "CANCELLED",
        "PERMISSION_DENIED",
        "INIT_FAILED",
        "INVALID_INPUT",
        "UNKNOWN_TYPE",
        "NOT_FOUND",
        "CONFLICT",
        "TIMEOUT",
      )
    val mapped =
      listOf(
        AuthenticationError.UserCancelled("a"),
        AuthenticationError.Unavailable("a"),
        AuthenticationError.Cancelled("a"),
        AuthenticationError.PermissionDenied("a"),
        AuthenticationError.InitFailed("a"),
        AuthenticationError.InvalidInput("a"),
        AuthenticationError.UnknownType("a"),
        AuthenticationError.NotFound("a"),
        AuthenticationError.Conflict("a"),
        AuthenticationError.Timeout("a"),
      ).map(ErrorCodeMapper::mapCode).toSet()
    assertEquals(knownCodes, mapped)
  }

  @Test
  fun `state mismatch surface maps to INVALID_INPUT not a new code`() {
    // Web parity (web.ts): state mismatches reject with INVALID_INPUT. No
    // ERR_STATES_NOT_MATCH-like code may be invented on any platform.
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(AuthenticationError.InvalidInput("state_mismatch")))
  }
}
