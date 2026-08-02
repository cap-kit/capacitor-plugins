package io.capkit.fortress.config

import com.getcapacitor.Plugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RawConfigData(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean = false,
  @SerialName("logLevel")
  val logLevel: String? = null,
  @SerialName("lockAfterMs")
  val lockAfterMs: Int? = null,
  @SerialName("enablePrivacyScreen")
  val enablePrivacyScreen: Boolean? = null,
  @SerialName("privacyOverlayText")
  val privacyOverlayText: String? = null,
  @SerialName("privacyOverlayImageName")
  val privacyOverlayImageName: String? = null,
  @SerialName("privacyOverlayShowText")
  val privacyOverlayShowText: Boolean? = null,
  @SerialName("privacyOverlayShowImage")
  val privacyOverlayShowImage: Boolean? = null,
  @SerialName("privacyOverlayTextColor")
  val privacyOverlayTextColor: String? = null,
  @SerialName("privacyOverlayBackgroundOpacity")
  val privacyOverlayBackgroundOpacity: Double? = null,
  @SerialName("privacyOverlayTheme")
  val privacyOverlayTheme: String? = null,
  @SerialName("obfuscationPrefix")
  val obfuscationPrefix: String? = null,
  @SerialName("requireStrongBox")
  val requireStrongBox: Boolean? = null,
  @SerialName("allowDevicePasscode")
  val allowDevicePasscode: Boolean? = null,
  @SerialName("fallbackStrategy")
  val fallbackStrategy: String? = null,
  @SerialName("biometricPromptText")
  val biometricPromptText: String? = null,
  @SerialName("prefix")
  val prefix: String? = null,
  @SerialName("allowCachedAuthentication")
  val allowCachedAuthentication: Boolean? = null,
  @SerialName("cachedAuthenticationTimeoutMs")
  val cachedAuthenticationTimeoutMs: Int? = null,
  @SerialName("cryptoStrategy")
  val cryptoStrategy: String? = null,
  @SerialName("keySize")
  val keySize: Int? = null,
  @SerialName("maxBiometricAttempts")
  val maxBiometricAttempts: Int? = null,
  @SerialName("lockoutDurationMs")
  val lockoutDurationMs: Int? = null,
  @SerialName("requireFreshAuthenticationMs")
  val requireFreshAuthenticationMs: Int? = null,
  @SerialName("encryptionAlgorithm")
  val encryptionAlgorithm: String? = null,
  @SerialName("persistSessionState")
  val persistSessionState: Boolean? = null,
)

