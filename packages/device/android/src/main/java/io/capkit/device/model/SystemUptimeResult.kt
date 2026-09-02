package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getSystemUptime()` method.
 *
 * Mirrors the TypeScript `SystemUptime` interface:
 * - uptimeSeconds: time since last boot in seconds
 */
@Serializable
data class SystemUptimeResult(
  val uptimeSeconds: Double,
)
