package io.capkit.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.webkit.WebView
import io.capkit.device.logger.DeviceLogger

/**
 * Platform-specific native implementation for the Device plugin.
 *
 * This class contains pure Android logic and MUST NOT depend directly on
 * Capacitor bridge APIs or PluginCall objects.
 *
 * Responsibilities:
 * - Hosting pure Android device-information logic.
 * - Translating configuration into native behavior.
 */
class DeviceImpl(
  private val context: Context,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: DeviceConfig

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * This method MUST be called exactly once during the plugin [DevicePlugin.load]
   * phase. It initializes internal state and configures logging verbosity.
   *
   * @param newConfig The immutable configuration instance.
   */
  fun updateConfig(newConfig: DeviceConfig) {
    this.config = newConfig
    DeviceLogger.verbose = newConfig.verboseLogging
    DeviceLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Memory & Disk
  // ---------------------------------------------------------------------------

  /**
   * Returns the memory currently used by the application process, in bytes.
   */
  fun getMemUsed(): Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

  /**
   * Returns the free disk space on the legacy system partition, in bytes.
   */
  fun getDiskFree(): Long {
    val statFs = StatFs(Environment.getRootDirectory().absolutePath)
    return statFs.availableBlocksLong * statFs.blockSizeLong
  }

  /**
   * Returns the total size of the legacy system partition, in bytes.
   */
  fun getDiskTotal(): Long {
    val statFs = StatFs(Environment.getRootDirectory().absolutePath)
    return statFs.blockCountLong * statFs.blockSizeLong
  }

  /**
   * Returns the free disk space on the normal data storage path, in bytes.
   */
  fun getRealDiskFree(): Long {
    val statFs = StatFs(Environment.getDataDirectory().absolutePath)
    return statFs.availableBlocksLong * statFs.blockSizeLong
  }

  /**
   * Returns the total size of the normal data storage path, in bytes.
   */
  fun getRealDiskTotal(): Long {
    val statFs = StatFs(Environment.getDataDirectory().absolutePath)
    return statFs.blockCountLong * statFs.blockSizeLong
  }

  // ---------------------------------------------------------------------------
  // Identity
  // ---------------------------------------------------------------------------

  /**
   * Returns the platform name of the underlying operating system.
   */
  fun getPlatform(): String = "android"

  /**
   * Returns the ANDROID_ID assigned to this app-signing key, user, and device.
   *
   * @throws DeviceError.Unavailable when the identifier cannot be read.
   */
  fun getUuid(): String =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
      ?: throw DeviceError.Unavailable("Id not available")

  /**
   * Returns the consumer-visible device name, or null when unsupported.
   */
  fun getName(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
    } else {
      null
    }

  /**
   * Returns the device model identifier, e.g. "Pixel 8".
   */
  fun getModel(): String = Build.MODEL

  /**
   * Returns the operating system version string.
   */
  fun getOsVersion(): String = Build.VERSION.RELEASE

  /**
   * Returns the Android SDK version number.
   */
  fun getAndroidSDKVersion(): Int = Build.VERSION.SDK_INT

  /**
   * Returns the device manufacturer, e.g. "Google".
   */
  fun getManufacturer(): String = Build.MANUFACTURER

  /**
   * Returns whether the app is running on an emulator or virtual device.
   */
  fun isVirtual(): Boolean = Build.FINGERPRINT.contains("generic") || Build.PRODUCT.contains("sdk")

  // ---------------------------------------------------------------------------
  // Battery
  // ---------------------------------------------------------------------------

  /**
   * Returns the current battery charge level as a fraction from 0 to 1.
   */
  fun getBatteryLevel(): Float {
    val batteryStatus = queryBatteryStatus()
    var level = -1
    var scale = -1
    if (batteryStatus != null) {
      level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    }
    return level / scale.toFloat()
  }

  /**
   * Returns whether the device is currently charging or fully charged.
   */
  fun isCharging(): Boolean {
    val batteryStatus = queryBatteryStatus() ?: return false
    val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
  }

  private fun queryBatteryStatus(): Intent? =
    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

  // ---------------------------------------------------------------------------
  // WebView
  // ---------------------------------------------------------------------------

  /**
   * Returns the version of the current system WebView implementation,
   * falling back to the OS version when it cannot be determined.
   */
  fun getWebViewVersion(): String {
    val info: PackageInfo? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WebView.getCurrentWebViewPackage()
      } else {
        try {
          context.packageManager.getPackageInfo(WEBVIEW_PACKAGE_NAME, 0)
        } catch (e: PackageManager.NameNotFoundException) {
          DeviceLogger.error("Default WebView package not found.", e)
          null
        }
      }
    return info?.versionName ?: Build.VERSION.RELEASE
  }

  private companion object {
    private const val WEBVIEW_PACKAGE_NAME = "com.android.chrome"
  }
}
