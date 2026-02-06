package io.capkit.integrity

import android.content.Context
import android.content.pm.PackageManager
import io.capkit.integrity.utils.IntegrityLogger
import java.io.File
import java.net.Socket

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
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Cached immutable plugin configuration.
   *
   * CONTRACT:
   * - Injected exactly once by the Plugin layer during load()
   * - Treated as read-only afterwards
   *
   * WARNING:
   * - Accessing this before updateConfig() is a programming error.
   */
  private lateinit var config: IntegrityConfig

  /**
   * Applies static plugin configuration.
   *
   * CONTRACT:
   * - MUST be called exactly once
   * - Caller (Plugin layer) guarantees lifecycle correctness
   *
   * NOTE:
   * - Defensive double-initialization guards are intentionally omitted
   *   to keep the Impl layer minimal and deterministic.
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
   *
   * CONTRACT:
   * - Synchronous execution only
   * - MUST NOT perform UI operations
   * - MUST return a fully structured, platform-agnostic report
   *
   * NOTE:
   * - Scoring and compromise threshold are heuristic-based.
   * - This method assumes configuration has already been injected.
   */
  fun performCheck(options: IntegrityCheckOptions): Map<String, Any> {
    val signals = mutableListOf<Map<String, Any>>()

    // --- BASIC ---------------------------------------------------------------

    // NOTE:
    // BASIC checks are deterministic and safe to cache.

    signals.addAll(checkRootSignals())

    val isEmulator = checkEmulator()
    if (isEmulator) {
      signals.add(
        signal(
          id = "android_emulator",
          category = "emulator",
          confidence = "high",
          options = options,
        ),
      )
    }

    // --- STANDARD ------------------------------------------------------------

    // NOTE:
    // STANDARD checks may introduce false positives
    // and are therefore weighted accordingly.

    if (options.level != "basic") {
      // checkDebug returns a list of signals
      signals.addAll(checkDebug(options))

      // --- HOOKING DETECTION -------------------------------------------------

      // Check Frida via memory maps
      val fridaMemoryDetected = checkFridaMemory()
      if (fridaMemoryDetected) {
        signals.add(
          signal(
            id = "android_frida_memory",
            category = "hook",
            confidence = "high",
            options = options,
          ),
        )
      }

      // Check Frida via known ports
      val fridaPortDetected = checkFridaPorts()
      if (fridaPortDetected) {
        signals.add(
          signal(
            id = "android_frida_port",
            category = "hook",
            confidence = "medium",
            options = options,
          ),
        )
      }

      // --- SIGNAL CORRELATION ------------------------------------------------
      // If both memory artifacts and ports are detected, emit a high-confidence
      // correlation signal to confirm active instrumentation.
      if (fridaMemoryDetected && fridaPortDetected) {
        signals.add(
          signal(
            id = "android_frida_correlation_confirmed",
            category = "hook",
            confidence = "high",
            metadata = mapOf("source" to "memory+port"),
            options = options,
          ),
        )
      }
    }

    // --- STRICT --------------------------------------------------------------

    // NOTE:
    // STRICT checks are allowed to be more invasive
    // and SHOULD be used sparingly by the host app.

    if (options.level == "strict") {
      if (!checkAppSignature()) {
        signals.add(
          signal(
            id = "android_signature_invalid",
            category = "tamper",
            confidence = "high",
            options = options,
          ),
        )
      }
    }

    val score = computeScore(signals)

    return mapOf(
      "signals" to signals,
      "score" to score,
      "compromised" to (score >= 30),
      "environment" to
        mapOf(
          "platform" to "android",
          "isEmulator" to isEmulator,
          "isDebugBuild" to false,
        ),
      "timestamp" to System.currentTimeMillis(),
    )
  }

  // ---------------------------------------------------------------------------
  // Signal helper
  // ---------------------------------------------------------------------------

  /**
   * Internal helper to construct a signal map.
   *
   * CONTRACT:
   * - Returned maps MUST be JSON-serializable
   * - Keys MUST remain stable across platforms
   *
   * NOTE:
   * - Description and metadata are optional and gated by options.
   */
  private fun signal(
    id: String,
    category: String,
    confidence: String,
    description: String? = null,
    metadata: Map<String, Any>? = null,
    options: IntegrityCheckOptions,
  ): Map<String, Any> {
    val map =
      mutableMapOf<String, Any>(
        "id" to id,
        "category" to category,
        "confidence" to confidence,
      )

    // NOTE:
    // Description is included ONLY when explicitly requested
    // to avoid leaking sensitive diagnostics by default.

    // Include description only if requested in options
    if (options.includeDebugInfo && description != null) {
      map["description"] = description
    }

    // Include metadata if present
    if (metadata != null) {
      map["metadata"] = metadata
    }

    return map
  }

  // ---------------------------------------------------------------------------
  // Root detection
  // ---------------------------------------------------------------------------

  /**
   * Cached root-related signals.
   *
   * CONTRACT:
   * - Root checks are deterministic and cached
   * - Cache lifetime == process lifetime
   *
   * LIMITATION:
   * - Does NOT detect runtime-only or kernel-level rooting
   */
  private var cachedRootSignals: List<Map<String, Any>>? = null

  /**
   * Performs expanded root detection using filesystem heuristics.
   *
   * @throws IntegrityError.Unavailable
   *   If filesystem access is restricted by the OS.
   */
  fun checkRootSignals(): List<Map<String, Any>> {
    // WARNING:
    // Filesystem heuristics may be bypassed by advanced root hiding tools.

    cachedRootSignals?.let { return it }

    val signals = mutableListOf<Map<String, Any>>()

    // Expanded su paths
    val suPaths =
      listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/app/Superuser.apk",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/su/bin/su",
      )

    try {
      for (path in suPaths) {
        if (File(path).exists()) {
          signals.add(
            mapOf(
              "id" to "android_root_su",
              "category" to "root",
              "confidence" to "high",
              // Diagnostic metadata
              "metadata" to mapOf("path" to path),
            ),
          )
          break
        }
      }
    } catch (e: SecurityException) {
      throw IntegrityError.Unavailable(
        "Filesystem access denied while performing root checks.",
      )
    }

    val buildTags = android.os.Build.TAGS
    if (buildTags?.contains("test-keys") == true) {
      signals.add(
        mapOf(
          "id" to "android_test_keys",
          "category" to "root",
          "confidence" to "medium",
          "metadata" to mapOf("tags" to buildTags),
        ),
      )
    }

    cachedRootSignals = signals
    return signals
  }

  // ---------------------------------------------------------------------------
  // Emulator detection
  // ---------------------------------------------------------------------------

  /**
   * Detects emulators using correlated build properties.
   *
   * NOTE:
   * - This method uses a best-effort heuristic approach.
   *
   * LIMITATION:
   * - No single signal is authoritative.
   * - Results MUST always be interpreted in aggregate.
   */
  fun checkEmulator(): Boolean {
    return try {
      val fingerprint = android.os.Build.FINGERPRINT
      val model = android.os.Build.MODEL
      val manufacturer = android.os.Build.MANUFACTURER
      val hardware = android.os.Build.HARDWARE
      val product = android.os.Build.PRODUCT

      fingerprint.contains("generic") ||
        fingerprint.startsWith("unknown") ||
        model.contains("google_sdk") ||
        model.contains("Emulator", ignoreCase = true) ||
        model.contains("Android SDK built for x86") ||
        manufacturer.contains("Genymotion", ignoreCase = true) ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        product.contains("sdk_google") ||
        product.contains("google_sdk") ||
        product.contains("vbox86p")
    } catch (_: Exception) {
      false
    }
  }

  // ---------------------------------------------------------------------------
  // Debug detection
  // ---------------------------------------------------------------------------

  /**
   * Detects debugging conditions.
   *
   * CONTRACT:
   * - Returns zero or more debug-related signals
   *
   * LIMITATION:
   * - Does NOT detect:
   *   - native ptrace-based debugging
   *   - kernel-level instrumentation
   */
  fun checkDebug(options: IntegrityCheckOptions): List<Map<String, Any>> {
    val debugSignals = mutableListOf<Map<String, Any>>()
    val isDebuggerConnected = android.os.Debug.isDebuggerConnected()
    val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    if (isDebuggerConnected) {
      debugSignals.add(
        signal(
          id = "android_debugger_attached",
          category = "debug",
          confidence = "high",
          metadata = mapOf("method" to "Debug.isDebuggerConnected"),
          options = options,
        ),
      )
    }

    if (isDebuggable) {
      debugSignals.add(
        signal(
          id = "android_debuggable_build",
          category = "debug",
          confidence = "medium",
          metadata = mapOf("flag" to "FLAG_DEBUGGABLE"),
          options = options,
        ),
      )
    }

    return debugSignals
  }

  // ---------------------------------------------------------------------------
  // Frida detection
  // ---------------------------------------------------------------------------

  /**
   * Frida detection via process memory map inspection.
   *
   * NOTE:
   * - Chosen over process listing (`ps`) for:
   *   - better stealth
   *   - lower overhead
   *
   * LIMITATION:
   * - May miss renamed or obfuscated Frida payloads
   */
  fun checkFridaMemory(): Boolean {
    // NOTE:
    // SecurityException is intentionally swallowed here
    // to allow other checks to complete.

    return try {
      val mapsFile = File("/proc/self/maps")
      if (mapsFile.exists()) {
        mapsFile.useLines { lines ->
          lines.any { it.contains("frida", ignoreCase = true) || it.contains("gadget", ignoreCase = true) }
        }
      } else {
        false
      }
    } catch (e: SecurityException) {
      // We prefer returning false to allow other signals to complete
      // unless you strictly require knowing why it failed.
      false
    } catch (_: Exception) {
      false
    }
  }

  // ---------------------------------------------------------------------------
  // Frida detection — ports
  // ---------------------------------------------------------------------------

  /**
   * Detects known Frida server ports on localhost.
   *
   * CONTRACT:
   * - Only known, fixed ports are checked
   *
   * WARNING:
   * - MUST NOT be extended to arbitrary port scanning
   *
   * @throws IntegrityError.Unavailable
   *   If socket creation is restricted.
   */
  fun checkFridaPorts(): Boolean {
    val ports = listOf(27042, 27043)

    return ports.any { port ->
      try {
        Socket("127.0.0.1", port).use { true }
      } catch (e: SecurityException) {
        throw IntegrityError.Unavailable(
          "Socket access denied while checking Frida ports.",
        )
      } catch (_: Exception) {
        false
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Signature integrity
  // ---------------------------------------------------------------------------

  /**
   * Performs a basic application signature integrity check.
   *
   * NOTE:
   * - This check validates presence, not trust chain correctness.
   *
   * LIMITATION:
   * - Does NOT detect:
   *   - repackaging with a different valid key
   *   - runtime code injection
   *
   * @throws IntegrityError.InitFailed
   *   If signing info cannot be accessed.
   */
  fun checkAppSignature(): Boolean {
    try {
      val packageInfo =
        context.packageManager.getPackageInfo(
          context.packageName,
          PackageManager.GET_SIGNING_CERTIFICATES,
        )

      return packageInfo.signingInfo?.apkContentsSigners?.isNotEmpty() == true
    } catch (e: Exception) {
      throw IntegrityError.InitFailed(
        "Failed to read application signing information.",
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Scoring
  // ---------------------------------------------------------------------------

  /**
   * Computes a heuristic risk score from collected signals.
   *
   * CONTRACT:
   * - Scoring MUST remain platform-agnostic
   * - Platform-specific weighting is FORBIDDEN here
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
}
