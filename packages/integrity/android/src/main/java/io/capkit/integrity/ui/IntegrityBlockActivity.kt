package io.capkit.integrity.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Dedicated activity used to present the integrity block page.
 *
 * Responsibilities:
 * - Display a developer-provided HTML block page
 * - Support query parameters (e.g. "reason")
 * - Optionally allow dismissal via native UI controls
 *
 * Security note:
 * - The block page is NOT dismissible by default
 * - Dismissal must be explicitly enabled by the host application
 */
class IntegrityBlockActivity : AppCompatActivity() {
  /**
   * Whether the block page can be dismissed by the user.
   *
   * Defaults to false (secure-by-default).
   */
  private var dismissible: Boolean = false

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // -------------------------------------------------------------------------
    // Read options from Intent
    // -------------------------------------------------------------------------

    dismissible = intent.getBooleanExtra("dismissible", false)

    // -------------------------------------------------------------------------
    // Back button handling (modern API)
    // -------------------------------------------------------------------------

    if (!dismissible) {
      onBackPressedDispatcher.addCallback(
        this,
        object : androidx.activity.OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            // Intentionally disabled (secure-by-default)
          }
        },
      )
    }

    // -------------------------------------------------------------------------
    // Root layout (vertical)
    // -------------------------------------------------------------------------

    val root =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams =
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
          )
      }

    // -------------------------------------------------------------------------
    // Optional native toolbar (dismissible only)
    // -------------------------------------------------------------------------

    if (dismissible) {
      val toolbar =
        Toolbar(this).apply {
          setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
          setNavigationOnClickListener { finish() }
          title = ""
        }

      root.addView(toolbar)
    }

    // -------------------------------------------------------------------------
    // WebView setup
    // -------------------------------------------------------------------------

    val webView =
      WebView(this).apply {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = false
        settings.domStorageEnabled = false
      }

    // Fill remaining space
    val webViewParams =
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f,
      )
    root.addView(webView, webViewParams)

    setContentView(root)

    // -------------------------------------------------------------------------
    // URL handling
    // -------------------------------------------------------------------------

    // The plugin always passes the URL explicitly via Intent extras
    val url = intent.getStringExtra("url") ?: return

    // Remote URLs (http / https) are loaded directly
    if (url.startsWith("http")) {
      webView.loadUrl(url)
      return
    }

    // -------------------------------------------------------------------------
    // Local asset loading with query support
    // -------------------------------------------------------------------------

    // Example:
    //   url = "public/integrity-block.html?reason=integrity_failed"
    val assetPath = url.substringBefore("?")
    val query = url.substringAfter("?", "")

    // Read the HTML asset manually
    val html = readAsset(assetPath)

    // Use a synthetic base URL so that:
    // - window.location.search is populated
    // - relative paths continue to work
    webView.loadDataWithBaseURL(
      "file:///android_asset/$assetPath?$query",
      html,
      "text/html",
      "UTF-8",
      null,
    )

    // -------------------------------------------------------------------------
    // Back navigation blocking
    // -------------------------------------------------------------------------

    // Disable back gesture on Android 13+ (API 33+)
    if (!dismissible && Build.VERSION.SDK_INT >= 33) {
      onBackInvokedDispatcher.registerOnBackInvokedCallback(
        android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
      ) {
        // Intentionally disabled
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Utilities
  // ---------------------------------------------------------------------------

  /**
   * Reads a file from the android_asset directory.
   *
   * @param path Relative asset path (e.g. "public/integrity-block.html")
   */
  private fun readAsset(path: String): String {
    val inputStream = assets.open(path)
    val reader = BufferedReader(InputStreamReader(inputStream))
    return reader.use { it.readText() }
  }
}
