package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.utils.ErrorCodeMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file AppleErrorMapperTest.kt
 * RED-GREEN contract for the Apple error mapper: sign-in cancellation maps to
 * `USER_CANCELLED`, nonce/state problems to `INVALID_INPUT`, missing
 * configuration and network/server failures to `INIT_FAILED` (mirroring the Web
 * reducer's semantics), and unknown strings to `INIT_FAILED`. No new error codes —
 * all results resolve through the existing 10-code [ErrorCodeMapper].
 */
class AppleErrorMapperTest {
  @Test
  fun `maps user cancellation to USER_CANCELLED`() {
    val error = AppleErrorMapper.map(AppleSignInError.USER_CANCELLED)
    assertTrue(error is AuthenticationError.UserCancelled)
    assertEquals("USER_CANCELLED", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps invalid nonce to INVALID_INPUT`() {
    val error = AppleErrorMapper.map(AppleSignInError.INVALID_NONCE)
    assertTrue(error is AuthenticationError.InvalidInput)
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps invalid state to INVALID_INPUT`() {
    val error = AppleErrorMapper.map(AppleSignInError.INVALID_STATE)
    assertTrue(error is AuthenticationError.InvalidInput)
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps missing configuration to INIT_FAILED`() {
    val error = AppleErrorMapper.map(AppleSignInError.MISSING_CONFIGURATION)
    assertTrue(error is AuthenticationError.InitFailed)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps network and server failures to INIT_FAILED`() {
    val network = AppleErrorMapper.map(AppleSignInError.NETWORK)
    assertTrue(network is AuthenticationError.InitFailed)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(network))
  }

  @Test
  fun `maps unknown errors to INIT_FAILED`() {
    val error = AppleErrorMapper.map(AppleSignInError.UNKNOWN)
    assertTrue(error is AuthenticationError.InitFailed)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps raw sdk error strings onto the ten-code set`() {
    val cancelStrings =
      listOf("popup_closed_by_user", "user_cancelled_authorize", "access_denied")
    cancelStrings.forEach { sdkCode ->
      val mapped = AppleErrorMapper.mapSdkError(sdkCode, "user dismissed")
      assertEquals("USER_CANCELLED", ErrorCodeMapper.mapCode(mapped))
    }

    val invalidNonce = AppleErrorMapper.mapSdkError("invalid_nonce", "nonce mismatch")
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(invalidNonce))

    val unknown = AppleErrorMapper.mapSdkError("server_error", "server rejected")
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(unknown))
  }

  @Test
  fun `every enum value maps through the ten-code mapper without new codes`() {
    val mappedCodes = AppleSignInError.entries.map { ErrorCodeMapper.mapCode(AppleErrorMapper.map(it)) }
    val allowedCodes =
      setOf(
        "UNAVAILABLE",
        "CANCELLED",
        "USER_CANCELLED",
        "PERMISSION_DENIED",
        "INIT_FAILED",
        "INVALID_INPUT",
        "UNKNOWN_TYPE",
        "NOT_FOUND",
        "CONFLICT",
        "TIMEOUT",
      )
    mappedCodes.forEach { code ->
      assertTrue("unexpected code $code", allowedCodes.contains(code))
    }
    assertEquals(AppleSignInError.entries.size, mappedCodes.size)
  }
}
