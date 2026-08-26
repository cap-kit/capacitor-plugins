package io.capkit.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import android.webkit.WebView
import io.capkit.device.config.DeviceConfig
import io.capkit.device.error.ErrorMessages
import io.capkit.device.error.NativeError
import io.capkit.device.logger.DeviceLogger
import java.io.BufferedReader
import java.io.FileReader
import java.util.Locale

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

  /** Previous CPU ticks for delta-based usage computation. */
  private var previousCpuTicks: CpuTicks? = null

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
   * @throws NativeError.Unavailable when the identifier cannot be read.
   */
  fun getUuid(): String =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
      ?: throw NativeError.Unavailable(ErrorMessages.UNAVAILABLE)

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
  fun isVirtual(): Boolean =
    try {
      val result =
        Settings.Secure.getString(
          context.contentResolver,
          Settings.Secure.ANDROID_ID,
        )
      result.isNullOrEmpty() || result == "unknown"
    } catch (e: Exception) {
      false
    }

  // ---------------------------------------------------------------------------
  // Locale
  // ---------------------------------------------------------------------------

  /**
   * Returns the two-character language code of the current locale.
   */
  fun getLanguageCode(): String = Locale.getDefault().language

  /**
   * Returns the BCP 47 language tag of the current locale.
   */
  fun getLanguageTag(): String = Locale.getDefault().toLanguageTag()

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
  // Display Info
  // ---------------------------------------------------------------------------

  /**
   * Returns display metrics including resolution, density, and refresh rate.
   */
  fun getDisplayInfo(): Map<String, Any> {
    val metrics = Resources.getSystem().displayMetrics
    val refreshRate =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = windowManager?.defaultDisplay
        display?.refreshRate?.toInt() ?: 60
      } else {
        60
      }

    return mapOf(
      "widthPx" to metrics.widthPixels,
      "heightPx" to metrics.heightPixels,
      "densityDpi" to metrics.densityDpi,
      "scale" to metrics.density.toDouble(),
      "refreshRateHz" to refreshRate,
    )
  }

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Returns the current device configuration: orientation, dark mode, font scale, idiom, and screen size.
   */
  fun getConfiguration(): Map<String, Any> {
    val config = context.resources.configuration

    val orientation =
      when (config.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> "portrait"
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        else -> "unknown"
      }

    val uiMode = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val isDarkMode = uiMode == Configuration.UI_MODE_NIGHT_YES

    val screenSize =
      when (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
        Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
        Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
        Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
        Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
        else -> "unknown"
      }

    val idiom = if (config.smallestScreenWidthDp >= 600) "tablet" else "phone"

    return mapOf(
      "orientation" to orientation,
      "isDarkMode" to isDarkMode,
      "fontScale" to config.fontScale.toDouble(),
      "idiom" to idiom,
      "screenSize" to screenSize,
    )
  }

  // ---------------------------------------------------------------------------
  // Power State
  // ---------------------------------------------------------------------------

  /**
   * Returns the power state: low power mode and thermal state.
   */
  fun getPowerState(): Map<String, Any> {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    val isLowPowerMode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        powerManager?.isPowerSaveMode ?: false
      } else {
        false
      }

    val thermalState =
      try {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = temp / 10.0
        when {
          tempCelsius < 35.0 -> "nominal"
          tempCelsius < 40.0 -> "fair"
          tempCelsius < 45.0 -> "serious"
          else -> "critical"
        }
      } catch (e: Exception) {
        "nominal"
      }

    return mapOf(
      "isLowPowerMode" to isLowPowerMode,
      "thermalState" to thermalState,
    )
  }

  // ---------------------------------------------------------------------------
  // Memory Info
  // ---------------------------------------------------------------------------

  /**
   * Returns memory information: physical RAM, CPU cores, memory class, low-RAM flag,
   * delta-based CPU usage, and memory pressure level.
   */
  fun getMemoryInfo(): Map<String, Any> {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    val memoryClassMb = activityManager?.memoryClass ?: 0

    val isLowRamDevice =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        activityManager?.isLowRamDevice ?: false
      } else {
        false
      }

    val cpuCores = Runtime.getRuntime().availableProcessors()

    val memInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memInfo)
    val physicalRam = memInfo.totalMem

    // Delta-based CPU usage
    val cpuUsage = getCpuUsage()

    // Memory pressure
    val memoryPressure = getMemoryPressure(memInfo)

    val resultMap =
      mutableMapOf<String, Any>(
        "physicalRam" to physicalRam,
        "cpuCores" to cpuCores,
        "memoryClassMb" to memoryClassMb,
        "isLowRamDevice" to isLowRamDevice,
      )
    cpuUsage?.let { resultMap["cpuUsagePercent"] = it }
    resultMap["memoryPressure"] = memoryPressure
    return resultMap
  }

  // ---------------------------------------------------------------------------
  // CPU Usage (Delta-based)
  // ---------------------------------------------------------------------------

  /**
   * Reads /proc/stat to compute delta-based CPU usage as a percentage (0–100).
   * Returns null on the first call (no delta) or if the file cannot be read.
   */
  private fun getCpuUsage(): Double? {
    return try {
      BufferedReader(FileReader("/proc/stat")).use { reader ->
        val line = reader.readLine() ?: return null
        // Format: cpu user nice system idle iowait irq softirq steal
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size < 5) return null

        val user = parts[1].toLong()
        val nice = parts[2].toLong()
        val system = parts[3].toLong()
        val idle = parts[4].toLong()
        val iowait = if (parts.size > 5) parts[5].toLong() else 0L
        val irq = if (parts.size > 6) parts[6].toLong() else 0L
        val softirq = if (parts.size > 7) parts[7].toLong() else 0L
        val steal = if (parts.size > 8) parts[8].toLong() else 0L

        val total = user + nice + system + idle + iowait + irq + softirq + steal
        val idleTotal = idle + iowait

        val current = CpuTicks(total, idleTotal)

        val previous = previousCpuTicks
        previousCpuTicks = current

        if (previous == null) {
          null // First call, no delta available
        } else {
          val totalDelta = current.total - previous.total
          val idleDelta = current.idle - previous.idle
          if (totalDelta > 0) {
            ((totalDelta - idleDelta).toDouble() / totalDelta.toDouble()) * 100.0
          } else {
            0.0
          }
        }
      }
    } catch (e: Exception) {
      DeviceLogger.debug("Failed to read /proc/stat: ${e.message}")
      null
    }
  }

  // ---------------------------------------------------------------------------
  // Memory Pressure
  // ---------------------------------------------------------------------------

  /**
   * Derives memory pressure from available memory relative to total RAM.
   */
  private fun getMemoryPressure(memInfo: ActivityManager.MemoryInfo): String {
    val lowMemory = memInfo.lowMemory
    val availMem = memInfo.availMem
    val totalMem = memInfo.totalMem

    return when {
      lowMemory -> "critical"
      totalMem > 0 && availMem < totalMem * 0.10 -> "critical"
      totalMem > 0 && availMem < totalMem * 0.20 -> "warning"
      else -> "normal"
    }
  }

  // ---------------------------------------------------------------------------
  // Storage Info
  // ---------------------------------------------------------------------------

  /**
   * Returns disk storage information for the primary data volume.
   */
  fun getStorageInfo(): Map<String, Any> =
    try {
      val statFs = StatFs(Environment.getDataDirectory().absolutePath)
      val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
      val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong
      val usedBytes = totalBytes - freeBytes
      val usedPercent = if (totalBytes > 0) (usedBytes.toDouble() / totalBytes.toDouble()) * 100.0 else 0.0

      mapOf(
        "totalBytes" to totalBytes,
        "freeBytes" to freeBytes,
        "usedBytes" to usedBytes,
        "usedPercent" to usedPercent,
      )
    } catch (e: Exception) {
      mapOf(
        "totalBytes" to 0L,
        "freeBytes" to 0L,
        "usedBytes" to 0L,
        "usedPercent" to 0.0,
      )
    }

  // ---------------------------------------------------------------------------
  // System Uptime
  // ---------------------------------------------------------------------------

  /**
   * Returns the system uptime in seconds.
   */
  fun getSystemUptime(): Map<String, Any> {
    val uptimeMillis = SystemClock.elapsedRealtime()
    return mapOf(
      "uptimeSeconds" to (uptimeMillis / 1000.0),
    )
  }

  // ---------------------------------------------------------------------------
  // App Version
  // ---------------------------------------------------------------------------

  /**
   * Returns the app version name and build number.
   */
  fun getAppVersion(): Map<String, Any> {
    val packageInfo =
      try {
        context.packageManager.getPackageInfo(context.packageName, 0)
      } catch (e: Exception) {
        return mapOf("version" to "unknown", "buildNumber" to 0)
      }

    @Suppress("DEPRECATION")
    val versionName = packageInfo.versionName ?: "unknown"
    val versionCode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
      } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode
      }

    return mapOf(
      "version" to versionName,
      "buildNumber" to versionCode,
    )
  }

  // ---------------------------------------------------------------------------
  // Battery Extras
  // ---------------------------------------------------------------------------

  /**
   * Returns extended battery information: charge source, detailed state, health, and temperature.
   */
  fun getBatteryExtras(): Map<String, Any> {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val chargeSource =
      when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "ac"
        BatteryManager.BATTERY_PLUGGED_USB -> "usb"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
        else -> "unknown"
      }

    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val detailedState =
      when (status) {
        BatteryManager.BATTERY_STATUS_UNKNOWN -> "unknown"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "unplugged"
        BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
        BatteryManager.BATTERY_STATUS_FULL -> "full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not-charging"
        else -> "unknown"
      }

    // Health status (Android only)
    val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val healthState =
      when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
        BatteryManager.BATTERY_HEALTH_COLD -> "cold"
        BatteryManager.BATTERY_HEALTH_UNKNOWN -> "unknown"
        else -> "unspecified"
      }

    // Temperature in tenths of degree Celsius → convert to degrees
    val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val temperature = tempTenths / 10.0

    return mapOf(
      "chargeSource" to chargeSource,
      "detailedState" to detailedState,
      "health" to healthState,
      "temperature" to temperature,
    )
  }

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

  // ---------------------------------------------------------------------------
  // Data Classes
  // ---------------------------------------------------------------------------

  private data class CpuTicks(
    val total: Long,
    val idle: Long,
  )
}
