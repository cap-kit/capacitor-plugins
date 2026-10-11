package io.capkit.authentication.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file AppleAuthUrlBuilderTest.kt
 * RED-GREEN contract for building the Apple OAuth authorization URL:
 * `response_type=code id_token`, `response_mode=form_post`,
 * plus the fixed `client_id`, `redirect_uri`, `scope`, `state` and `nonce`. Pure
 * string building, no Android runtime deps.
 */
class AppleAuthUrlBuilderTest {
  @Test
  fun `builds authorize URL with code id_token and form_post response mode`() {
    val url =
      AppleAuthUrlBuilder.buildAuthUrl(
        clientId = "com.example.service",
        redirectUri = "https://app.example.com/oauth/callback",
        state = "st-1",
        nonce = "nc-1",
        scopes = listOf("name", "email"),
      )

    assertTrue(url.startsWith("https://appleid.apple.com/auth/authorize?"))
    assertTrue(url.contains("response_type=code%20id_token"))
    assertTrue(url.contains("response_mode=form_post"))
    assertTrue(url.contains("client_id=com.example.service"))
    assertTrue(url.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Foauth%2Fcallback"))
    assertTrue(url.contains("scope=name%20email"))
    assertTrue(url.contains("state=st-1"))
    assertTrue(url.contains("nonce=nc-1"))
  }

  @Test
  fun `custom scopes are joined with space and encoded`() {
    val url =
      AppleAuthUrlBuilder.buildAuthUrl(
        clientId = "com.example.service",
        redirectUri = "https://app.example.com/oauth/callback",
        state = "st-1",
        nonce = "nc-1",
        scopes = listOf("name"),
      )

    assertTrue(url.contains("scope=name"))
    assertTrue(!url.contains("scope=email"))
  }

  @Test
  fun `redirect uri is fully percent encoded`() {
    val url =
      AppleAuthUrlBuilder.buildAuthUrl(
        clientId = "com.example.service",
        redirectUri = "https://app.example.com:8443/oauth/cb?x=1&y=2",
        state = "st-1",
        nonce = "nc-1",
        scopes = listOf("name", "email"),
      )

    assertTrue(url.contains("redirect_uri=https%3A%2F%2Fapp.example.com%3A8443%2Foauth%2Fcb%3Fx%3D1%26y%3D2"))
  }

  @Test
  fun `state and nonce with special characters are encoded`() {
    val url =
      AppleAuthUrlBuilder.buildAuthUrl(
        clientId = "com.example.service",
        redirectUri = "https://app.example.com/oauth/callback",
        state = "st 1",
        nonce = "nc+1/=",
        scopes = listOf("name", "email"),
      )

    assertTrue(url.contains("state=st%201"))
    assertTrue(url.contains("nonce=nc%2B1%2F%3D"))
  }

  @Test
  fun `endpoint constant is the apple authorization host`() {
    assertEquals("https://appleid.apple.com/auth/authorize", AppleAuthUrlBuilder.AUTHORIZE_ENDPOINT)
  }
}
