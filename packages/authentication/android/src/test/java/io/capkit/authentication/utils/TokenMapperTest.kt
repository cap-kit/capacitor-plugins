package io.capkit.authentication.utils

import io.capkit.authentication.model.NameParts
import io.capkit.authentication.model.SocialAuthResultUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * JUnit4 tests for [TokenMapper].
 *
 * Maps the platform token fields into the normalized token bundle shape that
 * mirrors the TypeScript `OAuthTokenSet` (optional access/refresh/id/code).
 * Pure and side-effect free.
 */
class TokenMapperTest {
  @Test
  fun `full token set maps all four normalized fields`() {
    val mapped =
      TokenMapper.map(
        accessToken = "at-1",
        refreshToken = "rt-1",
        idToken = "idt-1",
        serverAuthCode = "sac-1",
      )

    assertEquals(4, mapped.size)
    assertEquals("at-1", mapped["accessToken"])
    assertEquals("rt-1", mapped["refreshToken"])
    assertEquals("idt-1", mapped["idToken"])
    assertEquals("sac-1", mapped["serverAuthCode"])
  }

  @Test
  fun `map omits optional fields that are absent`() {
    val mapped = TokenMapper.map(accessToken = "only-access")

    assertEquals(1, mapped.size)
    assertEquals("only-access", mapped["accessToken"])
    assertFalse(mapped.containsKey("refreshToken"))
    assertFalse(mapped.containsKey("idToken"))
    assertFalse(mapped.containsKey("serverAuthCode"))
  }

  @Test
  fun `map distinguishes differently valued fields`() {
    val first = TokenMapper.map(accessToken = "at-a", idToken = "id-a")
    val second = TokenMapper.map(accessToken = "at-b", idToken = "id-b")

    assertEquals("at-a", first["accessToken"])
    assertEquals("at-b", second["accessToken"])
    assertEquals("id-a", first["idToken"])
    assertEquals("id-b", second["idToken"])
  }

  @Test
  fun `apple token set maps authorizationCode and user`() {
    val mapped =
      TokenMapper.map(
        accessToken = "at-1",
        authorizationCode = "apple-auth-code-1",
        user =
          SocialAuthResultUser(
            id = "001234.abcd",
            email = "ada@example.com",
            name = NameParts(firstName = "Ada", lastName = "Lovelace"),
          ),
      )

    assertEquals("apple-auth-code-1", mapped["authorizationCode"])
    assertEquals(
      SocialAuthResultUser(
        id = "001234.abcd",
        email = "ada@example.com",
        name = NameParts(firstName = "Ada", lastName = "Lovelace"),
      ),
      mapped["user"],
    )
  }

  @Test
  fun `map omits authorizationCode and user when absent`() {
    val mapped = TokenMapper.map(accessToken = "only-access")

    assertEquals(1, mapped.size)
    assertEquals("only-access", mapped["accessToken"])
    assertFalse(mapped.containsKey("authorizationCode"))
    assertFalse(mapped.containsKey("user"))
  }

  // MARK: - facebook shared slots (homogeneous shape)

  @Test
  fun `map includes the four facebook shared slots when present`() {
    val mapped =
      TokenMapper.map(
        accessToken = "fb-at-1",
        userId = "12345",
        grantedPermissions = listOf("public_profile", "email"),
        declinedPermissions = listOf("user_photos"),
        expiresAt = 1700000000L,
      )

    assertEquals("12345", mapped["userId"])
    assertEquals(listOf("public_profile", "email"), mapped["grantedPermissions"])
    assertEquals(listOf("user_photos"), mapped["declinedPermissions"])
    assertEquals(1700000000L, mapped["expiresAt"])
  }

  @Test
  fun `map omits the four shared slots when absent (google and apple stay null)`() {
    val mapped = TokenMapper.map(accessToken = "at-1")

    assertEquals(1, mapped.size)
    assertFalse(mapped.containsKey("userId"))
    assertFalse(mapped.containsKey("grantedPermissions"))
    assertFalse(mapped.containsKey("declinedPermissions"))
    assertFalse(mapped.containsKey("expiresAt"))
  }

  @Test
  fun `map carries the facebook picture profile url through the user profile`() {
    val mapped =
      TokenMapper.map(
        accessToken = "fb-at-1",
        userId = "12345",
        user =
          SocialAuthResultUser(
            id = "12345",
            email = "grace@example.com",
            picture = "https://example.com/fb.jpg",
          ),
      )

    assertEquals(
      SocialAuthResultUser(
        id = "12345",
        email = "grace@example.com",
        picture = "https://example.com/fb.jpg",
      ),
      mapped["user"],
    )
  }
}
