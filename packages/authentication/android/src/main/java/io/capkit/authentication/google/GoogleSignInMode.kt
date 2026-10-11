package io.capkit.authentication.google

/**
 * @file GoogleSignInMode.kt
 * One Tap UX mode: the silent `GetGoogleIdOption` attempt
 * runs first when [AUTO_SELECT] is configured; [FULL_BUTTON] goes straight to the
 * full `GetSignInWithGoogleOption` account chooser. Pure data.
 */
enum class GoogleSignInMode {
  AUTO_SELECT,
  FULL_BUTTON,
}

/**
 * @file GoogleSignInModeResolver.kt
 * Pure resolver mapping the configured `autoSelect` flag to the sign-in mode.
 * `autoSelect` defaults to `false` when not configured (explicit chooser).
 */
object GoogleSignInModeResolver {
  /**
   * @param autoSelect the configured One Tap silent attempt flag.
   * @return [GoogleSignInMode.AUTO_SELECT] when enabled, [GoogleSignInMode.FULL_BUTTON] otherwise.
   */
  fun resolve(autoSelect: Boolean = false): GoogleSignInMode =
    if (autoSelect) GoogleSignInMode.AUTO_SELECT else GoogleSignInMode.FULL_BUTTON
}
