package io.capkit.settings.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Utility helpers for resolving Android system settings Intents.
 *
 * This object contains ONLY mapping logic and MUST NOT:
 * - start activities
 * - access Context or PackageManager
 * - depend on Capacitor APIs
 *
 * It maps JavaScript-facing option strings to Android Intent instances.
 */
object SettingsUtils {
  /**
   * Resolves a JavaScript settings option into an Android Intent.
   *
   * @param option JavaScript-facing option key
   * @param packageName Application package name (used where required)
   * @return A configured Intent, or null if the option is unsupported
   */
  fun resolveIntent(
    option: String,
    packageName: String,
  ): Intent? {
    return when (option) {
      // --- App-specific ---
      "application_details" ->
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.parse("package:$packageName")
        }

      "app_notification" ->
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
          putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }

      // --- Core system settings ---
      "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
      "airplane_mode" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
      "apn" -> Intent(Settings.ACTION_APN_SETTINGS)
      "application" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
      "battery_optimization" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
      "cast" -> Intent(Settings.ACTION_CAST_SETTINGS)
      "data_roaming" -> Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
      "date" -> Intent(Settings.ACTION_DATE_SETTINGS)
      "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
      "home" -> Intent(Settings.ACTION_HOME_SETTINGS)
      "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
      "nfc" -> Intent(Settings.ACTION_NFC_SETTINGS)
      "nfcsharing" -> Intent(Settings.ACTION_NFCSHARING_SETTINGS)
      "nfc_payment" -> Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS)
      "print" -> Intent(Settings.ACTION_PRINT_SETTINGS)
      "security" -> Intent(Settings.ACTION_SECURITY_SETTINGS)
      "settings" -> Intent(Settings.ACTION_SETTINGS)
      "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
      "storage" -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
      "usage" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
      "vpn" -> Intent(Settings.ACTION_VPN_SETTINGS)
      "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)

      // --- Advanced / device-dependent ---
      "zen_mode" -> Intent("android.settings.ZEN_MODE_SETTINGS")
      "zen_mode_priority" -> Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
      "zen_mode_blocked_effects" -> Intent("android.settings.ZEN_MODE_BLOCKED_EFFECTS_SETTINGS")
      "text_to_speech" -> Intent("com.android.settings.TTS_SETTINGS")

      else -> null
    }
  }
}
