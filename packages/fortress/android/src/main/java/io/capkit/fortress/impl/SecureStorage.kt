package io.capkit.fortress.impl

import android.content.Context
import android.util.Base64
import io.capkit.fortress.error.ErrorMessages
import io.capkit.fortress.error.NativeError
import io.capkit.fortress.utils.KeystoreHelper

/**
 * Secure storage implementation using Android Keystore.
 *
 * This layer provides encrypted storage using Android Keystore
 * with hardware-backed security when available, and StrongBox support
 * for enhanced security on supported devices.
 *
 * Architectural rules:
 * - Pure Kotlin implementation
 * - Stateless - no internal state
 * - Delegates actual encryption operations to KeystoreHelper
 * - Throws NativeError for all failure cases
 */
class SecureStorage(
  private val context: Context,
) {
  /**
   * Stores a secure value in the encrypted vault, passing the StrongBox requirement from config.
   *
   * @param key Unique key identifier
   * @param value String value to store securely
   * @throws NativeError if storage fails
   */
  fun set(
    key: String,
    value: String,
    requireStrongBox: Boolean = false,
  ) {
    try {
      // Pass the hardware-security requirement down to the KeystoreHelper
      val encryptedData = KeystoreHelper.encryptString(context, key, value, requireStrongBox)
      val encodedData = Base64.encodeToString(encryptedData, Base64.NO_WRAP)
      val prefs = getSecurePrefs()
      prefs.edit().putString(key, encodedData).apply()
    } catch (e: KeystoreHelper.KeystoreError) {
      throw mapKeystoreError(e)
    } catch (e: Exception) {
      throw NativeError.InitFailed(ErrorMessages.INIT_FAILED)
    }
  }

  /**
   * Retrieves a secure value from the encrypted vault.
   *
   * @param key Unique key identifier
   * @return Stored string value, or null if not found
   * @throws NativeError if retrieval fails unexpectedly
   */
  fun get(key: String): String? {
    return try {
      val prefs = getSecurePrefs()
      val encodedData = prefs.getString(key, null) ?: return null
      val encryptedData = Base64.decode(encodedData, Base64.NO_WRAP)
      KeystoreHelper.decryptString(context, key, encryptedData)
    } catch (e: KeystoreHelper.KeystoreError) {
      // If the data is corrupted or the key is lost, we remove the orphaned reference in the Prefs
      if (e is KeystoreHelper.KeystoreError.UnableToDecrypt || e is KeystoreHelper.KeystoreError.KeyNotFound) {
        remove(key)
      }
      throw mapKeystoreError(e)
    } catch (e: Exception) {
      throw NativeError.InitFailed(ErrorMessages.INIT_FAILED)
    }
  }

  /**
   * Removes a secure value from the encrypted vault.
   *
   * @param key Unique key identifier
   * @throws NativeError if deletion fails unexpectedly
   */
  fun remove(key: String) {
    try {
      KeystoreHelper.deleteKey(key)
      getSecurePrefs().edit().remove(key).apply()
    } catch (e: KeystoreHelper.KeystoreError) {
      if (e is KeystoreHelper.KeystoreError.KeyNotFound) {
        getSecurePrefs().edit().remove(key).apply()
      } else {
        throw mapKeystoreError(e)
      }
    } catch (e: Exception) {
      throw NativeError.InitFailed(ErrorMessages.INIT_FAILED)
    }
  }

  /**
   * Clears all secure values from the vault.
   *
   * @throws NativeError if clear operation fails
   */
  fun clearAll() {
    try {
      val prefs = getSecurePrefs()
      prefs.edit().clear().apply()
      KeystoreHelper.clearAll()
    } catch (e: KeystoreHelper.KeystoreError) {
      throw mapKeystoreError(e)
    } catch (e: Exception) {
      throw NativeError.InitFailed(ErrorMessages.INIT_FAILED)
    }
  }

  /**
   * Checks whether a key exists in the secure vault.
   *
   * @param key Unique key identifier
   * @return true if the key exists, false otherwise
   * @throws NativeError if the check fails unexpectedly
   */
  fun hasKey(key: String): Boolean =
    try {
      // Optimization: Check Keystore alias first before opening SharedPreferences
      KeystoreHelper.hasAlias(key) && getSecurePrefs().contains(key)
    } catch (e: Exception) {
      false
    }

  // -----------------------------------------------------------------------------
  // Private Helpers
  // -----------------------------------------------------------------------------

  /**
   * Gets or creates the secure shared preferences.
   */
  private fun getSecurePrefs(): android.content.SharedPreferences =
    context.getSharedPreferences("fortress_secure", Context.MODE_PRIVATE)

  /**
   * Maps KeystoreError to NativeError.
   */
  private fun mapKeystoreError(error: KeystoreHelper.KeystoreError): NativeError =
    when (error) {
      is KeystoreHelper.KeystoreError.UnableToGenerateKey ->
        NativeError.InitFailed("Failed to generate encryption key")
      is KeystoreHelper.KeystoreError.UnableToEncrypt ->
        NativeError.InitFailed("Failed to encrypt data")
      is KeystoreHelper.KeystoreError.UnableToDecrypt ->
        NativeError.SecurityViolation("Failed to decrypt data")
      is KeystoreHelper.KeystoreError.UnableToDelete ->
        NativeError.InitFailed("Failed to delete key")
      is KeystoreHelper.KeystoreError.KeyNotFound ->
        NativeError.NotFound(ErrorMessages.NOT_FOUND)
      is KeystoreHelper.KeystoreError.DataConversionFailed ->
        NativeError.InvalidInput(ErrorMessages.INVALID_INPUT)
    }
}
