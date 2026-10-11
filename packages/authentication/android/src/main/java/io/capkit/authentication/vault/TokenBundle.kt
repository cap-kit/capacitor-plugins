package io.capkit.authentication.vault

import io.capkit.authentication.model.SocialAuthResultUser

/**
 * @file TokenBundle.kt
 * Typed token set persisted by the secure vault (Android `EncryptedSharedPreferences`).
 *
 * Mirrors the TypeScript `OAuthTokenSet` shape (access, refresh, id, server auth
 * code, plus the provider-agnostic optional `authorizationCode` and `user`).
 * All fields are nullable because flows legitimately produce partial sets; a bundle
 * without an access token is *no credential* and decodes as `null` (see
 * [TokenBundleCodec]). Never serialized to JS — vault material is native-only.
 *
 * Field presence is per-provider: Apple populates `authorizationCode`/`user` and
 * omits a refresh token; Google does the reverse. Absent optional fields stay
 * absent — no default value fabricates data.
 */
data class TokenBundle(
  val accessToken: String?,
  val refreshToken: String? = null,
  val idToken: String? = null,
  val serverAuthCode: String? = null,
  val authorizationCode: String? = null,
  val user: SocialAuthResultUser? = null,
  // Facebook shared access-token slots:
  // populated only by the facebook flow; google/apple leave them null (absent).
  val userId: String? = null,
  val grantedPermissions: List<String>? = null,
  val declinedPermissions: List<String>? = null,
  val expiresAt: Long? = null,
) {
  /** True when the bundle carries a usable access token (non-blank). */
  val hasCredential: Boolean = !accessToken.isNullOrBlank()
}
