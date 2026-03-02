package io.capkit.integrity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import io.capkit.integrity.config.Config
import io.capkit.integrity.error.ErrorMessages
import io.capkit.integrity.error.NativeError
import io.capkit.integrity.logger.Logger
import io.capkit.integrity.models.CheckOptions
import io.capkit.integrity.utils.Utils

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
 * - Map native NativeError to JS-facing error codes
 *
 * Forbidden:
 * - Platform-specific business logic
 * - Direct system API usage outside lifecycle-bound orchestration
 * - Throwing uncaught exceptions
 */
@CapacitorPlugin(
  name = "Integrity",
  permissions = [
    Permission(
      alias = "network",
      strings = [android.Manifest.permission.INTERNET],
    ),
  ],
)
class IntegrityPlugin : Plugin() {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Immutable plugin configuration.
   *
   * CONTRACT:
   * - Initialized exactly once in `load()`
   * - Treated as read-only afterward
   * - MUST NOT be mutated at runtime
   * - MUST NOT be accessed by the Impl layer
   */
  private lateinit var config: Config

  /**
   * Native implementation layer.
   *
   * CONTRACT:
   * - Owned by the Plugin layer
   * - Lifetime == plugin lifetime
   * - MUST NOT access PluginCall or Capacitor APIs
   * - MUST NOT perform UI operations
   */
  private lateinit var implementation: Integrity

  // ---------------------------------------------------------------------------
  // Event-related properties
  // ---------------------------------------------------------------------------

  /**
   * In-memory buffer for integrity signals detected before
   * a JavaScript listener is registered.
   *
   * Thread-safe implementation to prevent race conditions during
   * asynchronous event emission.
   */
  private val bufferedSignals = java.util.Collections.synchronizedList(mutableListOf<JSObject>())

  /**
   * BroadcastReceiver used to observe passive system events
   * relevant for integrity monitoring.
   */
  private lateinit var eventReceiver: IntegrityEventReceiver

  private companion object {
    /**
     * Canonical event name emitted to the JavaScript layer.
     *
     * CONTRACT:
     * - MUST remain stable across releases
     * - MUST match the JS-side event subscription name
     */
    private const val EVENT_INTEGRITY_SIGNAL = "integritySignal"
  }

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
   *   - register system event listeners (BroadcastReceivers)
   *
   * WARNING:
   * - Re-initializing config or implementation outside this method
   *   is considered a plugin defect.
   */
  override fun load() {
    super.load()

    config = Config(this)
    implementation = Integrity(context)
    implementation.updateConfig(config)

    Logger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)

