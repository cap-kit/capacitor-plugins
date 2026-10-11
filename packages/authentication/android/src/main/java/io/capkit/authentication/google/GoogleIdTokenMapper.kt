package io.capkit.authentication.google

import io.capkit.authentication.vault.TokenBundle

/**
 * @file GoogleIdTokenMapper.kt
 * Pure merge of a modern Credential-Manager sign-in result over the current vault
 * state. Google's Credential Manager (`googleid 1.2.x`) exposes only the ID token
 * to the client; the full token set (access + server-auth code) is obtained via the
 * separate Google authorization chain (`play-services-auth` AuthorizationClient),
 * NOT from the Credential Manager path. This mapper preserves previously vaulted
 * refresh/server-auth-code material across re-sign-ins instead of destroying it.
 * Side-effect free.
 */
object GoogleIdTokenMapper {
  /**
   * @param existing the current vault state, or `null` before the first sign-in.
   * @param idToken the ID token from `GoogleIdTokenCredential` (may be blank).
   * @return the merged [TokenBundle] to persist.
   */
  fun mergeSignInResult(
    existing: TokenBundle?,
    idToken: String?,
  ): TokenBundle =
    TokenBundle(
      accessToken = existing?.accessToken,
      refreshToken = existing?.refreshToken,
      idToken = idToken?.trim().takeUnless { it.isNullOrEmpty() } ?: existing?.idToken,
      serverAuthCode = existing?.serverAuthCode,
    )
}
