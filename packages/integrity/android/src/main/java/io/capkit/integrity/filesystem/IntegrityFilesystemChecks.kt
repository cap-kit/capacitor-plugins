package io.capkit.integrity.filesystem

import io.capkit.integrity.IntegrityCheckOptions
import io.capkit.integrity.IntegritySignalBuilder
import java.io.File

/**
 * Performs filesystem integrity checks related to sandbox escape
 * and suspicious directory permissions.
 */
object IntegrityFilesystemChecks {
  /**
   * Attempts to detect sandbox escape by checking write access
   * to protected system locations without performing mutations.
   *
   * NOTE:
   * - This check is best-effort and heuristic-based
   * - No filesystem writes are performed (read-only probes)
   */
  fun checkSandboxEscape(options: IntegrityCheckOptions): Map<String, Any>? {
    val protectedPaths =
      listOf(
        "/system",
        "/system/bin",
        "/system/xbin",
      )

    return try {
      for (path in protectedPaths) {
        val file = File(path)
        if (file.exists() && file.canWrite()) {
          return IntegritySignalBuilder.build(
            id = "android_sandbox_escaped",
            category = "tamper",
            confidence = "high",
            description = "Write access detected on protected system directory",
            metadata = mapOf("path" to path),
            options = options,
          )
        }
      }
      null
    } catch (_: SecurityException) {
      // Access denied → expected in a secure environment
      null
    } catch (_: Exception) {
      // Expected failure in a secure environment
      null
    }
  }
}
