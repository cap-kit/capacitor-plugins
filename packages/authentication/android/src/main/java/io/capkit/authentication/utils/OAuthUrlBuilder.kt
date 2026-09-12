package io.capkit.authentication.utils

import java.net.URLEncoder

/**
 * Builds OAuth2 authorization-endpoint URLs (PKCE "authorization code" flow).
 *
 * Pure and side-effect free. Produces a deterministic query string from the
 * provided parameters; the HTTPS redirect is performed by the caller.
 */
object OAuthUrlBuilder {
  /**
   * Builds the authorization URL for the authorization-code + PKCE flow.
   *
   * @param endpoint the OAuth authorization endpoint, e.g.
   *   `https://accounts.google.com/o/oauth2/v2/auth`.
   * @param clientId the OAuth client id.
   * @param redirectUri the registered redirect URI.
   * @param state a CSRF-protection state value; validated by the callback.
   * @param codeChallenge the PKCE code_challenge (see [PkceGenerator]).
   * @param scopes the requested OAuth scopes, joined with spaces.
   * @param extra additional query parameters merged into the URL.
   * @return the fully formed authorization URL.
   */
  fun buildAuthUrl(
    endpoint: String,
    clientId: String,
    redirectUri: String,
    state: String,
    codeChallenge: String,
    scopes: List<String>,
    extra: Map<String, String> = emptyMap(),
  ): String {
    val params = mutableListOf<String>()

    params += "response_type=code"
    params += "client_id=${encode(clientId)}"
    params += "redirect_uri=${encode(redirectUri)}"
    if (scopes.isNotEmpty()) {
      params += "scope=${encode(scopes.joinToString(" "))}"
    }
    params += "state=${encode(state)}"
    params += "code_challenge=${encode(codeChallenge)}"
    params += "code_challenge_method=S256"

    extra.forEach { (key, value) ->
      params += "${encode(key)}=${encode(value)}"
    }

    return "$endpoint?${params.joinToString("&")}"
  }

  /**
   * URL-encodes a value for use in a query string. Spaces become `%20` (standard
   * for OAuth space-delimited scopes) rather than the `+` form of `URLEncoder`.
   */
  private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
