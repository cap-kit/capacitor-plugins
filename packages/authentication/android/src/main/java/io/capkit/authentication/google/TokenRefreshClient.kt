package io.capkit.authentication.google

import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.vault.TokenBundle
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * @file TokenRefreshClient.kt
 * OAuth2 token-endpoint refresh transport (OkHttp on Android).
 * Thin device adapter: request shaping and response parsing are the JVM-proven
 * pure units in this package; this class only maps the HTTP round-trip.
 *
 * Refreshes with the vaulted refresh token against the Google token endpoint,
 * then merges the response over the existing bundle (vaulted refresh material is
 * preserved). The callback runs on an OkHttp dispatcher thread, never the main
 * thread.
 */
class TokenRefreshClient {
  private val client = OkHttpClient()

  /** Result surface decoupled from the Capacitor bridge (Impl layer rule). */
  interface RefreshCallback {
    fun onResult(bundle: TokenBundle)

    fun onError(error: AuthenticationError)
  }

  /**
   * Starts an async refresh.
   *
   * @param clientId the public OAuth2 client id (validated upstream).
   * @param existing the current vault state; its refresh token drives the grant.
   * @param vault optional vault to persist the merged bundle on success (W1 fix).
   * @param callback the typed result surface.
   */
  fun refresh(
    clientId: String,
    existing: TokenBundle,
    vault: io.capkit.authentication.vault.TokenVault? = null,
    callback: RefreshCallback,
  ) {
    val refreshToken = existing.refreshToken
    if (refreshToken.isNullOrBlank()) {
      // Web parity: refresh with no stored refresh token rejects with INVALID_INPUT.
      callback.onError(AuthenticationError.InvalidInput("No refresh token available"))
      return
    }
    if (clientId.isBlank()) {
      callback.onError(AuthenticationError.InvalidInput("Google client id is not configured"))
      return
    }

    val body =
      RefreshRequestBuilder
        .build(clientId, refreshToken)
        .toRequestBody("application/x-www-form-urlencoded".toMediaType())
    val request =
      Request
        .Builder()
        .url(TOKEN_ENDPOINT)
        .post(body)
        .build()

    client.newCall(request).enqueue(
      object : Callback {
        override fun onFailure(
          call: Call,
          e: java.io.IOException,
        ) {
          callback.onError(
            AuthenticationError.InitFailed("Token refresh request failed: ${e.message ?: "network error"}"),
          )
        }

        override fun onResponse(
          call: Call,
          response: Response,
        ) {
          val bodyText = response.body?.string().orEmpty()
          when (val outcome = RefreshResponseParser.parse(bodyText)) {
            is RefreshResponseParser.Result.Success -> {
              val merged =
                existing.copy(
                  accessToken = outcome.accessToken,
                  idToken = outcome.idToken ?: existing.idToken,
                )
              vault?.store(merged)
              callback.onResult(merged)
            }
            is RefreshResponseParser.Result.Error -> callback.onError(outcome.error)
          }
        }
      },
    )
  }

  private companion object {
    const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
  }
}
