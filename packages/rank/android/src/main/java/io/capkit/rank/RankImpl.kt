package io.capkit.rank

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.android.play.core.review.ReviewManagerFactory
import io.capkit.rank.error.RankErrorMessages
import io.capkit.rank.utils.RankLogger
import io.capkit.rank.utils.RankUtils
import java.util.concurrent.atomic.AtomicBoolean

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
  data class ReviewEnvironmentDiagnostic(
    val canRequestReview: Boolean,
    val reason: String? = null,
    val error: RankError? = null,
  )

  // ---------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: RankConfig

  /**
   * Prevents concurrent review flow executions.
   */
  private val isReviewInProgress = AtomicBoolean(false)

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
    checkReviewEnvironment { diagnostic ->
      onResult(diagnostic.canRequestReview)
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
  fun checkReviewEnvironment(onResult: (ReviewEnvironmentDiagnostic) -> Unit) {
    val playStorePackage = "com.android.vending"
    val playStoreIntent =
      Intent(Intent.ACTION_VIEW, RankUtils.marketDetailsUri(context.packageName)).apply {
        setPackage(playStorePackage)
      }

    if (playStoreIntent.resolveActivity(context.packageManager) == null) {
      onResult(
        ReviewEnvironmentDiagnostic(
          canRequestReview = false,
          reason = "PLAY_STORE_NOT_AVAILABLE",
        ),
      )
      return
    }

    val installerPackage =
      try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
          context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getInstallerPackageName(context.packageName)
        }
      } catch (_: PackageManager.NameNotFoundException) {
        null
      } catch (_: Exception) {
        null
      }

    if (installerPackage != playStorePackage) {
      onResult(
        ReviewEnvironmentDiagnostic(
          canRequestReview = false,
          reason = "NOT_INSTALLED_FROM_PLAY_STORE",
        ),
      )
      return
    }

    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()

    request.addOnCompleteListener { task ->
      if (task.isSuccessful) {
        onResult(ReviewEnvironmentDiagnostic(canRequestReview = true))
      } else {
        onResult(
          ReviewEnvironmentDiagnostic(
            canRequestReview = false,
            error =
              RankError.Unavailable(
                task.exception?.message ?: RankErrorMessages.PLAY_CORE_REVIEW_API_UNAVAILABLE,
              ),
          ),
        )
      }
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
    if (!isReviewInProgress.compareAndSet(false, true)) {
      onComplete(IllegalStateException("Review flow already in progress"))
      return
    }

    val manager = ReviewManagerFactory.create(context)

    // Use cache if available, otherwise fetch new info on the fly
    if (cachedReviewInfo != null) {
      val flow = manager.launchReviewFlow(activity, cachedReviewInfo!!)
      flow.addOnCompleteListener { _ ->
        cachedReviewInfo = null // Clear cache after use to prevent reuse of expired info
        isReviewInProgress.set(false)
        onComplete(null)
      }
    } else {
      val request = manager.requestReviewFlow()
      request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val reviewInfo = task.result
          val flow = manager.launchReviewFlow(activity, reviewInfo)
          flow.addOnCompleteListener { _ ->
            isReviewInProgress.set(false)
            onComplete(null)
          }
        } else {
          isReviewInProgress.set(false)
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
      Intent(Intent.ACTION_VIEW, RankUtils.marketDetailsUri(targetPackage)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      // Fallback to web browser if Play Store application is unavailable
      val webIntent =
        Intent(
          Intent.ACTION_VIEW,
          RankUtils.playStoreDetailsHttpsUri(targetPackage),
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
      Intent(Intent.ACTION_VIEW, RankUtils.marketDetailsUri(appId)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      val webIntent =
        Intent(
          Intent.ACTION_VIEW,
          RankUtils.playStoreDetailsHttpsUri(appId),
        ).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(webIntent)
    }
  }

  /**
   * Launches the Google Play Store search results for the provided terms.
   *
   * @param terms The search query string.
   */
  fun search(terms: String) {
    val intent =
      Intent(Intent.ACTION_VIEW, RankUtils.marketSearchUri(terms)).apply {
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
      Intent(Intent.ACTION_VIEW, RankUtils.playStoreDeveloperUri(devId)).apply {
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
      Intent(Intent.ACTION_VIEW, RankUtils.playStoreCollectionUri(name)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }
}
