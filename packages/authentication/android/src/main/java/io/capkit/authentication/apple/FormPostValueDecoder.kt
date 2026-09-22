package io.capkit.authentication.apple

import kotlinx.serialization.json.Json
import java.net.URLDecoder

/**
 * @file FormPostValueDecoder.kt
 * URL-decodes the raw `application/x-www-form-urlencoded` body that the
 * [AppleSignInActivity] WebView intercepts from the Apple `form_post` callback,
 * and re-emits it as the JSON body [FormPostParser] expects.
 *
 * Apple percent-encodes every posted value — including the stringified `user`
 * JSON — so a value must be decoded BEFORE [FormPostParser] attempts to parse
 * it; decoding late (or never) turns the `user` profile and any reserved
 * character inside `code`/`state` into garbage. An optional malformed `user`
 * value is kept as a string so [FormPostParser] drops the profile while
 * preserving `code`/`id_token` — an optional bad profile field never destroys a
 * valid credential. Pure and side-effect free; malformed input resolves
 * to an empty map or `null` — never a crash.
 */
object FormPostValueDecoder {
  /**
   * Decodes an `application/x-www-form-urlencoded` form body into key/value
   * pairs, decoding every key and value with `URLDecoder` (charset UTF-8,
   * where `+` means space).
   *
   * Pairs whose key is blank or whose value cannot be percent-decoded are
   * skipped, so a partially malformed body still yields the healthy pairs.
   *
   * @param body the raw callback body, or `null` when the bridge surfaced none.
   * @return the decoded pairs; empty when `body` is null/blank or fully malformed.
   */
  fun decodeFormBody(body: String?): Map<String, String> {
    if (body.isNullOrBlank()) return emptyMap()
    return body
      .split('&')
      .mapNotNull { pair ->
        val eq = pair.indexOf('=')
        val rawKey = if (eq >= 0) pair.substring(0, eq) else pair
        val rawValue = if (eq >= 0) pair.substring(eq + 1) else ""
        if (rawKey.isBlank() && eq < 0) return@mapNotNull null
        val key = runCatching { URLDecoder.decode(rawKey, Charsets.UTF_8.name()) }.getOrNull() ?: return@mapNotNull null
        if (key.isBlank()) return@mapNotNull null
        val value =
          runCatching { URLDecoder.decode(rawValue, Charsets.UTF_8.name()) }
            .getOrNull()
            ?: return@mapNotNull null
        key to value
      }.toMap()
  }

  /**
   * Re-emits decoded pairs as the JSON body [FormPostParser.parse] consumes.
   *
   * The `user` value — a stringified JSON object as delivered by Apple — is
   * embedded as a nested JSON object when it parses, and kept as a JSON string
   * otherwise, so [FormPostParser] can apply its own tolerant user decoding.
   *
   * @param decoded the pairs produced by [decodeFormBody].
   * @return the JSON body for [FormPostParser], or `null` when there is nothing
   *         to emit (no pairs with non-blank values).
   */
  fun toFormPostJson(decoded: Map<String, String>): String? {
    val entries =
      decoded
        .filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
    if (entries.isEmpty()) return null

    return buildString {
      append('{')
      var first = true
      for ((key, value) in entries) {
        if (!first) append(',')
        first = false
        append('"').append(escape(key)).append("\":")
        if (key == USER_KEY) {
          append(embedUser(value))
        } else {
          append('"').append(escape(value)).append('"')
        }
      }
      append('}')
    }
  }

  /** Whether the payload carries a decoded `user` field that is a real JSON element. */
  private fun embedUser(value: String): String {
    val parsesAsJson = runCatching { Json.parseToJsonElement(value) }.isSuccess
    return if (parsesAsJson) value else quote(escape(value))
  }

  private fun quote(value: String): String = "\"$value\""

  private fun escape(value: String): String =
    buildString(value.length) {
      for (ch in value) {
        when (ch) {
          '"' -> append("\\\"")
          '\\' -> append("\\\\")
          '\n' -> append("\\n")
          '\r' -> append("\\r")
          '\t' -> append("\\t")
          '\b' -> append("\\b")
          '\u000C' -> append("\\f")
          else -> append(ch)
        }
      }
    }

  private const val USER_KEY = "user"
}
