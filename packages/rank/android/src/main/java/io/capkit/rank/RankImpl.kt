package io.capkit.rank

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import io.capkit.rank.utils.RankLogger

/**
 * Platform-specific native implementation for the Rank plugin.
 *
 * This class contains pure Android logic and MUST NOT depend directly on
 * Capacitor bridge APIs or PluginCall objects.
 *
 * Responsibilities:
 * - Orchestrating the Google Play Review SDK.
 * - Managing Intent-based Store navigation.
 * - Translating configuration into native behavior.
 */
class RankImpl(
  private val context: Context,
) {
  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: RankConfig

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   *
   * This method MUST be called exactly once during the plugin [RankPlugin.load]
   * phase. It initializes internal state and configures logging verbosity.
   *
   * @param newConfig The immutable configuration instance.
   */
  fun updateConfig(newConfig: RankConfig) {
    this.config = newConfig
    RankLogger.verbose = newConfig.verboseLogging
    RankLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  // ---------------------------------------------------------------------------
  // Availability check
  // ---------------------------------------------------------------------------

  /**
   * Checks whether the In-App Review feature is available on this device.
   *
   * This is a semantic availability check intended for UI decisions.
   * Internally, it delegates to the diagnostic environment check.
   */
  fun isAvailable(onResult: (Boolean) -> Unit) {
    checkReviewEnvironment { canRequest ->
      onResult(canRequest)
    }
  }

  /**
   * Performs a diagnostic check for the Google Play Review environment.
   *
   * This verifies whether Google Play Services can provide
   * a ReviewInfo instance for the current application and device.
   *
   * No UI is triggered by this operation.
   */
  fun checkReviewEnvironment(onResult: (Boolean) -> Unit) {
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()

    request.addOnCompleteListener { task ->
      onResult(task.isSuccessful)
    }
  }

  // ---------------------------------------------------------------------------
  // Pre-warm Logic
  // ---------------------------------------------------------------------------

  /**
   * Cached ReviewInfo object to speed up the review flow display.
   */
  private var cachedReviewInfo: com.google.android.play.core.review.ReviewInfo? = null

  /**
   * Pre-fetches the ReviewInfo from Google Play Services.
   * This should be called early (e.g., during plugin load).
   */
  fun preloadReviewInfo() {
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
      if (task.isSuccessful) {
        cachedReviewInfo = task.result
        RankLogger.debug("ReviewInfo pre-loaded successfully.")
      } else {
        RankLogger.debug("ReviewInfo pre-load failed.")
      }
    }
  }

  // ---------------------------------------------------------------------------
  // In-App Review Logic
  // ---------------------------------------------------------------------------

  /**
   * Triggers the Google Play In-App Review flow.
   * Uses the cached ReviewInfo if available to ensure an immediate prompt.
   *
   * @param activity The current Android Activity required to display the UI.
   * @param onComplete Callback invoked when the flow completes or fails.
   */
  fun requestReview(
    activity: Activity,
    onComplete: (Exception?) -> Unit,
  ) {
    val manager = ReviewManagerFactory.create(context)

    // Use cache if available, otherwise fetch new info on the fly
    if (cachedReviewInfo != null) {
      val flow = manager.launchReviewFlow(activity, cachedReviewInfo!!)
      flow.addOnCompleteListener { _ ->
        cachedReviewInfo = null // Clear cache after use to prevent reuse of expired info
        onComplete(null)
      }
    } else {
      val request = manager.requestReviewFlow()
      request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val reviewInfo = task.result
          val flow = manager.launchReviewFlow(activity, reviewInfo)
          flow.addOnCompleteListener { _ -> onComplete(null) }
        } else {
          onComplete(task.exception)
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Store Navigation Logic
  // ---------------------------------------------------------------------------

  /**
   * Opens the Google Play Store page for the specified package.
   *
   * It attempts to use the "market://" URI scheme first to open the native
   * Play Store app. If the app is missing, it falls back to a browser-based
   * "https://" URL.
   *
   * @param packageName The application package name. Defaults to the host app if null.
   */
  fun openStore(packageName: String?) {
    val targetPackage = packageName ?: context.packageName

    // Construct the native market Intent
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPackage")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      // Fallback to web browser if Play Store application is unavailable
      val webIntent =
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://play.google.com/store/apps/details?id=$targetPackage"),
        ).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(webIntent)
    }
  }

  /**
   * Opens the Google Play Store listing for a specific application ID.
   *
   * @param appId The Android package name or application ID to display.
   */
  fun openStoreListing(appId: String) {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  /**
   * Launches the Google Play Store search results for the provided terms.
   *
   * @param terms The search query string.
   */
  fun search(terms: String) {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$terms")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  /**
   * Navigates to a specific developer's page on the Google Play Store.
   *
   * @param devId The unique developer identifier (numeric or string ID).
   */
  fun openDevPage(devId: String) {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=$devId")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  /**
   * Opens a curated collection or category on the Google Play Store.
   *
   * @param name The collection identifier (e.g., "featured", "editors_choice").
   */
  fun openCollection(name: String) {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/collection/$name")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }
}
