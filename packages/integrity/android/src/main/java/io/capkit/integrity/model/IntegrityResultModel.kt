package io.capkit.integrity.model

import kotlinx.serialization.Serializable

/**
 * Canonical result models for the Integrity plugin (Android).
 *
 * These DTOs mirror the public JavaScript payloads returned by the plugin
 * methods and are serialized to a JSObject bridge payload via
 * kotlinx.serialization. Nullable/defaulted fields are omitted when unset
 * (the encoding Json instance does NOT set encodeDefaults), matching the
 * optional TypeScript properties.
 *
 * These models are consumed ONLY by the bridge (IntegrityPlugin) layer and
 * are produced from the platform-agnostic report maps assembled by the
 * business layer (Integrity / assemble).
 */
@Serializable
data class IntegrityResultModel(
  /** Ordered list of all detected integrity signals. */
  val signals: List<IntegritySignalResult>,
  /** Numeric integrity score derived from signal confidence. */
  val score: Int,
  /** Convenience flag indicating whether the device should be considered compromised. */
  val compromised: Boolean,
  /** Static environment metadata describing the runtime context. */
  val environment: IntegrityEnvironmentResult,
  /** Informational explanation describing how the score was derived. */
  val scoreExplanation: IntegrityScoreExplanationResult? = null,
  /** Millisecond-precision UNIX timestamp of report generation. */
  val timestamp: Long,
)

/**
 * A single integrity signal detected on the current device.
 *
 * Mirrors the TypeScript `IntegritySignal` interface:
 * - id: stable identifier (MUST NOT be pattern-matched)
 * - category: high-level, platform-agnostic category
 * - confidence: one of low | medium | high
 * - description: optional diagnostic text (only when debug info is requested)
 * - metadata: optional diagnostic metadata (informational only)
 */
@Serializable
data class IntegritySignalResult(
  /** Stable identifier for the signal. */
  val id: String,
  /** High-level category of the signal. */
  val category: String,
  /** Confidence level of the detection: low | medium | high. */
  val confidence: String,
  /** Optional human-readable description. */
  val description: String? = null,
  /** Additional diagnostic metadata associated with the signal. */
  val metadata: Map<String, String>? = null,
)

/**
 * Summary of the execution environment in which the check was performed.
 *
 * Mirrors the TypeScript `IntegrityEnvironment` interface.
 */
@Serializable
data class IntegrityEnvironmentResult(
  /** Current platform: ios | android | web. */
  val platform: String,
  /** Whether the app runs in an emulator or simulator environment. */
  val isEmulator: Boolean,
  /** Whether the app was built in debug/development mode. */
  val isDebugBuild: Boolean,
)

/**
 * Describes how the integrity score was derived from detected signals.
 *
 * Mirrors the TypeScript `IntegrityScoreExplanation` interface.
 */
@Serializable
data class IntegrityScoreExplanationResult(
  /** Total number of detected signals. */
  val totalSignals: Int,
  /** Breakdown of signals by confidence level. */
  val byConfidence: IntegrityConfidenceBreakdownResult,
  /** List of signal identifiers that contributed to the score. */
  val contributors: List<String>,
)

/**
 * Breakdown of detected signals by confidence level.
 */
@Serializable
data class IntegrityConfidenceBreakdownResult(
  /** Number of high-confidence signals. */
  val high: Int,
  /** Number of medium-confidence signals. */
  val medium: Int,
  /** Number of low-confidence signals. */
  val low: Int,
)

/**
 * Result of the `presentBlockPage()` method.
 *
 * Mirrors the TypeScript `PresentBlockPageResult` interface:
 * - presented: whether the block page was actually presented.
 *
 * NOTE:
 * - Returning `presented: false` is NOT an error.
 */
@Serializable
data class BlockPageResult(
  /** Indicates whether the block page was actually presented. */
  val presented: Boolean,
)

/**
 * Result of the `getPluginVersion()` method.
 *
 * Mirrors the TypeScript `PluginVersionResult` interface:
 * - version: native plugin version synchronized from package.json.
 */
@Serializable
data class PluginVersionResult(
  /** The native plugin version string. */
  val version: String,
)
