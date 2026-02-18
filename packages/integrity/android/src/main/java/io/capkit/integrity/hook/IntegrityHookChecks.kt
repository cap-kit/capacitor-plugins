package io.capkit.integrity.hook

import io.capkit.integrity.error.IntegrityError
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Detects instrumentation frameworks (like Frida) via memory inspection
 * and local network port scanning.
 */
object IntegrityHookChecks {
  /**
   * Frida detection via process memory map inspection.
   * Looks for known library artifacts in /proc/self/maps.
   */
  fun checkFridaMemory(): Boolean =
    try {
      val mapsFile = File("/proc/self/maps")
      if (mapsFile.exists()) {
        mapsFile.useLines { lines ->
          lines.any { it.contains("frida", ignoreCase = true) || it.contains("gadget", ignoreCase = true) }
        }
      } else {
        false
      }
    } catch (e: SecurityException) {
      // Swallowed to allow other checks to complete
      false
    } catch (_: Exception) {
      false
    }

  /**
   * Detects known Frida server ports on localhost.
   *
   * @throws IntegrityError.Unavailable If socket access is restricted.
   */
  fun checkFridaPorts(): Boolean {
    val ports = listOf(27042, 27043)
    val timeoutMs = 1000

    return ports.any { port ->
      try {
        Socket().use { socket ->
          // Use a timeout to prevent long-running blocking calls
          socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
          true
        }
      } catch (e: SecurityException) {
        throw IntegrityError.Unavailable(
          "Socket access denied while checking Frida ports.",
        )
      } catch (_: Exception) {
        // Connection failed or timed out, which is expected in clean environments
        false
      }
    }
  }
}
