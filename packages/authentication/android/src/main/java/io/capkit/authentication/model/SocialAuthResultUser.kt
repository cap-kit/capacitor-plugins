package io.capkit.authentication.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * @file SocialAuthResultUser.kt
 * Provider-agnostic user profile delivered with a sign-in result.
 *
 * Mirrors the TypeScript `SocialAuthResultUser` shape
 * (`{ id?, email?, name?, realUserStatus?, picture? }`): every field is optional
 * and omitted when absent. Present only for providers that share profile data
 * (Apple delivers it on first sign-in only; facebook provides it via `/me`).
 * No field here is ever a credential — treat id, email and name as profile
 * data only. `picture` is a profile image URL (string|null); facebook fills it,
 * google/apple leave it null/absent.
 */
@Serializable
data class SocialAuthResultUser(
  /**
   * Provider-specific user id, when shared.
   */
  val id: String? = null,
  /**
   * User email address, when shared by the provider.
   */
  val email: String? = null,
  /**
   * Display name. Apple delivers first/last name parts on first sign-in;
   * other providers may deliver a single display string.
   */
  val name: UserName? = null,
  /**
   * Apple's real-user estimation, when shared. Literal spellings are preserved
   * exactly as Apple issues them (including the historical `likleyRealUser`
   * typo, which is intentional for API stability). Android never populates
   * this field — the apple-provider spec scopes it iOS-only — the model keeps
   * the optional field for homogeneous shape parity.
   */
  val realUserStatus: String? = null,
  /**
   * Provider profile image URL, when shared (facebook `/me` picture; google/apple
   * return it null/absent). Social-auth-facade homogeneous shape.
   */
  val picture: String? = null,
)

/**
 * Name shape of a [SocialAuthResultUser]: either first/last name parts
 * (Apple's native split) or a single display string.
 *
 * Mirrors the TypeScript `{ firstName?, lastName? } | string` union on
 * `OAuthTokenSet.user.name`; serialized into the vault with an explicit
 * discriminator so both variants round-trip without lossy coercion.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("nameType")
sealed interface UserName

/**
 * First/last name parts delivered by Apple on first sign-in.
 */
@Serializable
@SerialName("nameParts")
data class NameParts(
  val firstName: String? = null,
  val lastName: String? = null,
) : UserName

/**
 * Single display string delivered by providers that do not split the name.
 */
@Serializable
@SerialName("displayName")
data class DisplayName(
  val value: String,
) : UserName
