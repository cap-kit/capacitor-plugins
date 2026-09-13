package io.capkit.fortress.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Canonical result models for the Fortress plugin (Android).
 *
 * These DTOs mirror the public JavaScript payloads returned by the plugin
 * methods and are serialized to a JSObject bridge payload via
 * kotlinx.serialization. Nullable/defaulted fields are omitted when unset
 * (the encoding Json instance does NOT set encodeDefaults), matching the
 * optional TypeScript properties.
 *
 * These models are consumed ONLY by the bridge (FortressPlugin) layer and
 * are produced from the platform-agnostic results assembled by the
 * business layer (Fortress).
 */

@Serializable
data class PluginVersionResult(
  @SerialName("version")
  val version: String,
)

@Serializable
data class FortressRuntimeConfig(
  @SerialName("verboseLogging")
  val verboseLogging: Boolean,
  @SerialName("logLevel")
  val logLevel: String,
  @SerialName("lockAfterMs")
  val lockAfterMs: Int,
  @SerialName("enablePrivacyScreen")
  val enablePrivacyScreen: Boolean,
  @SerialName("privacyOverlayText")
  val privacyOverlayText: String,
  @SerialName("privacyOverlayImageName")
  val privacyOverlayImageName: String,
  @SerialName("privacyOverlayShowText")
  val privacyOverlayShowText: Boolean,
  @SerialName("privacyOverlayShowImage")
  val privacyOverlayShowImage: Boolean,
  @SerialName("privacyOverlayTextColor")
  val privacyOverlayTextColor: String,
  @SerialName("privacyOverlayBackgroundOpacity")
  val privacyOverlayBackgroundOpacity: Double,
  @SerialName("privacyOverlayTheme")
  val privacyOverlayTheme: String,
  @SerialName("fallbackStrategy")
  val fallbackStrategy: String,
  @SerialName("allowCachedAuthentication")
  val allowCachedAuthentication: Boolean,
  @SerialName("cachedAuthenticationTimeoutMs")
  val cachedAuthenticationTimeoutMs: Int,
  @SerialName("maxBiometricAttempts")
  val maxBiometricAttempts: Int,
  @SerialName("lockoutDurationMs")
  val lockoutDurationMs: Int,
  @SerialName("requireFreshAuthenticationMs")
  val requireFreshAuthenticationMs: Int,
  @SerialName("encryptionAlgorithm")
  val encryptionAlgorithm: String,
  @SerialName("persistSessionState")
  val persistSessionState: Boolean,
)

@Serializable
data class ValueResult(
  @SerialName("value")
  val value: String?,
)

@Serializable
data class HasKeyResult(
  @SerialName("exists")
  val exists: Boolean,
)

@Serializable
data class ObfuscatedKeyResult(
  @SerialName("obfuscated")
  val obfuscated: String,
)

@Serializable
data class CreateKeysResult(
  @SerialName("publicKey")
  val publicKey: String,
)

@Serializable
data class BiometricKeysExistResult(
  @SerialName("keysExist")
  val keysExist: Boolean,
)

@Serializable
data class CreateSignatureResult(
  @SerialName("success")
  val success: Boolean,
  @SerialName("signature")
  val signature: String,
)

@Serializable
data class RegisterWithChallengeResult(
  @SerialName("publicKey")
  val publicKey: String,
  @SerialName("signature")
  val signature: String,
)

@Serializable
data class AuthenticateWithChallengeResult(
  @SerialName("signature")
  val signature: String,
)

@Serializable
data class GenerateChallengePayloadResult(
  @SerialName("payload")
  val payload: String,
)

@Serializable
data class DeviceSecurityStatusResult(
  @SerialName("isBiometricsAvailable")
  val isBiometricsAvailable: Boolean,
  @SerialName("isBiometricsEnabled")
  val isBiometricsEnabled: Boolean,
  @SerialName("isDeviceSecure")
  val isDeviceSecure: Boolean,
  @SerialName("biometryType")
  val biometryType: String,
)

@Serializable
data class FortressSessionResult(
  @SerialName("isLocked")
  val isLocked: Boolean,
  @SerialName("lastActiveAt")
  val lastActiveAt: Long,
)

@Serializable
data class IsLockedResult(
  @SerialName("isLocked")
  val isLocked: Boolean,
)
