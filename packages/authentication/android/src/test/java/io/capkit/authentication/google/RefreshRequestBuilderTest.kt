package io.capkit.authentication.google

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file RefreshRequestBuilderTest.kt
 * RED-GREEN contract for the OAuth2 token-endpoint refresh body (OkHttp
 * transport on Android). `grant_type=refresh_token` with a public
 * `client_id` and the vaulted refresh token; values are form-encoded so tokens
 * containing reserved characters cannot corrupt the request.
 */
class RefreshRequestBuilderTest {
  @Test
  fun `builds refresh token grant body with client id`() {
    val body = RefreshRequestBuilder.build(clientId = "123.apps.googleusercontent.com", refreshToken = "rt-1")

    assertTrue(body.contains("grant_type=refresh_token"))
    assertTrue(body.contains("client_id=123.apps.googleusercontent.com"))
    assertTrue(body.contains("refresh_token=rt-1"))
  }

  @Test
  fun `form-encodes reserved characters in the refresh token`() {
    val body = RefreshRequestBuilder.build(clientId = "c", refreshToken = "a b&c=d+e%20f")

    assertEquals("grant_type=refresh_token&client_id=c&refresh_token=a+b%26c%3Dd%2Be%2520f", body)
  }

  @Test
  fun `form-encodes reserved characters in the client id`() {
    val body = RefreshRequestBuilder.build(clientId = "c&x=y", refreshToken = "rt")

    assertTrue(body.contains("client_id=c%26x%3Dy"))
  }
}
