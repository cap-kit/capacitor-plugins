package io.capkit.integrity.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.os.Debug
import io.capkit.integrity.IntegrityCheckOptions
import io.capkit.integrity.IntegrityError
import io.capkit.integrity.IntegritySignalBuilder

/**
 * Runtime integrity checks related to debugging conditions
 * and application signing integrity.
 */
object IntegrityRuntimeChecks {
  /**
   * Detects debugging conditions such as attached debuggers or debuggable flags.
   */
  fun checkDebugSignals(
    context: Context,
    options: IntegrityCheckOptions,
  ): List<Map<String, Any>> {
    val debugSignals = mutableListOf<Map<String, Any>>()
    val isDebuggerConnected = Debug.isDebuggerConnected()
    val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    if (isDebuggerConnected) {
      debugSignals.add(
        IntegritySignalBuilder.build(
          id = "android_debugger_attached",
          category = "debug",
          confidence = "high",
          description = "A debugger is currently attached to the running process",
          metadata = mapOf("method" to "Debug.isDebuggerConnected"),
          options = options,
        ),
      )
    }

    if (isDebuggable) {
      debugSignals.add(
        IntegritySignalBuilder.build(
          id = "android_runtime_debuggable",
          category = "debug",
          confidence = "medium",
          description = "Process is debuggable at runtime",
          metadata = mapOf("flag" to "FLAG_DEBUGGABLE"),
          options = options,
        ),
      )
    }

    return debugSignals
  }

  /**
   * Performs a basic application signature integrity check.
   */
  fun checkAppSignature(context: Context): Boolean =
    try {
      val packageInfo =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
          context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
          )
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES,
          )
        }

      val signingInfo =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
          packageInfo.signingInfo?.apkContentsSigners
        } else {
          @Suppress("DEPRECATION")
          packageInfo.signatures
        }

      signingInfo?.isNotEmpty() == true
    } catch (e: Exception) {
      throw IntegrityError.InitFailed("Failed to read application signing information.")
    }
}
