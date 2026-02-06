package io.capkit.integrity

import android.content.Intent
import android.net.Uri
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.integrity.utils.IntegrityUtils

/**
 * Capacitor bridge for the Integrity plugin (Android).
 *
 * CONTRACT:
 * - This class is the ONLY entry point from JavaScript.
 * - All PluginCall instances MUST be resolved or rejected exactly once.
 *
 * Responsibilities:
 * - Parse JavaScript input
 * - Invoke the native implementation
 * - Resolve or reject PluginCall exactly once
 * - Map native IntegrityError to JS-facing error codes
 *
 * Forbidden:
 * - Platform-specific business logic
 * - System API usage (except Activity / Intent orchestration)
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
   *
   * CONTRACT:
   * - Initialized exactly once in load()
   * - Treated as read-only afterwards
   * - MUST NOT be mutated at runtime
   */
  private lateinit var config: IntegrityConfig

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - Lifetime == plugin lifetime
   * - MUST NOT access PluginCall or Capacitor APIs
   * - MUST NOT perform UI operations
   */
  private lateinit var implementation: IntegrityImpl

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * CONTRACT:
   * - Called exactly once
   * - This is the ONLY valid place to:
   *   - read static configuration
   *   - initialize the native implementation
   *   - inject configuration into the implementation
   *
   * WARNING:
   * - Re-initializing config or implementation outside this method
   *   is considered a plugin defect.
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
   * CONTRACT:
   * - This method is the ONLY place where native errors
   *   are translated into JS-visible failures.
   * - Error codes MUST be:
   *   - stable
   *   - documented
   *   - identical across platforms
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
   * CONTRACT:
   * - Resolves exactly once on success
   * - Rejects exactly once on failure
   * - Never throws outside this method
   *
   * NOTE:
   * - Option defaulting happens here by design.
   * - The Impl layer MUST receive fully normalized options.
   */
  @PluginMethod
  fun check(call: PluginCall) {
    // NOTE:
    // JSObject creation happens here to avoid leaking
    // Android-specific data structures outside the Plugin layer.

    // WARNING:
    // Any exception escaping performCheck() MUST be caught here.
    // Uncaught native exceptions are considered a plugin defect.

    try {
      val options =
        IntegrityCheckOptions(
          level = call.getString("level") ?: "basic",
          includeDebugInfo = call.getBoolean("includeDebugInfo") ?: false,
        )

      val result = implementation.performCheck(options)

      val jsResult = IntegrityUtils.toJSObject(result)

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
   * CONTRACT:
   * - This method NEVER decides when it should be called.
   * - The decision is fully delegated to the host application.
   *
   * NOTE:
   * - Returning `{ presented: false }` is NOT an error.
   * - This allows deterministic branching on the JS side.
   *
   * WARNING:
   * - UI navigation is allowed ONLY in the Plugin layer.
   * - The Impl layer MUST NEVER start Activities.
   */
  @PluginMethod
  fun presentBlockPage(call: PluginCall) {
    // WARNING:
    // This method relies on FLAG_ACTIVITY_NEW_TASK because
    // Capacitor plugins do not own an Activity lifecycle.

    // NOTE:
    // Clearing the task when not dismissible enforces
    // a hard block policy without relying on JS state.

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
   * - Used for diagnostics and compatibility checks only
   *
   * NOTE:
   * - This method is guaranteed not to fail.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }
}
