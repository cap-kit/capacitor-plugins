package io.capkit.authentication.vault

import io.capkit.authentication.model.SocialAuthResultUser
import io.capkit.authentication.utils.StorageKeyProvider
import io.capkit.authentication.utils.TokenKind
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * @file TokenBundleCodec.kt
 * Pure, side-effect-free mapping between a [TokenBundle] and the per-provider
 * storage keys derived by [StorageKeyProvider] (`auth_{provider}_{kind}`).
 *
 * Contract (secure-token-storage spec):
 * - Absent optional fields are never persisted (omitted keys).
 * - A bundle without a nullable non-blank access token decodes as `null` — there is
 *   no partial "credential-like" state.
 * - Keys are provider-namespaced; tokens never bleed between providers.
 * - The provider-agnostic `authorizationCode`/`user` fields round-trip under the
 *   `AUTHORIZATION_CODE`/`USER` kinds; the `user` profile is stored as its JSON
 *   serialization under a single key. No migration: legacy vaults without the new
 *   keys decode with the optional fields `null` (`user` JSON corruption also
 *   degrades to `null` rather than failing the credential read).
 */
object TokenBundleCodec {
  private val vaultJson =
    Json {
      ignoreUnknownKeys = true
      encodeDefaults = false
      explicitNulls = false
    }

  /**
   * Encodes a [TokenBundle] into the vault key/value map for the provider.
   *
   * @param provider the provider key (e.g. `google`, `apple`).
   * @param bundle the token set to persist.
   * @return a map containing only the non-null token fields.
   */
  fun encode(
    provider: String,
    bundle: TokenBundle,
  ): Map<String, String> {
    val result = linkedMapOf<String, String>()
    bundle.accessToken?.let { result[StorageKeyProvider.storageKey(provider, TokenKind.ACCESS)] = it }
    bundle.refreshToken?.let { result[StorageKeyProvider.storageKey(provider, TokenKind.REFRESH)] = it }
    bundle.idToken?.let { result[StorageKeyProvider.storageKey(provider, TokenKind.ID)] = it }
    bundle.serverAuthCode?.let { result[StorageKeyProvider.storageKey(provider, TokenKind.SERVER_AUTH_CODE)] = it }
    bundle.authorizationCode?.let { result[StorageKeyProvider.storageKey(provider, TokenKind.AUTHORIZATION_CODE)] = it }
    bundle.user?.let {
      result[StorageKeyProvider.storageKey(provider, TokenKind.USER)] =
        vaultJson.encodeToString(SocialAuthResultUser.serializer(), it)
    }
    bundle.userId?.let { result[StorageKeyProvider.storageKey(provider, TokenKind.USER_ID)] = it }
    bundle.grantedPermissions?.let {
      result[StorageKeyProvider.storageKey(provider, TokenKind.GRANTED_PERMISSIONS)] =
        vaultJson.encodeToString(ListSerializer(String.serializer()), it)
    }
    bundle.declinedPermissions?.let {
      result[StorageKeyProvider.storageKey(provider, TokenKind.DECLINED_PERMISSIONS)] =
        vaultJson.encodeToString(ListSerializer(String.serializer()), it)
    }
    bundle.expiresAt?.let {
      result[StorageKeyProvider.storageKey(provider, TokenKind.EXPIRES_AT)] = it.toString()
    }
    return result
  }

  /**
   * Decodes a vault key/value map back into a [TokenBundle].
   *
   * @param provider the provider key used when storing.
   * @param stored the raw vault map (may contain unrelated keys, which are ignored).
   * @return the [TokenBundle], or `null` when the access token key is absent or blank.
   */
  fun decode(
    provider: String,
    stored: Map<String, String>,
  ): TokenBundle? {
    val access = stored[StorageKeyProvider.storageKey(provider, TokenKind.ACCESS)]?.trim().orEmpty()
    if (access.isEmpty()) return null
    return TokenBundle(
      accessToken = access,
      refreshToken = stored[StorageKeyProvider.storageKey(provider, TokenKind.REFRESH)],
      idToken = stored[StorageKeyProvider.storageKey(provider, TokenKind.ID)],
      serverAuthCode = stored[StorageKeyProvider.storageKey(provider, TokenKind.SERVER_AUTH_CODE)],
      authorizationCode = stored[StorageKeyProvider.storageKey(provider, TokenKind.AUTHORIZATION_CODE)],
      user =
        stored[StorageKeyProvider.storageKey(provider, TokenKind.USER)]?.let { value ->
          runCatching { vaultJson.decodeFromString(SocialAuthResultUser.serializer(), value) }.getOrNull()
        },
      userId = stored[StorageKeyProvider.storageKey(provider, TokenKind.USER_ID)],
      grantedPermissions =
        stored[StorageKeyProvider.storageKey(provider, TokenKind.GRANTED_PERMISSIONS)]?.let { value ->
          runCatching { vaultJson.decodeFromString(ListSerializer(String.serializer()), value) }.getOrNull()
        },
      declinedPermissions =
        stored[StorageKeyProvider.storageKey(provider, TokenKind.DECLINED_PERMISSIONS)]?.let { value ->
          runCatching { vaultJson.decodeFromString(ListSerializer(String.serializer()), value) }.getOrNull()
        },
      expiresAt =
        stored[StorageKeyProvider.storageKey(provider, TokenKind.EXPIRES_AT)]?.let { value ->
          value.toLongOrNull()
        },
    )
  }
}
