package io.capkit.authentication.utils

import io.capkit.authentication.model.SocialAuthResultUser

/**
 * Normalizes provider token fields into the typed token bundle shape that
 * mirrors the TypeScript `OAuthTokenSet` (access, refresh, id, server auth code,
 * authorization code, user profile).
 *
 * Pure and side-effect free; null/absent fields are omitted so the result matches
 * the optional TypeScript contract. Flat credential fields are `String` values;
 * the structured `user` profile is passed through as its [SocialAuthResultUser]
 * model so the bridge can emit the nested `user` object without lossy coercion.
 */
object TokenMapper {
  /**
   * Builds the normalized token map, omitting any absent optional field.
   *
   * @param accessToken the OAuth access token (required by the flow).
   * @param refreshToken optional refresh token.
   * @param idToken optional OpenID Connect id token.
   * @param serverAuthCode optional Google server auth code.
   * @param authorizationCode optional provider authorization code (Apple: all platforms).
   * @param user optional provider user profile (Apple: first sign-in only; facebook: `/me`).
   * @param userId optional provider-scoped user id (facebook).
   * @param grantedPermissions optional facebook granted permissions.
   * @param declinedPermissions optional facebook declined permissions.
   * @param expiresAt optional absolute epoch-seconds token expiry (facebook).
   * @return a map keyed by the TypeScript `OAuthTokenSet` field names.
   */
  fun map(
    accessToken: String?,
    refreshToken: String? = null,
    idToken: String? = null,
    serverAuthCode: String? = null,
    authorizationCode: String? = null,
    user: SocialAuthResultUser? = null,
    userId: String? = null,
    grantedPermissions: List<String>? = null,
    declinedPermissions: List<String>? = null,
    expiresAt: Long? = null,
  ): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>()
    accessToken?.let { result["accessToken"] = it }
    refreshToken?.let { result["refreshToken"] = it }
    idToken?.let { result["idToken"] = it }
    serverAuthCode?.let { result["serverAuthCode"] = it }
    authorizationCode?.let { result["authorizationCode"] = it }
    user?.let { result["user"] = it }
    userId?.let { result["userId"] = it }
    grantedPermissions?.let { result["grantedPermissions"] = it }
    declinedPermissions?.let { result["declinedPermissions"] = it }
    expiresAt?.let { result["expiresAt"] = it }
    return result
  }
}
