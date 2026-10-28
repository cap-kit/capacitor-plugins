package io.capkit.securestorage.config

import android.content.Context
import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data Transfer Object (DTO) for parsing capacitor.config.ts configuration.
 */
@Serializable
private data class SecureStorageConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("enableICloudKeychainSync")
  val enableICloudKeychainSync: Boolean = false,
  @SerialName("obfuscateKeys")
  val obfuscateKeys: Boolean = false,
  @SerialName("obfuscationPrefix")
  val obfuscationPrefix: String = "",
)

/**
 * Plugin configuration holder for the SecureStorage plugin.
 *
 * This class is responsible for reading and parsing static configuration values
 * defined under the `plugins.SecureStorage` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization in the load() phase.
 * - Configuration values are read-only at runtime.
 * - Consumed only by native code.
 */
class SecureStorageConfig(
  plugin: Plugin,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Android application context.
   * Accessible for native implementation components that require system services.
   */
  val context: Context = plugin.context

  /**
   * Enables verbose native logging via SecureStorageLogger.
   *
   * When true, additional debug information and lifecycle events are printed to Logcat.
   * Default: false
   */
  val verboseLogging: Boolean

  /**
   * Enables iCloud Keychain synchronization. iOS only; parsed for
   * API parity and ignored on Android.
   * Default: false
   */
  val enableICloudKeychainSync: Boolean

  /**
   * Base64-encodes stored key names (both tiers) on top of any prefix.
   * Obfuscation hides names from casual inspection; it is not encryption.
   * Default: false
   */
  val obfuscateKeys: Boolean

  /**
   * Prefix prepended to stored key names before optional encoding.
   * Default: '' (no namespacing beyond the tier scope)
   */
  val obfuscationPrefix: String

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val parsedConfig = parseConfig(plugin)

    verboseLogging = parsedConfig.verboseLogging
    enableICloudKeychainSync = parsedConfig.enableICloudKeychainSync
    obfuscateKeys = parsedConfig.obfuscateKeys
    obfuscationPrefix = parsedConfig.obfuscationPrefix
  }

  private companion object {
    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(plugin: Plugin): SecureStorageConfigData =
      try {
        val configJsonObject = plugin.config.getConfigJSON()
        if (configJsonObject != null) {
          json.decodeFromString<SecureStorageConfigData>(configJsonObject.toString())
        } else {
          SecureStorageConfigData()
        }
      } catch (_: Exception) {
        SecureStorageConfigData()
      }
  }
}
