package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getPowerState()` method.
 *
 * Mirrors the TypeScript `PowerState` interface:
 * - isLowPowerMode: whether battery saver is active
 * - thermalState: "nominal" | "fair" | "serious" | "critical" | "unknown"
 */
@Serializable
data class PowerStateResult(
  val isLowPowerMode: Boolean,
  val thermalState: String,
)
