package io.capkit.integrity

/**
 * Helper responsible for constructing standardized integrity signal maps.
 *
 * This mirrors the logic found in iOS IntegrityUtils to ensure
 * cross-platform payload consistency.
 */
object IntegritySignalBuilder {
  /**
   * Builds a JSON-serializable integrity signal.
   *
   * CONTRACT:
   * - id MUST be stable across platforms
   * - category MUST be platform-agnostic
   * - confidence MUST be one of: low | medium | high
   */
  fun build(
    id: String,
    category: String,
    confidence: String,
    description: String? = null,
    metadata: Map<String, Any>? = null,
    options: IntegrityCheckOptions,
  ): Map<String, Any> {
    val signal =
      mutableMapOf<String, Any>(
        "id" to id,
        "category" to category,
        "confidence" to confidence,
      )

    // Include description only if requested in options to avoid leaking diagnostics
    if (options.includeDebugInfo && description != null) {
      signal["description"] = description
    }

    // Include metadata if present
    if (metadata != null) {
      signal["metadata"] = metadata
    }

    return signal
  }
}

object IntegritySignalIds {
  // Root
  const val ANDROID_ROOT_SU = "android_root_su"
  const val ANDROID_TEST_KEYS = "android_test_keys"
  const val ANDROID_ROOT_PACKAGE = "android_root_package"

  // Emulator
  const val ANDROID_EMULATOR = "android_emulator"

  // Debug
  const val ANDROID_DEBUGGER_ATTACHED = "android_debugger_attached"
  const val ANDROID_RUNTIME_DEBUGGABLE = "android_runtime_debuggable"

  // Hook / Instrumentation
  const val ANDROID_FRIDA_MEMORY = "android_frida_memory"
  const val ANDROID_FRIDA_PORT = "android_frida_port"
  const val ANDROID_FRIDA_CORRELATION = "android_frida_correlation_confirmed"

  // Tamper
  const val ANDROID_SANDBOX_ESCAPED = "android_sandbox_escaped"
  const val ANDROID_SIGNATURE_INVALID = "android_signature_invalid"
}
