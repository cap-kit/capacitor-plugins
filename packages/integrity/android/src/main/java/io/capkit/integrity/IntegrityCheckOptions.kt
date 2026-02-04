package io.capkit.integrity

/**
 * Native representation of `Integrity.check()` options.
 *
 * This model mirrors the JavaScript options object and is used
 * to control how integrity checks are executed.
 *
 * Design principles:
 * - Independent from the Plugin (Bridge) layer
 * - Safe to use inside the native Implementation layer
 * - Derived exclusively from JS input
 * - Does NOT alter the public JS API shape
 *
 * Notes:
 * - Default values are applied in the Plugin layer
 * - The Implementation layer assumes normalized values
 */
data class IntegrityCheckOptions(
  /**
   * Desired strictness level for integrity checks.
   *
   * Supported values:
   * - "basic": minimal checks (root, emulator)
   * - "standard": adds debug and instrumentation checks
   * - "strict": enables all available heuristics
   */
  val level: String,
  /**
   * Whether debug-only information should be included
   * in returned integrity signals.
   *
   * When true, signals MAY contain a human-readable
   * `description` field for diagnostic purposes.
   */
  val includeDebugInfo: Boolean,
)
