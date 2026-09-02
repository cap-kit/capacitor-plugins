package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getAppVersion()` method.
 *
 * Mirrors the TypeScript `AppVersion` interface:
 * - version: app version name string
 * - buildNumber: integer build number
 */
@Serializable
data class AppVersionResult(
  val version: String,
  val buildNumber: Int,
)
