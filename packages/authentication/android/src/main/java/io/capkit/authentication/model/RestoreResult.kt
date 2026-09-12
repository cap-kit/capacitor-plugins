package io.capkit.authentication.model

import kotlinx.serialization.Serializable

/**
 * Result of the restore-credential surface (`createRestoreCredential`,
 * `getRestoreCredential`, `clearRestoreCredential`).
 *
 * Mirrors the TypeScript `{ available: boolean; requestJson?: string }` shape.
 * `available` reports whether the device supports Restore Credentials (never
 * `unimplemented`); `requestJson` carries the stored credential only for `get`.
 */
@Serializable
data class RestoreResult(
  /**
   * Whether restore credentials are supported and the operation was reachable.
   */
  val available: Boolean,
  /**
   * The stored credential's requestJson, when one was retrieved.
   */
  val requestJson: String? = null,
)
