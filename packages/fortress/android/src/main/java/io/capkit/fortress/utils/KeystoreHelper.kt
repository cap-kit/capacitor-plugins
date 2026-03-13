package io.capkit.fortress.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Helper for interacting with Android Keystore.
 *
 * This implementation provides secure storage using Android Keystore
 * with hardware-backed encryption when available, and StrongBox support
 * for enhanced security on supported devices.
 *
 * Architectural rules:
 * - Pure Kotlin, no Capacitor dependencies
 * - Stateless utility functions
 * - Uses AES-256-GCM for encryption
 */
object KeystoreHelper {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_PREFIX = "fortress_"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 12

  // Biometric signature settings (keep deterministic for backend verification)
  private const val SIGNATURE_ALGORITHM_EC = "SHA256withECDSA"
  private const val SIGNATURE_ALGORITHM_RSA = "SHA256withRSA"
  private const val EC_CURVE = "secp256r1" // aka NIST P-256 / prime256v1

  // -----------------------------------------------------------------------------
  // Error Types
  // -----------------------------------------------------------------------------

  sealed class KeystoreError(
    message: String,
  ) : Exception(message) {
    class UnableToGenerateKey : KeystoreError("Failed to generate encryption key")

    class UnableToEncrypt : KeystoreError("Failed to encrypt data")

    class UnableToDecrypt : KeystoreError("Failed to decrypt data")

    class UnableToDelete : KeystoreError("Failed to delete key")

    class KeyNotFound : KeystoreError("Key not found")

    class DataConversionFailed : KeystoreError("Data conversion failed")
  }

  // -----------------------------------------------------------------------------
  // StrongBox Detection
  // -----------------------------------------------------------------------------

  /**
   * Checks if the device has StrongBox support.
   *
   * StrongBox is a hardware security subsystem available on some Android devices
   * that provides an additional layer of security for cryptographic operations.
   */
  @Suppress("DEPRECATION")
  fun hasStrongBox(context: Context): Boolean =
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

      val builder =
        KeyGenParameterSpec
          .Builder(
            "${KEY_PREFIX}strongbox_check",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setIsStrongBoxBacked(true)

      keyGenerator.init(builder.build())
      keyGenerator.generateKey()

      true
    } catch (e: Exception) {
      false
    }

  // -----------------------------------------------------------------------------
  // Key Management
  // -----------------------------------------------------------------------------

  /**
   * Generates or retrieves an encryption key for the given alias.
   *
   * @param context Android context
   * @param alias Unique identifier for the key
   * @param requireStrongBox If true, only StrongBox-backed keys are accepted
   * @return The SecretKey for encryption/decryption
   * @throws KeystoreError if key generation fails
   */
  @Throws(KeystoreError::class)
  fun getOrCreateKey(
    context: Context,
    alias: String,
    requireStrongBox: Boolean = false,
  ): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
    keyStore.load(null)

    val keyAlias = "$KEY_PREFIX$alias"

    // Return existing key if available
    keyStore.getKey(keyAlias, null)?.let { key ->
      return key as SecretKey
    }

