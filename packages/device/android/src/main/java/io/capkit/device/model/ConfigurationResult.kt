package io.capkit.device.model

import kotlinx.serialization.Serializable

/**
 * Result of the `getConfiguration()` method.
 *
 * Mirrors the TypeScript `DeviceConfiguration` interface:
 * - orientation: "portrait" | "landscape" | "unknown"
 * - isDarkMode: whether dark mode is active
 * - fontScale: user-configured font scale factor
 * - idiom: "phone" | "tablet"
 * - screenSize: "small" | "normal" | "large" | "xlarge" | "unknown"
 */
@Serializable
data class ConfigurationResult(
  val orientation: String,
  val isDarkMode: Boolean,
  val fontScale: Double,
  val idiom: String,
  val screenSize: String,
)
