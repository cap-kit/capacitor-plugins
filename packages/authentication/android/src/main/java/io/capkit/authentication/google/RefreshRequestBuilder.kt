package io.capkit.authentication.google

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * @file RefreshRequestBuilder.kt
 * Pure builder for the OAuth2 token-endpoint refresh body (OkHttp
 * transport on Android). Uses the `refresh_token` grant with the public
 * `client_id`; every value is form-url-encoded so reserved characters in tokens
 * cannot corrupt the request. Side-effect free.
 */
object RefreshRequestBuilder {
  /**
   * @param clientId the public OAuth2 client id.
   * @param refreshToken the vaulted refresh token.
   * @return an `application/x-www-form-urlencoded` request body.
   */
  fun build(
    clientId: String,
    refreshToken: String,
  ): String =
    buildString {
      append("grant_type=refresh_token")
      append("&client_id=").append(encode(clientId))
      append("&refresh_token=").append(encode(refreshToken))
    }

  private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
