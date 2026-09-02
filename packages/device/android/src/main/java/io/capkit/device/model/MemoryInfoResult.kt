package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getMemoryInfo()` method.
 *
 * Mirrors the TypeScript `MemoryInfo` interface:
 * - physicalRam: total device RAM in bytes
 * - cpuCores: number of available CPU cores
 * - memoryClassMb: standard app memory budget in MB
 * - isLowRamDevice: whether the device is classified as low-RAM
 * - isEstimated: whether fallback values were used
 * - cpuUsagePercent: delta-based CPU usage (null on first call)
 * - memoryPressure: derived memory pressure level
 */
@Serializable
data class MemoryInfoResult(
  val physicalRam: Long,
  val cpuCores: Int,
  val memoryClassMb: Int,
  val isLowRamDevice: Boolean,
  val isEstimated: Boolean = false,
  val cpuUsagePercent: Double? = null,
  val memoryPressure: String = "unknown",
)
