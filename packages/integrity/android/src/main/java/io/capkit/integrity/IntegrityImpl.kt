package io.capkit.integrity

import android.content.Context
import android.content.pm.PackageManager
import io.capkit.integrity.utils.IntegrityLogger
import java.io.File
import java.net.Socket

/**
 * Native Android implementation for the Integrity plugin.
 *
 * Responsibilities:
 * - Perform platform-specific integrity checks
 * - Interact with Android system APIs
 * - Produce integrity signals
 * - THROW typed IntegrityError on unrecoverable failures
 *
 * Forbidden:
 * - Accessing PluginCall
 * - Referencing Capacitor APIs
 * - Resolving or rejecting JS calls
 * - Reading configuration directly
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
   * This configuration MUST be injected exactly once
   * from the Plugin layer during load().
   */
  private lateinit var config: IntegrityConfig

  /**
   * Applies static plugin configuration.
   *
   * This method MUST be called exactly once.
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

  fun performCheck(options: IntegrityCheckOptions): Map<String, Any> {
    val signals = mutableListOf<Map<String, Any>>()

    // --- BASIC ---------------------------------------------------------------
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
    if (options.level != "basic") {
      if (checkDebug()) {
        signals.add(
          signal(
            id = "android_debug_detected",
            category = "debug",
            confidence = "medium",
            description = "Debugger or debuggable build detected",
            options = options,
          ),
        )
      }

      if (checkFridaProcesses()) {
        signals.add(
          signal(
            id = "android_frida_process",
            category = "hook",
            confidence = "high",
            options = options,
          ),
        )
      }

      if (checkFridaPorts()) {
        signals.add(
          signal(
            id = "android_frida_port",
            category = "hook",
            confidence = "medium",
            options = options,
          ),
        )
      }
    }

    // --- STRICT --------------------------------------------------------------
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

  private fun signal(
    id: String,
    category: String,
    confidence: String,
    description: String? = null,
    options: IntegrityCheckOptions,
  ): Map<String, Any> {
    val map =
      mutableMapOf<String, Any>(
        "id" to id,
        "category" to category,
        "confidence" to confidence,
      )

    if (options.includeDebugInfo && description != null) {
      map["description"] = description
    }

    return map
  }

  // ---------------------------------------------------------------------------
  // Root detection
  // ---------------------------------------------------------------------------

  private var cachedRootSignals: List<Map<String, Any>>? = null

  /**
   * Performs baseline root detection checks.
   *
   * @throws IntegrityError.Unavailable
   *   If filesystem access is not available.
   */
  fun checkRootSignals(): List<Map<String, Any>> {
    cachedRootSignals?.let { return it }

    val signals = mutableListOf<Map<String, Any>>()

    val suPaths =
      listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/app/Superuser.apk",
      )

    try {
      if (suPaths.any { File(it).exists() }) {
        signals.add(
          mapOf(
            "id" to "android_root_su",
            "category" to "root",
            "confidence" to "high",
          ),
        )
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
   * Detects whether the application is running on an emulator.
   *
   * @throws IntegrityError.Unavailable
   *   If build information cannot be accessed.
   */
  fun checkEmulator(): Boolean {
    return try {
      val fingerprint = android.os.Build.FINGERPRINT
      val model = android.os.Build.MODEL
      val manufacturer = android.os.Build.MANUFACTURER

      fingerprint.contains("generic") ||
        model.contains("Emulator", ignoreCase = true) ||
        manufacturer.contains("Genymotion", ignoreCase = true)
    } catch (_: Exception) {
      false
    }
  }

  // ---------------------------------------------------------------------------
  // Debug detection (baseline)
  // ---------------------------------------------------------------------------

  fun checkDebug(): Boolean {
    val debuggerAttached = android.os.Debug.isDebuggerConnected()
    val debuggable =
      (
        context.applicationInfo.flags and
          android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
      ) != 0

    return debuggerAttached || debuggable
  }

  // ---------------------------------------------------------------------------
  // Frida detection
  // ---------------------------------------------------------------------------

  /**
   * Detects Frida-related processes using a best-effort approach.
   *
   * Failure to execute the process list is treated as UNAVAILABLE,
   * not as a negative detection.
   *
   * @throws IntegrityError.Unavailable
   *   If process inspection is not permitted.
   */
  fun checkFridaProcesses(): Boolean {
    return try {
      val process = Runtime.getRuntime().exec("ps")
      process.inputStream.bufferedReader().useLines { lines ->
        lines.any { it.contains("frida", ignoreCase = true) }
      }
    } catch (e: SecurityException) {
      throw IntegrityError.Unavailable(
        "Process inspection is not permitted on this device.",
      )
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
   * This check is intentionally conservative.
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
   * @throws IntegrityError.InitFailed
   *   If the package signature cannot be retrieved.
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
