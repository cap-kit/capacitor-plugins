package io.capkit.authentication.google

import io.capkit.authentication.error.AuthenticationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * @file RefreshResponseParser.kt
 * Pure parser for the OAuth2 token-endpoint refresh response (oauth2-client spec;
 * web parity): a non-blank `access_token` is required; a token-endpoint `error` of
 * `access_denied`/`user_cancelled` maps to USER_CANCELLED; any other error or a
 * malformed body maps to INIT_FAILED. Side-effect free.
 */
object RefreshResponseParser {
  /** Typed parse outcome; errors carry the mapped [AuthenticationError]. */
  sealed interface Result {
    data class Success(
      val accessToken: String,
      val refreshToken: String? = null,
      val idToken: String? = null,
      val expiresIn: Long? = null,
    ) : Result

    data class Error(
      val error: AuthenticationError,
    ) : Result
  }

  private val json =
    Json {
      ignoreUnknownKeys = true
    }

  /**
   * @param body the raw token-endpoint response body.
   * @return [Result.Success] with the normalized token set, or [Result.Error].
   */
  fun parse(body: String): Result {
    val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
    if (root == null) {
      return Result.Error(AuthenticationError.InitFailed("Token refresh response was not valid JSON."))
    }

    val errorField = root["error"]?.jsonPrimitive?.contentOrNull()
    when (errorField) {
      "access_denied", "user_cancelled" ->
        return Result.Error(AuthenticationError.UserCancelled("Token refresh was cancelled."))
      null -> Unit
      else -> return Result.Error(AuthenticationError.InitFailed("Token refresh failed: $errorField"))
    }

    val accessToken =
      root["access_token"]
        ?.jsonPrimitive
        ?.contentOrNull()
        ?.trim()
        .orEmpty()
    if (accessToken.isEmpty()) {
      // Web parity: a missing access token is surfaced as the token-endpoint
      // `invalid_grant` error, which maps to INIT_FAILED on every platform.
      return Result.Error(AuthenticationError.InitFailed("No access token in token refresh response."))
    }

    return Result.Success(
      accessToken = accessToken,
      refreshToken = root["refresh_token"]?.jsonPrimitive?.contentOrNull(),
      idToken = root["id_token"]?.jsonPrimitive?.contentOrNull(),
      expiresIn = root["expires_in"]?.jsonPrimitive?.contentOrNull()?.toLongOrNull(),
    )
  }

  private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
    if (this is kotlinx.serialization.json.JsonNull) null else content
}
