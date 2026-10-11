package io.capkit.authentication.facebook

import io.capkit.authentication.model.DisplayName
import io.capkit.authentication.model.SocialAuthResultUser
import io.capkit.authentication.vault.TokenBundle

/**
 * @file FacebookTokenMapper.kt
 * Pure mapping from Facebook SDK token/profile primitives onto the shared token
 * bundle: the four optional slots
 * (`userId`, `grantedPermissions`, `declinedPermissions`, `expiresAt` absolute
 * epoch seconds) fill from SDK values; `idToken`/`authorizationCode`/
 * `serverAuthCode`/`refreshToken` stay null for facebook;
 * `/me` maps to [SocialAuthResultUser] with a display-name string and the `picture`
 * URL. Permission lists are trimmed/deduped — facebook already delivers granted-only
 * permissions on Android, so no refresh-time filtering is needed here.
 * Pure and side-effect free; the SDK wiring binds the classes (`AccessToken`, `/me`)
 * onto these primitives.
 */
object FacebookTokenMapper {
  private const val MILLIS_PER_SECOND = 1000L

  /**
   * Converts an SDK expiry in epoch milliseconds to the canonical absolute epoch
   * seconds slot value (floor division of `getExpires().time / 1000`).
   */
  fun expiresAtEpochSeconds(expiresAtMillis: Long?): Long? = expiresAtMillis?.div(MILLIS_PER_SECOND)

  /**
   * Normalizes an SDK permission collection: trims, drops blanks, dedupes preserving
   * first occurrence. `null` stays `null` (slot omitted); an empty collection stays
   * empty.
   */
  fun sanitizePermissions(permissions: Collection<String>?): List<String>? =
    permissions
      ?.map { it.trim() }
      ?.filter { it.isNotEmpty() }
      ?.distinct()

  /**
   * Maps an SDK access token plus its optional slot primitives onto a [TokenBundle].
   * Non-applicable facebook fields (`idToken`, `refreshToken`, `authorizationCode`,
   * `serverAuthCode`) are never set — absent by design.
   */
  fun mapToken(
    accessToken: String,
    userId: String? = null,
    grantedPermissions: Collection<String>? = null,
    declinedPermissions: Collection<String>? = null,
    expiresAtMillis: Long? = null,
  ): TokenBundle =
    TokenBundle(
      accessToken = accessToken,
      userId = userId,
      grantedPermissions = sanitizePermissions(grantedPermissions),
      declinedPermissions = sanitizePermissions(declinedPermissions),
      expiresAt = expiresAtEpochSeconds(expiresAtMillis),
    )

  /**
   * Maps the granted-only `/me` fields onto the shared user profile. `realUserStatus`
   * is NEVER set for facebook; absent fields stay `null`.
   */
  fun mapProfile(
    id: String? = null,
    name: String? = null,
    email: String? = null,
    pictureUrl: String? = null,
  ): SocialAuthResultUser =
    SocialAuthResultUser(
      id = id,
      email = email,
      name = name?.takeIf { it.isNotBlank() }?.let { DisplayName(it) },
      picture = pictureUrl,
    )

  /**
   * Combines token slot mapping and the `/me` profile into the bundle persisted by
   * the sign-in flow (vault persist step).
   */
  fun mapSignInResult(
    accessToken: String,
    userId: String? = null,
    grantedPermissions: Collection<String>? = null,
    declinedPermissions: Collection<String>? = null,
    expiresAtMillis: Long? = null,
    profile: SocialAuthResultUser? = null,
  ): TokenBundle =
    mapToken(
      accessToken = accessToken,
      userId = userId,
      grantedPermissions = grantedPermissions,
      declinedPermissions = declinedPermissions,
      expiresAtMillis = expiresAtMillis,
    ).copy(user = profile)
}
