package io.capkit.integrity

import android.content.Intent
import android.net.Uri
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Capacitor bridge for the Integrity plugin (Android).
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Invoke the native implementation
 * - Resolve or reject PluginCall exactly once
 * - Map native IntegrityError to JS-facing error codes
 *
 * Forbidden:
 * - Platform-specific business logic
 * - System API usage
 * - Throwing uncaught exceptions
 */
@CapacitorPlugin(
  name = "Integrity",
)
class IntegrityPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration.
   * Read once during plugin initialization.
   */
  private lateinit var config: IntegrityConfig

  /**
   * Native implementation layer.
   * Contains platform-specific logic only.
   */
  private lateinit var implementation: IntegrityImpl

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read static configuration
   * - initialize the native implementation
   * - inject configuration into the implementation
   */
  override fun load() {
    super.load()

    config = IntegrityConfig(this)
    implementation = IntegrityImpl(context)
    implementation.updateConfig(config)
  }

  // ---------------------------------------------------------------------------
  // Error mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native IntegrityError values to JavaScript-facing error codes.
   *
   * This method MUST be the only place where native errors
   * are translated into JS-visible failures.
   */
  private fun reject(
    call: PluginCall,
    error: IntegrityError,
  ) {
    val code =
      when (error) {
        is IntegrityError.Unavailable -> "UNAVAILABLE"
        is IntegrityError.PermissionDenied -> "PERMISSION_DENIED"
        is IntegrityError.InitFailed -> "INIT_FAILED"
        is IntegrityError.UnknownType -> "UNKNOWN_TYPE"
      }

    call.reject(error.message, code)
  }

  // ---------------------------------------------------------------------------
  // Check
  // ---------------------------------------------------------------------------

  /**
   * Executes an integrity check.
   *
   * This method:
   * - parses JS options
   * - delegates execution to the native implementation
   * - resolves a structured IntegrityReport
   */
  @PluginMethod
  fun check(call: PluginCall) {
    try {
      val options =
        IntegrityCheckOptions(
          level = call.getString("level") ?: "basic",
          includeDebugInfo = call.getBoolean("includeDebugInfo") ?: false,
        )

      val result = implementation.performCheck(options)

      val jsResult = JSObject()
      for ((key, value) in result) {
        jsResult.put(key, value)
      }
      call.resolve(jsResult)
    } catch (e: IntegrityError) {
      reject(call, e)
    } catch (e: Exception) {
      call.reject(
        "Unexpected native error during integrity check.",
        "INIT_FAILED",
      )
    }
  }

  // ---------------------------------------------------------------------------
  // PresentBlockPage
  // ---------------------------------------------------------------------------

  /**
   * Presents the configured integrity block page, if enabled.
   *
   * This method NEVER decides when it should be called.
   * The decision is fully delegated to the host application.
   */
  @PluginMethod
  fun presentBlockPage(call: PluginCall) {
    if (!config.blockPageEnabled || config.blockPageUrl == null) {
      call.resolve(JSObject().put("presented", false))
      return
    }

    val reason = call.getString("reason")
    val dismissible = call.getBoolean("dismissible") ?: false

    val url =
      if (reason != null) {
        "${config.blockPageUrl}?reason=${Uri.encode(reason)}"
      } else {
        config.blockPageUrl
      }

    val intent =
      Intent(
        context,
        io.capkit.integrity.ui.IntegrityBlockActivity::class.java,
      ).apply {
        putExtra("url", url)
        putExtra("dismissible", dismissible)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // If not dismissible, clear the back stack
        if (!dismissible) {
          addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
      }

    context.startActivity(intent)

    call.resolve(JSObject().put("presented", true))
  }

  // ---------------------------------------------------------------------------
  // Version
  // ---------------------------------------------------------------------------

  /**
   * Returns the native plugin version.
   *
   * This method is guaranteed not to fail.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }
}
