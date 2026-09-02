package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getStorageInfo()` method.
 *
 * Mirrors the TypeScript `StorageInfo` interface:
 * - totalBytes: total volume capacity
 * - freeBytes: available storage
 * - usedBytes: consumed storage
 * - usedPercent: usage percentage (0–100)
 * - isEstimated: whether fallback zero values were returned
 */
@Serializable
data class StorageInfoResult(
  val totalBytes: Long,
  val freeBytes: Long,
  val usedBytes: Long,
  val usedPercent: Double,
  val isEstimated: Boolean = false,
)
