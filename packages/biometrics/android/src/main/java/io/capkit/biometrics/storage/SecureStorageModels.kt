package io.capkit.biometrics.storage

/**
 * Biometric access control applied to a value stored in the secure store.
 *
 * Mirrors the TypeScript `AccessControl` union (`src/definitions.ts`):
 * - `biometryAny`: the value survives biometric enrollment changes.
 * - `biometryCurrentSet`: the Keystore key is invalidated when the enrolled
 *   biometric set changes, which makes the stored data unrecoverable.
 *
 * The store is biometry-bound by design; there is no unprotected mode.
 */
enum class SecureAccessControl(
  val invalidatedByEnrollment: Boolean,
) {
  BIOMETRY_ANY(false),
  BIOMETRY_CURRENT_SET(true),
  ;

  companion object {
    /**
     * Resolves a raw option string to its enum value.
     * Returns null when the value is missing or unknown; callers decide how
     * to surface the failure (the bridge maps it to INVALID_INPUT).
     */
    fun fromRaw(raw: String?): SecureAccessControl? =
      when (raw) {
        "biometryAny" -> BIOMETRY_ANY
        "biometryCurrentSet" -> BIOMETRY_CURRENT_SET
        else -> null
      }
  }
}

/**
 * Validated options for the write path of the secure store.
 *
 * Built exclusively by the bridge layer after input validation; the
 * implementation layer must not re-parse raw plugin options.
 */
data class SetSecureDataOptions(
  val key: String,
  val value: String,
  val accessControl: SecureAccessControl,
  val title: String?,
  val negativeButtonText: String?,
  val maxAttempts: Int,
)

/**
 * Validated options for the read path of the secure store.
 *
 * iOS-only options (`reason`, `fallbackTitle`) are accepted by the bridge and
 * intentionally not carried here, mirroring how the existing authenticate
 * flow ignores `iosFallbackButtonText` on Android.
 */
data class GetSecureDataOptions(
  val key: String,
  val title: String?,
  val subtitle: String?,
  val description: String?,
  val negativeButtonText: String?,
  val maxAttempts: Int,
)
