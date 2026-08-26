package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getBatteryExtras()` method.
 *
 * Mirrors the TypeScript `BatteryExtras` interface:
 * - chargeSource: "ac" | "usb" | "wireless" | "unknown"
 * - detailedState: "charging" | "full" | "unplugged" | "not-charging" | "unknown"
 * - health: Android-only battery health status
 * - temperature: Android-only battery temperature in Celsius
 */
@Serializable
data class BatteryExtrasResult(
  val chargeSource: String,
  val detailedState: String,
  val health: String? = null,
  val temperature: Double? = null,
)
