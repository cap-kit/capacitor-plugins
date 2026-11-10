package io.capkit.localnetwork.permission

import android.os.Build

/**
 * Local-network runtime permission contract.
 *
 * Android 17 (API 37) introduced `android.permission.ACCESS_LOCAL_NETWORK`.
 * On older SDKs the permission is unknown to `PackageManager`: it is neither
 * declared nor enforced, and must be treated as granted so no prompt can ever
 * appear there.
 *
 * Architectural rules:
 * - No Capacitor or UI dependencies.
 * - The API-level gate lives here so both sides are unit-testable on the JVM.
 */
object LocalNetworkPermission {
  /**
   * Permission alias exposed to JavaScript.
   */
  const val ALIAS: String = "localNetwork"

  /**
   * Manifest permission string introduced in API 37.
   *
   * Kept as a literal because the project may compile against a SDK below 37,
   * where `Manifest.permission.ACCESS_LOCAL_NETWORK` does not exist.
   */
  const val NAME: String = "android.permission.ACCESS_LOCAL_NETWORK"

  /**
   * API level that introduced the local-network runtime permission.
   *
   * Kept as a literal because `Build.VERSION_CODES` for API 37 may not exist
   * at compile time.
   */
  const val ANDROID_API_LOCAL_NETWORK: Int = 37

  /**
   * Checks whether the local-network permission is enforced on the current device.
   */
  fun isEnforced(): Boolean = isEnforced(Build.VERSION.SDK_INT)

  /**
   * Checks whether the local-network permission is enforced for the given API level.
   *
   * @param sdkInt An API level, e.g. `Build.VERSION.SDK_INT`.
   * @return True on API 37+, false on older SDKs where the permission is unknown.
   */
  fun isEnforced(sdkInt: Int): Boolean = sdkInt >= ANDROID_API_LOCAL_NETWORK
}
