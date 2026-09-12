package io.capkit.authentication.restore

/**
 * @file RestoreSupport.kt
 * Device-level availability gate for Restore Credentials (Android-only Zero Tap).
 *
 * The Play Restore Credentials API requires API 28+ (Android 9) and GMS core
 * 242200000+; below 28 the surface reports `available=false` (graceful
 * unavailability per google-restore-credential spec — never `unimplemented`).
 */
object RestoreSupport {
  /** Minimum SDK_INT for the Restore Credentials API. */
  const val MIN_RESTORE_SDK_INT = 28

  /**
   * @param sdkInt the device `Build.VERSION.SDK_INT`.
   * @return `true` when the Restore Credentials surface may be attempted.
   */
  fun isRestoreSupported(sdkInt: Int): Boolean = sdkInt >= MIN_RESTORE_SDK_INT
}
