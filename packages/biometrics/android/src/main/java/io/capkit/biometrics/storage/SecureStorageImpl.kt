package io.capkit.biometrics.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.capkit.biometrics.error.BiometricsError
import io.capkit.biometrics.error.BiometricsErrorMessages
import io.capkit.biometrics.logger.BiometricsLogger
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.ProviderException
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Biometric-bound secure key-value store (Android).
 *
 * Mechanism (Capgo parity): every storage key owns an AES-256-GCM Keystore
 * key generated with `setUserAuthenticationRequired(true)`; the ciphertext is
 * persisted (Base64 of IV + GCM ciphertext+tag) in a plugin-owned
 * SharedPreferences file. Reads AND writes run a BiometricPrompt carrying a
 * `CryptoObject` bound to the per-key cipher, so the payload can only be
 * encrypted/decrypted right after a fresh strong-biometric authentication.
 *
 * Storage prompts are biometric-strong-only: DEVICE_CREDENTIAL cannot carry a
 * CryptoObject, so device credentials are never part of the authenticator set.
 *
 * Layering contract:
 * - Throws [BiometricsError] variants (Capacitor-free) or pure Throwables.
 * - JS error-code mapping happens ONLY in the bridge (BiometricsPlugin).
 */
class SecureStorageImpl(
  private val context: Context,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * In-flight storage prompt, mirroring the BiometricsImpl pattern so a
   * failed-attempts overflow can cancel its own ceremony.
   */
  @Volatile private var activePrompt: BiometricPrompt? = null

  // ---------------------------------------------------------------------------
  // Write path
  // ---------------------------------------------------------------------------

  /**
   * Encrypts [options.value] under the per-key Keystore key, protecting the
   * write with a biometric prompt bound to the encryption cipher.
   *
   * @param completion Result callback, invoked exactly once.
   */
  fun setSecureData(
    activity: FragmentActivity,
    options: SetSecureDataOptions,
    completion: (Result<Unit>) -> Unit,
  ) {
    try {
      ensureBiometryAvailable()
      val prefsEntry = SecureStorageKeys.prefsEntry(options.key)
      val keyStore = loadKeyStore()
      val alias = SecureStorageKeys.keystoreAlias(options.key)
      val cipher =
        createWriteCipher(keyStore, alias, options.accessControl.invalidatedByEnrollment, prefsEntry)
      runCryptoPrompt(
        activity,
        options.title ?: DEFAULT_WRITE_TITLE,
        null,
        null,
        options.negativeButtonText,
        options.maxAttempts,
        BiometricPrompt.CryptoObject(cipher),
      ) { result ->
        result
          .onSuccess { authResult ->
            try {
              persistCiphertext(authResult.cryptoObject?.cipher, prefsEntry, options.value)
              completion(Result.success(Unit))
            } catch (error: Throwable) {
              completion(Result.failure(error))
            }
          }.onFailure { error ->
            completion(Result.failure(error))
          }
      }
    } catch (error: Throwable) {
      completion(Result.failure(error))
    }
  }

  // ---------------------------------------------------------------------------
  // Read path
  // ---------------------------------------------------------------------------

  /**
   * Decrypts the value stored for [options.key] behind a biometric prompt
   * bound to the decryption cipher.
   *
   * Missing or unrecoverable data (never stored, deleted, or invalidated by
   * a biometric enrollment change) rejects with [BiometricsError.NotFound].
   *
   * @param completion Result callback, invoked exactly once.
   */
  fun getSecureData(
    activity: FragmentActivity,
    options: GetSecureDataOptions,
    completion: (Result<String>) -> Unit,
  ) {
    try {
      val prefsEntry = SecureStorageKeys.prefsEntry(options.key)
      val encoded = prefs().getString(prefsEntry, null) ?: throw secureDataNotFound()
      val blob = decodeBlob(encoded)
      ensureBiometryAvailable()
      val keyStore = loadKeyStore()
      val alias = SecureStorageKeys.keystoreAlias(options.key)
      val cipher = createReadCipher(keyStore, alias, prefsEntry, blob)
      runCryptoPrompt(
        activity,
        options.title ?: DEFAULT_READ_TITLE,
        options.subtitle,
        options.description,
        options.negativeButtonText,
        options.maxAttempts,
        BiometricPrompt.CryptoObject(cipher),
      ) { result ->
        result
          .onSuccess { authResult ->
            try {
              completion(Result.success(decryptPayload(authResult.cryptoObject?.cipher, blob)))
            } catch (error: Throwable) {
              completion(Result.failure(error))
            }
          }.onFailure { error ->
            completion(Result.failure(error))
          }
      }
    } catch (error: Throwable) {
      completion(Result.failure(error))
    }
  }

  // ---------------------------------------------------------------------------
  // Non-interactive operations
  // ---------------------------------------------------------------------------

  /**
   * Deletes the stored value and its Keystore key. Never prompts; idempotent.
   */
  fun deleteSecureData(key: String) {
    try {
      loadKeyStore().deleteEntry(SecureStorageKeys.keystoreAlias(key))
    } catch (_: Exception) {
      // Best effort: the alias may not exist; deletion stays idempotent.
    }
    removePrefsEntry(SecureStorageKeys.prefsEntry(key))
  }

  /**
   * Reports whether a ciphertext exists for the key. Never prompts.
   */
  fun isSecureDataSaved(key: String): Boolean = prefs().getString(SecureStorageKeys.prefsEntry(key), null) != null

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  /**
   * Rejects early when strong biometrics cannot run, so the caller gets a
   * stable UNAVAILABLE instead of a system prompt error.
   */
  private fun ensureBiometryAvailable() {
    val status =
      BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (status != BiometricManager.BIOMETRIC_SUCCESS) {
      throw BiometricsError.Unavailable("Biometrics unavailable on this device.")
    }
  }

  /**
   * Creates the write cipher bound to a usable per-key Keystore key.
   * When the existing key cannot initialize (e.g. the enrolled biometric set
   * changed with `biometryCurrentSet`), the stale ciphertext is unrecoverable:
   * key + ciphertext are dropped and a fresh key is minted for this write.
   */
  private fun createWriteCipher(
    keyStore: KeyStore,
    alias: String,
    invalidatedByEnrollment: Boolean,
    prefsEntry: String,
  ): Cipher {
    val key = getOrCreateKey(keyStore, alias, invalidatedByEnrollment)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    try {
      cipher.init(Cipher.ENCRYPT_MODE, key)
      return cipher
    } catch (error: InvalidKeyException) {
      BiometricsLogger.warn("Secure store key unusable on write; regenerating. " + error.message)
      keyStore.deleteEntry(alias)
      removePrefsEntry(prefsEntry)
    }
    val freshKey = getOrCreateKey(keyStore, alias, invalidatedByEnrollment)
    try {
      cipher.init(Cipher.ENCRYPT_MODE, freshKey)
    } catch (_: InvalidKeyException) {
      throw BiometricsError.InitFailed("Failed to initialize the secure store cipher.")
    }
    return cipher
  }

  /**
   * Creates the read cipher bound to the per-key Keystore key.
   * Missing alias or unrecoverable key state cleans up the stale entry and
   * surfaces [BiometricsError.NotFound], matching the documented storage
   * contract for enrollment-invalidated data.
   */
  private fun createReadCipher(
    keyStore: KeyStore,
    alias: String,
    prefsEntry: String,
    blob: ByteArray,
  ): Cipher {
    if (!keyStore.containsAlias(alias)) {
      removePrefsEntry(prefsEntry)
      throw secureDataNotFound()
    }
    val key = requireKey(keyStore, alias)
    if (blob.size <= GCM_IV_LENGTH_BYTES) {
      removePrefsEntry(prefsEntry)
      throw secureDataNotFound()
    }
    val iv = blob.copyOfRange(0, GCM_IV_LENGTH_BYTES)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    try {
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
      return cipher
    } catch (error: InvalidKeyException) {
      BiometricsLogger.warn("Secure store key unusable on read; dropping entry. " + error.message)
      keyStore.deleteEntry(alias)
      removePrefsEntry(prefsEntry)
      throw secureDataNotFound()
    }
  }

  /**
   * Gets or mints the per-key AES Keystore key.
   */
  private fun getOrCreateKey(
    keyStore: KeyStore,
    alias: String,
    invalidatedByEnrollment: Boolean,
  ): SecretKey {
    if (!keyStore.containsAlias(alias)) {
      generateKeyOrThrow(alias, invalidatedByEnrollment)
    }
    return requireKey(keyStore, alias)
  }

  /**
   * Generates the per-key key, retrying without the enrollment-invalidation
   * flag when the OEM keystore rejects it (Capgo parity).
   */
  private fun generateKeyOrThrow(
    alias: String,
    invalidatedByEnrollment: Boolean,
  ) {
    try {
      generateKey(alias, invalidatedByEnrollment)
    } catch (_: ProviderException) {
      if (!invalidatedByEnrollment) {
        throw BiometricsError.InitFailed(KEY_GENERATION_FAILED)
      }
      try {
        generateKey(alias, false)
      } catch (_: ProviderException) {
        throw BiometricsError.InitFailed(KEY_GENERATION_FAILED)
      }
    }
  }

  /**
   * Mints the AES-256-GCM per-operation key bound to strong biometrics.
   */
  private fun generateKey(
    alias: String,
    invalidatedByEnrollment: Boolean,
  ) {
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
    val builder =
      KeyGenParameterSpec
        .Builder(
          alias,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(AES_KEY_SIZE_BITS)
        .setUserAuthenticationRequired(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Per-operation strong-biometric authentication; required for
      // BiometricPrompt CryptoObject binding.
      builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
    } else {
      // -1 = authentication is required for every crypto operation.
      builder.setUserAuthenticationValidityDurationSeconds(-1)
    }
    builder.setInvalidatedByBiometricEnrollment(invalidatedByEnrollment)

    generator.init(builder.build())
    generator.generateKey()
  }

  /**
   * Loads the per-key secret key, mapping keystore failures to InitFailed.
   */
  private fun requireKey(
    keyStore: KeyStore,
    alias: String,
  ): SecretKey {
    val key =
      try {
        keyStore.getKey(alias, null) as? SecretKey
      } catch (_: GeneralSecurityException) {
        null
      }
    return key ?: throw BiometricsError.InitFailed("Keystore key is unreadable.")
  }

  /**
   * Loads the Android keystore, mapping failures to InitFailed.
   */
  private fun loadKeyStore(): KeyStore {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
    try {
      keyStore.load(null)
    } catch (_: Exception) {
      throw BiometricsError.InitFailed("Failed to load the Android keystore.")
    }
    return keyStore
  }

  /**
   * Persists the ciphertext (Base64 of IV + GCM ciphertext+tag) after the
   * prompt-authorized cipher completes the encryption.
   */
  private fun persistCiphertext(
    cipher: Cipher?,
    prefsEntry: String,
    value: String,
  ) {
    val activeCipher = cipher ?: throw BiometricsError.InitFailed("Biometric cipher is missing.")
    val encrypted =
      try {
        activeCipher.doFinal(value.toByteArray(Charsets.UTF_8))
      } catch (_: GeneralSecurityException) {
        throw BiometricsError.InitFailed("Failed to encrypt secure data.")
      }
    val iv = activeCipher.iv ?: throw BiometricsError.InitFailed("Secure store IV is missing.")
    prefs().edit().putString(prefsEntry, encodeBlob(iv + encrypted)).apply()
    BiometricsLogger.debug("Secure store entry persisted.")
  }

  /**
   * Decrypts the stored blob with the prompt-authorized cipher.
   */
  private fun decryptPayload(
    cipher: Cipher?,
    blob: ByteArray,
  ): String {
    val activeCipher = cipher ?: throw BiometricsError.InitFailed("Biometric cipher is missing.")
    val plaintext =
      try {
        activeCipher.doFinal(blob, GCM_IV_LENGTH_BYTES, blob.size - GCM_IV_LENGTH_BYTES)
      } catch (_: GeneralSecurityException) {
        throw BiometricsError.InitFailed("Failed to decrypt secure data.")
      }
    return String(plaintext, Charsets.UTF_8)
  }

  /**
   * Decodes a persisted ciphertext, treating corrupt entries as NOT_FOUND.
   */
  private fun decodeBlob(encoded: String): ByteArray =
    try {
      Base64.decode(encoded, Base64.NO_WRAP)
    } catch (_: IllegalArgumentException) {
      throw secureDataNotFound()
    }

  /**
   * Encodes the ciphertext blob for persistence.
   */
  private fun encodeBlob(blob: ByteArray): String = Base64.encodeToString(blob, Base64.NO_WRAP)

  /**
   * Runs the storage biometric ceremony with the given CryptoObject.
   *
   * The prompt is biometric-strong-only (DEVICE_CREDENTIAL cannot carry a
   * CryptoObject). Cancels itself after [maxAttempts] failed matches,
   * mirroring the Capgo behavior.
   *
   * @param completion Result callback, invoked exactly once.
   */
  private fun runCryptoPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String?,
    description: String?,
    negativeButtonText: String?,
    maxAttempts: Int,
    cryptoObject: BiometricPrompt.CryptoObject,
    completion: (Result<BiometricPrompt.AuthenticationResult>) -> Unit,
  ) {
    val completed = AtomicBoolean(false)
    var failedAttempts = 0
    val executor = ContextCompat.getMainExecutor(activity)

    val prompt =
      BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            activePrompt = null
            if (completed.compareAndSet(false, true)) {
              completion(Result.success(result))
            }
          }

          override fun onAuthenticationError(
            errorCode: Int,
            errString: CharSequence,
          ) {
            activePrompt = null
            if (completed.compareAndSet(false, true)) {
              completion(Result.failure(SecurePromptErrors.map(errorCode, errString.toString())))
            }
          }

          override fun onAuthenticationFailed() {
            failedAttempts++
            if (failedAttempts >= maxAttempts) {
              activePrompt?.cancelAuthentication()
              activePrompt = null
              if (completed.compareAndSet(false, true)) {
                completion(Result.failure(BiometricsError.Timeout(ATTEMPTS_EXHAUSTED_MESSAGE)))
              }
            }
          }
        },
      )
    activePrompt = prompt

    val promptInfo =
      BiometricPrompt.PromptInfo
        .Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setDescription(description)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText(negativeButtonText ?: DEFAULT_NEGATIVE_BUTTON)
        .build()

    Handler(Looper.getMainLooper()).post {
      prompt.authenticate(promptInfo, cryptoObject)
    }
  }

  /**
   * Missing-data error for the storage surface (stable NOT_FOUND code).
   */
  private fun secureDataNotFound(): BiometricsError =
    BiometricsError.NotFound(BiometricsErrorMessages.SECURE_DATA_NOT_FOUND)

  /**
   * Plugin-owned ciphertext storage.
   */
  private fun prefs(): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Removes a ciphertext entry (best effort).
   */
  private fun removePrefsEntry(prefsEntry: String) {
    prefs().edit().remove(prefsEntry).apply()
  }

  private companion object {
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val AES_KEY_SIZE_BITS = 256
    const val GCM_IV_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128
    const val PREFS_NAME = "BiometricsSecureStorage"
    const val DEFAULT_WRITE_TITLE = "Protect Data"
    const val DEFAULT_READ_TITLE = "Authenticate"
    const val DEFAULT_NEGATIVE_BUTTON = "Cancel"
    const val ATTEMPTS_EXHAUSTED_MESSAGE = "Too many failed biometric attempts."
    const val KEY_GENERATION_FAILED = "Keystore key generation failed."
  }
}
