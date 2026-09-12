package io.capkit.authentication.google

/**
 * @file AuthorizationRequestSpec.kt
 * Pure request spec for the Google `Identity.getAuthorizationClient(...).authorize(...)`
 * chain (Capawesome-proven path that surfaces `accessToken` + `serverAuthCode`, which
 * Credential Manager's `GetGoogleIdOption`/`GetSignInWithGoogleOption` never yield).
 *
 * The spec is intentionally free of GMS types so the mapping logic is JVM-testable;
 * the device adapter translates it into `AuthorizationRequest`.
 */
data class AuthorizationRequestSpec(
  /** OAuth scopes requested from the authorization server. */
  val scopes: List<String>,
  /**
   * Client id passed to `requestOfflineAccess(...)`: enables server-side refresh-token
   * issuance from the returned `serverAuthCode`.
   */
  val offlineAccessClientId: String,
)

/**
 * @file AuthorizationRequestSpecMapper.kt
 * Pure mapper from the typed Google configuration to an [AuthorizationRequestSpec].
 *
 * - Default scopes mirror the Web provider (`openid email profile`).
 * - Offline access is always requested with the server client id so the host backend
 *   can exchange the `serverAuthCode` for a refresh token.
 * - Returns `null` when no valid client id exists (the caller must fail the flow).
 */
object AuthorizationRequestSpecMapper {
  fun buildSpec(config: GoogleConfig): AuthorizationRequestSpec? {
    val clientId = config.serverClientId.trim()
    if (clientId.isEmpty()) {
      return null
    }
    val scopes = config.scopes.takeIf { it.isNotEmpty() } ?: GoogleConfig.DEFAULT_SCOPES
    return AuthorizationRequestSpec(scopes = scopes, offlineAccessClientId = clientId)
  }
}
