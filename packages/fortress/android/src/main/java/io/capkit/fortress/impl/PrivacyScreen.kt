package io.capkit.fortress.impl

import android.app.Activity
import android.view.WindowManager

class PrivacyScreen {
  /**
   * Enables protection by adding FLAG_SECURE to the window.
   * This prevents screenshots and masks the app in the task switcher.
   */
  fun lock(activity: Activity?) {
    activity?.runOnUiThread {
      activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }
  }

  /**
   * Disables protection by removing FLAG_SECURE.
   */
  fun unlock(activity: Activity?) {
    activity?.runOnUiThread {
      activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
  }

  fun setContentVisibility(
    activity: android.app.Activity?,
    visible: Boolean,
  ) {
    activity?.runOnUiThread {
      val view = activity.window.decorView
      if (visible) {
        view.visibility = android.view.View.VISIBLE
      } else {
        view.visibility = android.view.View.INVISIBLE
      }
    }
  }
}
