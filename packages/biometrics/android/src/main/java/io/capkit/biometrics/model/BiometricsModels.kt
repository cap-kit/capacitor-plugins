package io.capkit.biometrics.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of the `getPluginVersion()` method.
 */
@Serializable
data class BiometricsVersionResult(
  @SerialName("version")
  val version: String,
)

/**
 * Result of `isAvailable()`.
 */
@Serializable
data class IsAvailableResult(
  @SerialName("isAvailable")
  val isAvailable: Boolean,
)

/**
 * Result of `isEnrolled()`.
 */
@Serializable
data class IsEnrolledResult(
  @SerialName("isEnrolled")
  val isEnrolled: Boolean,
)

/**
 * Result of `getBiometricType()`.
 */
@Serializable
data class BiometricTypeResult(
  @SerialName("biometryType")
  val biometryType: String,
)

/**
 * Result of `getBiometricTypes()`.
 */
@Serializable
data class BiometricTypesResult(
  @SerialName("biometryTypes")
  val biometryTypes: List<String>,
)

/**
 * Result of `hasDeviceCredential()`.
 */
@Serializable
data class HasDeviceCredentialResult(
  @SerialName("hasDeviceCredential")
  val hasDeviceCredential: Boolean,
)

/**
 * Result of `isDeviceSecure()`.
 */
@Serializable
data class IsDeviceSecureResult(
  @SerialName("isDeviceSecure")
  val isDeviceSecure: Boolean,
)

/**
 * Result of `getBiometricStrengthLevel()`.
 */
@Serializable
data class BiometricStrengthResult(
  @SerialName("strengthLevel")
  val strengthLevel: String,
)

/**
 * Result of `getAuthenticationType()`.
 */
@Serializable
data class AuthenticationTypeResult(
  @SerialName("authenticationType")
  val authenticationType: String,
)

/**
 * Result of `getSecureData()`.
 */
@Serializable
data class SecureDataResult(
  @SerialName("value")
  val value: String,
)

/**
 * Result of `isSecureDataSaved()`.
 */
@Serializable
data class IsSecureDataSavedResult(
  @SerialName("isSaved")
  val isSaved: Boolean,
)
