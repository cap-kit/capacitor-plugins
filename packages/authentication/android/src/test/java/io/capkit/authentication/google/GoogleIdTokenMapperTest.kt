package io.capkit.authentication.google

import io.capkit.authentication.vault.TokenBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file GoogleIdTokenMapperTest.kt
 * RED-GREEN contract for merging a modern Credential-Manager sign-in result
 * (idToken-only) over the current vault state. Access/server-auth-code are no
 * longer exposed by Google's Credential Manager (googleid 1.2.x); the merge
 * preserves previously vaulted refresh/server-auth-code material across re-sign-ins.
 */
class GoogleIdTokenMapperTest {
  @Test
  fun `first sign-in stores only the id token`() {
    val merged = GoogleIdTokenMapper.mergeSignInResult(existing = null, idToken = "id-1")

    assertEquals(TokenBundle(accessToken = null, idToken = "id-1"), merged)
    assertNull(merged.refreshToken)
    assertNull(merged.serverAuthCode)
  }

  @Test
  fun `re sign-in preserves previously vaulted refresh token and server auth code`() {
    val existing = TokenBundle(accessToken = "at-old", refreshToken = "rt-old", serverAuthCode = "sac-old")
    val merged = GoogleIdTokenMapper.mergeSignInResult(existing = existing, idToken = "id-new")

    assertEquals("rt-old", merged.refreshToken)
    assertEquals("sac-old", merged.serverAuthCode)
    assertEquals("id-new", merged.idToken)
  }

  @Test
  fun `blank id token keeps the previous id token`() {
    val existing = TokenBundle(accessToken = null, idToken = "id-old")
    val merged = GoogleIdTokenMapper.mergeSignInResult(existing = existing, idToken = "  ")

    assertEquals("id-old", merged.idToken)
  }

  @Test
  fun `access token survives when the vault already has one`() {
    val existing = TokenBundle(accessToken = "at-kept", refreshToken = "rt-1")
    val merged = GoogleIdTokenMapper.mergeSignInResult(existing = existing, idToken = "id-1")

    assertEquals("at-kept", merged.accessToken)
  }
}
