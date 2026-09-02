package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getPluginVersion()` method.
 *
 * Mirrors the TypeScript `PluginVersionResult` interface:
 * - version: native plugin version synchronized from package.json
 */
@Serializable
data class DevicePluginVersionResult(
  /**
   * The native plugin version string.
   */
  val version: String,
)
