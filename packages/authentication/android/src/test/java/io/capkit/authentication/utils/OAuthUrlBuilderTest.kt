package io.capkit.authentication.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JUnit4 tests for [OAuthUrlBuilder].
 *
 * The Google OAuth authorization endpoint (`accounts.google.com/o/oauth2/v2/auth`)
 * redirect URL carries state + PKCE challenge and is validated per the google-provider
 * and oauth2-client specs.
 */
class OAuthUrlBuilderTest {
  private val endpoint = "https://accounts.google.com/o/oauth2/v2/auth"
  private val clientId = "1234-abcd.apps.googleusercontent.com"
  private val redirectUri = "https://app.example.com/callback"
  private val state = "state-abc123"
  private val codeChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

  @Test
  fun `buildAuthUrl carries all OAuth params and response_type code`() {
    val url =
      OAuthUrlBuilder.buildAuthUrl(
        endpoint = endpoint,
        clientId = clientId,
        redirectUri = redirectUri,
        state = state,
        codeChallenge = codeChallenge,
        scopes = listOf("openid", "email"),
      )

    assertTrue("must target the provided endpoint", url.startsWith(endpoint + "?"))
    assertTrue("must set response_type=code", url.contains("response_type=code"))
    assertTrue("must include client_id", url.contains("client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")))
    assertTrue(
      "must include redirect_uri",
      url.contains(
        "redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8"),
      ),
    )
    assertTrue("must include state", url.contains("state=state-abc123"))
    assertTrue("must include code_challenge", url.contains("code_challenge=$codeChallenge"))
    assertTrue("must declare S256 challenge method", url.contains("code_challenge_method=S256"))
  }

  @Test
  fun `buildAuthUrl encodes the space-separated scope`() {
    val url =
      OAuthUrlBuilder.buildAuthUrl(
        endpoint = endpoint,
        clientId = clientId,
        redirectUri = redirectUri,
        state = state,
        codeChallenge = codeChallenge,
        scopes = listOf("openid", "email"),
      )

    // Scope is space separated per OAuth; in a query string the space must be encoded.
    assertTrue("scopes must be present", url.contains("scope=openid%20email"))
  }

  @Test
  fun `different state yields a different url for the same endpoint`() {
    val withStateA =
      OAuthUrlBuilder.buildAuthUrl(
        endpoint = endpoint,
        clientId = clientId,
        redirectUri = redirectUri,
        state = "state-AAA",
        codeChallenge = codeChallenge,
        scopes = listOf("openid"),
      )
    val withStateB =
      OAuthUrlBuilder.buildAuthUrl(
        endpoint = endpoint,
        clientId = clientId,
        redirectUri = redirectUri,
        state = "state-BBB",
        codeChallenge = codeChallenge,
        scopes = listOf("openid"),
      )

    assertFalse("state must differentiate the urls", withStateA == withStateB)
    assertTrue(withStateA.contains("state=state-AAA"))
    assertTrue(withStateB.contains("state=state-BBB"))
  }

  @Test
  fun `empty scopes produce no scope parameter`() {
    val url =
      OAuthUrlBuilder.buildAuthUrl(
        endpoint = endpoint,
        clientId = clientId,
        redirectUri = redirectUri,
        state = state,
        codeChallenge = codeChallenge,
        scopes = emptyList(),
      )

    assertFalse("empty scopes should be omitted", url.contains("scope="))
  }
}
