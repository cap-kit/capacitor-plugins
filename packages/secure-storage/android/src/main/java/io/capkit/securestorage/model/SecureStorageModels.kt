package io.capkit.securestorage.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of the `getPluginVersion()` method.
 */
@Serializable
data class SecureStorageVersionResult(
  @SerialName("version")
  val version: String,
)

/**
 * Read result. `value` is null when the key does not exist.
 */
@Serializable
data class GetResult(
  @SerialName("value")
  val value: String?,
)

/**
 * Removal result.
 */
@Serializable
data class RemoveResult(
  @SerialName("existed")
  val existed: Boolean,
)

/**
 * Existence-check result.
 */
@Serializable
data class HasResult(
  @SerialName("exists")
  val exists: Boolean,
)

/**
 * Key-enumeration result.
 */
@Serializable
data class KeysResult(
  @SerialName("keys")
  val keys: List<String>,
)

/**
 * Batch-read result. Missing keys map to null.
 */
@Serializable
data class GetManyResult(
  @SerialName("values")
  val values: Map<String, String?>,
)

/**
 * Synchronization-state result.
 */
@Serializable
data class SynchronizeResult(
  @SerialName("synchronize")
  val synchronize: Boolean,
)
