package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getDisplayInfo()` method.
 *
 * Mirrors the TypeScript `DisplayInfo` interface:
 * - widthPx, heightPx: physical pixel dimensions
 * - densityDpi: screen density in DPI
 * - scale: device pixel density ratio
 * - refreshRateHz: display refresh rate in Hz
 */
@Serializable
data class DisplayInfoResult(
  val widthPx: Int,
  val heightPx: Int,
  val densityDpi: Int,
  val scale: Double,
  val refreshRateHz: Int,
)
