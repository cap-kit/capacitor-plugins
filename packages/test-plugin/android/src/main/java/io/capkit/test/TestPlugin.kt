package io.capkit.test

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Capacitor bridge for the Test plugin (Android).
 *
 * This class represents the boundary between JavaScript and native Android code.
 *
 * Responsibilities:
 * - read plugin configuration
 * - validate PluginCall input
 * - delegate logic to TestImpl
 * - map native errors to JS-facing error codes
 * - resolve or reject calls exactly once
 */
@CapacitorPlugin(
  name = "Test",
)
class TestPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration parsed from capacitor.config.ts.
   */
  private lateinit var config: TestConfig

  /**
   * Native implementation containing platform-specific logic only.
   *
   * IMPORTANT:
   * - Must NOT access PluginCall
   * - Must NOT reference Capacitor APIs
   */
  private lateinit var implementation: TestImpl

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read static configuration
   * - initialize native resources
   * - inject configuration into the implementation
   */
  override fun load() {
    super.load()

    config = TestConfig(this)
    implementation = TestImpl(context)
    implementation.updateConfig(config)
  }

  // ---------------------------------------------------------------------------
  // Error Mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native TestError instances to JS-facing TestErrorCode values.
   *
   * IMPORTANT:
   * - This is the ONLY place where native errors are translated
   * - TestImpl must NEVER know about JS or Capacitor error codes
   */
  private fun reject(
    call: PluginCall,
    error: TestError,
  ) {
    val code =
      when (error) {
        is TestError.Unavailable -> "UNAVAILABLE"
        is TestError.PermissionDenied -> "PERMISSION_DENIED"
        is TestError.InitFailed -> "INIT_FAILED"
        is TestError.UnknownType -> "UNKNOWN_TYPE"
      }

    call.reject(error.message, code)
  }

  // ---------------------------------------------------------------------------
  // Echo
  // ---------------------------------------------------------------------------

  /**
   * Echoes a string back to JavaScript.
   *
   * - Reads input from PluginCall
   * - Applies configuration-derived behavior
   * - Delegates logic to TestImpl
   */
  @PluginMethod
  fun echo(call: PluginCall) {
    val jsValue = call.getString("value") ?: ""

    val valueToEcho =
      jsValue.ifEmpty {
        config.customMessage
      }

    val ret = JSObject()
    ret.put("value", implementation.echo(valueToEcho))
    call.resolve(ret)
  }

  // ---------------------------------------------------------------------------
  // Version
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version.
   *
   * NOTE:
   * - This method is guaranteed not to fail
   * - Therefore it does NOT use TestError
   * - Version is injected at build time from package.json
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }

  // ---------------------------------------------------------------------------
  // Settings
  // ---------------------------------------------------------------------------

  /**
   * Opens the application details settings page.
   *
   * This allows the user to manually manage permissions.
   */
  @PluginMethod
  fun openAppSettings(call: PluginCall) {
    val result = implementation.openAppSettings()

    result.fold(
      onSuccess = {
        try {
          val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = "package:${context.packageName}".toUri()
            }

          val activity = activity
          if (activity != null) {
            activity.startActivity(intent)
          } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
          }

          call.resolve()
        } catch (e: Exception) {
          reject(call, TestError.Unavailable("Failed to open app settings"))
        }
      },
      onFailure = {
        reject(call, it as TestError)
      },
    )
  }
}
