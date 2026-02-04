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
  // Signal categories & constants
  // ---------------------------------------------------------------------------

  private companion object {
    const val CATEGORY_ROOT = "root"
    const val CATEGORY_EMULATOR = "emulator"
    const val CATEGORY_HOOK = "hook"
    const val CATEGORY_TAMPER = "tamper"

    const val SIGNAL_ANDROID_EMULATOR = "android_emulator"
    const val SIGNAL_ANDROID_FRIDA_PROCESS = "android_frida_process"
    const val SIGNAL_ANDROID_FRIDA_PORT = "android_frida_port"
    const val SIGNAL_ANDROID_SIGNATURE_INVALID = "android_signature_invalid"
  }

  /**
   * Confidence weights used to compute the integrity score.
   *
   * NOTE:
   * This scoring model is provisional and non-normative.
   */
  private val confidenceWeights =
    mapOf(
      "high" to 30,
      "medium" to 15,
      "low" to 5,
    )

  // ---------------------------------------------------------------------------
  // Check
  // ---------------------------------------------------------------------------

  /**
   * Executes a baseline integrity check.
   *
   * This method:
   * - aggregates platform-specific integrity signals
   * - computes a provisional integrity score
   * - derives a compromised flag
   *
   * Errors thrown by the native implementation
   * are propagated to JavaScript via Promise rejection.
   */
  @PluginMethod
  fun check(call: PluginCall) {
    try {
      val signals = mutableListOf<Map<String, Any>>()

      // --- Root detection ---
      signals.addAll(implementation.checkRootSignals())

      // --- Emulator detection ---
      val isEmulator = implementation.checkEmulator()
      if (isEmulator) {
        signals.add(
          mapOf(
            "id" to SIGNAL_ANDROID_EMULATOR,
            "category" to CATEGORY_EMULATOR,
            "confidence" to "high",
          ),
        )
      }

      // --- Frida / instrumentation detection ---
      if (implementation.checkFridaProcesses()) {
        signals.add(
          mapOf(
            "id" to SIGNAL_ANDROID_FRIDA_PROCESS,
            "category" to CATEGORY_HOOK,
            "confidence" to "high",
          ),
        )
      }

      if (implementation.checkFridaPorts()) {
        signals.add(
          mapOf(
            "id" to SIGNAL_ANDROID_FRIDA_PORT,
            "category" to CATEGORY_HOOK,
            "confidence" to "medium",
          ),
        )
      }

      // --- Tamper / signature integrity ---
      if (!implementation.checkAppSignature()) {
        signals.add(
          mapOf(
            "id" to SIGNAL_ANDROID_SIGNATURE_INVALID,
            "category" to CATEGORY_TAMPER,
            "confidence" to "high",
          ),
        )
      }

      // --- Score computation ---
      var score = 0
      for (signal in signals) {
        val confidence = signal["confidence"] as? String
        score += confidenceWeights[confidence] ?: 0
      }

      val result =
        JSObject().apply {
          put("signals", signals)
          put("score", score)
          put("compromised", score >= 30)
          put(
            "environment",
            JSObject().apply {
              put("platform", "android")
              put("isEmulator", isEmulator)
              put("isDebugBuild", false)
            },
          )
          put("timestamp", System.currentTimeMillis())
        }

      call.resolve(result)
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