    registerEventReceiver()
  }

  /**
   * Custom addListener implementation to flush early boot signals.
   *
   * PURPOSE:
   * - Ensures that signals captured during the early boot phase or
   * while the app was in the background are delivered as soon as
   * the JavaScript side registers a listener.
   */
  @PluginMethod
  override fun addListener(call: PluginCall) {
    val eventName = call.getString("eventName")
    super.addListener(call)

    if (eventName == EVENT_INTEGRITY_SIGNAL) {
      flushBufferedSignals()
    }
  }

  /**
   * Called when the host Activity resumes.
   *
   * PURPOSE:
   * - Flush any integrity signals captured while no JS listeners
   * were registered.
   * - Perform a targeted check for debugger attachment during resume.
   */
  override fun handleOnResume() {
    super.handleOnResume()
    flushBufferedSignals()

    // Immediate check for debugger attachment upon resume (Real-time monitor)
    if (android.os.Debug.isDebuggerConnected()) {
      val options = CheckOptions(level = "standard", includeDebugInfo = false)
      execute {
        try {
          val result = implementation.performCheck(options)
          val jsResult = Utils.toJSObject(result)
          emitOrBufferSignal(jsResult)
        } catch (e: Exception) {
          Logger.error("Real-time monitor: Debugger check failed: ${e.message}")
        }
      }
    }
  }

  /**
   * Flushes all buffered integrity signals to JavaScript listeners.
   * Ensures FIFO delivery and prevents duplicate emissions.
   *
   * NOTE:
   * - This method is thread-safe and idempotent.
   * - Buffer is cleared immediately after dispatch.
   */
  private fun flushBufferedSignals() {
    synchronized(bufferedSignals) {
      if (hasListeners(EVENT_INTEGRITY_SIGNAL) && bufferedSignals.isNotEmpty()) {
        Logger.debug("Flushing ${bufferedSignals.size} buffered signals to JS")
        val iterator = bufferedSignals.iterator()
        while (iterator.hasNext()) {
          val signal = iterator.next()
          notifyListeners(EVENT_INTEGRITY_SIGNAL, signal, true)
          iterator.remove()
        }
      }
    }
  }

  /**
   * Called when the plugin is being destroyed.
   *
   * CONTRACT:
   * - All BroadcastReceivers registered by this plugin
   *   MUST be unregistered here.
   */
  override fun handleOnDestroy() {
    super.handleOnDestroy()
    unregisterEventReceiver()
  }

  // ---------------------------------------------------------------------------
  // Event emission and buffering
  // ---------------------------------------------------------------------------

  /**
   * Emits an integrity signal to JavaScript or buffers it
   * if no listeners are currently registered.
   *
   * @param signal Fully-formed integrity signal payload.
   */
  private fun emitOrBufferSignal(signal: JSObject) {
    synchronized(bufferedSignals) {
      if (hasListeners(EVENT_INTEGRITY_SIGNAL)) {
        notifyListeners(EVENT_INTEGRITY_SIGNAL, signal, true)
      } else {
        bufferedSignals.add(signal)
      }
    }
  }

  /**
   * Registers the BroadcastReceiver used for passive integrity signals.
   *
   * NOTE:
   * - No polling or background services are introduced.
   * - Observers are scoped strictly to the plugin lifecycle.
   */
  private fun registerEventReceiver() {
    eventReceiver =
      IntegrityEventReceiver { signal ->
        emitOrBufferSignal(signal)
      }

    val intentFilter =
      IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addDataScheme("package")
      }

    val flags =
      // UPSIDE_DOWN_CAKE API LEVEL 34
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.RECEIVER_NOT_EXPORTED
      } else {
        0
      }

    // TIRAMISU API LEVEL 33
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(eventReceiver, intentFilter, flags)
    } else {
      context.registerReceiver(eventReceiver, intentFilter)
    }
  }

  /**
   * Unregisters the integrity BroadcastReceiver.
   *
   * NOTE:
   * - It is safe to call this method even if the receiver
   *   was never registered.
   */
  private fun unregisterEventReceiver() {
    // It's safe to call unregisterReceiver even if the receiver was never registered
    try {
      context.unregisterReceiver(eventReceiver)
    } catch (_: IllegalArgumentException) {
      // Receiver wasn't registered, ignore
    }
  }

  // ---------------------------------------------------------------------------
  // Error mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps native NativeError values to JavaScript-facing error codes.
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
    error: NativeError,
  ) {
    val message = error.message ?: ErrorMessages.INTERNAL_ERROR
    call.reject(message, error.errorCode)
  }

  private fun handleError(
    call: PluginCall,
    throwable: Throwable,
  ) {
    if (throwable is NativeError) {
      reject(call, throwable)
    } else {
      val message = throwable.message ?: ErrorMessages.UNEXPECTED_NATIVE_ERROR
      reject(call, NativeError.InitFailed(message))
    }
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
    val options =
      CheckOptions(
        level = call.getString("level") ?: "basic",
        includeDebugInfo = call.getBoolean("includeDebugInfo") ?: false,
      )

    // Execute integrity checks off the plugin thread to avoid future ANR risks.
    execute {
      try {
        val result = implementation.performCheck(options)
        val jsResult = Utils.toJSObject(result)
        call.resolve(jsResult)
      } catch (e: NativeError) {
        handleError(call, e)
      } catch (e: Exception) {
        handleError(call, e)
      }
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
    val blockPage = config.blockPage
    if (blockPage == null || !blockPage.enabled || blockPage.url == null) {
      call.resolve(JSObject().put("presented", false))
      return
    }

    val reason = call.getString("reason")
    val dismissible = call.getBoolean("dismissible") ?: false
    val customUrl = call.getString("customUrl")

    // Apply URL limit validation
    if (customUrl != null && customUrl.length > 2048) {
      handleError(call, NativeError.InvalidInput("customUrl exceeds maximum length of 2048 characters"))
      return
    }

    // Determine final URL: customUrl takes precedence over config
    val urlBase = customUrl ?: blockPage.url

    // Build query string with reason and context
    val queryParams = mutableListOf<String>()

    if (reason != null) {
      queryParams.add("reason=${Uri.encode(reason)}")
    }

    val contextObj = call.getObject("context")
    if (contextObj != null && contextObj.length() > 0) {
      val contextJson = contextObj.toString()
      queryParams.add("context=${Uri.encode(contextJson)}")
    }

    val finalUrl: String
    finalUrl =
      if (queryParams.isEmpty()) {
        urlBase
      } else {
        val queryString = queryParams.joinToString("&")
        if (urlBase.contains("?")) {
          "$urlBase&$queryString"
        } else {
          "$urlBase?$queryString"
        }
      }

    val intent =
      Intent(
        context,
        io.capkit.integrity.ui.BlockActivity::class.java,
      ).apply {
        putExtra("url", finalUrl)
        putExtra("dismissible", dismissible)
        putExtra("preventTapJacking", blockPage.preventTapJacking)
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
   * NOTE:
   * - Used exclusively for diagnostics and compatibility checks.
   * - Must not be used for feature detection.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }

  // ---------------------------------------------------------------------------
  // BroadcastReceiver implementation
  // ---------------------------------------------------------------------------

  /**
   * BroadcastReceiver for passive system integrity signals.
   *
   * PURPOSE:
   * - Reacts to system-level events that may indicate integrity changes
   *   (e.g. package installation or replacement).
   *
   * NOTE:
   * - Failures are logged but NOT propagated to JavaScript.
   * - This receiver is observational and non-blocking.
   */
  private inner class IntegrityEventReceiver(
    private val onSignalDetected: (JSObject) -> Unit,
  ) : BroadcastReceiver() {
    override fun onReceive(
      context: Context?,
      intent: Intent?,
    ) {
      when (intent?.action) {
        Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
          val options =
            CheckOptions(
              level = "standard",
              includeDebugInfo = false,
            )

          // Execute integrity checks off the main/plugin thread to avoid blocking
          execute {
            try {
              val result = implementation.performCheck(options)
              val jsResult = Utils.toJSObject(result)
              onSignalDetected(jsResult)
            } catch (e: NativeError) {
              Logger.error(
                "IntegrityEventReceiver: Error during package change check: ${e.message}",
              )
            } catch (e: Exception) {
              Logger.error(
                "IntegrityEventReceiver: Unexpected error during package change check: ${e.message}",
              )
            }
          }
        }
      }
    }
  }
}
