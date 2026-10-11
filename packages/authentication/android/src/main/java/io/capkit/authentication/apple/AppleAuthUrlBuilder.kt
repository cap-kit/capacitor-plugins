package io.capkit.authentication.apple

import java.net.URLEncoder

/**
 * Builds the Apple OAuth authorization URL.
 *
 * Apple requires `response_type=code id_token` and `response_mode=form_post` for
 * the WebView flow. Pure and side-effect free; produces a
 * deterministic query string that the caller launches in the WebView.
 */
object AppleAuthUrlBuilder {
  /** Apple authorization endpoint. */
  const val AUTHORIZE_ENDPOINT: String = "https://appleid.apple.com/auth/authorize"

  /**
   * Builds the authorization URL for the Apple sign-in flow.
   *
   * @param clientId the Apple Services ID (client id).
   * @param redirectUri the registered redirect URI.
   * @param state a CSRF-protection state value; validated by the callback.
   * @param nonce an OpenID Connect nonce; bound to the issued id_token.
   * @param scopes the requested OAuth scopes, joined with spaces.
   * @return the fully formed authorization URL.
   */
  fun buildAuthUrl(
    clientId: String,
    redirectUri: String,
    state: String,
    nonce: String,
    scopes: List<String>,
  ): String {
    val params = mutableListOf<String>()

    params += "response_type=code%20id_token"
    params += "response_mode=form_post"
    params += "client_id=${encode(clientId)}"
    params += "redirect_uri=${encode(redirectUri)}"
    if (scopes.isNotEmpty()) {
      params += "scope=${encode(scopes.joinToString(" "))}"
    }
    params += "state=${encode(state)}"
    params += "nonce=${encode(nonce)}"

    return "$AUTHORIZE_ENDPOINT?${params.joinToString("&")}"
  }

  /**
   * URL-encodes a value for use in a query string. Spaces become `%20` (standard
   * for OAuth space-delimited scopes) rather than the `+` form of `URLEncoder`.
   */
  private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
