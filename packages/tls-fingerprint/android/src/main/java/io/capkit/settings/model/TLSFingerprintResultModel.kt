package io.capkit.tlsfingerprint.model

/**
 * Canonical, nominal result model for TLS fingerprint validation operations on Android.
 * This data class is exchanged between the native implementation (TLSFingerprintImpl)
 * and the bridge (TLSFingerprintPlugin) and serialized to JavaScript via a JSObject.
 *
 * Fields mirror the public JS payload:
 * - actualFingerprint: server fingerprint used for matching
 * - fingerprintMatched: whether the fingerprint check succeeded (true) or not (false)
 * - matchedFingerprint: the fingerprint that matched (only present for fingerprint mode)
 * - excludedDomain: indicates an excluded-domain bypass (true when applicable)
 * - mode: active mode: "fingerprint" | "excluded"
 * - error: human-readable error (empty on success/match)
 * - errorCode: canonical error code string (empty on success)
 */
data class TLSFingerprintResultModel(
  /** Actual server fingerprint used for matching (if available). */
  val actualFingerprint: String?,
  /** Indicates whether the fingerprint check succeeded or not. */
  val fingerprintMatched: Boolean,
  /** The fingerprint that matched (if fingerprint mode). */
  val matchedFingerprint: String? = null,
  /** Whether the host is excluded and pinning bypass uses system trust. */
  val excludedDomain: Boolean? = null,
  /** Active mode: "fingerprint" | "excluded". */
  val mode: String? = null,
  /** Human-readable error description, if any. */
  val error: String? = null,
  /** Canonical error code for JS consumption. */
  val errorCode: String? = null,
)
