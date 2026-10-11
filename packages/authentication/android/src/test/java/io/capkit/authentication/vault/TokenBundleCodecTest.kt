package io.capkit.authentication.vault

import io.capkit.authentication.model.DisplayName
import io.capkit.authentication.model.NameParts
import io.capkit.authentication.model.SocialAuthResultUser
import io.capkit.authentication.utils.StorageKeyProvider
import io.capkit.authentication.utils.TokenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file TokenBundleCodecTest.kt
 * RED-GREEN contract for the secure-token-storage codec: the vault stores exactly
 * the per-provider `auth_{provider}_{kind}` keys derived by [StorageKeyProvider],
 * absent optional fields are never persisted, and a bundle without an access token
 * decodes as "no credential" (`null`) rather than a partial bundle.
 */
class TokenBundleCodecTest {
  private val full =
    TokenBundle(
      accessToken = "at-1",
      refreshToken = "rt-1",
      idToken = "id-1",
      serverAuthCode = "sac-1",
    )

  @Test
  fun `encode writes all four tokens under the StorageKeyProvider keys`() {
    val encoded = TokenBundleCodec.encode("google", full)

    assertEquals(4, encoded.size)
    assertEquals("at-1", encoded[StorageKeyProvider.storageKey("google", TokenKind.ACCESS)])
    assertEquals("rt-1", encoded[StorageKeyProvider.storageKey("google", TokenKind.REFRESH)])
    assertEquals("id-1", encoded[StorageKeyProvider.storageKey("google", TokenKind.ID)])
    assertEquals("sac-1", encoded[StorageKeyProvider.storageKey("google", TokenKind.SERVER_AUTH_CODE)])
  }

  @Test
  fun `encode omits absent optional fields`() {
    val encoded = TokenBundleCodec.encode("google", TokenBundle(accessToken = "at-1"))

    assertEquals(1, encoded.size)
    assertEquals("at-1", encoded[StorageKeyProvider.storageKey("google", TokenKind.ACCESS)])
  }

  @Test
  fun `decode round-trips the full bundle`() {
    val decoded = TokenBundleCodec.decode("google", TokenBundleCodec.encode("google", full))

    assertNotNull(decoded)
    assertEquals(full, decoded)
  }

  @Test
  fun `decode returns null when the access token key is absent`() {
    val map =
      mapOf(
        StorageKeyProvider.storageKey("google", TokenKind.REFRESH) to "rt-1",
        StorageKeyProvider.storageKey("google", TokenKind.ID) to "id-1",
      )
    assertNull(TokenBundleCodec.decode("google", map))
  }

  @Test
  fun `decode ignores unknown keys and missing optional fields`() {
    val map =
      mapOf(
        StorageKeyProvider.storageKey("google", TokenKind.ACCESS) to "at-1",
        "unrelated_key" to "junk",
      )
    val decoded = TokenBundleCodec.decode("google", map)

    assertNotNull(decoded)
    assertEquals(TokenBundle(accessToken = "at-1"), decoded)
  }

  @Test
  fun `keys are namespaced per provider so tokens never bleed between providers`() {
    val google = TokenBundleCodec.encode("google", full)
    val other = TokenBundleCodec.encode("credentials", full)

    google.keys.forEach { assertNull(other[it]) }
    assertEquals(
      google[StorageKeyProvider.storageKey("google", TokenKind.ACCESS)],
      other[StorageKeyProvider.storageKey("credentials", TokenKind.ACCESS)],
    )
  }

  @Test
  fun `decode treats blank access token value as missing credential`() {
    val map =
      mapOf(
        StorageKeyProvider.storageKey("google", TokenKind.ACCESS) to "   ",
      )
    val decoded = TokenBundleCodec.decode("google", map)
    assertNull(decoded)
  }

  @Test
  fun `encode writes apple authorizationCode and user under the two new token kinds`() {
    val apple =
      TokenBundle(
        accessToken = "at-1",
        authorizationCode = "apple-auth-code-1",
        user =
          SocialAuthResultUser(
            id = "001234.abcd",
            email = "ada@example.com",
            name = NameParts(firstName = "Ada", lastName = "Lovelace"),
          ),
      )

    val encoded = TokenBundleCodec.encode("apple", apple)

    assertEquals(
      "apple-auth-code-1",
      encoded[StorageKeyProvider.storageKey("apple", TokenKind.AUTHORIZATION_CODE)],
    )
    assertNotNull(encoded[StorageKeyProvider.storageKey("apple", TokenKind.USER)])
  }

  @Test
  fun `decode round-trips an apple bundle with authorizationCode and user`() {
    val apple =
      TokenBundle(
        accessToken = "at-1",
        authorizationCode = "apple-auth-code-1",
        user =
          SocialAuthResultUser(
            id = "001234.abcd",
            email = "ada@example.com",
            name = NameParts(firstName = "Ada", lastName = "Lovelace"),
          ),
      )

    val decoded = TokenBundleCodec.decode("apple", TokenBundleCodec.encode("apple", apple))

    assertNotNull(decoded)
    assertEquals(apple, decoded)
  }

  @Test
  fun `apple bundle round-trip writes no refresh key`() {
    val apple = TokenBundle(accessToken = "at-1", authorizationCode = "apple-auth-code-1")

    val encoded = TokenBundleCodec.encode("apple", apple)
    val decoded = TokenBundleCodec.decode("apple", encoded)

    assertFalse(encoded.containsKey(StorageKeyProvider.storageKey("apple", TokenKind.REFRESH)))
    assertNotNull(decoded)
    assertNull(decoded?.refreshToken)
    assertEquals("apple-auth-code-1", decoded?.authorizationCode)
    assertNull(decoded?.user)
  }

