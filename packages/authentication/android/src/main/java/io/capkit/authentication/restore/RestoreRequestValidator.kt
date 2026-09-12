package io.capkit.authentication.restore

import io.capkit.authentication.error.AuthenticationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * @file RestoreRequestValidator.kt
 * Pure input gate for `createRestoreCredential`: `requestJson` must be a non-blank
 * JSON object (WebAuthn-style credential request). Anything else is INVALID_INPUT,
 * mirroring the cross-platform typed error contract. Side-effect free.
 */
object RestoreRequestValidator {
  private val json =
    Json {
      ignoreUnknownKeys = true
    }

  /**
   * @param requestJson the caller-supplied credential request JSON.
   * @return an [AuthenticationError.InvalidInput] describing the problem, or `null`
   *         when the input is acceptable.
   */
  fun validate(requestJson: String?): AuthenticationError? {
    val trimmed = requestJson?.trim().orEmpty()
    if (trimmed.isEmpty()) {
      return AuthenticationError.InvalidInput("requestJson must be a non-blank JSON object.")
    }
    val isJsonObject =
      runCatching {
        json.parseToJsonElement(trimmed).jsonObject
      }.getOrNull() is JsonObject
    if (!isJsonObject) {
      return AuthenticationError.InvalidInput("requestJson must be a valid JSON object.")
    }
    return null
  }
}
