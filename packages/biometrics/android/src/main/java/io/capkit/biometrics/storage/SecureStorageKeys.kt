package io.capkit.biometrics.storage

/**
 * Pure key derivation helpers for the biometric-bound secure store.
 *
 * MUST NOT depend on Android APIs so they stay unit-testable on the JVM.
 */
object SecureStorageKeys {
  /**
   * Prefix for every Keystore alias minted for the secure store.
   * One key exists per storage key, so the alias is derived from the
   * caller-provided key.
   */
  const val KEYSTORE_ALIAS_PREFIX = "BiometricsSecureKey_"

  /**
   * Prefix for every ciphertext entry persisted by the secure store
   * (SharedPreferences entry name, Capgo-compatible `secure_` shape).
   */
  const val PREFS_ENTRY_PREFIX = "secure_"

  /**
   * Maximum accepted length for a caller-provided storage key.
   * Keystore aliases and SharedPreferences entries must stay comfortably
   * below platform limits; the fork documents this cap as part of the
   * storage contract.
   */
  const val MAX_KEY_LENGTH = 128

  /** Keystore alias that holds the biometry-bound key for the given key. */
  fun keystoreAlias(key: String): String = KEYSTORE_ALIAS_PREFIX + key

  /** SharedPreferences entry name that persists the ciphertext for the key. */
  fun prefsEntry(key: String): String = PREFS_ENTRY_PREFIX + key
}
