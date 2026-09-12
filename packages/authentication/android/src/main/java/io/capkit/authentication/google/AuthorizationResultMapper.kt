package io.capkit.authentication.google

import io.capkit.authentication.vault.TokenBundle

/**
 * @file AuthorizationResultMapper.kt
 * Pure mapper for the authorization outcome into the vaulted [TokenBundle].
 *
 * The device adapter feeds this mapper the primitives from the GMS
 * `AuthorizationResult` (direct `authorize` success or `getAuthorizationResultFromIntent`
 * recovery): `hasResolution()`, `getAccessToken()` and `getServerAuthCode()`.
 * All GMS types stay in the adapter; this object is fully JVM-testable.
 */
object AuthorizationResultMapper {
  /**
   * Normalized view of an authorization outcome. `hasTokens` signals whether the
   * payload actually carries a token (used by the adapter to decide between the
   * resolution path and a terminal error).
   */
  data class AuthorizationPayload(
    val accessToken: String?,
    val serverAuthCode: String?,
    val hasResolution: Boolean,
  ) {
    val hasTokens: Boolean = !accessToken.isNullOrBlank() || !serverAuthCode.isNullOrBlank()
  }

  /**
   * Normalizes raw GMS values: trims whitespace and nulls blank strings so the
   * vault never persists empty token fields.
   */
  fun extract(
    accessToken: String?,
    serverAuthCode: String?,
    hasResolution: Boolean,
  ): AuthorizationPayload =
    AuthorizationPayload(
      accessToken = accessToken?.trim().takeUnless { it.isNullOrEmpty() },
      serverAuthCode = serverAuthCode?.trim().takeUnless { it.isNullOrEmpty() },
      hasResolution = hasResolution,
    )

  /**
   * True when the authorize call requests a UI resolution (the pending-intent
   * launch path); false when the result already carries the tokens.
   */
  fun shouldLaunchResolution(payload: AuthorizationPayload): Boolean = payload.hasResolution

  /**
   * Merges the authorization tokens over the existing bundle (null-omit: absent
   * payload tokens keep the previously vaulted values, never wiping them).
   * The `idToken` from the Credential Manager step and any prior `refreshToken`
   * are always preserved.
   */
  fun mergeTokens(
    existing: TokenBundle,
    payload: AuthorizationPayload,
  ): TokenBundle =
    TokenBundle(
      accessToken = payload.accessToken ?: existing.accessToken,
      refreshToken = existing.refreshToken,
      idToken = existing.idToken,
      serverAuthCode = payload.serverAuthCode ?: existing.serverAuthCode,
    )
}
