package io.capkit.authentication.google

/**
 * @file AuthorizationPolicy.kt
 * Pure decision rule for when the Android Google sign-in path MUST additionally run
 * the `AuthorizationClient.authorize` step to satisfy the spec MUST: "the full Google
 * token set (id, access, refresh, and server auth code) from a completed sign-in".
 *
 * - FULL_BUTTON (interactive chooser): ALWAYS authorizes — the full token set is
 *   mandatory on the interactive path.
 * - AUTO_SELECT (silent One Tap): stays idToken-focused (zero-UX) unless custom
 *   scopes were configured, in which case scopes were explicitly requested and the
 *   full set must be surfaced.
 */
object AuthorizationPolicy {
  fun shouldAuthorize(
    config: GoogleConfig,
    mode: GoogleSignInMode,
  ): Boolean = mode == GoogleSignInMode.FULL_BUTTON || config.scopes != GoogleConfig.DEFAULT_SCOPES
}