    // Generate new key
    return try {
      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

      val builder =
        KeyGenParameterSpec
          .Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(256)
          .setRandomizedEncryptionRequired(true)

      // Apply StrongBox if requested and available
      if (requireStrongBox) {
        builder.setIsStrongBoxBacked(true)
      }

      keyGenerator.init(builder.build())
      keyGenerator.generateKey()
    } catch (e: Exception) {
      throw KeystoreError.UnableToGenerateKey()
    }
  }

  /**
   * Checks whether a key exists in the Keystore.
   *
   * @param alias Unique identifier for the key
   * @return true if the key exists, false otherwise
   */
  fun hasAlias(alias: String): Boolean =
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)
      keyStore.containsAlias("$KEY_PREFIX$alias")
    } catch (e: Exception) {
      false
    }

  /**
   * Deletes a key from the Keystore.
   *
   * @param alias Unique identifier for the key
   * @throws KeystoreError if deletion fails
   */
  @Throws(KeystoreError::class)
  fun deleteKey(alias: String) {
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)
      keyStore.deleteEntry("$KEY_PREFIX$alias")
    } catch (e: Exception) {
      throw KeystoreError.UnableToDelete()
    }
  }

  // -----------------------------------------------------------------------------
  // Asymmetric Key Pair Management (Phase 3.2)
  // -----------------------------------------------------------------------------

  /**
   * Generates or retrieves an asymmetric P-256 key pair in Android Keystore.
   *
   * @param alias Unique identifier for the key pair
   * @param requireStrongBox If true, requests StrongBox-backed generation
   * @return Public key in PEM format
   * @throws KeystoreError if generation fails
   */
  @Throws(KeystoreError::class)
  @Suppress("DEPRECATION")
  fun getOrCreateKeyPair(
    alias: String,
    requireStrongBox: Boolean = false,
    cryptoStrategy: String = "auto",
    keySize: Int = 2048,
  ): String {
    val keyAlias = "$KEY_PREFIX$alias"

    return try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      val existingCertificate = keyStore.getCertificate(keyAlias)
      if (existingCertificate != null) {
        return toPem(existingCertificate.publicKey.encoded)
      }

      val useRsa = cryptoStrategy == "rsa"
      val keyAlgorithm = if (useRsa) KeyProperties.KEY_ALGORITHM_RSA else KeyProperties.KEY_ALGORITHM_EC
      val keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm, ANDROID_KEYSTORE)
      val builder =
        KeyGenParameterSpec
          .Builder(
            keyAlias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
          ).setDigests(KeyProperties.DIGEST_SHA256)
          .setUserAuthenticationRequired(false)

      if (useRsa) {
        builder
          .setKeySize(if (keySize == 4096) 4096 else 2048)
          .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
      } else {
        builder.setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
      }

      if (requireStrongBox) {
        builder.setIsStrongBoxBacked(true)
      }

      keyPairGenerator.initialize(builder.build())
      val keyPair = keyPairGenerator.generateKeyPair()

      val publicKey =
        if (useRsa) {
          keyPair.public as? RSAPublicKey
        } else {
          keyPair.public
        } ?: throw KeystoreError.UnableToGenerateKey()
      toPem(publicKey.encoded)
    } catch (e: Exception) {
      throw KeystoreError.UnableToGenerateKey()
    }
  }

  /**
   * Checks whether an asymmetric key pair exists in Android Keystore.
   *
   * @param alias Unique identifier for the key pair
   * @return true if key pair exists
   */
  fun hasKeyPair(alias: String): Boolean = hasAlias(alias)

  /**
   * Deletes an asymmetric key pair from Android Keystore.
   *
   * @param alias Unique identifier for the key pair
   * @throws KeystoreError if deletion fails
   */
  @Throws(KeystoreError::class)
  fun deleteKeyPair(alias: String) {
    deleteKey(alias)
  }

  /**
   * Signs payload data with the private key from the asymmetric key pair.
   *
   * @param alias Unique identifier for the key pair
   * @param payload Raw payload bytes to sign
   * @return Signature bytes
   * @throws KeystoreError if signing fails or key pair is missing
   */
  @Throws(KeystoreError::class)
  fun sign(
    alias: String,
    payload: ByteArray,
  ): ByteArray {
    val keyAlias = "$KEY_PREFIX$alias"

    return try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      val privateKey =
        keyStore.getKey(keyAlias, null) as? PrivateKey
          ?: throw KeystoreError.KeyNotFound()

      val signatureAlgorithm =
        if (privateKey.algorithm.equals(
            "RSA",
            ignoreCase = true,
          )
        ) {
          SIGNATURE_ALGORITHM_RSA
        } else {
          SIGNATURE_ALGORITHM_EC
        }
      val signature =
        Signature.getInstance(signatureAlgorithm).apply {
          initSign(privateKey)
          update(payload)
        }
      val result = signature.sign()
      result
    } catch (e: KeystoreError) {
      throw e
    } catch (e: android.security.keystore.UserNotAuthenticatedException) {
      // Logic: Specific mapping for biometric timeout/invalidation
      throw KeystoreError.KeyNotFound()
    } catch (e: Exception) {
      throw KeystoreError.UnableToEncrypt()
    }
  }

  // -----------------------------------------------------------------------------
  // Encryption/Decryption
  // -----------------------------------------------------------------------------

  /**
   * Encrypts data using the Android Keystore.
   *
   * @param context Android context
   * @param alias Unique identifier for the encryption key
   * @param data Plaintext data to encrypt
   * @return Encrypted data (IV + ciphertext + tag)
   * @throws KeystoreError if encryption fails
   */
  @Throws(KeystoreError::class)
  fun encrypt(
    context: Context,
    alias: String,
    data: ByteArray,
    requireStrongBox: Boolean = false,
  ): ByteArray =
    try {
      // Respect the requireStrongBox configuration from the Config layer
      val key = getOrCreateKey(context, alias, requireStrongBox)

      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, key)

      val ciphertext = cipher.doFinal(data)
      val iv = cipher.iv.copyOf()

      // Combine IV + ciphertext
      ByteArray(iv.size + ciphertext.size).apply {
        System.arraycopy(iv, 0, this, 0, iv.size)
        System.arraycopy(ciphertext, 0, this, iv.size, ciphertext.size)
      }
    } catch (e: Exception) {
      throw KeystoreError.UnableToEncrypt()
    }

  /**
   * Decrypts data using the Android Keystore.
   *
   * @param context Android context
   * @param alias Unique identifier for the encryption key
   * @param encryptedData Encrypted data (IV + ciphertext + tag)
   * @return Decrypted plaintext data
   * @throws KeystoreError if decryption fails
   */
  @Throws(KeystoreError::class)
  fun decrypt(
    context: Context,
    alias: String,
    encryptedData: ByteArray,
  ): ByteArray =
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      val keyAlias = "$KEY_PREFIX$alias"
      val key =
        keyStore.getKey(keyAlias, null) as? SecretKey
          ?: throw KeystoreError.KeyNotFound()

      // Extract IV and ciphertext
      val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
      val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

      val cipher = Cipher.getInstance(TRANSFORMATION)
      val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
      cipher.init(Cipher.DECRYPT_MODE, key, spec)

      cipher.doFinal(ciphertext)
    } catch (e: KeystoreError) {
      throw e
    } catch (e: Exception) {
      throw KeystoreError.UnableToDecrypt()
    }

  // -----------------------------------------------------------------------------
  // Convenience Methods for Strings
  // -----------------------------------------------------------------------------

  /**
   * Encrypts a string using the Android Keystore.
   *
   * @param context Android context
   * @param alias Unique identifier for the encryption key
   * @param value Plaintext string to encrypt
   * @return Encrypted data as Base64 string
   * @throws KeystoreError if encryption fails
   */
  @Throws(KeystoreError::class)
  fun encryptString(
    context: Context,
    alias: String,
    value: String,
    requireStrongBox: Boolean = false,
  ): ByteArray = encrypt(context, alias, value.toByteArray(Charsets.UTF_8), requireStrongBox)

  /**
   * Decrypts a string using the Android Keystore.
   *
   * @param context Android context
   * @param alias Unique identifier for the encryption key
   * @param encryptedData Encrypted data
   * @return Decrypted plaintext string
   * @throws KeystoreError if decryption fails
   */
  @Throws(KeystoreError::class)
  fun decryptString(
    context: Context,
    alias: String,
    encryptedData: ByteArray,
  ): String = String(decrypt(context, alias, encryptedData), Charsets.UTF_8)

  // -----------------------------------------------------------------------------
  // Clear All
  // -----------------------------------------------------------------------------

  /**
   * Clears all keys stored by this plugin.
   *
   * Note: This only deletes keys that start with the fortress prefix.
   */
  @Throws(KeystoreError::class)
  fun clearAll() {
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      keyStore.aliases().toList().forEach { alias ->
        if (alias.startsWith(KEY_PREFIX)) {
          keyStore.deleteEntry(alias)
        }
      }
    } catch (e: Exception) {
      throw KeystoreError.UnableToDelete()
    }
  }

  private fun toPem(keyBytes: ByteArray): String {
    val rawBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    val base64 = rawBase64.chunked(64).joinToString("\n")
    return "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----"
  }
}
