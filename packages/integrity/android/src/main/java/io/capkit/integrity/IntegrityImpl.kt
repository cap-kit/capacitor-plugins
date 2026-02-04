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
  // Internal cache
  // ---------------------------------------------------------------------------

  /**
   * Cached root-related signals.
   *
   * Root checks are relatively expensive and deterministic,
   * therefore they are cached for the lifetime of the process.
   */
  private var cachedRootSignals: List<Map<String, Any>>? = null

  // ---------------------------------------------------------------------------
  // Root detection
  // ---------------------------------------------------------------------------

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
    if (buildTags != null && buildTags.contains("test-keys")) {
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
    try {
      val fingerprint = android.os.Build.FINGERPRINT
      val model = android.os.Build.MODEL
      val manufacturer = android.os.Build.MANUFACTURER

      return fingerprint.contains("generic") ||
        model.contains("Emulator", ignoreCase = true) ||
        manufacturer.contains("Genymotion", ignoreCase = true)
    } catch (e: Exception) {
      throw IntegrityError.Unavailable(
        "Unable to determine emulator status.",
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Frida detection — processes
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
  // Application signature
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

      val signingInfo =
        packageInfo.signingInfo
          ?: throw IntegrityError.InitFailed(
            "Signing information not available.",
          )

      return signingInfo.apkContentsSigners.isNotEmpty()
    } catch (e: IntegrityError) {
      throw e
    } catch (e: Exception) {
      throw IntegrityError.InitFailed(
        "Failed to read application signing information.",
      )
    }
  }
}
