package io.capkit.settings.model

import kotlinx.serialization.Serializable

/**
 * Canonical result models for the Settings plugin (Android).
 *
 * These DTOs mirror the public JavaScript payloads returned by the plugin
 * methods and are serialized to a JSObject bridge payload via
 * kotlinx.serialization. Nullable/defaulted fields are omitted when unset
 * (the encoding Json instance does NOT set encodeDefaults), matching the
 * optional TypeScript properties and the iOS `toDictionary()` behavior.
 *
 * These models are consumed ONLY by the bridge (SettingsPlugin) layer.
 */
@Serializable
data class PluginVersionResult(
  /** The native plugin version string. */
  val version: String,
)
