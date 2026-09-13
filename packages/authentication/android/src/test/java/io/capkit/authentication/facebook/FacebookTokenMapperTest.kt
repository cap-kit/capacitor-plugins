package io.capkit.authentication.facebook

import io.capkit.authentication.model.DisplayName
import io.capkit.authentication.model.SocialAuthResultUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file FacebookTokenMapperTest.kt
 * RED-GREEN contract for the pure Facebook token/profile mapping:
 * the four shared slots (`userId`, `grantedPermissions`,
 * `declinedPermissions`, `expiresAt` epoch-seconds) fill from SDK token primitives;
 * `idToken`/`authorizationCode`/`serverAuthCode`/`refreshToken` stay null for
 * facebook; `/me` maps to [SocialAuthResultUser] with a display-name string and the
 * `picture` URL; permission lists are trimmed/deduped (granted-only filtering at
 * profile time).
 */
class FacebookTokenMapperTest {
  @Test
  fun `expiry converts from SDK epoch millis to absolute epoch seconds by thousand`() {
    assertEquals(1700000123L, FacebookTokenMapper.expiresAtEpochSeconds(1700000123456L))
    assertEquals(0L, FacebookTokenMapper.expiresAtEpochSeconds(999L))
    assertNull(FacebookTokenMapper.expiresAtEpochSeconds(null))
  }

  @Test
  fun `mapToken fills the four shared slots and the access token`() {
    val bundle =
      FacebookTokenMapper.mapToken(
        accessToken = "fb-at-1",
        userId = "12345",
        grantedPermissions = listOf("public_profile", "email"),
        declinedPermissions = listOf("user_photos"),
        expiresAtMillis = 1700000123456L,
      )

    assertEquals("fb-at-1", bundle.accessToken)
    assertEquals("12345", bundle.userId)
    assertEquals(listOf("public_profile", "email"), bundle.grantedPermissions)
    assertEquals(listOf("user_photos"), bundle.declinedPermissions)
    assertEquals(1700000123L, bundle.expiresAt)
  }

  @Test
  fun `mapToken leaves non-applicable facebook fields null`() {
    val bundle =
      FacebookTokenMapper.mapToken(
        accessToken = "fb-at-1",
        userId = "12345",
        expiresAtMillis = 1700000000000L,
      )

    assertNull(bundle.idToken)
    assertNull(bundle.refreshToken)
    assertNull(bundle.authorizationCode)
    assertNull(bundle.serverAuthCode)
  }

  @Test
  fun `mapToken omits absent shared slots as null`() {
    val bundle = FacebookTokenMapper.mapToken(accessToken = "fb-at-1")

    assertNull(bundle.userId)
    assertNull(bundle.grantedPermissions)
    assertNull(bundle.declinedPermissions)
    assertNull(bundle.expiresAt)
  }

  @Test
  fun `sanitizePermissions trims drops blanks and dedupes preserving first occurrence`() {
    val permissions =
      FacebookTokenMapper.sanitizePermissions(
        listOf(" public_profile ", "email", "", "email", "  "),
      )
    assertEquals(listOf("public_profile", "email"), permissions)
  }

  @Test
  fun `sanitizePermissions returns null for null input and empty for empty input`() {
    assertNull(FacebookTokenMapper.sanitizePermissions(null))
    assertEquals(emptyList<String>(), FacebookTokenMapper.sanitizePermissions(emptyList()))
  }

  @Test
  fun `mapProfile maps granted me fields with display name and picture`() {
    val user =
      FacebookTokenMapper.mapProfile(
        id = "12345",
        name = "Grace Hopper",
        email = "grace@example.com",
        pictureUrl = "https://example.com/fb.jpg",
      )

    assertEquals("12345", user.id)
    assertEquals("grace@example.com", user.email)
    assertEquals(DisplayName("Grace Hopper"), user.name)
    assertEquals("https://example.com/fb.jpg", user.picture)
    assertNull(user.realUserStatus)
  }

  @Test
  fun `mapProfile omits absent fields and blank names`() {
    val user = FacebookTokenMapper.mapProfile(id = "12345")

    assertNull(user.email)
    assertNull(user.name)
    assertNull(user.picture)

    val blankName = FacebookTokenMapper.mapProfile(id = "12345", name = "   ")
    assertNull(blankName.name)
  }

  @Test
  fun `mapSignInResult combines token slots and the me profile into the persisted bundle`() {
    val bundle =
      FacebookTokenMapper.mapSignInResult(
        accessToken = "fb-at-1",
        userId = "12345",
        grantedPermissions = listOf("public_profile", "email"),
        expiresAtMillis = 1700000123456L,
        profile =
          SocialAuthResultUser(
            id = "12345",
            email = "grace@example.com",
            picture = "https://example.com/fb.jpg",
          ),
      )

    assertEquals("fb-at-1", bundle.accessToken)
    assertEquals("12345", bundle.userId)
    assertEquals(1700000123L, bundle.expiresAt)
    assertEquals("grace@example.com", bundle.user?.email)
    assertEquals("https://example.com/fb.jpg", bundle.user?.picture)
    assertNull(bundle.idToken)
    assertNull(bundle.refreshToken)
  }

  @Test
  fun `mapSignInResult attaches no user profile when me is absent`() {
    val bundle = FacebookTokenMapper.mapSignInResult(accessToken = "fb-at-1", userId = "12345")
    assertNull(bundle.user)
  }
}
