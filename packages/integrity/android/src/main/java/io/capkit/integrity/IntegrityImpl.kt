package io.capkit.integrity

import android.content.Context
import io.capkit.integrity.emulator.IntegrityEmulatorChecks
import io.capkit.integrity.filesystem.IntegrityFilesystemChecks
import io.capkit.integrity.hook.IntegrityHookChecks
import io.capkit.integrity.remote.IntegrityRemoteAttestor
import io.capkit.integrity.root.IntegrityRootDetector
import io.capkit.integrity.runtime.IntegrityRuntimeChecks
import io.capkit.integrity.ui.IntegrityUISignals
import io.capkit.integrity.utils.IntegrityLogger
import java.util.Collections

/**
 * Native Android implementation for the Integrity plugin.
 *
 * CONTRACT:
 * - This class MUST NOT reference:
 *   - PluginCall
 *   - Capacitor APIs
 *   - Activities or UI components
 *
 * Responsibilities:
 * - Perform platform-specific integrity checks
 * - Interact with Android system APIs
 * - Produce platform-agnostic integrity signals
 *
 * Error handling:
 * - MUST throw typed IntegrityError on unrecoverable failures
 * - MUST NOT swallow fatal initialization errors
 */
class IntegrityImpl(
  private val context: Context,
) {
  // ---------------------------------------------------------------------------
  // Signal lifecycle & execution model
  // ---------------------------------------------------------------------------

  /**
   * Signal production in this implementation follows a two-phase model:
   *
   * 1. Early boot phase (optional, best-effort)
   *    - Signals may be captured before the Capacitor bridge is initialized.
   *    - Early signals are collected via the static `onApplicationCreate` entry point.
   *    - Captured signals are stored in a volatile in-memory buffer (`bootSignals`).
   *    - Early boot detection is opportunistic and NOT guaranteed.
   *
   * 2. Runtime phase (authoritative)
   *    - Signals are produced when `check(...)` is invoked from JavaScript.
   *    - Any previously captured boot signals are merged into the first report.
   *    - Subsequent checks operate exclusively on runtime detection.
   *
   * IMPORTANT SEMANTICS:
   * - Boot signals are best-effort and may be absent depending on
   *   process lifecycle, OS behavior, or warm starts.
   * - Boot signals do NOT provide stronger guarantees than runtime signals.
   * - The absence of boot signals does NOT indicate a clean environment.
   *
   * This design improves detection timing, not detection coverage.
   */
  companion object {
    /**
     * Volatile buffer for signals captured during early boot.
     */
    private val bootSignals = Collections.synchronizedList(mutableListOf<Map<String, Any>>())

    /**
     * Native entry point for MainActivity.onCreate.
     * Captures security signals before the Capacitor bridge is initialized.
     */
    @JvmStatic
    fun onApplicationCreate(context: Context) {
      // Capture basic root signals immediately at boot using the dedicated detector
      val signals =
        IntegrityRootDetector.checkRootSignals(
          IntegrityCheckOptions(
            level = "basic",
            includeDebugInfo = false,
          ),
          allowCache = false,
        )
      bootSignals.addAll(signals)
    }
  }

  // Negative cache for expensive integrity checks.
  // Caches only "no-signal" results for a short time window.
  private data class NegativeCacheEntry(
    val timestampMs: Long,
  )

  private val negativeCache = mutableMapOf<String, NegativeCacheEntry>()

  private val negativeCacheTtlMs = 30_000L

  // ---------------------------------------------------------------------------
  // Remote Attestation
  // ---------------------------------------------------------------------------

  /**
   * Stub for Google Play Integrity integration.
   * To be implemented in a future evolution step.
   */
  fun getPlayIntegritySignal(options: IntegrityCheckOptions): Map<String, Any>? =
    IntegrityRemoteAttestor.getPlayIntegritySignal(context, options)

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Cached immutable plugin configuration.
   */
  private lateinit var config: IntegrityConfig

  /**
   * Applies static plugin configuration.
   */
  fun updateConfig(newConfig: IntegrityConfig) {
    this.config = newConfig
    IntegrityLogger.verbose = newConfig.verboseLogging
    IntegrityLogger.debug(
      "Integrity configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Options orchestrator
  // ---------------------------------------------------------------------------

  /**
   * Executes the requested integrity checks and aggregates signals.
   */
  fun performCheck(options: IntegrityCheckOptions): Map<String, Any> {
    // Apply negative cache only for standard / strict levels.
    // Cached results represent a recent "no-signal" execution.
    if (options.level != "basic" && isNegativeCacheValid(options.level)) {
      IntegrityLogger.debug(
        "Negative cache hit for integrity check:",
        options.level,
      )

      return IntegrityReportBuilder.buildReport(
        emptyList(),
        isEmulator = false,
      )
    }

    val signals = mutableListOf<Map<String, Any>>()

    // --- BOOT SIGNALS ----------------------------------------------------
    // Merge signals captured during early boot and clear the buffer.
    synchronized(bootSignals) {
      signals.addAll(bootSignals)
      bootSignals.clear()
    }

    // --- BASIC -----------------------------------------------------------

    // Root filesystem & build-tag detection (cached, runtime)
    signals.addAll(
      IntegrityRootDetector.checkRootSignals(
        options = options,
        allowCache = true,
      ),
    )

    // Known root management packages (runtime-only)
    signals.addAll(
      IntegrityRootDetector.checkRootPackages(
        context = context,
        options = options,
      ),
    )

    // Sandbox escape heuristics (non-deterministic, filesystem-based)
    IntegrityFilesystemChecks
      .checkSandboxEscape(options)
      ?.let { signals.add(it) }

    val isEmulator = IntegrityEmulatorChecks.isEmulator()
    if (isEmulator) {
      signals.add(
        IntegritySignalBuilder.build(
          id = "android_emulator",
          category = "emulator",
          confidence = "high",
          description = "Execution environment matches known emulator characteristics",
          // Added metadata to identify common emulator properties
          metadata =
            mapOf(
              "model" to android.os.Build.MODEL,
              "manufacturer" to android.os.Build.MANUFACTURER,
              "hardware" to android.os.Build.HARDWARE,
            ),
          options = options,
        ),
      )
    }

    // --- STANDARD & STRICT -----------------------------------------------

    if (options.level != "basic") {
      // checkDebug returns a list of signals
      signals.addAll(IntegrityRuntimeChecks.checkDebugSignals(context, options))

      // UI and Overlay detection (RASP)
      IntegrityUISignals
        .checkOverlaySignals(context, options)
        ?.let { signals.add(it) }

      // --- HOOKING DETECTION --------------------------------------------

      // Frida detection via memory maps
      val memHook = IntegrityHookChecks.checkFridaMemory()

      if (memHook) {
        signals.add(
          IntegritySignalBuilder.build(
            id = "android_frida_memory",
            category = "hook",
            confidence = "high",
            description = "Process memory contains known instrumentation artifacts",
            options = options,
          ),
        )
      }

      // Frida detection via known ports
      val portHook = IntegrityHookChecks.checkFridaPorts()
      if (portHook) {
        signals.add(
          IntegritySignalBuilder.build(
            id = "android_frida_port",
            category = "hook",
            confidence = "medium",
            description = "Known instrumentation ports are reachable on localhost",
            options = options,
          ),
        )
      }

      // Signal correlation logic
      if (memHook && portHook) {
        signals.add(
          IntegritySignalBuilder.build(
            id = "android_frida_correlation_confirmed",
            category = "hook",
            confidence = "high",
            description = "Multiple instrumentation indicators detected simultaneously",
            metadata = mapOf("source" to "memory+port"),
            options = options,
          ),
        )
      }
    }

    // --- STRICT --------------------------------------------------------------

    if (options.level == "strict") {
      // Google Play Integrity (future remote attestation)
      IntegrityRemoteAttestor
        .getPlayIntegritySignal(context, options)
        ?.let { signals.add(it) }

      // Verify application signature integrity
      if (!IntegrityRuntimeChecks.checkAppSignature(context)) {
        signals.add(
          IntegritySignalBuilder.build(
            id = "android_signature_invalid",
            category = "tamper",
            confidence = "high",
            description = "Application signing information does not match expected format",
            // Added metadata to specify the source of the signature check failure
            metadata = mapOf("package" to context.packageName),
            options = options,
          ),
        )
      }
    }

    // Update negative cache only when no integrity signals are detected.
    // Any detected signal invalidates the cached clean state.
    if (options.level != "basic") {
      if (signals.isEmpty()) {
        updateNegativeCache(options.level)
      } else {
        clearNegativeCache(options.level)
      }
    }

    // Final report assembly using the dedicated builder
    return IntegrityReportBuilder.buildReport(
      signals,
      isEmulator,
    )
  }

  private fun cacheKey(level: String): String = "android:$level"

  private fun isNegativeCacheValid(level: String): Boolean {
    val entry = negativeCache[cacheKey(level)] ?: return false
    return System.currentTimeMillis() - entry.timestampMs <= negativeCacheTtlMs
  }

  private fun updateNegativeCache(level: String) {
    negativeCache[cacheKey(level)] =
      NegativeCacheEntry(System.currentTimeMillis())
  }

  private fun clearNegativeCache(level: String) {
    negativeCache.remove(cacheKey(level))
  }
}
