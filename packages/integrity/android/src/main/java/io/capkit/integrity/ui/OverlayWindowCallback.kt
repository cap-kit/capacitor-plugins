package io.capkit.integrity.ui

import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window

/**
 * Window.Callback wrapper that detects overlay attacks by monitoring
 * touch and key events for the FLAG_WINDOW_IS_OBSCURED flag.
 *
 * This provides an additional layer of RASP (Runtime Application Self-Protection)
 * beyond the existing AccessibilityManager-based detection.
 *
 * Security note:
 * - This callback is only active while the BlockActivity is visible
 * - It must be properly unregistered to avoid memory leaks
 */
class OverlayWindowCallback(
  private val originalCallback: Window.Callback,
  private val onOverlayDetected: (eventType: String, flags: Int) -> Unit,
) : Window.Callback by originalCallback {
  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    checkForOverlay(event.action, event.flags)
    return originalCallback.dispatchTouchEvent(event)
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    checkForOverlay(event.action, event.flags)
    return originalCallback.dispatchKeyEvent(event)
  }

  private fun checkForOverlay(
    action: Int,
    flags: Int,
  ) {
    // Only check on ACTION_DOWN to avoid flooding with events
    if (action != MotionEvent.ACTION_DOWN && action != KeyEvent.ACTION_DOWN) {
      return
    }

    // Check if the window is obscured by another window
    val isObscured =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0
      } else {
        // Fallback: check FLAG_WINDOW_IS_PARTIALLY_OBSCURED for older APIs
        @Suppress("DEPRECATION")
        flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
      }

    if (isObscured) {
      onOverlayDetected("touch", flags)
    }
  }
}
