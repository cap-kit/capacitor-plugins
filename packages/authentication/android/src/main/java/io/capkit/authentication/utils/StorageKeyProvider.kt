package io.capkit.authentication.utils

/**
 * Token storage kinds persisted by the secure vault.
 */
enum class TokenKind {
  ACCESS,
  REFRESH,
  ID,
  SERVER_AUTH_CODE,
  AUTHORIZATION_CODE,
  USER,
  USER_ID,
  GRANTED_PERMISSIONS,
  DECLINED_PERMISSIONS,
  EXPIRES_AT,
}

/**
 * Derives per-provider storage key names for the secure token vault.
 *
 * Every key is namespaced by provider so tokens never bleed between providers
 * and raw material is never exposed outside the vault. The `AUTHORIZATION_CODE`
 * and `USER` kinds support the Apple flows (apple-provider/secure-token-storage)
 * alongside the Google kinds; the scheme stays fully generic over the provider
 * key (`auth_vault_apple` resolves through the same `{provider}` template as
 * `auth_vault_google` — no per-provider key logic exists).
 */
object StorageKeyProvider {
  private const val PREFIX = "auth"

  /**
   * Computes the storage key for a provider and token kind.
   *
   * @param provider the provider key, e.g. `google`.
   * @param kind the token kind to store/read.
   * @return a namespaced key such as `auth_google_access`.
   */
  fun storageKey(
    provider: String,
    kind: TokenKind,
  ): String = "${PREFIX}_${provider}_${tokenType(kind)}"

  private fun tokenType(kind: TokenKind): String =
    when (kind) {
      TokenKind.ACCESS -> "access"
      TokenKind.REFRESH -> "refresh"
      TokenKind.ID -> "id"
      TokenKind.SERVER_AUTH_CODE -> "server_auth_code"
      TokenKind.AUTHORIZATION_CODE -> "authorization_code"
      TokenKind.USER -> "user"
      TokenKind.USER_ID -> "user_id"
      TokenKind.GRANTED_PERMISSIONS -> "granted_permissions"
      TokenKind.DECLINED_PERMISSIONS -> "declined_permissions"
      TokenKind.EXPIRES_AT -> "expires_at"
    }
}
