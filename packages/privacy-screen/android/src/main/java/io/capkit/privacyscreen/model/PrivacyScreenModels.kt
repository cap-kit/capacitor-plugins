package io.capkit.privacyscreen.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of the `getPluginVersion()` method.
 *
 * Mirrors the TypeScript `PluginVersionResult` interface:
 * - version: native plugin version synchronized from package.json
 */
@Serializable
data class PrivacyScreenVersionResult(
  /**
   * The native plugin version string.
   */
  @SerialName("version")
  val version: String,
)

/**
 * Result returned when privacy protection is toggled.
 *
 * Mirrors the TypeScript `PrivacyScreenActionResult` interface and the
 * official `@capacitor/privacy-screen` API.
 */
@Serializable
data class PrivacyScreenActionResult(
  /**
   * Whether the native operation completed.
   */
  @SerialName("success")
  val success: Boolean,
)

/**
 * Current privacy-screen state.
 *
 * Mirrors the TypeScript `PrivacyScreenStatus` interface and the
 * official `@capacitor/privacy-screen` API.
 */
@Serializable
data class PrivacyScreenStatus(
  /**
   * Whether privacy protection is currently enabled.
   */
  @SerialName("enabled")
  val enabled: Boolean,
)
