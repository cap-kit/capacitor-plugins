package io.capkit.privacyscreen

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Looper
import android.util.TypedValue
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
 *   screenshots and screen recordings); when a static `color` is configured,
 *   the blank switcher card is colored via the activity task description
 *   background color (what the system reads for secure snapshots).
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

  /**
   * Original theme background color, resolved before the first switcher card
   * color application so `disable()` can restore it. Null when no color was
   * applied yet.
   */
  private var originalSwitcherBackgroundColor: Int? = null

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
    applySwitcherCardColor(activity)
  }

  /** Disables protection: clears `FLAG_SECURE` and removes the scrim. */
  fun disable(activity: android.app.Activity?) {
    enabled = false
    setWindowSecure(activity, false)
    restoreSwitcherCard(activity)
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

  /**
   * Resolves the scrim color: the static `color` option wins when set
   * (explicit user intent), matching the current light/dark appearance and
   * falling back to the other variant when only one is configured.
   * Otherwise the legacy behavior applies (semi-transparent dim or opaque
   * black).
   */
  private fun scrimColor(
    activity: android.app.Activity?,
    dim: Boolean,
  ): Int {
    val hex = configuredHex(activity)
    if (hex != null) {
      return PrivacyScreenValidators.parseHexColor(hex)
    }
    return if (dim) Color.argb(160, 0, 0, 0) else Color.BLACK
  }

  /**
   * Returns the configured cover hex for the current light/dark appearance:
   * the matching variant wins, falling back to the other variant when only
   * one is set. Null when no variant is configured.
   *
   * Note: the appearance is sampled at call time; a system dark-mode change
   * while protection is already enabled is picked up on the next
   * enable/disable cycle, not live.
   */
  private fun configuredHex(activity: android.app.Activity?): String? {
    val isDark =
      activity
        ?.resources
        ?.configuration
        ?.uiMode
        ?.and(android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val primary = if (isDark) config.colorDark else config.colorLight
    val secondary = if (isDark) config.colorLight else config.colorDark
    return primary ?: secondary
  }

  /**
   * Colors the app-switcher card with the configured cover color.
   *
   * When a task's window is `FLAG_SECURE`, the system cannot snapshot its
   * content and instead paints the blank switcher card from the task's
   * `TaskDescription` background color (AOSP
   * `AbsAppSnapshotController.drawAppThemeSnapshot` reads
   * `taskDescription.getBackgroundColor()`). Setting it here therefore
   * colors the switcher card; the app label and icon are preserved. No-op
   * when no cover color is configured.
   */
  private fun applySwitcherCardColor(activity: android.app.Activity?) {
    if (activity == null) {
      return
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      // The TaskDescription.Builder (API 28+) is the only public API that
      // sets the background color the secure snapshot painter reads; older
      // APIs keep the platform-default blank card.
      return
    }
    val hex = configuredHex(activity) ?: return
    val apply = {
      if (originalSwitcherBackgroundColor == null) {
        originalSwitcherBackgroundColor = themeBackgroundColor(activity)
      }
      activity.setTaskDescription(switcherTaskDescription(activity, PrivacyScreenValidators.parseHexColor(hex)))
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      apply()
    } else {
      activity.runOnUiThread(apply)
    }
  }

  /** Restores the theme background color captured by [applySwitcherCardColor]. */
  private fun restoreSwitcherCard(activity: android.app.Activity?) {
    if (activity == null) {
      return
    }
    val original = originalSwitcherBackgroundColor ?: return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return
    }
    val apply = {
      activity.setTaskDescription(switcherTaskDescription(activity, original))
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      apply()
    } else {
      activity.runOnUiThread(apply)
    }
    originalSwitcherBackgroundColor = null
  }

  /**
   * Builds a task description that keeps the app label and icon (what the
   * launcher shows) and only overrides the background color the secure
   * snapshot painter reads. Requires API 28+ (`TaskDescription.Builder`);
   * callers must guard the API level before calling this.
   */
  private fun switcherTaskDescription(
    activity: android.app.Activity,
    backgroundColor: Int,
  ): ActivityManager.TaskDescription {
    val appInfo = activity.applicationInfo
    val builder = ActivityManager.TaskDescription.Builder()
    builder.setLabel(appInfo.loadLabel(activity.packageManager).toString())
    if (appInfo.icon != 0) {
      builder.setIcon(appInfo.icon)
    }
    builder.setBackgroundColor(backgroundColor)
    return builder.build()
  }

  /** Resolves the activity theme's `android:colorBackground` (0 when unset). */
  private fun themeBackgroundColor(activity: android.app.Activity): Int {
    val value = TypedValue()
    return if (activity.theme.resolveAttribute(android.R.attr.colorBackground, value, true)) {
      value.data
    } else {
      0
    }
  }

  private fun showOverlay(
    activity: android.app.Activity?,
    dim: Boolean,
  ) {
    activity?.runOnUiThread {
      val existing = overlayView
      if (existing != null) {
        existing.setBackgroundColor(scrimColor(activity, dim))
        return@runOnUiThread
      }

      val overlay =
        FrameLayout(activity).apply {
          tag = OVERLAY_TAG
          isClickable = false
          isFocusable = false
          setBackgroundColor(scrimColor(activity, dim))
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
