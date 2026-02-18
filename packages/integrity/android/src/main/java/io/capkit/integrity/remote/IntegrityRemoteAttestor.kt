package io.capkit.integrity.remote

import android.content.Context
import io.capkit.integrity.models.IntegrityCheckOptions

/**
 * Handles remote attestation signals (Play Integrity API).
 *
 * IMPORTANT:
 * - Play Integrity is NOT implemented yet.
 * - Unavailability is reported explicitly.
 * - The emitted signal is observational only and LOW confidence.
 */
object IntegrityRemoteAttestor {
  /**
   * Returns a LOW confidence signal indicating that Play Integrity
   * attestation is not implemented or not available.
   *
   * The signal is emitted only when strict mode is requested.
   */
  fun getPlayIntegritySignal(
    context: Context,
    options: IntegrityCheckOptions,
  ): Map<String, Any>? {
    if (options.level != "strict") {
      return null
    }

    val signal =
      mutableMapOf<String, Any>(
        "id" to "android_play_integrity_unavailable",
        "category" to "environment",
        "confidence" to "low",
        "metadata" to
          mapOf(
            "attestation" to "unsupported",
            "provider" to "play_integrity",
            "reason" to "not_implemented",
          ),
      )

    if (options.includeDebugInfo) {
      signal["description"] =
        "Google Play Integrity attestation is not implemented or not available"
    }

    return signal
  }
}
