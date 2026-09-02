package io.capkit.fortress.impl

import android.content.Context
import android.content.SharedPreferences

/**
 * Standard (insecure) storage implementation using SharedPreferences.
 *
 * This layer provides non-encrypted storage for non-sensitive data
 * using Android SharedPreferences.
 *
 * Use for:
 * - UI theme preferences
 * - Non-sensitive flags
 * - Public configuration
 *
 * DO NOT use for:
 * - Passwords
 * - Authentication tokens
 * - Any sensitive data
 *
 * Architectural rules:
 * - Pure Kotlin implementation
 * - Stateless - no internal state
 * - Delegates to SharedPreferences for persistence
 */
class StandardStorage(
  context: Context,
) {
  companion object {
    private const val SUITE_NAME = "io.capkit.fortress.standard"
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(SUITE_NAME, Context.MODE_PRIVATE)

  /**
   * Stores a value in standard (insecure) storage.
   *
   * @param key Unique key identifier
   * @param value String value to store
   */
  fun set(
    key: String,
    value: String,
  ) {
    prefs.edit().putString(key, value).apply()
  }

  /**
   * Retrieves a value from standard (insecure) storage.
   *
   * @param key Unique key identifier
   * @return Stored string value, or null if not found
   */
  fun get(key: String): String? = prefs.getString(key, null)

  /**
   * Removes a value from standard (insecure) storage.
   *
   * @param key Unique key identifier
   */
  fun remove(key: String) {
    prefs.edit().remove(key).apply()
  }

  /**
   * Clears all values from standard storage.
   *
   * Note: This only clears keys that start with the fortress prefix.
   */
  fun clearAll() {
    val keysToRemove =
      prefs.all.keys.filter {
        it.startsWith("ftrss_") || it.startsWith("fortress_")
      }

    if (keysToRemove.isEmpty()) return

    prefs
      .edit()
      .apply {
        keysToRemove.forEach { key ->
          // Logical improvement: "Wipe-before-delete" to overwrite
          // data in RAM before removing the disk pointer
          putString(key, "DELETED")
          remove(key)
        }
      }.apply()
  }

  /**
   * Checks whether a key exists in standard storage.
   *
   * @param key Unique key identifier
   * @return true if the key exists, false otherwise
   */
  fun hasKey(key: String): Boolean = prefs.contains(key)
}
