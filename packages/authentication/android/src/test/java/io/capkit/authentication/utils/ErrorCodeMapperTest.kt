package io.capkit.authentication.utils

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JUnit4 tests for [ErrorCodeMapper].
 *
 * Enforces the cross-platform `CustomError` code parity required by the
 * oauth2-client and social-auth-facade specs (identical codes on Web/iOS/Android),
 * including the `USER_CANCELLED` code across all three platforms.
 */
class ErrorCodeMapperTest {
  @Test
  fun `user cancellation maps to USER_CANCELLED`() {
    assertEquals(
      "USER_CANCELLED",
      ErrorCodeMapper.mapCode(AuthenticationError.UserCancelled("user dismissed")),
    )
  }

  @Test
  fun `cancelled maps to CANCELLED`() {
    assertEquals("CANCELLED", ErrorCodeMapper.mapCode(AuthenticationError.Cancelled("cancelled")))
  }

  @Test
  fun `unavailable maps to UNAVAILABLE`() {
    assertEquals("UNAVAILABLE", ErrorCodeMapper.mapCode(AuthenticationError.Unavailable("n/a")))
  }

  @Test
  fun `invalid input maps to INVALID_INPUT`() {
    assertEquals(
      "INVALID_INPUT",
      ErrorCodeMapper.mapCode(AuthenticationError.InvalidInput("bad input")),
    )
  }

  @Test
  fun `init failure maps to INIT_FAILED`() {
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(AuthenticationError.InitFailed("boot")))
  }
}
