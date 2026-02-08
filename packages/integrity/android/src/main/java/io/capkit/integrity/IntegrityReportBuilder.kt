package io.capkit.integrity

/**
 * Helper responsible for assembling the final integrity report payload.
 *
 * Responsibilities:
 * - Aggregate signals
 * - Compute integrity score based on signal confidence
 * - Build environment metadata
 * - Produce a JS-bridge-safe map
 *
 * This builder contains NO platform-specific logic.
 */
object IntegrityReportBuilder {
  /**
   * Default compromise threshold.
   *
   * POLICY:
   * - A single high-confidence signal is sufficient to mark
   *   the environment as compromised.
   *
   * NOTE:
   * - This value MUST remain aligned across platforms.
   */
  private const val COMPROMISE_THRESHOLD = 30

  /**
   * Builds the final integrity report returned to the JavaScript layer.
   *
   * CONTRACT:
   * - Output structure MUST remain platform-agnostic
   * - Scoring MUST be deterministic
   * - No enforcement logic may be introduced here
   */
  fun buildReport(
    signals: List<Map<String, Any>>,
    isEmulator: Boolean,
    platform: String = "android",
  ): Map<String, Any> {
    val score = computeScore(signals)
    val scoreExplanation = buildScoreExplanation(signals)

    return mapOf(
      // Ordered list of all detected integrity signals.
      "signals" to signals,
      // Numeric integrity score derived from signal confidence.
      "score" to score,
      // Convenience flag indicating whether the device
      // should be considered compromised.
      "compromised" to (score >= COMPROMISE_THRESHOLD),
      // Static environment metadata describing the runtime context.
      "environment" to
        mapOf(
          "platform" to platform,
          "isEmulator" to isEmulator,
          // Reserved for future use
          "isDebugBuild" to false,
        ),
      // Informational explanation describing how the score was derived.
      // This metadata MUST NOT be treated as a security decision.
      "scoreExplanation" to scoreExplanation,
      // Millisecond-precision UNIX timestamp of report generation.
      "timestamp" to System.currentTimeMillis(),
    )
  }

  // ---------------------------------------------------------------------------
  // Scoring
  // ---------------------------------------------------------------------------

  /**
   * Computes a heuristic risk score from collected signals.
   *
   * Scoring policy (aligned with iOS):
   * - high   -> 30 points
   * - medium -> 15 points
   * - low    -> 5 points
   */
  private fun computeScore(signals: List<Map<String, Any>>): Int {
    return signals.sumOf {
      when (it["confidence"]) {
        "high" -> 30
        "medium" -> 15
        "low" -> 5
        else -> 0
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Score explanation
  // ---------------------------------------------------------------------------

  /**
   * Builds an informational explanation describing how the integrity
   * score was derived from the detected signals.
   *
   * IMPORTANT:
   * - This metadata is informational only.
   * - It MUST NOT influence scoring or enforcement.
   */
  private fun buildScoreExplanation(signals: List<Map<String, Any>>): Map<String, Any> {
    var high = 0
    var medium = 0
    var low = 0

    val contributors = mutableListOf<String>()

    for (signal in signals) {
      (signal["id"] as? String)?.let { contributors.add(it) }

      when (signal["confidence"]) {
        "high" -> high++
        "medium" -> medium++
        "low" -> low++
      }
    }

    return mapOf(
      "totalSignals" to signals.size,
      "byConfidence" to
        mapOf(
          "high" to high,
          "medium" to medium,
          "low" to low,
        ),
      "contributors" to contributors,
    )
  }
}
