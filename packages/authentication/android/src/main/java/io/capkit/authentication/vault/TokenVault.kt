package io.capkit.authentication.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * @file TokenVault.kt
 * Secure per-provider token vault backed by `EncryptedSharedPreferences`
 * (AES256-GCM values, AES256-SIV keys; master key lives in Android Keystore).
 *
 * Security contract (secure-token-storage spec):
 * - Native-only: raw token material never reaches JavaScript.
 * - Per-provider namespace: one encrypted preferences file per provider.
 * - `store` is exact-state: prior session tokens are cleared before writing, so a
 *   new sign-in never leaves stale refresh/auth-code material behind.
 *
 * Not JVM-testable (device runtime dependency); the mapping contract it applies is
 * proven by [TokenBundleCodec] on the JVM.
 */
class TokenVault(
  context: Context,
  private val provider: String,
) {
  private val preferences: SharedPreferences

  init {
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    preferences =
      EncryptedSharedPreferences.create(
        context,
        "$VAULT_FILE_PREFIX$provider",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      )
  }

  /**
   * Persists the token bundle for the vault provider, replacing any previous set.
   *
   * @param bundle the token set to persist (absent optional fields are dropped).
   */
  fun store(bundle: TokenBundle) {
    val editor = preferences.edit().clear()
    TokenBundleCodec.encode(provider, bundle).forEach { (key, value) ->
      editor.putString(key, value)
    }
    editor.apply()
  }

  /**
   * @return the persisted [TokenBundle], or `null` when no credential is stored.
   */
  fun read(): TokenBundle? = TokenBundleCodec.decode(provider, preferences.all.mapValues { it.value.toString() })

  /** Deletes every token for the vault provider. */
  fun delete() {
    preferences.edit().clear().apply()
  }

  private companion object {
    const val VAULT_FILE_PREFIX = "auth_vault_"
  }
}
