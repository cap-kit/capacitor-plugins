package io.capkit.authentication.facebook

import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.utils.ErrorCodeMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file FacebookErrorMapperTest.kt
 * RED-GREEN contract for the Facebook error mapper: cancellation
 * → `USER_CANCELLED`; missing SDK/config and the Android keyhash `1349094` →
 * actionable `INIT_FAILED`; Graph/network → `UNAVAILABLE`; blank/malformed → * `INVALID_INPUT`; `refreshToken('facebook')` → `INVALID_INPUT` with the exact
 * "Facebook issues no refresh token" message. Zero new codes — every result
 * resolves through the existing ten-code [ErrorCodeMapper].
 */
class FacebookErrorMapperTest {
  @Test
  fun `maps user cancellation to USER_CANCELLED`() {
    val error = FacebookErrorMapper.map(FacebookSignInError.USER_CANCELLED)
    assertTrue(error is AuthenticationError.UserCancelled)
    assertEquals("USER_CANCELLED", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps missing configuration to INIT_FAILED`() {
    val error = FacebookErrorMapper.map(FacebookSignInError.MISSING_CONFIGURATION)
    assertTrue(error is AuthenticationError.InitFailed)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps key hash mismatch to actionable INIT_FAILED`() {
    val error = FacebookErrorMapper.map(FacebookSignInError.KEY_HASH_MISMATCH)
    assertTrue(error is AuthenticationError.InitFailed)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(error))
    assertTrue(error.errorMessage.contains("verify key hash matches Facebook developer settings"))
  }

  @Test
  fun `maps graph and network failures to UNAVAILABLE`() {
    val error = FacebookErrorMapper.map(FacebookSignInError.GRAPH_NETWORK)
    assertTrue(error is AuthenticationError.Unavailable)
    assertEquals("UNAVAILABLE", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps malformed input to INVALID_INPUT`() {
    val error = FacebookErrorMapper.map(FacebookSignInError.MALFORMED_INPUT)
    assertTrue(error is AuthenticationError.InvalidInput)
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `maps refresh not supported to INVALID_INPUT with the exact message`() {
    val error = FacebookErrorMapper.map(FacebookSignInError.REFRESH_NOT_SUPPORTED)
    assertTrue(error is AuthenticationError.InvalidInput)
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(error))
    assertEquals("Facebook issues no refresh token", error.errorMessage)
  }

  @Test
  fun `maps unknown errors to INIT_FAILED`() {
    val error = FacebookErrorMapper.map(FacebookSignInError.UNKNOWN)
    assertTrue(error is AuthenticationError.InitFailed)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `sdk message containing the android key hash 1349094 detects a key hash mismatch`() {
    assertTrue(
      FacebookErrorMapper.isKeyHashMismatch(
        "Key hash 1349094 does not match any stored key hashes.",
      ),
    )
    assertFalse(FacebookErrorMapper.isKeyHashMismatch("Graph request error"))
    assertFalse(FacebookErrorMapper.isKeyHashMismatch(null))
  }

  @Test
  fun `raw sdk message with the key hash maps to actionable INIT_FAILED`() {
    val error =
      FacebookErrorMapper.mapSdkMessage(
        "Key hash 1349094 does not match any stored key hashes.",
      )
    assertTrue(error is AuthenticationError.InitFailed)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode(error))
    assertTrue(error.errorMessage.contains("verify key hash matches Facebook developer settings"))
  }

  @Test
  fun `blank or null sdk message maps to INVALID_INPUT`() {
    val blank = FacebookErrorMapper.mapSdkMessage("   ")
    val nullMessage = FacebookErrorMapper.mapSdkMessage(null)
    assertTrue(blank is AuthenticationError.InvalidInput)
    assertTrue(nullMessage is AuthenticationError.InvalidInput)
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(blank))
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(nullMessage))
  }

  @Test
  fun `generic sdk graph message maps to UNAVAILABLE`() {
    val error = FacebookErrorMapper.mapSdkMessage("Graph request error: invalid token")
    assertTrue(error is AuthenticationError.Unavailable)
    assertEquals("UNAVAILABLE", ErrorCodeMapper.mapCode(error))
  }

  @Test
  fun `every enum value maps through the ten-code mapper without new codes`() {
    val knownTenCodes =
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
    val mappedCodes = FacebookSignInError.entries.map { ErrorCodeMapper.mapCode(FacebookErrorMapper.map(it)) }

    assertEquals(FacebookSignInError.entries.size, mappedCodes.size)
    mappedCodes.forEach { code ->
      assertTrue("unexpected code $code", knownTenCodes.contains(code))
    }
  }
}
