package io.capkit.splashmotion

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import io.capkit.splashmotion.config.SplashMotionConfig
import io.capkit.splashmotion.logger.SplashMotionLogger
import io.capkit.splashmotion.model.EntranceOptionsData
import io.capkit.splashmotion.model.HapticOptionsData
import io.capkit.splashmotion.model.HideOptionsData
import io.capkit.splashmotion.model.ImageOptionsData
import io.capkit.splashmotion.model.ShowOptionsData
import io.capkit.splashmotion.utils.SplashMotionValidators
import io.capkit.splashmotion.utils.SplashMotionZipReader
import java.net.URL
import androidx.core.splashscreen.SplashScreen as SystemSplashScreen

/**
 * Platform-specific native implementation for the SplashMotion plugin.
 *
 * Renders a full-screen splash surface (static image or Lottie animation)
 * with fade transitions, optional haptic feedback and auto-hide scheduling.
 * MUST NOT depend on Capacitor bridge APIs or PluginCall objects.
 *
 * Responsibilities:
 * - Show/hide the splash surface with fade in/out durations.
 * - Render a Lottie animation when `lottie` options are provided.
 * - Fire haptic feedback on visibility transitions.
 * - Auto-hide after `showDuration` when `autoHide` is enabled.
 */
class SplashMotionImpl(
  /**
   * Android application context.
   *
   * Nullable so the pure state logic can be exercised headless in JVM unit
   * tests (no Context exists there). The implementation never reads it
   * directly: every visual side effect is driven by the activity parameter
   * passed to each method.
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
  private lateinit var config: SplashMotionConfig

  /** Current splash surface, when visible. Main-thread only. */
  private var dialog: Dialog? = null

  /** Root content view of the active splash surface (fade target). */
  private var rootView: View? = null

  /** Pending auto-hide callback, when scheduled. */
  private var autoHideRunnable: Runnable? = null

  /**
   * Platform (Android 12 SplashScreen compat API) handle when the launch
   * path used the system splash. When set, the launch splash is the
   * system-rendered one from the activity's launch theme and NO dialog
   * surface is drawn on top of it.
   */
  private var systemSplash: SystemSplashScreen? = null

  /** Whether the system launch splash is currently held on screen. */
  private var launchSplashVisible = false

  /**
   * Whether the launch splash hid the system bars on the HOST activity
   * window; drives the restore when the launch splash is released.
   */
  private var launchBarsHidden = false

  /** When the launch splash was requested; drives the minimum hold time. */
  private var launchShownAt: Long = 0L

  /**
   * Duration (ms) of the launch lottie composition, captured when the
   * composition loads. Clamps the launch minimum hold so the splash is never
   * released mid-animation (see [launchMinimumHoldRemainingMs]).
   */
  private var launchLottieDurationMs: Long = 0L

  /** Whether the system launch splash exit animation is running. */
  private var launchSplashHiding = false

  /**
   * Content view whose first draw is blocked while the system launch splash
   * is held (parity with the official plugin's OnPreDrawListener): the app
   * content must not paint behind the splash, so the exit animation fades
   * over freshly painted content instead of a stale or blank frame.
   */
  private var launchContentView: View? = null

  /** Active pre-draw blocker for [launchContentView], when attached. */
  private var launchPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null

  // Lazy so headless JVM unit tests can construct the implementation
  // without touching Looper (not mockable in plain unit tests).
  private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * MUST be called exactly once during the plugin load phase.
   */
  fun updateConfig(newConfig: SplashMotionConfig) {
    this.config = newConfig
    SplashMotionLogger.verbose = newConfig.verboseLogging
    SplashMotionLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
    // Guard rail: the launch lottie takes precedence over the static
    // launch image (renderLottie/renderStaticImage are mutually
    // exclusive); report the ignored configuration instead of letting
    // it silently do nothing.
    if (newConfig.launchLottie != null && newConfig.launchImageExplicitlyConfigured) {
      SplashMotionLogger.warn(
        "launchLottie is configured together with a static launch image (imageResourceName); " +
          "the animated launch lottie takes precedence and the static image is ignored",
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Splash control
  // ---------------------------------------------------------------------------

  /**
   * Shows the splash surface.
   *
   * When a splash is already visible, the surface is kept and only the
   * auto-hide schedule and haptic feedback are refreshed.
   *
   * @param activity Host activity or null (the call is then a no-op).
   * @param options Parsed show options.
   */
  fun show(
    activity: Activity?,
    options: ShowOptionsData,
  ) {
    if (activity == null) {
      SplashMotionLogger.warn("show() ignored: no host activity")
      return
    }

    activity.runOnUiThread {
      val existing = dialog
      if (existing != null && existing.isShowing) {
        SplashMotionLogger.debug("show() called while visible: refreshing schedule")
        scheduleAutoHide(activity, options)
        fireHaptic(options.haptic)
        return@runOnUiThread
      }
      if (launchSplashVisible || launchSplashHiding) {
        // System launch splash is on screen: do not draw a second surface
        // on top of it (parity with the official plugin, whose show() is a
        // no-op while the launch splash is visible).
        SplashMotionLogger.debug("show() called while system launch splash visible: ignored")
        return@runOnUiThread
      }
      presentSplash(activity, options)
    }
  }

  /**
   * Hides the splash surface with a fade out.
   *
   * @param activity Host activity or null (the call is then a no-op).
   * @param options Parsed hide options.
   */
  fun hide(
    activity: Activity?,
    options: HideOptionsData,
  ) {
    if (activity == null) {
      return
    }

    activity.runOnUiThread {
      val remaining = launchMinimumHoldRemainingMs()
      if (remaining > 0) {
        mainHandler.postDelayed({ doHide(activity, options) }, remaining)
        return@runOnUiThread
      }
      doHide(activity, options)
    }
  }

  /**
   * Remaining time the launch splash must stay visible under the minimum
   * hold rule; zero when there is no launch marker or auto-hide is on (in
   * that mode launchShowDuration already controls the auto-hide schedule).
   */
  private fun launchMinimumHoldRemainingMs(): Long {
    if (launchShownAt == 0L || config.launchAutoHide) {
      return 0L
    }
    // Clamped to the launch lottie duration: a loaded launch animation must
    // always play to completion before the splash is released.
    val minimum =
      SplashMotionValidators.effectiveLaunchMinimumHoldMs(
        config.launchShowDuration,
        launchLottieDurationMs,
      )
    val elapsed = SystemClock.elapsedRealtime() - launchShownAt
    return (minimum - elapsed).coerceAtLeast(0L)
  }

  private fun doHide(
    activity: Activity,
    options: HideOptionsData,
  ) {
    launchShownAt = 0L
    launchLottieDurationMs = 0L
    if (systemSplash != null && (launchSplashVisible || launchSplashHiding)) {
      // Launch path: release the system cover and, when the launchLottie
      // path presented the animated dialog above it, dismiss that too.
      cancelAutoHide()
      releaseLaunchSplash(activity, options.fadeOutDuration, options.haptic)
      return
    }
    cancelAutoHide()
    dismissSplash(activity, options.fadeOutDuration, options.haptic)
  }

  /**
   * Releases the whole launch surface: the system cover (official parity)
   * plus the animated plugin dialog when the launchLottie path presented
   * one. Shared by the auto-hide schedule and hide().
   */
  private fun releaseLaunchSplash(
    activity: Activity,
    fadeOutDuration: Long?,
    haptic: HapticOptionsData?,
  ) {
    releaseSystemLaunchSplash()
    if (launchBarsHidden) {
      launchBarsHidden = false
      showSystemBars(activity.window)
    }
    if (dialog?.isShowing == true) {
      dismissSplash(activity, fadeOutDuration, haptic)
    }
  }

  /**
   * Restarts the launch lottie animation from its first frame. Used when
   * the system starting window exits: that window sits above every app
   * window, so the animation has been playing underneath (unseen) while
   * the WebView painted its first frame; restarting it makes the user
   * watch the full intro from the moment the animated surface is visible.
   * No-op when there is no launch dialog or no lottie view in it.
   */
  private fun restartLaunchLottie() {
    if (dialog?.isShowing != true) {
      SplashMotionLogger.debug("Launch lottie restart skipped: launch dialog not showing")
      return
    }
    val lottieView = rootView?.findViewWithTag<LottieAnimationView>(LOTTIE_VIEW_TAG)
    if (lottieView == null) {
      SplashMotionLogger.debug("Launch lottie restart skipped: no lottie view in the launch surface")
      return
    }
    lottieView.progress = 0f
    lottieView.playAnimation()
    SplashMotionLogger.debug("Restarting launch lottie for the revealed surface")
  }

  /**
   * Animated launch surface: presents the plugin splash dialog (config
   * launchLottie) above the held system splash and releases the system
   * cover once the dialog's first frame is drawn, so the system splash's
   * exit fade happens fully behind the animated surface.
   */
  private fun presentLaunchSurface(activity: Activity) {
    val options =
      ShowOptionsData(
        autoHide = false,
        fadeInDuration = 0,
        fadeOutDuration = 0,
        showDuration = config.launchShowDuration,
        lottie = config.launchLottie,
        haptic = config.launchHaptic,
      )
    presentSplash(activity, options)
    // Hide the system bars on the HOST activity window for the whole
    // launch: the dialog's own insets request settles late, and a plain
    // dialog window does not cover the status-bar/cutout area, so the
    // earliest and most reliable hide point is the activity window.
    // Restored when the launch splash is released.
    if (config.splashImmersive || config.splashFullScreen) {
      hideSystemBars(activity.window)
      launchBarsHidden = true
    }
    val surface = rootView
    surface?.viewTreeObserver?.addOnPreDrawListener(
      object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
          surface.viewTreeObserver.removeOnPreDrawListener(this)
          releaseSystemLaunchSplash()
          return true
        }
      },
    )
  }

  /** Whether the splash surface is currently visible. */
  fun isVisible(): Boolean = dialog?.isShowing == true || launchSplashVisible || launchSplashHiding

  // ---------------------------------------------------------------------------
  // Launch behavior
  // ---------------------------------------------------------------------------

  /**
   * Shows the launch splash when static configuration requests it,
   * honoring `launchShowDuration`, `launchAutoHide`, `launchLottie`
   * and `launchHaptic`.
   *
   * Mirrors the official splash-screen plugin's launch behavior: the
   * platform (Android 12 SplashScreen compat API) holds the
   * system-rendered launch theme splash on screen until [hide] is called
   * (or until `launchShowDuration` elapses when `launchAutoHide` is on).
   * No dialog surface is drawn on top of the system splash.
   *
   * When the platform API is unavailable or fails, falls back to the
   * dialog-based rendering path below.
   *
   * Called from the plugin load phase.
   *
   * @param activity Host activity or null (the call is then a no-op).
   */
  fun showLaunchSplash(activity: Activity?) {
    if (activity == null) {
      SplashMotionLogger.warn("launch splash skipped: no host activity")
      return
    }

    try {
      val system = activity.installSplashScreen()
      systemSplash = system
      launchSplashVisible = true
      // With launchAutoHide off, launchShowDuration acts as the minimum time
      // the splash stays visible (see hide()); measured from this request.
      launchShownAt = SystemClock.elapsedRealtime()

      system.setKeepOnScreenCondition { launchSplashVisible || launchSplashHiding }

      system.setOnExitAnimationListener { view ->
        // The system starting window sits above every app window (plugin
        // dialogs included), so an animated launch lottie plays underneath
        // while the WebView paints its first frame and is never seen.
        // Restart it now: this is the first moment the animated surface is
        // actually visible to the user.
        restartLaunchLottie()
        val fadeOutDuration = SplashMotionValidators.clampDuration(config.launchFadeOutDuration, 0L)
        launchSplashVisible = false
        if (fadeOutDuration > 0) {
          launchSplashHiding = true
          view.view
            .animate()
            .alpha(0f)
            .setDuration(fadeOutDuration)
            .withEndAction {
              view.remove()
              launchSplashHiding = false
              systemSplash = null
            }.start()
        } else {
          view.remove()
          launchSplashHiding = false
          systemSplash = null
        }
      }

      if (config.launchAutoHide) {
        val showDuration = SplashMotionValidators.clampDuration(config.launchShowDuration, 0L)
        mainHandler.postDelayed({
          releaseLaunchSplash(activity, config.launchFadeOutDuration, config.launchHaptic)
        }, showDuration)
      }

      if (config.launchLottie != null) {
        // Animated launch parity: present the plugin splash dialog (the
        // configured launchLottie) above the held system cover and release
        // the system splash once the dialog draws its first frame. The
        // system splash only bridges process start -> plugin load; from
        // then on the animated surface is what the user watches (iOS
        // renders the same lottie natively).
        presentLaunchSurface(activity)
      }

      // Parity with the official plugin: block the app content's drawing
      // while the system launch splash is held. The WebView must not paint
      // behind the splash; when the splash is released, its exit animation
      // (launchFadeOutDuration) fades over freshly painted content instead
      // of a blank or stale frame (this is what causes perceived flicker).
      val content = activity.findViewById<View>(android.R.id.content)
      launchContentView = content
      val blocker =
        object : ViewTreeObserver.OnPreDrawListener {
          override fun onPreDraw(): Boolean {
            if (!launchSplashVisible && !launchSplashHiding) {
              content.viewTreeObserver.removeOnPreDrawListener(this)
              launchPreDrawListener = null
              launchContentView = null
              return true
            }
            return false
          }
        }
      launchPreDrawListener = blocker
      content.viewTreeObserver.addOnPreDrawListener(blocker)

      SplashMotionLogger.debug("Showing system launch splash. Duration:", config.launchShowDuration.toString())
    } catch (error: Exception) {
      SplashMotionLogger.warn("System splash API failed; falling back to dialog splash")
      launchSplashVisible = false
      systemSplash = null

      val options =
        ShowOptionsData(
          autoHide = config.launchAutoHide,
          fadeInDuration = 0,
          fadeOutDuration = config.launchFadeOutDuration,
          showDuration = config.launchShowDuration,
          lottie = config.launchLottie,
          haptic = config.launchHaptic,
        )

      SplashMotionLogger.debug("Showing launch splash. Duration:", config.launchShowDuration.toString())
      show(activity, options)
    }
  }

  // ---------------------------------------------------------------------------
  // Native mechanics
  // ---------------------------------------------------------------------------

  /**
   * Releases the system launch splash (official parity): flips the
   * keep-on-screen condition (which fires the exit animation with
   * `launchFadeOutDuration`) and unblocks the app content drawing so the
   * WebView paints beneath the exit fade instead of flickering in later.
   */
  private fun releaseSystemLaunchSplash() {
    if (!launchSplashVisible) {
      return
    }
    SplashMotionLogger.debug("Releasing system launch splash")
    launchSplashVisible = false
    val listener = launchPreDrawListener
    val content = launchContentView
    if (listener != null && content != null) {
      content.viewTreeObserver.removeOnPreDrawListener(listener)
    }
    launchPreDrawListener = null
    launchContentView = null

    // Cross-fade: while the splash exit animation fades the splash out
    // (launchFadeOutDuration), fade the app content in over the same
    // duration. Without this, the content appears abruptly beneath the
    // fading splash and the transition feels hard.
    val fadeDuration = SplashMotionValidators.clampDuration(config.launchFadeOutDuration, 0L)
    if (fadeDuration > 0 && content != null) {
      content.alpha = 0f
      content
        .animate()
        .alpha(1f)
        .setDuration(fadeDuration)
        .start()
    }
  }

  private fun presentSplash(
    activity: Activity,
    options: ShowOptionsData,
  ) {
    val fadeInDuration = SplashMotionValidators.clampDuration(options.fadeInDuration, DEFAULT_FADE_DURATION)
    val isDark = isDarkMode(activity)

    val root =
      FrameLayout(activity).apply {
        applySplashBackground(this, options, isDark)
      }

    val dialog =
      Dialog(activity).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(
          root,
          FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          ),
        )
        window?.let { configureWindow(it) }
        setCancelable(false)
      }

    this.dialog = dialog
    this.rootView = root

    renderContent(activity, root, options, isDark)

    // Start transparent and fade in, then schedule auto-hide.
    root.alpha = 0f
    dialog.show()
    // Re-apply the system-bars policy once the window is attached: insets
    // requests issued before attachment are silently dropped, which left
    // the status bar visible during the animated launch splash.
    dialog.window?.let { applySystemBarsPolicy(it) }
    root
      .animate()
      .alpha(1f)
      .setDuration(fadeInDuration)
      .start()

    fireHaptic(options.haptic)
    scheduleAutoHide(activity, options)
    SplashMotionLogger.debug("Splash shown")
  }

  /**
   * Applies the splash surface background: a vertical gradient when
   * configured and valid, otherwise the flat background color (with dark
   * mode support).
   */
  private fun applySplashBackground(
    root: FrameLayout,
    options: ShowOptionsData,
    isDark: Boolean,
  ) {
    val gradient = options.backgroundGradient ?: config.backgroundGradient
    if (gradient != null) {
      val colors =
        SplashMotionValidators.resolveGradient(
          gradient.top,
          gradient.bottom,
          gradient.topDark,
          gradient.bottomDark,
          isDark,
        )
      if (colors != null) {
        root.background =
          GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(colors.first, colors.second))
        return
      }
      SplashMotionLogger.debug("Invalid background gradient colors; falling back to flat background")
    }

    val backgroundColor =
      SplashMotionValidators.resolveSplashColor(
        config.backgroundColor,
        config.backgroundColorDark,
        isDark,
      )
        ?: DEFAULT_BACKGROUND_COLOR
    root.setBackgroundColor(backgroundColor)
  }

  private fun renderContent(
    activity: Activity,
    root: FrameLayout,
    options: ShowOptionsData,
    isDark: Boolean,
  ) {
    // All content layers live inside a single transparent container so the
    // entrance animation covers them together, above the background.
    val container =
      FrameLayout(activity).apply {
        layoutParams =
          FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
      }
    root.addView(container)

    renderImageLayer(
      activity,
      container,
      options.backgroundImage ?: config.backgroundImage,
      isDark,
      fillsSurface = true,
      defaultGravity = null,
      defaultMarginDp = 0.0,
    )

    val lottie = options.lottie
    if (lottie != null) {
      renderLottie(activity, container, lottie)
    } else {
      renderStaticImage(activity, container, options.image, isDark)
    }

    renderImageLayer(
      activity,
      container,
      options.brandingImage ?: config.brandingImage,
      isDark,
      fillsSurface = false,
      defaultGravity = DEFAULT_BRANDING_GRAVITY,
      defaultMarginDp = DEFAULT_BRANDING_MARGIN_DP,
    )

    renderSpinner(activity, container, options)

    applyEntrance(container, options.entrance ?: config.entrance)
  }

  /**
   * Renders the loading spinner as the topmost centered layer of the
   * content container, mirroring the official plugin behavior (indeterminate
   * ProgressBar, official style mapping, optional hex tint). It lives inside
   * the container so the entrance animation covers it, and it disappears
   * with the surface on hide.
   */
  private fun renderSpinner(
    activity: Activity,
    container: FrameLayout,
    options: ShowOptionsData,
  ) {
    val perCall = options.spinner
    val show: Boolean
    val styleName: String?
    val colorHex: String?
    if (perCall != null) {
      // Per-call convention: the object replaces the whole static spinner
      // configuration; passing it implies the spinner is shown unless
      // `show` is explicitly false.
      show = perCall.show ?: true
      styleName = perCall.style
      colorHex = perCall.color
    } else {
      show = config.showSpinner
      styleName = config.androidSpinnerStyle
      colorHex = config.spinnerColor
    }
    if (!show) {
      return
    }

    val color = SplashMotionValidators.parseColor(colorHex)
    val styleAttr = SplashMotionValidators.parseSpinnerStyleAttr(styleName)
    val spinner =
      ProgressBar(activity, null, styleAttr).apply {
        isIndeterminate = true
        layoutParams =
          FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
          )
        color?.let { indeterminateTintList = ColorStateList.valueOf(it) }
      }

    container.addView(spinner)
    SplashMotionLogger.debug("Spinner rendered")
  }

  /**
   * Renders one image layer inside the content container.
   *
   * When [fillsSurface] is true the view fills the whole container and
   * gravity/margin are ignored (background layer and legacy static image).
   * Otherwise the view wraps its bitmap and is positioned by gravity.
   */
  private fun renderImageLayer(
    activity: Activity,
    container: FrameLayout,
    image: ImageOptionsData?,
    isDark: Boolean,
    fillsSurface: Boolean,
    defaultGravity: String?,
    defaultMarginDp: Double,
  ) {
    if (image == null) {
      return
    }

    val source = if (isDark) image.darkSource ?: image.source else image.source
    val drawable = resolveImageDrawable(activity, source)
    if (drawable == null) {
      SplashMotionLogger.debug("Splash image resource not found:", source)
      return
    }

    val imageView =
      ImageView(activity).apply {
        scaleType = SplashMotionValidators.parseScaleType(image.scaleType ?: "CENTER_CROP")
        alpha = SplashMotionValidators.normalizeAlpha(image.alpha)
        setImageDrawable(drawable)
      }

    imageView.layoutParams =
      if (fillsSurface) {
        FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      } else {
        val params =
          FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            SplashMotionValidators.parseGravity(image.gravity ?: defaultGravity),
          )
        val marginDp = SplashMotionValidators.normalizeMargin(image.margin, defaultMarginDp)
        val marginPx = (marginDp * activity.resources.displayMetrics.density).toInt()
        params.setMargins(marginPx, marginPx, marginPx, marginPx)
        params
      }

    container.addView(imageView)
  }

  /**
   * Resolves a splash image source into a drawable: a value containing "/"
   * is decoded from the web asset tree, a plain name is first looked up as
   * a drawable resource and then as a bundle asset.
   */
  private fun resolveImageDrawable(
    activity: Activity,
    source: String,
  ): Drawable? {
    if (source.contains('/')) {
      return decodeAssetImage(activity, resolveAssetPath(source))
    }

    val resourceId =
      activity.resources.getIdentifier(source, "drawable", activity.packageName)
    if (resourceId != 0) {
      return ResourcesCompat.getDrawable(activity.resources, resourceId, activity.theme)
    }
    return decodeAssetImage(activity, resolveAssetPath(source))
  }

  private fun decodeAssetImage(
    activity: Activity,
    assetPath: String,
  ): Drawable? =
    try {
      BitmapFactory.decodeStream(activity.assets.open(assetPath))?.let { bitmap ->
        BitmapDrawable(activity.resources, bitmap)
      }
    } catch (error: Exception) {
      null
    }

  /**
   * Renders the static splash image, either the per-call override or the
   * static configuration (with dark-mode resource selection).
   */
  private fun renderStaticImage(
    activity: Activity,
    container: FrameLayout,
    override: ImageOptionsData?,
    isDark: Boolean,
  ) {
    // Legacy behavior (no override, no imageGravity): the image view fills
    // the surface and androidScaleType scales the bitmap, unchanged from
    // the official plugin (resolved config defaults to FIT_XY). With a
    // gravity, the view wraps its bitmap, where the scale type has no
    // visual effect.
    val fillsSurface = override == null && config.imageGravity == null
    val image =
      override
        ?: ImageOptionsData(
          source =
            if (isDark) {
              config.imageResourceNameDark ?: config.imageResourceName
            } else {
              config.imageResourceName
            },
          gravity = config.imageGravity,
          scaleType = config.androidScaleType,
        )

    renderImageLayer(
      activity,
      container,
      image,
      isDark,
      fillsSurface = fillsSurface,
      defaultGravity = DEFAULT_CONTENT_GRAVITY,
      defaultMarginDp = 0.0,
    )
  }

  /**
   * Applies the content entrance animation (fade or scale) to the whole
   * content container.
   */
  private fun applyEntrance(
    container: FrameLayout,
    entrance: EntranceOptionsData?,
  ) {
    val type = SplashMotionValidators.normalizeEntranceType(entrance?.type)
    if (type == "none") {
      return
    }

    val duration = SplashMotionValidators.clampEntranceDuration(entrance?.durationMs)
    when (type) {
      "fade" -> {
        container.alpha = 0f
        container
          .animate()
          .alpha(1f)
          .setDuration(duration)
          .start()
      }
      else -> {
        container.scaleX = 0.85f
        container.scaleY = 0.85f
        container.alpha = 0f
        container
          .animate()
          .scaleX(1f)
          .scaleY(1f)
          .alpha(1f)
          .setDuration(duration)
          .start()
      }
    }
    SplashMotionLogger.debug("Entrance animation applied:", type)
  }

  private fun renderLottie(
    activity: Activity,
    root: FrameLayout,
    lottie: io.capkit.splashmotion.model.LottieOptionsData,
  ) {
    val animationView =
      LottieAnimationView(activity).apply {
        tag = LOTTIE_VIEW_TAG
        // Full-surface layout: a full-canvas splash composition (e.g. 375x748
        // with a baked background) must cover the whole screen. The
        // composition scales with fit-center, so devices whose screen aspect
        // differs from the composition letterbox; configure
        // backgroundColor/backgroundColorDark to the composition's baked
        // background color to make those bands invisible.
        layoutParams =
          FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER,
          )
        scaleType = ImageView.ScaleType.FIT_CENTER
        val speed = (lottie.speed ?: DEFAULT_LOTTIE_SPEED).coerceIn(0.1, 10.0)
        setSpeed(speed.toFloat())
      }

    // Attach the animation view to the splash container. Without this the
    // view is orphaned (composition loads and plays off-hierarchy) and the
    // splash renders only its background.
    root.addView(animationView)

    // Dark mode picks the darkSource variant when provided.
    val source =
      if (isDarkMode(activity)) {
        lottie.darkSource ?: lottie.source
      } else {
        lottie.source
      }

    if (source.endsWith(DOT_LOTTIE_EXTENSION)) {
      renderDotLottie(activity, animationView, source, lottie)
      return
    }

    if (source.startsWith("http://") || source.startsWith("https://")) {
      LottieCompositionFactory.fromUrl(activity, source).addListener { composition ->
        applyComposition(animationView, composition, lottie)
      }
      SplashMotionLogger.debug("Lottie loading from URL")
    } else {
      val assetPath = resolveAssetPath(source)
      LottieCompositionFactory.fromAsset(activity, assetPath).addListener { composition ->
        applyComposition(animationView, composition, lottie)
      }
      SplashMotionLogger.debug("Lottie loading from asset:", assetPath)
    }
  }

  /**
   * Renders a dotLottie (.lottie) archive: a zip container whose selected
   * animation JSON is extracted first and then parsed as a normal
   * composition. Local sources are read from assets; remote sources are
   * downloaded on a background thread (network access is forbidden on the
   * main thread) and the result is posted back.
   */
  private fun renderDotLottie(
    activity: Activity,
    animationView: LottieAnimationView,
    source: String,
    lottie: io.capkit.splashmotion.model.LottieOptionsData,
  ) {
    if (source.startsWith("http://") || source.startsWith("https://")) {
      SplashMotionLogger.debug("dotLottie loading from URL")
      Thread {
        try {
          val json = SplashMotionZipReader.firstAnimationJson(URL(source).openStream())
          mainHandler.post {
            if (json != null) {
              parseJsonComposition(animationView, json, source, lottie)
            } else {
              SplashMotionLogger.error("dotLottie archive contains no animation JSON")
            }
          }
        } catch (error: Exception) {
          mainHandler.post {
            SplashMotionLogger.error("Failed to load dotLottie from URL", error)
          }
        }
      }.start()
      return
    }

    try {
      val assetPath = resolveAssetPath(source)
      val json = SplashMotionZipReader.firstAnimationJson(activity.assets.open(assetPath))
      if (json != null) {
        parseJsonComposition(animationView, json, source, lottie)
        SplashMotionLogger.debug("dotLottie loading from asset:", assetPath)
      } else {
        SplashMotionLogger.error("dotLottie archive contains no animation JSON: $assetPath")
      }
    } catch (error: Exception) {
      SplashMotionLogger.error("Failed to load dotLottie from asset", error)
    }
  }

  private fun parseJsonComposition(
    animationView: LottieAnimationView,
    json: String,
    cacheKey: String,
    lottie: io.capkit.splashmotion.model.LottieOptionsData,
  ) {
    LottieCompositionFactory
      .fromJsonString(json, cacheKey)
      .addListener { composition ->
        applyComposition(animationView, composition, lottie)
      }.addFailureListener { error ->
        SplashMotionLogger.error("Failed to parse dotLottie animation", error as? Exception)
      }
  }

  private fun isDarkMode(activity: Activity): Boolean {
    val nightMode =
      activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
  }

  private fun applyComposition(
    animationView: LottieAnimationView,
    composition: LottieComposition,
    lottie: io.capkit.splashmotion.model.LottieOptionsData,
  ) {
    val loop = lottie.loop ?: DEFAULT_LOTTIE_LOOP
    // Launch clamp: record the launch lottie's real duration so the launch
    // minimum hold never releases the splash mid-animation. Only the launch
    // surface counts (an explicit show() during launch must not extend it).
    if (
      launchShownAt != 0L &&
      config.launchLottie != null &&
      isLaunchSurfaceDescendant(animationView)
    ) {
      launchLottieDurationMs = composition.duration.toLong()
      SplashMotionLogger.debug("Launch lottie duration:", launchLottieDurationMs.toString())
    }
    animationView.setComposition(composition)
    animationView.repeatCount = if (loop) LottieDrawable.INFINITE else 0
    animationView.playAnimation()
  }

  /** Whether [view] lives inside the launch splash surface. */
  private fun isLaunchSurfaceDescendant(view: View): Boolean {
    var parent: ViewParent? = view.parent
    while (parent != null) {
      if (parent === rootView) {
        return true
      }
      parent = parent.parent
    }
    return false
  }

  private fun configureWindow(window: Window) {
    window.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
    )
    window.setBackgroundDrawableResource(android.R.color.transparent)
    window.setGravity(Gravity.CENTER)
    applySystemBarsPolicy(window)
  }

  /**
   * Hides the system bars for the splash window according to the
   * splashFullScreen / splashImmersive configuration. Called when the
   * window is configured and again right after the dialog is shown:
   * insets requests issued before the window is attached to the window
   * manager are silently dropped, so the post-show pass guarantees the
   * hide request reaches the system.
   */
  private fun applySystemBarsPolicy(window: Window) {
    if (config.splashImmersive || config.splashFullScreen) {
      // Legacy fullscreen flag: honored directly pre-30 and translated
      // into a status-bar insets request on 30+; belt-and-braces next to
      // the insets-controller call below.
      @Suppress("DEPRECATION")
      window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
      )
      // Full-bleed window: a plain MATCH_PARENT dialog is laid out below
      // the status-bar inset area, so even with the bars hidden a dark
      // unpainted strip (status-bar area + display cutout) remains at the
      // top. NO_LIMITS lifts that boundary; SHORT_EDGES extends the
      // surface into the cutout.
      window.setFlags(
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes.layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    if (config.splashFullScreen) {
      controller.hide(WindowInsetsCompat.Type.statusBars())
    }
    if (config.splashImmersive) {
      controller.hide(WindowInsetsCompat.Type.systemBars())
      controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  /**
   * Hides the system bars on the host activity window for the launch
   * splash (see presentLaunchSurface). The activity window drives the
   * app's real system-UI visibility, so this takes effect immediately.
   */
  private fun hideSystemBars(window: Window) {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    if (config.splashImmersive) {
      controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    controller.hide(
      if (config.splashImmersive) {
        WindowInsetsCompat.Type.systemBars()
      } else {
        WindowInsetsCompat.Type.statusBars()
      },
    )
    SplashMotionLogger.debug("System bars hidden on the host activity window")
  }

  /**
   * Restores the system bars on the host activity window after the launch
   * splash is released; the app screens show them normally again.
   */
  private fun showSystemBars(window: Window) {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    controller.show(WindowInsetsCompat.Type.systemBars())
  }

  private fun dismissSplash(
    activity: Activity,
    fadeOutDuration: Long?,
    haptic: HapticOptionsData?,
  ) {
    val currentDialog = dialog ?: return
    val root = rootView ?: return
    val duration = SplashMotionValidators.clampDuration(fadeOutDuration, DEFAULT_FADE_DURATION)

    fireHaptic(haptic)
    root
      .animate()
      .alpha(0f)
      .setDuration(duration)
      .withEndAction {
        try {
          currentDialog.dismiss()
        } catch (error: Exception) {
          SplashMotionLogger.error("Failed to dismiss splash", error)
        }
        if (dialog === currentDialog) {
          dialog = null
          rootView = null
        }
      }.start()
    SplashMotionLogger.debug("Splash hiding")
  }

  private fun scheduleAutoHide(
    activity: Activity,
    options: ShowOptionsData,
  ) {
    cancelAutoHide()

    val autoHide = options.autoHide ?: DEFAULT_AUTO_HIDE
    if (!autoHide) {
      return
    }

    val showDuration = SplashMotionValidators.clampDuration(options.showDuration, DEFAULT_SHOW_DURATION)
    val runnable =
      Runnable {
        dismissSplash(activity, options.fadeOutDuration, null)
      }
    autoHideRunnable = runnable
    mainHandler.postDelayed(runnable, showDuration)
  }

  private fun cancelAutoHide() {
    autoHideRunnable?.let { mainHandler.removeCallbacks(it) }
    autoHideRunnable = null
  }

  // ---------------------------------------------------------------------------
  // Haptics
  // ---------------------------------------------------------------------------

  private fun fireHaptic(haptic: HapticOptionsData?) {
    if (haptic == null) {
      return
    }

    val hostContext = context ?: return
    val vibrator = resolveVibrator(hostContext) ?: return
    if (!vibrator.hasVibrator()) {
      return
    }

    val style = SplashMotionValidators.normalizeHapticStyle(haptic.style)
    val (durationMs, amplitude) =
      when (style) {
        "light" -> 20L to 64
        "heavy" -> 80L to 255
        else -> 40L to 128
      }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
      }
      SplashMotionLogger.debug("Haptic fired:", style)
    } catch (error: Exception) {
      SplashMotionLogger.error("Failed to fire haptic feedback", error)
    }
  }

  private fun resolveVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

  private companion object {
    const val DEFAULT_FADE_DURATION = 200L
    const val DEFAULT_SHOW_DURATION = 3000L
    const val DEFAULT_AUTO_HIDE = true
    const val DEFAULT_LOTTIE_SPEED = 1.0
    const val DEFAULT_LOTTIE_LOOP = true
    const val DEFAULT_BACKGROUND_COLOR = 0xFFFFFFFF.toInt()
    const val DEFAULT_CONTENT_GRAVITY = "center"
    const val DEFAULT_BRANDING_GRAVITY = "bottomEnd"
    const val DEFAULT_BRANDING_MARGIN_DP = 16.0
    const val DOT_LOTTIE_EXTENSION = ".lottie"
    const val LOTTIE_VIEW_TAG = "splash-motion-lottie"

    /**
     * Resolves a Lottie source path to a bundle asset path.
     * Capacitor serves web assets from `assets/public/`.
     */
    fun resolveAssetPath(source: String): String =
      if (source.startsWith("public/")) {
        source
      } else {
        "public/$source".removePrefix("//")
      }
  }
}
