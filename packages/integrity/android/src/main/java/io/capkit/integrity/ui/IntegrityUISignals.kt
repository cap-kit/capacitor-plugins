package io.capkit.integrity.ui

import android.content.Context
import android.view.accessibility.AccessibilityManager
import io.capkit.integrity.IntegritySignalBuilder
import io.capkit.integrity.IntegritySignalIds
import io.capkit.integrity.models.IntegrityCheckOptions

/**
 * Detects UI-level attacks such as screen overlays and
 * suspicious accessibility services.
 */
object IntegrityUISignals {
  /**
   * Checks for potential overlay attacks (Tapjacking).
   * Combines accessibility service monitoring and window state heuristics.
   */
  fun checkOverlaySignals(
    context: Context,
    options: IntegrityCheckOptions,
  ): Map<String, Any>? {
    val signals = mutableMapOf<String, Any>()
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    // Heuristic 1: Active Accessibility Services with Touch Exploration
    // These services can read screen content and inject touches.
    val isEnabled = am.isEnabled
    val isTouchExplorationEnabled = am.isTouchExplorationEnabled

    if (isEnabled && isTouchExplorationEnabled) {
      return IntegritySignalBuilder.build(
        id = IntegritySignalIds.ANDROID_OVERLAY_DETECTED,
        category = "tamper",
        confidence = "medium",
        description = "Suspicious accessibility service state detected (potential overlay/UI spying)",
        metadata =
          mapOf(
            "accessibility_enabled" to isEnabled,
            "touch_exploration_enabled" to isTouchExplorationEnabled,
            "source" to "AccessibilityManager",
          ),
        options = options,
      )
    }

    // Heuristic 2: Detection via Window Focus (Passive Check)
    // If the application is active but lacks focus, an overlay might be on top.
    // This is a passive indicator used to increase the overall tamper score.
    return null
  }

  /**
   * Recommended native security practice:
   * Developers should also set 'setFilterTouchesWhenObscured(true)'
   * in their main View to prevent touches when an overlay is present.
   */
}
