package io.capkit.privacyscreen

import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import io.capkit.privacyscreen.config.PrivacyScreenConfig
import io.capkit.privacyscreen.logger.PrivacyScreenLogger
import io.capkit.privacyscreen.utils.PrivacyScreenValidators

/**
 * Platform-specific native implementation for the PrivacyScreen plugin.
 *
 * Pure Android logic extracted from the Fortress privacy engine and adapted
 * to the standalone contract. MUST NOT depend on Capacitor bridge APIs or
 * PluginCall objects.
 *
 * Responsibilities:
 * - Mask snapshots via `FLAG_SECURE` while enabled (app switcher, recents,
 *   screenshots and screen recordings).
 * - Render a temporary scrim when the activity is hidden, honoring the
 *   `privacyModeOnActivityHidden` session option.
 */
class PrivacyScreenImpl(
  /**
   * Android application context.
   *
   * Nullable so the pure state logic can be exercised headless in JVM unit
   * tests (no Context exists there). The implementation never reads it
   * directly: every window/scrim side effect is driven by the activity
   * parameter passed to each method.
   */
  private val context: Context?,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: PrivacyScreenConfig

  /** Current protection state. Source of truth for `isEnabled`. */
  private var enabled = false

  /** Session option: dim scrim instead of an opaque one. */
  private var dimBackground = false

  /** Session option: overlay shown while the activity is hidden. */
  private var privacyModeOnActivityHidden = "none"

  private var overlayView: View? = null

  private companion object {
    const val OVERLAY_TAG = 9961
  }

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * MUST be called exactly once during the plugin load phase.
   */
  fun updateConfig(newConfig: PrivacyScreenConfig) {
    this.config = newConfig
    PrivacyScreenLogger.verbose = newConfig.verboseLogging
    PrivacyScreenLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Protection control
  // ---------------------------------------------------------------------------

  /**
   * Enables protection by applying `FLAG_SECURE`, which blanks the app
   * snapshot in the app switcher and blocks screenshots and recordings.
   * No steady scrim is shown while the app is active.
   *
   * @param activity Host activity or null (state is still tracked).
   * @param dimBackground Unused legacy option (kept for API compatibility).
   * @param hiddenMode Session option `privacyModeOnActivityHidden`.
   */
  fun enable(
    activity: android.app.Activity?,
    dimBackground: Boolean,
    hiddenMode: String,
  ) {
    this.dimBackground = dimBackground
    this.privacyModeOnActivityHidden = PrivacyScreenValidators.normalizeHiddenMode(hiddenMode)
    enabled = true
    setWindowSecure(activity, true)
  }

  /** Disables protection: clears `FLAG_SECURE` and removes the scrim. */
  fun disable(activity: android.app.Activity?) {
    enabled = false
    setWindowSecure(activity, false)
    hideOverlay(activity)
  }

  /** Current protection state. */
  fun isEnabled(): Boolean = enabled

  /**
   * Called when the activity is hidden (e.g. a system biometric prompt).
   * Shows a temporary scrim honoring the session hidden mode. `FLAG_SECURE`
   * already protects switcher snapshots; this covers live-view fragments.
   */
  fun onActivityHidden(activity: android.app.Activity?) {
    if (!enabled || activity == null) {
      return
    }
    when (privacyModeOnActivityHidden) {
      "dim" -> showOverlay(activity, dim = true)
      "splash" -> showOverlay(activity, dim = false)
      else -> Unit
    }
  }

  /**
   * Removes the transient hidden-activity scrim when the activity is shown
   * again. `FLAG_SECURE` keeps protecting the switcher snapshot.
   */
  fun onActivityShown(activity: android.app.Activity?) {
    if (!enabled) {
      return
    }
    hideOverlay(activity)
  }

  // ---------------------------------------------------------------------------
  // Native mechanics
  // ---------------------------------------------------------------------------

  private fun setWindowSecure(
    activity: android.app.Activity?,
    secure: Boolean,
  ) {
    val hostActivity = activity ?: return
    val applyFlag = {
      if (secure) {
        hostActivity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
      } else {
        hostActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
      applyFlag()
    } else {
      hostActivity.runOnUiThread(applyFlag)
    }
  }

  private fun scrimColor(dim: Boolean): Int = if (dim) Color.argb(160, 0, 0, 0) else Color.BLACK

  private fun showOverlay(
    activity: android.app.Activity?,
    dim: Boolean,
  ) {
    activity?.runOnUiThread {
      val existing = overlayView
      if (existing != null) {
        existing.setBackgroundColor(scrimColor(dim))
        return@runOnUiThread
      }

      val overlay =
        FrameLayout(activity).apply {
          tag = OVERLAY_TAG
          isClickable = false
          isFocusable = false
          setBackgroundColor(scrimColor(dim))
          layoutParams =
            FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

      activity.window.addContentView(overlay, overlay.layoutParams)
      overlayView = overlay
    }
  }

  private fun hideOverlay(activity: android.app.Activity?) {
    activity?.runOnUiThread {
      overlayView?.let { view ->
        try {
          (view.parent as? ViewGroup)?.removeView(view)
        } catch (_: Exception) {
        }
      }
      overlayView = null
    }
  }
}