  @Test
  fun `decode of a legacy google vault map yields null optional fields`() {
    val legacy =
      mapOf(
        StorageKeyProvider.storageKey("google", TokenKind.ACCESS) to "at-1",
      )

    val decoded = TokenBundleCodec.decode("google", legacy)

    assertNotNull(decoded)
    assertEquals(TokenBundle(accessToken = "at-1"), decoded)
    assertNull(decoded?.authorizationCode)
    assertNull(decoded?.user)
  }

  // MARK: - facebook shared slots (secure-token-storage round-trip)

  @Test
  fun `encode writes facebook shared slots under their storage keys`() {
    val facebook =
      TokenBundle(
        accessToken = "fb-at-1",
        userId = "12345",
        grantedPermissions = listOf("public_profile", "email"),
        declinedPermissions = listOf("user_photos"),
        expiresAt = 1700000000L,
      )

    val encoded = TokenBundleCodec.encode("facebook", facebook)

    assertEquals(
      "12345",
      encoded[StorageKeyProvider.storageKey("facebook", TokenKind.USER_ID)],
    )
    assertEquals(
      "[\"public_profile\",\"email\"]",
      encoded[StorageKeyProvider.storageKey("facebook", TokenKind.GRANTED_PERMISSIONS)],
    )
    assertEquals(
      "[\"user_photos\"]",
      encoded[StorageKeyProvider.storageKey("facebook", TokenKind.DECLINED_PERMISSIONS)],
    )
    assertEquals(
      "1700000000",
      encoded[StorageKeyProvider.storageKey("facebook", TokenKind.EXPIRES_AT)],
    )
  }

  @Test
  fun `decode round-trips the four facebook shared slots`() {
    val facebook =
      TokenBundle(
        accessToken = "fb-at-1",
        userId = "12345",
        grantedPermissions = listOf("public_profile", "email"),
        declinedPermissions = listOf("user_photos"),
        expiresAt = 1700000000L,
      )

    val decoded = TokenBundleCodec.decode("facebook", TokenBundleCodec.encode("facebook", facebook))

    assertNotNull(decoded)
    assertEquals(facebook, decoded)
    assertEquals("12345", decoded?.userId)
    assertEquals(listOf("public_profile", "email"), decoded?.grantedPermissions)
    assertEquals(listOf("user_photos"), decoded?.declinedPermissions)
    assertEquals(1700000000L, decoded?.expiresAt)
  }

  @Test
  fun `pre-existing google bytes decode with new slots null and picture absent`() {
    // Pre-change google bundle encoded with ONLY the original kinds (no facebook slots).
    val legacy =
      mapOf(
        StorageKeyProvider.storageKey("google", TokenKind.ACCESS) to "at-1",
      )

    val decoded = TokenBundleCodec.decode("google", legacy)

    assertNotNull(decoded)
    assertEquals(TokenBundle(accessToken = "at-1"), decoded)
    assertNull(decoded?.userId)
    assertNull(decoded?.grantedPermissions)
    assertNull(decoded?.declinedPermissions)
    assertNull(decoded?.expiresAt)
    assertNull(decoded?.user?.picture)
  }

  @Test
  fun `re-encoding legacy google bytes omits the new null slots`() {
    val legacy =
      mapOf(
        StorageKeyProvider.storageKey("google", TokenKind.ACCESS) to "at-1",
      )
    val decoded = TokenBundleCodec.decode("google", legacy)

    val reEncoded = TokenBundleCodec.encode("google", decoded!!)

    // No facebook-slot keys present — bytes remain decode-compatible, no migration.
    assertFalse(reEncoded.containsKey(StorageKeyProvider.storageKey("google", TokenKind.USER_ID)))
    assertFalse(reEncoded.containsKey(StorageKeyProvider.storageKey("google", TokenKind.GRANTED_PERMISSIONS)))
    assertFalse(reEncoded.containsKey(StorageKeyProvider.storageKey("google", TokenKind.DECLINED_PERMISSIONS)))
    assertFalse(reEncoded.containsKey(StorageKeyProvider.storageKey("google", TokenKind.EXPIRES_AT)))
    assertEquals("at-1", reEncoded[StorageKeyProvider.storageKey("google", TokenKind.ACCESS)])
  }

  @Test
  fun `decode round-trips a google bundle with picture on the user profile`() {
    val google =
      TokenBundle(
        accessToken = "at-1",
        user =
          SocialAuthResultUser(
            id = "g-123",
            email = "grace@example.com",
            picture = "https://example.com/g.jpg",
          ),
      )

    val decoded = TokenBundleCodec.decode("google", TokenBundleCodec.encode("google", google))

    assertNotNull(decoded)
    assertEquals("https://example.com/g.jpg", decoded?.user?.picture)
    assertNull(decoded?.userId)
  }

  @Test
  fun `decode round-trips name parts and display name variants distinctly`() {
    val parts =
      TokenBundle(
        accessToken = "at-1",
        user = SocialAuthResultUser(name = NameParts(firstName = "Ada", lastName = "Lovelace")),
      )
    val display =
      TokenBundle(
        accessToken = "at-1",
        user = SocialAuthResultUser(name = DisplayName("Ada Lovelace")),
      )

    val partsDecoded = TokenBundleCodec.decode("apple", TokenBundleCodec.encode("apple", parts))
    val displayDecoded = TokenBundleCodec.decode("apple", TokenBundleCodec.encode("apple", display))

    assertNotNull(partsDecoded)
    assertNotNull(displayDecoded)
    assertEquals(parts, partsDecoded)
    assertEquals(display, displayDecoded)
    assertNotEquals(partsDecoded?.user?.name, displayDecoded?.user?.name)
  }
}
