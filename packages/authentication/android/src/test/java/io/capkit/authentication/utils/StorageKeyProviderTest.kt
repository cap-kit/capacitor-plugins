package io.capkit.authentication.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JUnit4 tests for [StorageKeyProvider].
 *
 * The secure vault must namespace token keys per provider so tokens never
 * leak across providers (secure-token-storage / no-raw-exposure). This slice
 * defines the pure key-derivation contract only.
 */
class StorageKeyProviderTest {
  @Test
  fun `access key is namespaced by provider`() {
    assertEquals(
      "auth_google_access",
      StorageKeyProvider.storageKey(provider = "google", kind = TokenKind.ACCESS),
    )
  }

  @Test
  fun `refresh key is namespaced by provider`() {
    assertEquals(
      "auth_google_refresh",
      StorageKeyProvider.storageKey(provider = "google", kind = TokenKind.REFRESH),
    )
  }

  @Test
  fun `different providers never share a key space`() {
    val google = StorageKeyProvider.storageKey(provider = "google", kind = TokenKind.ACCESS)
    val microsoft = StorageKeyProvider.storageKey(provider = "microsoft", kind = TokenKind.ACCESS)

    assertFalse("provider keyspaces must be isolated", google == microsoft)
  }

  @Test
  fun `every token kind yields a distinct key for the same provider`() {
    val keys = TokenKind.entries.map { StorageKeyProvider.storageKey("google", it) }.toSet()

    assertEquals(TokenKind.entries.size, keys.size)
    assertTrue(keys.contains("auth_google_server_auth_code"))
  }

  @Test
  fun `authorization code key resolves for the apple provider`() {
    assertEquals(
      "auth_apple_authorization_code",
      StorageKeyProvider.storageKey(provider = "apple", kind = TokenKind.AUTHORIZATION_CODE),
    )
  }

  @Test
  fun `user key resolves for any provider`() {
    assertEquals(
      "auth_custom_user",
      StorageKeyProvider.storageKey(provider = "custom", kind = TokenKind.USER),
    )
  }

  @Test
  fun `facebook shared-slot kinds resolve to auth_facebook suffixes`() {
    assertEquals(
      "auth_facebook_user_id",
      StorageKeyProvider.storageKey(provider = "facebook", kind = TokenKind.USER_ID),
    )
    assertEquals(
      "auth_facebook_granted_permissions",
      StorageKeyProvider.storageKey(provider = "facebook", kind = TokenKind.GRANTED_PERMISSIONS),
    )
    assertEquals(
      "auth_facebook_declined_permissions",
      StorageKeyProvider.storageKey(provider = "facebook", kind = TokenKind.DECLINED_PERMISSIONS),
    )
    assertEquals(
      "auth_facebook_expires_at",
      StorageKeyProvider.storageKey(provider = "facebook", kind = TokenKind.EXPIRES_AT),
    )
  }

  @Test
  fun `the four facebook slot kinds stay distinct from existing kinds`() {
    val facebook = TokenKind.entries.map { StorageKeyProvider.storageKey("facebook", it) }.toSet()
    assertEquals(TokenKind.entries.size, facebook.size)
    assertTrue(facebook.contains("auth_facebook_user_id"))
    assertTrue(facebook.contains("auth_facebook_granted_permissions"))
    assertTrue(facebook.contains("auth_facebook_declined_permissions"))
    assertTrue(facebook.contains("auth_facebook_expires_at"))
  }
}
