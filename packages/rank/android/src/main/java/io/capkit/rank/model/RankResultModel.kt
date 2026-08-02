package io.capkit.rank.model

import kotlinx.serialization.Serializable

/**
 * Canonical result models for the Rank plugin (Android).
 *
 * These DTOs mirror the public JavaScript payloads returned by the plugin
 * methods and are serialized to a JSObject bridge payload via
 * kotlinx.serialization. Nullable/defaulted fields are omitted when unset
 * (the encoding Json instance does NOT set encodeDefaults), matching the
 * optional TypeScript properties and the iOS `toDictionary()` behavior.
 *
 * These models are consumed ONLY by the bridge (RankPlugin) layer.
 */
@Serializable
data class RankAvailabilityResult(
  /**
   * Indicates whether the native In-App Review UI is available.
   */
  val value: Boolean,
)

/**
 * Result of the `checkReviewEnvironment()` method.
 *
 * Mirrors the TypeScript `ReviewEnvironmentResult` interface:
 * - canRequestReview: whether the review dialog can be shown
 * - reason: diagnostic reason, only present when the dialog cannot be shown
 */
@Serializable
data class RankReviewEnvironmentResult(
  /**
   * True if the environment supports showing the review dialog.
   */
  val canRequestReview: Boolean,
  /**
   * Optional diagnostic reason when the review dialog cannot be shown.
   */
  val reason: String? = null,
)

/**
 * Result of the `getPluginVersion()` method.
 *
 * Mirrors the TypeScript `PluginVersionResult` interface:
 * - version: native plugin version synchronized from package.json
 */
@Serializable
data class RankPluginVersionResult(
  /**
   * The native plugin version string.
   */
  val version: String,
)
