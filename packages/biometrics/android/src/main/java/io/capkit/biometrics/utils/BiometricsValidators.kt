package io.capkit.biometrics.utils

import io.capkit.biometrics.error.BiometricsError
import io.capkit.biometrics.storage.SecureAccessControl
import io.capkit.biometrics.storage.SecureStorageKeys

/**
 * Collection of input validation helpers for the Biometrics plugin.
 *
 * These functions are pure and side-effect free.
 * They MUST NOT depend on Capacitor APIs or platform state.
 *
 * Secure store validators throw [BiometricsError.InvalidInput] directly so
 * the bridge layer maps them to the stable INVALID_INPUT code.
 */
object BiometricsValidators {
  /**
   * Validates a caller-provided secure store key.
   *
   * @return the validated key, unchanged.
   * @throws BiometricsError.InvalidInput when the key is missing, blank or longer than
   * [SecureStorageKeys.MAX_KEY_LENGTH].
   */
  fun requireSecureStoreKey(raw: String?): String {
    if (raw.isNullOrEmpty() || raw.isBlank()) {
      throw BiometricsError.InvalidInput("Secure store key is required.")
    }
    if (raw.length > SecureStorageKeys.MAX_KEY_LENGTH) {
      throw BiometricsError.InvalidInput(
        "Secure store key exceeds the maximum length of ${SecureStorageKeys.MAX_KEY_LENGTH}.",
      )
    }
    return raw
  }

  /**
   * Validates a caller-provided secure store value.
   * An empty string is a valid payload; only null is rejected.
   *
   * @return the validated value, unchanged.
   * @throws BiometricsError.InvalidInput when the value is missing.
   */
  fun requireSecureStoreValue(raw: String?): String {
    if (raw == null) {
      throw BiometricsError.InvalidInput("Secure store value is required.")
    }
    return raw
  }

  /**
   * Parses the REQUIRED access control option of the secure store.
   * The store is biometry-bound by design, so a missing or unknown value is
   * always invalid.
   *
   * @throws BiometricsError.InvalidInput when the value is missing or not one of
   * `biometryAny` / `biometryCurrentSet`.
   */
  fun parseAccessControl(raw: String?): SecureAccessControl =
    SecureAccessControl.fromRaw(raw)
      ?: throw BiometricsError.InvalidInput(
        "accessControl must be 'biometryAny' or 'biometryCurrentSet'.",
      )

  /**
   * Normalizes the optional maxAttempts option of the secure store prompts.
   * Missing or non-positive values fall back to [default]; the result is
   * clamped to the device maximum of 5 attempts (matching the system
   * BiometricPrompt behavior).
   */
  fun parseMaxAttempts(
    raw: Int?,
    default: Int = 1,
  ): Int {
    val resolved = if (raw == null || raw < 1) default else raw
    return resolved.coerceIn(1, MAX_PROMPT_ATTEMPTS)
  }

  private const val MAX_PROMPT_ATTEMPTS = 5
}