class Config(
  plugin: Plugin,
) {
  private companion object {
    val ALLOWED_LOG_LEVELS = setOf("error", "warn", "info", "debug", "verbose")
    val ALLOWED_OVERLAY_THEMES = setOf("system", "light", "dark")
    val ALLOWED_FALLBACK_STRATEGIES = setOf("deviceCredential", "none", "systemDefault")
    val ALLOWED_ENCRYPTION_ALGORITHMS = setOf("AES-GCM", "AES-CBC")

    private val json =
      Json {
        ignoreUnknownKeys = true
        isLenient = true
      }

    private fun parseConfig(jsonString: String?): RawConfigData {
      if (jsonString.isNullOrBlank()) return RawConfigData()
      return try {
        json.decodeFromString<RawConfigData>(jsonString)
      } catch (_: Exception) {
        RawConfigData()
      }
    }
  }

  var verboseLogging: Boolean
  var logLevel: String
  var lockAfterMs: Int
  var enablePrivacyScreen: Boolean
  var privacyOverlayText: String
  var privacyOverlayImageName: String
  var privacyOverlayShowText: Boolean
  var privacyOverlayShowImage: Boolean
  var privacyOverlayTextColor: String
  var privacyOverlayBackgroundOpacity: Double
  var privacyOverlayTheme: String
  var obfuscationPrefix: String
  var requireStrongBox: Boolean
  var allowDevicePasscode: Boolean
  var fallbackStrategy: String
  var biometricPromptText: String
  var prefix: String
  var allowCachedAuthentication: Boolean
  var cachedAuthenticationTimeoutMs: Int
  var cryptoStrategy: String
  var keySize: Int
  var maxBiometricAttempts: Int
  var lockoutDurationMs: Int
  var requireFreshAuthenticationMs: Int
  var encryptionAlgorithm: String
  var persistSessionState: Boolean

  init {
    val configObject = plugin.config.getConfigJSON()
    val parsedConfig = parseConfig(configObject?.toString())

    verboseLogging = parsedConfig.verboseLogging

    logLevel =
      parsedConfig.logLevel
        ?: if (verboseLogging) {
          "debug"
        } else {
          "info"
        }

    lockAfterMs = parsedConfig.lockAfterMs ?: 60000

    enablePrivacyScreen = parsedConfig.enablePrivacyScreen ?: true

    privacyOverlayText = parsedConfig.privacyOverlayText?.takeIf { it.isNotBlank() } ?: ""

    privacyOverlayImageName = parsedConfig.privacyOverlayImageName?.takeIf { it.isNotBlank() } ?: ""

    privacyOverlayShowText = parsedConfig.privacyOverlayShowText ?: true

    privacyOverlayShowImage = parsedConfig.privacyOverlayShowImage ?: true

    privacyOverlayTextColor = parsedConfig.privacyOverlayTextColor?.takeIf { it.isNotBlank() } ?: ""

    privacyOverlayBackgroundOpacity = parsedConfig.privacyOverlayBackgroundOpacity ?: -1.0

    privacyOverlayTheme = parsedConfig.privacyOverlayTheme ?: "system"

    obfuscationPrefix = parsedConfig.obfuscationPrefix?.takeIf { it.isNotBlank() } ?: "ftrss_"

    requireStrongBox = parsedConfig.requireStrongBox ?: false

    allowDevicePasscode = parsedConfig.allowDevicePasscode ?: true

    fallbackStrategy = parsedConfig.fallbackStrategy ?: "systemDefault"

    biometricPromptText = parsedConfig.biometricPromptText?.takeIf { it.isNotBlank() } ?: "Cancel"

    prefix = parsedConfig.prefix?.takeIf { it.isNotBlank() } ?: ""

    allowCachedAuthentication = parsedConfig.allowCachedAuthentication ?: false

    cachedAuthenticationTimeoutMs = parsedConfig.cachedAuthenticationTimeoutMs ?: 30000

    cryptoStrategy = parsedConfig.cryptoStrategy ?: "auto"

    keySize = parsedConfig.keySize ?: 2048

    maxBiometricAttempts = parsedConfig.maxBiometricAttempts ?: 5

    lockoutDurationMs = parsedConfig.lockoutDurationMs ?: 30000

    requireFreshAuthenticationMs = parsedConfig.requireFreshAuthenticationMs ?: 0

    encryptionAlgorithm = parsedConfig.encryptionAlgorithm ?: "AES-GCM"

    persistSessionState = parsedConfig.persistSessionState ?: false
  }

  fun applyRuntimeOverrides(overrides: com.getcapacitor.JSObject) {
    getBooleanOverride(overrides, "verboseLogging")?.let { verboseLogging = it }
    getStringOverride(overrides, "logLevel")?.let {
      if (ALLOWED_LOG_LEVELS.contains(it)) {
        logLevel = it
      }
    }
    getIntOverride(overrides, "lockAfterMs")?.let {
      if (it >= 0) {
        lockAfterMs = it
      }
    }
    getBooleanOverride(overrides, "enablePrivacyScreen")?.let { enablePrivacyScreen = it }
    getStringOverride(overrides, "privacyOverlayText")?.let { privacyOverlayText = it }
    getStringOverride(overrides, "privacyOverlayImageName")?.let { privacyOverlayImageName = it }
    getBooleanOverride(overrides, "privacyOverlayShowText")?.let { privacyOverlayShowText = it }
    getBooleanOverride(overrides, "privacyOverlayShowImage")?.let { privacyOverlayShowImage = it }
    getStringOverride(overrides, "privacyOverlayTextColor")?.let { privacyOverlayTextColor = it }
    getDoubleOverride(overrides, "privacyOverlayBackgroundOpacity")?.let {
      if (it == -1.0 || (it >= 0.0 && it <= 1.0)) {
        privacyOverlayBackgroundOpacity = it
      }
    }
    getStringOverride(overrides, "privacyOverlayTheme")?.let {
      if (ALLOWED_OVERLAY_THEMES.contains(it)) {
        privacyOverlayTheme = it
      }
    }
    getStringOverride(overrides, "fallbackStrategy")?.let {
      if (ALLOWED_FALLBACK_STRATEGIES.contains(it)) {
        fallbackStrategy = it
      }
    }
    getBooleanOverride(overrides, "allowCachedAuthentication")?.let { allowCachedAuthentication = it }
    getIntOverride(overrides, "cachedAuthenticationTimeoutMs")?.let {
      if (it >= 0) {
        cachedAuthenticationTimeoutMs = it
      }
    }
    getIntOverride(overrides, "maxBiometricAttempts")?.let {
      if (it >= 1) {
        maxBiometricAttempts = it
      }
    }
    getIntOverride(overrides, "lockoutDurationMs")?.let {
      if (it >= 0) {
        lockoutDurationMs = it
      }
    }
    getIntOverride(overrides, "requireFreshAuthenticationMs")?.let {
      if (it >= 0) {
        requireFreshAuthenticationMs = it
      }
    }
    getStringOverride(overrides, "encryptionAlgorithm")?.let {
      if (ALLOWED_ENCRYPTION_ALGORITHMS.contains(it)) {
        encryptionAlgorithm = it
      }
    }
    getBooleanOverride(overrides, "persistSessionState")?.let { persistSessionState = it }
  }

  fun toRuntimeOverrides(): com.getcapacitor.JSObject {
    val overrides = com.getcapacitor.JSObject()
    overrides.put("verboseLogging", verboseLogging)
    overrides.put("logLevel", logLevel)
    overrides.put("lockAfterMs", lockAfterMs)
    overrides.put("enablePrivacyScreen", enablePrivacyScreen)
    overrides.put("privacyOverlayText", privacyOverlayText)
    overrides.put("privacyOverlayImageName", privacyOverlayImageName)
    overrides.put("privacyOverlayShowText", privacyOverlayShowText)
    overrides.put("privacyOverlayShowImage", privacyOverlayShowImage)
    overrides.put("privacyOverlayTextColor", privacyOverlayTextColor)
    overrides.put("privacyOverlayBackgroundOpacity", privacyOverlayBackgroundOpacity)
    overrides.put("privacyOverlayTheme", privacyOverlayTheme)
    overrides.put("fallbackStrategy", fallbackStrategy)
    overrides.put("allowCachedAuthentication", allowCachedAuthentication)
    overrides.put("cachedAuthenticationTimeoutMs", cachedAuthenticationTimeoutMs)
    overrides.put("maxBiometricAttempts", maxBiometricAttempts)
    overrides.put("lockoutDurationMs", lockoutDurationMs)
    overrides.put("requireFreshAuthenticationMs", requireFreshAuthenticationMs)
    overrides.put("encryptionAlgorithm", encryptionAlgorithm)
    overrides.put("persistSessionState", persistSessionState)
    return overrides
  }

  private fun getBooleanOverride(
    source: com.getcapacitor.JSObject,
    key: String,
  ): Boolean? {
    if (!source.has(key) || source.isNull(key)) {
      return null
    }
    val value = source.opt(key)
    return value as? Boolean
  }

  private fun getStringOverride(
    source: com.getcapacitor.JSObject,
    key: String,
  ): String? {
    if (!source.has(key) || source.isNull(key)) {
      return null
    }
    val value = source.opt(key)
    return value as? String
  }

  private fun getIntOverride(
    source: com.getcapacitor.JSObject,
    key: String,
  ): Int? {
    if (!source.has(key) || source.isNull(key)) {
      return null
    }
    val value = source.opt(key)
    return when (value) {
      is Int -> value
      is Long -> value.toInt()
      is Double -> value.toInt()
      else -> null
    }
  }

  private fun getDoubleOverride(
    source: com.getcapacitor.JSObject,
    key: String,
  ): Double? {
    if (!source.has(key) || source.isNull(key)) {
      return null
    }
    val value = source.opt(key)
    return when (value) {
      is Double -> value
      is Int -> value.toDouble()
      is Long -> value.toDouble()
      else -> null
    }
  }
}
