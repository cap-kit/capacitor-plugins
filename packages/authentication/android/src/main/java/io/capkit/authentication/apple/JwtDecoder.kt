package io.capkit.authentication.apple

import io.capkit.authentication.utils.Base64Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * @file JwtDecoder.kt
 * Extracts the claims payload of an Apple `id_token` JWT.
 *
 * Extraction only — signature verification is deliberately out of scope (the
 * token is validated by the server-side exchange and the browser/WebView context);
 * this decoder surfaces `sub`, `email` and `nonce` for the plugin flow.
 * Malformed JWTs (too few segments, non-Base64URL payload,
 * non-JSON payload) resolve to `null` — never crash (security baseline).
 */
object JwtDecoder {
  private val json = Json { ignoreUnknownKeys = true }

  /** Claims extracted from an id_token payload. */
  data class JwtClaims(
    val sub: String? = null,
    val email: String? = null,
    val nonce: String? = null,
  )

  /**
   * Decodes the middle (payload) segment of a JWT.
   *
   * @param idToken the full `header.payload.signature` token.
   * @return the claims, or `null` when the token is blank, has fewer than three
   *         segments, the payload is not valid Base64URL, or is not JSON.
   */
  fun decode(idToken: String): JwtClaims? {
    val trimmed = idToken.trim()
    if (trimmed.isEmpty()) return null

    val segments = trimmed.split(".")
    if (segments.size < 3) return null

    val payloadBytes = runCatching { Base64Url.decode(segments[1]) }.getOrNull() ?: return null

    return runCatching {
      val root = json.parseToJsonElement(payloadBytes.toString(Charsets.UTF_8)) as? JsonObject ?: return null
      JwtClaims(
        sub = (root["sub"] as? JsonPrimitive)?.contentOrNull,
        email = (root["email"] as? JsonPrimitive)?.contentOrNull,
        nonce = (root["nonce"] as? JsonPrimitive)?.contentOrNull,
      )
    }.getOrNull()
  }
}
