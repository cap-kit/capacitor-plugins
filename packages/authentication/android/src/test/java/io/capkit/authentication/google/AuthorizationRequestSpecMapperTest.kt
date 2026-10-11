package io.capkit.authentication.google

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file AuthorizationRequestSpecMapperTest.kt
 * Pure-unit tests for the GMS `AuthorizationRequest` spec mapping (the
 * Capawesome-proven `Identity.getAuthorizationClient(...).authorize(...)` chain).
 */
class AuthorizationRequestSpecMapperTest {
  private val config =
    GoogleConfig(
      serverClientId = "web-client-id.apps.googleusercontent.com",
      scopes = listOf("openid", "email", "profile"),
    )

  @Test
  fun `buildSpec defaults scopes when config scopes are empty`() {
    val spec = AuthorizationRequestSpecMapper.buildSpec(config.copy(scopes = emptyList()))
    assertEquals(GoogleConfig.DEFAULT_SCOPES, spec?.scopes)
  }

  @Test
  fun `buildSpec preserves custom scopes`() {
    val customScopes = listOf("email", "https://www.googleapis.com/auth/youtube.readonly")
    val spec = AuthorizationRequestSpecMapper.buildSpec(config.copy(scopes = customScopes))
    assertEquals(customScopes, spec?.scopes)
  }

  @Test
  fun `buildSpec requests offline access with the server client id`() {
    val spec = AuthorizationRequestSpecMapper.buildSpec(config)
    assertEquals("web-client-id.apps.googleusercontent.com", spec?.offlineAccessClientId)
  }

  @Test
  fun `buildSpec returns null when the server client id is blank`() {
    assertNull(AuthorizationRequestSpecMapper.buildSpec(config.copy(serverClientId = "  ")))
  }
}

/**
 * @file AuthorizationPolicyTest.kt
 * Pure-unit tests for the authorization-step policy: when the Android sign-in
 * path MUST additionally run `AuthorizationClient.authorize` to surface the full
 * token set (access token + server auth code).
 */
class AuthorizationPolicyTest {
  private val config =
    GoogleConfig(
      serverClientId = "web-client-id.apps.googleusercontent.com",
      scopes = listOf("openid", "email", "profile"),
    )

  @Test
  fun `silent auto_select with default scopes keeps zero-ux idToken path`() {
    assertEquals(false, AuthorizationPolicy.shouldAuthorize(config, GoogleSignInMode.AUTO_SELECT))
  }

  @Test
  fun `full-button path always authorizes to surface the full token set`() {
    assertEquals(true, AuthorizationPolicy.shouldAuthorize(config, GoogleSignInMode.FULL_BUTTON))
  }

  @Test
  fun `any path with custom scopes authorizes`() {
    val customScopes =
      GoogleConfig(
        serverClientId = "web-client-id.apps.googleusercontent.com",
        scopes = listOf("https://www.googleapis.com/auth/youtube.readonly"),
      )
    assertEquals(true, AuthorizationPolicy.shouldAuthorize(customScopes, GoogleSignInMode.AUTO_SELECT))
  }

  @Test
  fun `full-button with custom scopes authorizes`() {
    val customScopes =
      GoogleConfig(
        serverClientId = "web-client-id.apps.googleusercontent.com",
        scopes = listOf("https://www.googleapis.com/auth/youtube.readonly"),
      )
    assertEquals(true, AuthorizationPolicy.shouldAuthorize(customScopes, GoogleSignInMode.FULL_BUTTON))
  }
}

/**
 * @file AuthorizationResultMapperTest.kt
 * Pure-unit tests for mapping an `AuthorizationResult` (or its launched-intent
 * recovery) onto the vaulted token bundle: payload normalization, resolution
 * decision and null-omit token merge.
 */
class AuthorizationResultMapperTest {
  @Test
  fun `extract preserves a fully populated payload`() {
    val payload = AuthorizationResultMapper.extract("ya29.access", "4/0.server", hasResolution = false)
    assertEquals("ya29.access", payload.accessToken)
    assertEquals("4/0.server", payload.serverAuthCode)
    assertEquals(false, payload.hasResolution)
  }

  @Test
  fun `extract nulls blank tokens`() {
    val payload = AuthorizationResultMapper.extract("  ", "", hasResolution = false)
    assertNull(payload.accessToken)
    assertNull(payload.serverAuthCode)
  }

  @Test
  fun `extract trims whitespace`() {
    val payload = AuthorizationResultMapper.extract("  ya29.access  ", " 4/0.server ", hasResolution = false)
    assertEquals("ya29.access", payload.accessToken)
    assertEquals("4/0.server", payload.serverAuthCode)
  }

  @Test
  fun `shouldLaunchResolution is driven by hasResolution flag`() {
    assertEquals(
      true,
      AuthorizationResultMapper.shouldLaunchResolution(
        AuthorizationResultMapper.extract(null, null, hasResolution = true),
      ),
    )
    assertEquals(
      false,
      AuthorizationResultMapper.shouldLaunchResolution(
        AuthorizationResultMapper.extract(null, null, hasResolution = false),
      ),
    )
  }

  @Test
  fun `mergeTokens writes access and server auth code preserving existing tokens`() {
    val existing =
      io.capkit.authentication.vault.TokenBundle(
        accessToken = null,
        refreshToken = "1//refresh",
        idToken = "header.payload.sig",
        serverAuthCode = null,
      )
    val payload = AuthorizationResultMapper.extract("ya29.access", "4/0.server", hasResolution = false)
    val merged = AuthorizationResultMapper.mergeTokens(existing, payload)
    assertEquals("ya29.access", merged.accessToken)
    assertEquals("4/0.server", merged.serverAuthCode)
    assertEquals("1//refresh", merged.refreshToken)
    assertEquals("header.payload.sig", merged.idToken)
  }

  @Test
  fun `mergeTokens keeps prior values when payload tokens are absent (null-omit semantics)`() {
    val existing =
      io.capkit.authentication.vault.TokenBundle(
        accessToken = "ya29.prior",
        serverAuthCode = "4/0.prior",
      )
    val payload = AuthorizationResultMapper.extract(null, null, hasResolution = false)
    val merged = AuthorizationResultMapper.mergeTokens(existing, payload)
    assertEquals("ya29.prior", merged.accessToken)
    assertEquals("4/0.prior", merged.serverAuthCode)
  }

  @Test
  fun `payload hasTokens is true only when a token is present`() {
    assertEquals(true, AuthorizationResultMapper.extract("ya29.access", null, hasResolution = false).hasTokens)
    assertEquals(false, AuthorizationResultMapper.extract(null, null, hasResolution = false).hasTokens)
  }
}
