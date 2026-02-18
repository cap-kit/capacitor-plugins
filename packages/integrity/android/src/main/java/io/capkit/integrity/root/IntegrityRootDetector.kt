package io.capkit.integrity.root

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.capkit.integrity.IntegritySignalBuilder
import io.capkit.integrity.error.IntegrityError
import io.capkit.integrity.models.IntegrityCheckOptions
import java.io.File

/**
 * Performs root detection using filesystem heuristics and package inspection.
 */
object IntegrityRootDetector {
  private val suPaths =
    listOf(
      "/system/bin/su",
      "/system/xbin/su",
      "/sbin/su",
      "/system/app/Superuser.apk",
      "/system/app/Superuser/Superuser.apk",
      "/data/local/xbin/su",
      "/data/local/bin/su",
      "/system/sd/xbin/su",
      "/su/bin/su",
      "/magisk/.core/bin/su",
      "/system/usr/we-need-root/su-backup/su",
      "/system/bin/.ext/.su/su",
      "/system/bin/failsafe/su",
      "/data/local/su",
    )

  private val rootPackages =
    listOf(
      "com.noshufou.android.su",
      "com.thirdparty.superuser",
      "eu.chainfire.supersu",
      "com.koushikdutta.superuser",
      "com.zachareew.systemuituner",
      "com.topjohnwu.magisk",
      "com.alephzain.framaroot",
      "org.adaway",
    )

  /**
   * Cache process-lifetime for deterministic root signals.
   *
   * IMPORTANT:
   * - Used ONLY for runtime checks
   * - Boot-time checks MUST bypass this cache
   */
  private var cachedRootSignals: List<Map<String, Any>>? = null

  /**
   * Performs root detection using filesystem heuristics and build metadata.
   *
   * @param allowCache
   *   - true  → reuse cached signals if available (runtime path)
   *   - false → force fresh detection (boot path)
   */
  fun checkRootSignals(
    options: IntegrityCheckOptions,
    allowCache: Boolean = true,
  ): List<Map<String, Any>> {
    if (allowCache) {
      cachedRootSignals?.let { return it }
    }

    val signals = mutableListOf<Map<String, Any>>()

    try {
      for (path in suPaths) {
        if (File(path).exists()) {
          signals.add(
            IntegritySignalBuilder.build(
              id = "android_root_su",
              category = "root",
              confidence = "high",
              description = "Presence of su binary detected in system paths",
              metadata = mapOf("path" to path),
              options = options,
            ),
          )
          break
        }
      }
    } catch (e: SecurityException) {
      throw IntegrityError.Unavailable("Filesystem access denied while performing root checks.")
    }

    if (Build.TAGS?.contains("test-keys") == true) {
      signals.add(
        IntegritySignalBuilder.build(
          id = "android_test_keys",
          category = "root",
          confidence = "medium",
          description = "Device build signed with test keys",
          metadata = mapOf("tags" to Build.TAGS),
          options = options,
        ),
      )
    }

    if (allowCache) {
      cachedRootSignals = signals
    }

    return signals
  }

  /**
   * Checks for the presence of known root management applications.
   */
  fun checkRootPackages(
    context: Context,
    options: IntegrityCheckOptions,
  ): List<Map<String, Any>> {
    val signals = mutableListOf<Map<String, Any>>()
    val pm = context.packageManager

    for (pkg in rootPackages) {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
          @Suppress("DEPRECATION")
          pm.getPackageInfo(pkg, 0)
        }

        signals.add(
          IntegritySignalBuilder.build(
            id = "android_root_package",
            category = "root",
            confidence = "high",
            description = "Detected known root management or related application",
            metadata = mapOf("package" to pkg),
            options = options,
          ),
        )
      } catch (_: PackageManager.NameNotFoundException) {
      }
    }
    return signals
  }
}
