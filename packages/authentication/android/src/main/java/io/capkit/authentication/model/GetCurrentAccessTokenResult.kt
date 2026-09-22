package io.capkit.authentication.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getCurrentAccessToken()` method (oauth2-client spec).
 *
 * Mirrors the TypeScript `{ accessToken?: string }` shape: the field is omitted
 * entirely when no access token is stored, so clients can distinguish
 * "no session" from a blank token.
 */
@Serializable
data class GetCurrentAccessTokenResult(
  /**
   * The stored access token, when one exists.
   */
  val accessToken: String? = null,
)
