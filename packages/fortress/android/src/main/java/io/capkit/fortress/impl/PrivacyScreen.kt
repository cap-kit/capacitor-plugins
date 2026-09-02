package io.capkit.fortress.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import io.capkit.fortress.R

/**
 * Android privacy-screen helper.
 *
 * Responsibilities:
 * - Enable screenshot/task-switcher masking via `FLAG_SECURE`
 * - Add custom overlay view with optional text and/or image for lock messaging
 * - Disable masking and remove overlay when vault content can be shown
 * - Control root-view visibility for additional protection flows
 */
class PrivacyScreen {
  private companion object {
    const val OVERLAY_TAG = 9961
  }

  private var overlayView: View? = null
  private var overlayImageView: ImageView? = null
  private var onTapUnlock: (() -> Unit)? = null

  // Privacy overlay configuration
  private var overlayText: String = ""
  private var showOverlayText: Boolean = true
  private var overlayTextColor: Int = Color.WHITE
  private var overlayImageName: String = ""
  private var showOverlayImage: Boolean = true
  private var backgroundOpacity: Float = -1f
  private var overlayTheme: String = "system"

  /**
   * Updates only the window FLAG_SECURE state.
   *
   * This protects app snapshots in recents/task-switcher even when
   * the visual overlay is not shown.
   */
  fun setWindowSecure(
    activity: Activity?,
    enabled: Boolean,
  ) {
    val hostActivity = activity ?: return
    val applyFlag = {
      if (enabled) {
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
   * Updates the privacy overlay configuration.
   */
  fun updateOverlayConfig(
    text: String,
    showText: Boolean,
    textColor: String,
    backgroundOpacity: Double,
    theme: String,
    imageName: String,
    showImage: Boolean,
  ) {
    this.overlayText = text
    this.showOverlayText = showText

    // Parse text color from hex string
    if (textColor.isNotEmpty()) {
      try {
        this.overlayTextColor = textColor.toColorInt()
      } catch (_: Exception) {
        this.overlayTextColor = Color.WHITE
      }
    } else {
      this.overlayTextColor = Color.WHITE
    }

    // Parse background opacity
    if (backgroundOpacity in 0.0..1.0) {
      this.backgroundOpacity = backgroundOpacity.toFloat()
    } else {
      this.backgroundOpacity = -1f
    }

    this.overlayTheme =
      if (theme == "light" || theme == "dark" || theme == "system") {
        theme
      } else {
        "system"
      }

    // Image configuration
    this.overlayImageName = imageName
    this.showOverlayImage = showImage

    // Update existing overlay if visible
    updateOverlayElements()
  }

  /**
   * Sets the callback invoked when the user taps the overlay.
   */
  fun setOnTapUnlock(callback: () -> Unit) {
    onTapUnlock = callback
  }

  /**
   * Updates the overlay text and image without recreating the view.
   */
  private fun updateOverlayElements() {
    val view = overlayView ?: return

    // Update text
    val textView = view.findViewById<TextView>(R.id.fortress_overlay_text)
    textView?.let {
      it.text = overlayText
      it.setTextColor(overlayTextColor)
      it.visibility = if (overlayText.isEmpty() || !showOverlayText) View.GONE else View.VISIBLE
    }

    // Update image
    val imageView = view.findViewById<ImageView>(R.id.fortress_overlay_image)
    imageView?.let {
      if (overlayImageName.isNotEmpty() && showOverlayImage) {
        val resourceId = resolveDrawableResourceId(it.context, overlayImageName)
        if (resourceId != 0) {
          it.setImageResource(resourceId)
          it.visibility = View.VISIBLE
        } else {
          it.visibility = View.GONE
        }
      } else {
        it.visibility = View.GONE
      }
    }

    view.setBackgroundColor(resolveOverlayBackgroundColor(view.context))
  }

  private fun resolveIsDarkTheme(context: Context): Boolean {
    val systemDark =
      (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    return when (overlayTheme) {
      "light" -> false
      "dark" -> true
      else -> systemDark
    }
  }

  private fun resolveOverlayBackgroundColor(context: Context): Int {
    val isDark = resolveIsDarkTheme(context)
    if (backgroundOpacity >= 0) {
      // Keep explicit opacity behavior stable and high-contrast for text/images.
      return Color.argb((backgroundOpacity * 255).toInt(), 0, 0, 0)
    }

    // Default background when opacity is not provided.
    val alpha = if (isDark) 255 else 220
    val channel = if (isDark) 0 else 255
    return Color.argb(alpha, channel, channel, channel)
  }

  @SuppressLint("DiscouragedApi")
  private fun resolveDrawableResourceId(
    context: Context,
    drawableName: String,
  ): Int = context.resources.getIdentifier(drawableName, "drawable", context.packageName)

  /**
   * Enables protection by adding FLAG_SECURE to the window
   * and showing an optional overlay with text and/or image.
   */
  fun lock(activity: Activity?) {
    activity?.runOnUiThread {
      activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

      // Don't add overlay if already present
      if (overlayView != null) {
        return@runOnUiThread
      }

      // Create overlay view
      val overlay =
        FrameLayout(activity).apply {
          tag = OVERLAY_TAG
          isClickable = true
          isFocusable = true
          isFocusableInTouchMode = true
          setOnClickListener { onTapUnlock?.invoke() }
          setBackgroundColor(resolveOverlayBackgroundColor(activity))
          layoutParams =
            FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

      // Create vertical container for image + text
      val containerParams =
        FrameLayout
          .LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
          ).apply {
            gravity = Gravity.CENTER
          }
      val container =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          gravity = Gravity.CENTER
          layoutParams = containerParams
        }

      // Create image view (top)
      val imageViewParams =
        LinearLayout
          .LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
          ).apply {
            gravity = Gravity.CENTER
          }
      val imageView =
        ImageView(activity).apply {
          id = R.id.fortress_overlay_image
          scaleType = ImageView.ScaleType.FIT_CENTER
          layoutParams = imageViewParams
          visibility =
            if (overlayImageName.isNotEmpty() && showOverlayImage) {
              val resourceId = resolveDrawableResourceId(context, overlayImageName)
              if (resourceId != 0) {
                setImageResource(resourceId)
                View.VISIBLE
              } else {
                View.GONE
              }
            } else {
              View.GONE
            }
        }

      overlayImageView = imageView

      // Create text view (bottom)
      val textView =
        TextView(activity).apply {
          id = R.id.fortress_overlay_text
          text = overlayText
          setTextColor(overlayTextColor)
          textSize = 20f
          gravity = Gravity.CENTER
          setPadding(32, 32, 32, 32)
          visibility = if (overlayText.isNotEmpty() && showOverlayText) View.VISIBLE else View.GONE
        }

      // Add to container (image first, then text)
      if (overlayImageName.isNotEmpty() && showOverlayImage) {
        container.addView(imageView)
      }
      if (overlayText.isNotEmpty() && showOverlayText) {
        container.addView(textView)
      }

      // Add container to overlay
      overlay.addView(container)

      // Add overlay to window
      activity.window.addContentView(overlay, overlay.layoutParams)
      overlayView = overlay
    }
  }

  /**
   * Removes overlay without changing window secure flags.
   */
  fun hideOverlay(activity: Activity?) {
    activity?.runOnUiThread {
      // Remove overlay only
      overlayView?.let { view ->
        try {
          val parent = view.parent as? ViewGroup
          parent?.removeView(view)
        } catch (_: Exception) {
          // View may already be removed
        }
      }
      overlayView = null
      overlayImageView = null
    }
  }

  /**
   * Disables privacy protection by clearing FLAG_SECURE and removing overlay.
   */
  fun unlock(activity: Activity?) {
    activity?.runOnUiThread {
      activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    hideOverlay(activity)
  }

  /**
   * Toggles content visibility on the current activity root view.
   *
   * This is used for extra masking flows where window-level protection
   * is not the only mechanism.
   */
  fun setContentVisibility(
    activity: Activity?,
    visible: Boolean,
  ) {
    activity?.runOnUiThread {
      val view = activity.window.decorView
      if (visible) {
        view.visibility = View.VISIBLE
      } else {
        view.visibility = View.INVISIBLE
      }
    }
  }
}
