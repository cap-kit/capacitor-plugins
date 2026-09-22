package io.capkit.authentication.google

import io.capkit.authentication.utils.ErrorCodeMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file RefreshResponseParserTest.kt
 * RED-GREEN contract for the OAuth2 token-endpoint response (oauth2-client spec,
 * web parity): a JSON `access_token` is required; a token-endpoint `error` of
 * `access_denied`/`user_cancelled` maps to USER_CANCELLED; any other failure maps
 * to INIT_FAILED; malformed bodies are never silently passed through.
 */
class RefreshResponseParserTest {
  @Test
  fun `parses a full refresh success payload`() {
    val result =
      RefreshResponseParser.parse(
        """{"access_token":"at-2","expires_in":3600,"id_token":"id-2"}""",
      )

    assertTrue(result is RefreshResponseParser.Result.Success)
    val success = result as RefreshResponseParser.Result.Success
    assertEquals("at-2", success.accessToken)
    assertEquals(3600L, success.expiresIn)
    assertEquals("id-2", success.idToken)
  }

  @Test
  fun `parses a success payload without optional fields`() {
    val result = RefreshResponseParser.parse("""{"access_token":"at-2"}""")

    assertTrue(result is RefreshResponseParser.Result.Success)
    val success = result as RefreshResponseParser.Result.Success
    assertEquals("at-2", success.accessToken)
    assertEquals(null, success.idToken)
  }

  @Test
  fun `maps access_denied endpoint error to USER_CANCELLED`() {
    val result = RefreshResponseParser.parse("""{"error":"access_denied"}""")
    assertRejectsWithUserCancelled(result)
  }

  @Test
  fun `maps user_cancelled endpoint error to USER_CANCELLED`() {
    val result = RefreshResponseParser.parse("""{"error":"user_cancelled"}""")
    assertRejectsWithUserCancelled(result)
  }

  @Test
  fun `maps other endpoint errors to INIT_FAILED`() {
    val result = RefreshResponseParser.parse("""{"error":"invalid_grant"}""")
    assertTrue(result is RefreshResponseParser.Result.Error)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode((result as RefreshResponseParser.Result.Error).error))
  }

  @Test
  fun `rejects a response without an access token as INIT_FAILED`() {
    val result = RefreshResponseParser.parse("""{"expires_in":3600}""")
    assertTrue(result is RefreshResponseParser.Result.Error)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode((result as RefreshResponseParser.Result.Error).error))
  }

  @Test
  fun `rejects malformed bodies as INIT_FAILED`() {
    val result = RefreshResponseParser.parse("not-json")
    assertTrue(result is RefreshResponseParser.Result.Error)
    assertEquals("INIT_FAILED", ErrorCodeMapper.mapCode((result as RefreshResponseParser.Result.Error).error))
  }

  private fun assertRejectsWithUserCancelled(result: RefreshResponseParser.Result) {
    assertTrue(result is RefreshResponseParser.Result.Error)
    assertEquals("USER_CANCELLED", ErrorCodeMapper.mapCode((result as RefreshResponseParser.Result.Error).error))
  }
}
