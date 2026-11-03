package io.capkit.securestorage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyProperties
import android.util.Base64
import io.capkit.securestorage.config.SecureStorageConfig
import io.capkit.securestorage.error.SecureStorageError
import io.capkit.securestorage.error.SecureStorageErrorMessages
import io.capkit.securestorage.logger.SecureStorageLogger
import io.capkit.securestorage.utils.SecureStorageUtils
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Platform-specific native implementation for the SecureStorage plugin.
 *
 * Hardware-backed key/value storage: each value is encrypted with AES-GCM
 * under a Keystore-held master key, and only opaque ciphertext lives in
 * SharedPreferences. MUST NOT depend on Capacitor bridge APIs or
 * PluginCall objects.
 *
 * Transparent store: no vault, no lock state, no authentication. Reads are
 * always available.
 */
class SecureStorageImpl(
  private val context: Context,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: SecureStorageConfig

  private val prefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  private companion object {
    const val PREFS_NAME = "securestorage_encrypted"
    const val PREFS_INSECURE_NAME = "securestorage_insecure"
    const val MASTER_ALIAS = "securestorage_master_key"
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_TAG_BITS = 128
    const val IV_BYTES = 12
  }

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * MUST be called exactly once during the plugin load phase.
   */
  fun updateConfig(newConfig: SecureStorageConfig) {
    this.config = newConfig
    SecureStorageLogger.verbose = newConfig.verboseLogging
    notePrefix(newConfig.obfuscationPrefix)
    SecureStorageLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Key/value operations
  // ---------------------------------------------------------------------------

  /**
   * Stores a value under a key, overwriting any previous entry.
   */
  fun set(
    key: String,
    value: String,
  ) {
    try {
      prefs.edit().putString(encodeName(key), encryptToPayload(value)).commit()
    } catch (error: SecureStorageError) {
      throw error
    } catch (_: Exception) {
      throw SecureStorageError.InitFailed(SecureStorageErrorMessages.INTERNAL_ERROR)
    }
  }

  /**
   * Reads the value for a key, or null when absent.
   */
  fun get(key: String): String? {
    for (name in nameCandidates(key)) {
      val stored = prefs.getString(name, null) ?: continue
      try {
        return decryptFromPayload(stored)
      } catch (error: SecureStorageError) {
        throw error
      } catch (_: Exception) {
        throw SecureStorageError.InitFailed(SecureStorageErrorMessages.INTERNAL_ERROR)
      }
    }
    return null
  }

  /**
   * Removes the entry for a key.
   *
   * @return Whether an entry was present.
   */
  fun remove(key: String): Boolean {
    var existed = false
    for (name in nameCandidates(key)) {
      if (prefs.contains(name)) {
        existed = true
      }
      prefs.edit().remove(name).commit()
    }
    return existed
  }

  /**
   * Checks whether an entry exists without reading it.
   */
  fun has(key: String): Boolean = nameCandidates(key).any { prefs.contains(it) }

  /**
   * Lists every stored key.
   */
  fun keys(): List<String> = prefs.all.keys.map { decodeName(it) }

  /**
   * Reads several keys in one call. Missing keys map to null.
   */
  fun getMany(keys: List<String>): Map<String, String?> = keys.associateWith { get(it) }

  /**
   * Removes every stored entry and rotates the master encryption key,
   * so previously captured ciphertext can never be decrypted again.
   */
  fun clear() {
    prefs.edit().clear().commit()
    deleteMasterKey()
  }

  // ---------------------------------------------------------------------------
  // Synchronization (iCloud has no Android equivalent; parity no-ops)
  // ---------------------------------------------------------------------------

  fun setSynchronize(enabled: Boolean) {
  }

  fun isSynchronized(): Boolean = false

  fun setDefaultAccess(access: String) {
  }

  // ---------------------------------------------------------------------------
  // Insecure tier (plaintext SharedPreferences, group-namespaced)
  // ---------------------------------------------------------------------------

  var insecureGroup: String = "CapacitorStorage"

  /**
   * Session obfuscation overrides (see [configure]). Null falls back to
   * the static configuration values.
   */
  var runtimeObfuscateKeys: Boolean? = null
  var runtimeObfuscationPrefix: String? = null

  private fun effectiveObfuscate(): Boolean = runtimeObfuscateKeys ?: config.obfuscateKeys

  private fun effectivePrefix(): String = runtimeObfuscationPrefix ?: config.obfuscationPrefix

  /**
   * Encodes a key name for storage: optional prefix plus optional base64.
   * Obfuscation hides names from casual inspection; it is not encryption.
   */
  private fun encodeName(key: String): String {
    val body = if (effectiveObfuscate()) SecureStorageUtils.encodeB64(key) else key
    return "${effectivePrefix()}$body"
  }

  /**
   * Candidate stored names, primary first. Reads try each in order so
   * toggling the flag never orphans existing entries.
   */
  private val seenPrefixes = mutableSetOf<String>()

  private fun notePrefix(prefix: String) {
    // The empty (default) prefix must be recorded too: entries written
    // before a runtime prefix change carry no prefix, and reads must
    // keep spanning it or those entries would be orphaned.
    seenPrefixes.add(prefix)
  }

  /**
   * Prefixes observed this session, current first. Reads span all of
   * them so prefix changes never orphan existing entries. The empty
   * prefix is always included as a legacy fallback: entries written
   * before any prefix existed carry no prefix.
   */
  private fun prefixVariants(): List<String> {
    val ordered = mutableListOf(effectivePrefix())
    for (seen in seenPrefixes) {
      if (seen != effectivePrefix()) {
        ordered.add(seen)
      }
    }
    if ("" !in ordered) {
      ordered.add("")
    }
    return ordered
  }

  private fun nameCandidates(key: String): List<String> {
    val bodies =
      if (effectiveObfuscate()) {
        listOf(SecureStorageUtils.encodeB64(key), key)
      } else {
        listOf(key, SecureStorageUtils.encodeB64(key))
      }
    val names = mutableListOf<String>()
    for (prefix in prefixVariants()) {
      for (body in bodies) {
        names.add("$prefix$body")
      }
    }
    return names
  }

  /**
   * Best-effort inverse of the name encoding: strips the prefix, then
   * base64-decodes only when the value round-trips cleanly.
   */
  private fun decodeName(stored: String): String {
    val remainders = mutableListOf(stored)
    for (prefix in prefixVariants()) {
      if (prefix.isNotEmpty() && stored.startsWith(prefix)) {
        remainders.add(stored.removePrefix(prefix))
      }
    }
    for (remainder in remainders) {
      SecureStorageUtils.decodeB64(remainder)?.let { return it }
    }
    val current = effectivePrefix()
    return if (current.isNotEmpty() && stored.startsWith(current)) {
      stored.removePrefix(current)
    } else {
      stored
    }
  }

  private val insecurePrefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_INSECURE_NAME, Context.MODE_PRIVATE)
  }

  fun configure(
    group: String?,
    obfuscateKeys: Boolean? = null,
    obfuscationPrefix: String? = null,
  ) {
    if (!group.isNullOrEmpty()) {
      insecureGroup = group
    }
    if (obfuscateKeys != null) {
      runtimeObfuscateKeys = obfuscateKeys
    }
    if (obfuscationPrefix != null) {
      runtimeObfuscationPrefix = obfuscationPrefix
      notePrefix(obfuscationPrefix)
    }
  }

  private fun insecureKey(key: String): String = "$insecureGroup.${encodeName(key)}"

  private fun insecureCandidates(key: String): List<String> {
    val bodies =
      if (effectiveObfuscate()) {
        listOf(SecureStorageUtils.encodeB64(key), key)
      } else {
        listOf(key, SecureStorageUtils.encodeB64(key))
      }
    val names = mutableListOf<String>()
    for (prefix in prefixVariants()) {
      for (body in bodies) {
        names.add("$insecureGroup.$prefix$body")
      }
    }
    return names
  }

  fun setInsecure(
    key: String,
    value: String,
  ) {
    insecurePrefs.edit().putString(insecureKey(key), value).commit()
  }

  fun getInsecure(key: String): String? {
    for (name in insecureCandidates(key)) {
      insecurePrefs.getString(name, null)?.let { return it }
    }
    return null
  }

  fun removeInsecure(key: String): Boolean {
    var existed = false
    for (name in insecureCandidates(key)) {
      if (insecurePrefs.contains(name)) {
        existed = true
      }
      insecurePrefs.edit().remove(name).commit()
    }
    return existed
  }

  fun hasInsecure(key: String): Boolean = insecureCandidates(key).any { insecurePrefs.contains(it) }

  fun keysInsecure(): List<String> {
    val prefix = "$insecureGroup."
    return insecurePrefs.all.keys
      .filter { it.startsWith(prefix) }
      .map { decodeName(it.removePrefix(prefix)) }
  }

  fun clearInsecure() {
    val prefix = "$insecureGroup."
    val doomed = insecurePrefs.all.keys.filter { it.startsWith(prefix) }
    if (doomed.isEmpty()) {
      return
    }
    insecurePrefs
      .edit()
      .apply {
        doomed.forEach { remove(it) }
      }.commit()
  }

  // ---------------------------------------------------------------------------
  // Keystore mechanics
  // ---------------------------------------------------------------------------

  private fun masterKey(): SecretKey {
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)
      (keyStore.getEntry(MASTER_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
        return it.secretKey
      }

      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      keyGenerator.init(
        android.security.keystore.KeyGenParameterSpec
          .Builder(
            MASTER_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setRandomizedEncryptionRequired(true)
          .build(),
      )
      return keyGenerator.generateKey()
    } catch (_: Exception) {
      throw SecureStorageError.Unavailable(SecureStorageErrorMessages.UNAVAILABLE)
    }
  }

  private fun deleteMasterKey() {
    try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)
      if (keyStore.containsAlias(MASTER_ALIAS)) {
        keyStore.deleteEntry(MASTER_ALIAS)
      }
    } catch (_: Exception) {
      // Best effort: preferences are already wiped; a stale master key
      // alone decrypts nothing without its ciphertext.
    }
  }

  private fun encryptToPayload(plainText: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, masterKey())
    val iv = cipher.iv
    require(iv.size == IV_BYTES) { "Unexpected GCM IV length." }
    val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
    val combined = iv + cipherText
    return Base64.encodeToString(combined, Base64.NO_WRAP)
  }

  private fun decryptFromPayload(payload: String): String {
    val combined = Base64.decode(payload, Base64.NO_WRAP)
    require(combined.size > IV_BYTES) { "Truncated payload." }
    val iv = combined.copyOfRange(0, IV_BYTES)
    val cipherText = combined.copyOfRange(IV_BYTES, combined.size)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
    return String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
  }
}
