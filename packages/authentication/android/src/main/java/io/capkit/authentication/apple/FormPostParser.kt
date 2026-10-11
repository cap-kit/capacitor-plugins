package io.capkit.authentication.apple

import io.capkit.authentication.model.NameParts
import io.capkit.authentication.model.SocialAuthResultUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * @file FormPostParser.kt
 * Parses the Apple OAuth `form_post` callback payload into structured parameters.
 * The Android WebView captures
 * the callback and hands this parser the raw JSON of the posted fields; malformed
 * input always resolves to `null` — never a crash (security baseline).
 *
 * Apple's `user` field is decoded through an Apple-shaped intermediate model and
 * mapped onto the provider-agnostic [SocialAuthResultUser]; Apple's native payload
 * carries no `nameType` discriminator, so direct model deserialization would fail.
 */
object FormPostParser {
  private val json =
    Json {
      ignoreUnknownKeys = true
    }

  data class ParsedFormPost(
    val authorizationCode: String? = null,
    val idToken: String? = null,
    val state: String? = null,
    val user: SocialAuthResultUser? = null,
  )

  /**
   * Parses the form_post JSON body.
   *
   * @param body the raw callback body.
   * @return the structured [ParsedFormPost], or `null` when the body is empty,
   *         not valid JSON, carries neither `code` nor `id_token`, or its `user`
   *         payload is malformed.
   */
  fun parse(body: String): ParsedFormPost? {
    val trimmed = body.trim()
    if (trimmed.isEmpty()) return null

    val root = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() as? JsonObject ?: return null

    val code = root.stringValue("code")
    val idToken = root.stringValue("id_token")
    val state = root.stringValue("state")
    if (code.isNullOrBlank() && idToken.isNullOrBlank()) return null

    val user =
      root["user"]?.let { userElement ->
        when (userElement) {
          is JsonObject -> decodeUserObject(userElement)
          is JsonPrimitive -> decodeUserString(userElement.contentOrNull)
          else -> null
        }
      }

    return ParsedFormPost(
      authorizationCode = code,
      idToken = idToken,
      state = state,
      user = user,
    )
  }

  private fun JsonObject.stringValue(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

  private fun decodeUserObject(userObject: JsonObject): SocialAuthResultUser? =
    runCatching { json.decodeFromJsonElement(AppleUserPayload.serializer(), userObject) }
      .getOrNull()
      ?.toModel()

  private fun decodeUserString(userString: String?): SocialAuthResultUser? {
    if (userString.isNullOrBlank()) return null
    return runCatching { json.decodeFromString(AppleUserPayload.serializer(), userString) }
      .getOrNull()
      ?.toModel()
  }
}

/**
 * Apple's native `user` payload shape, without the plugin's `nameType`
 * discriminator. Mapped onto [SocialAuthResultUser] by [FormPostParser].
 */
@Serializable
private data class AppleUserPayload(
  val id: String? = null,
  val email: String? = null,
  val name: AppleNamePayload? = null,
) {
  fun toModel(): SocialAuthResultUser =
    SocialAuthResultUser(
      id = id,
      email = email,
      name =
        name
          ?.takeUnless { it.firstName.isNullOrBlank() && it.lastName.isNullOrBlank() }
          ?.let { NameParts(firstName = it.firstName, lastName = it.lastName) },
    )
}

/**
 * Apple's native name part values.
 */
@Serializable
private data class AppleNamePayload(
  val firstName: String? = null,
  val lastName: String? = null,
)
