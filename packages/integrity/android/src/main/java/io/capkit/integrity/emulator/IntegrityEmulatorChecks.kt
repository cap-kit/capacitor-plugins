package io.capkit.integrity.emulator

import android.os.Build

/**
 * Detects emulators using correlated build properties.
 */
object IntegrityEmulatorChecks {
  /**
   * Determines if the current environment matches known emulator characteristics.
   *
   * NOTE: This is a best-effort heuristic approach where no single signal is
   * authoritative on its own.
   */
  fun isEmulator(): Boolean {
    return try {
      val fingerprint = Build.FINGERPRINT
      val model = Build.MODEL
      val manufacturer = Build.MANUFACTURER
      val hardware = Build.HARDWARE
      val product = Build.PRODUCT

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
}
