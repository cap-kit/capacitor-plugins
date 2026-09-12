package io.capkit.authentication.apple

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * @file AppleSignInActivity.kt
 * WebView-based Apple OAuth sign-in.
 *
 * Loads the Apple authorization URL in a sandboxed WebView, enforces the
 * Apple-host allowlist on every navigation, strips the Capacitor `; wv`
 * user-agent token, intercepts the OAuth `form_post` callback through a
 * JavaScript bridge before the auto-submitting form navigates the WebView, and
 * clears session cookies on finish.
 *
 * Security contract:
 * - Every navigation host is checked against [AppleHostAllowlist]; an
 *   unexpected host cancels the flow (→ `USER_CANCELLED`) without loading it.
 * - Form-post values are URL-decoded by [FormPostValueDecoder] BEFORE the
 *   payload is handed to [FormPostParser]: Apple percent-encodes every
 *   value, including the stringified `user` JSON.
 * - State/nonce validation happens in the pure [AppleSignInResultMapper] at the
 *   plugin layer; this Activity echoes the expected values back in its result.
 *
 * Result contract (consumed by the plugin `@ActivityCallback`):
 * - `RESULT_OK`: `EXTRA_PAYLOAD` (decoded form-post JSON) +
 *   `EXTRA_EXPECTED_STATE` + `EXTRA_EXPECTED_NONCE`.
 * - `RESULT_CANCELED`: optionally `EXTRA_ERROR_CODE` + `EXTRA_ERROR_MESSAGE`
 *   (raw SDK error surfaced by the WebView), or none for a plain cancellation.
 */
class AppleSignInActivity : Activity() {
  private lateinit var webView: WebView
  private var expectedState: String? = null
  private var expectedNonce: String? = null
  private var redirectUri: String? = null
  private var finished = false

  // ---------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    expectedState = intent.getStringExtra(EXTRA_EXPECTED_STATE)
    expectedNonce = intent.getStringExtra(EXTRA_EXPECTED_NONCE)
    redirectUri = intent.getStringExtra(EXTRA_REDIRECT_URI)

    webView =
      WebView(this).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.userAgentString =
          settings.userAgentString
            ?.replace("; wv", "")
            ?.trim()
        addJavascriptInterface(FormBridge(), BRIDGE_NAME)
        webViewClient = AppleWebViewClient()
      }

    CookieManager.getInstance().setAcceptCookie(true)
    setContentView(webView)

    intent.getStringExtra(EXTRA_AUTHORIZE_URL)?.let { webView.loadUrl(it) }
      ?: finishCanceled(sdkErrorCode = null, sdkErrorMessage = "Missing authorize URL.")
  }

  /**
   * Handles the system back button by cancelling the flow.
   *
   * The override is deprecated on API 33+ in favor of predictive-back callbacks,
   * but the plugin targets API 24+, so the classic override is the compatible
   * path (back always cancels the Apple flow with a USER_CANCELLED outcome).
   */
  @Suppress("DEPRECATION")
  override fun onBackPressed() {
    finishCanceled(sdkErrorCode = null, sdkErrorMessage = null)
  }

  override fun onDestroy() {
    clearCookies()
    super.onDestroy()
  }

  // ---------------------------------------------------------------------------
  // WebView client
  // ---------------------------------------------------------------------------

  /**
   * Enforces the Apple-host allowlist on every navigation and blocks navigation
   * to the redirect URI — the form_post data is captured by the JS bridge, this
   * is the security backstop.
   */
  private inner class AppleWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(
      view: WebView,
      request: WebResourceRequest,
    ): Boolean {
      val url = request.url
      val host = url.host
      val isRedirectUri = redirectUri?.let { url.toString().startsWith(it) } == true

      if (!isRedirectUri && !AppleHostAllowlist.isAllowed(host)) {
        finishCanceled(
          sdkErrorCode = SDK_ERROR_UNEXPECTED_HOST,
          sdkErrorMessage = "Navigation to unexpected host blocked: $host",
        )
        return true
      }
      if (isRedirectUri) {
        // The form_post data is captured before navigation via the JS bridge;
        // never let the WebView reach the redirect target.
        return true
      }
      return false
    }

    override fun onPageStarted(
      view: WebView,
      url: String?,
      favicon: Bitmap?,
    ) {
      super.onPageStarted(view, url, favicon)
      view.evaluateJavascript(FORM_CAPTURE_SCRIPT, null)
    }
  }

  // ---------------------------------------------------------------------------
  // JavaScript bridge
  // ---------------------------------------------------------------------------

  /**
   * Receives the serialized `form_post` body captured by the injected script
   * before Apple's auto-submitting form navigates the WebView.
   */
  private inner class FormBridge {
    @JavascriptInterface
    fun onFormData(rawBody: String) {
      val decoded = FormPostValueDecoder.decodeFormBody(rawBody)
      val payload = FormPostValueDecoder.toFormPostJson(decoded)
      runOnUiThread {
        if (payload != null) {
          finishSuccess(payload)
        } else {
          finishCanceled(
            sdkErrorCode = SDK_ERROR_INVALID_PAYLOAD,
            sdkErrorMessage = "The Apple form_post payload could not be decoded.",
          )
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Result helpers
  // ---------------------------------------------------------------------------

  /** Completes the flow successfully, carrying the decoded form-post JSON. */
  private fun finishSuccess(payload: String) {
    if (finished) return
    finished = true
    clearCookies()
    val data =
      Intent().apply {
        putExtra(EXTRA_PAYLOAD, payload)
        putExtra(EXTRA_EXPECTED_STATE, expectedState)
        putExtra(EXTRA_EXPECTED_NONCE, expectedNonce)
      }
    setResult(Activity.RESULT_OK, data)
    finish()
  }

  /** Completes the flow as cancelled/failed, clearing session cookies. */
  private fun finishCanceled(
    sdkErrorCode: String?,
    sdkErrorMessage: String?,
  ) {
    if (finished) return
    finished = true
    clearCookies()
    val data =
      Intent().apply {
        sdkErrorCode?.let { putExtra(EXTRA_ERROR_CODE, it) }
        sdkErrorMessage?.let { putExtra(EXTRA_ERROR_MESSAGE, it) }
      }
    setResult(Activity.RESULT_CANCELED, data)
    finish()
  }

  private fun clearCookies() {
    runCatching { CookieManager.getInstance().removeAllCookies(null) }
  }

  // ---------------------------------------------------------------------------
  // Intent factory
  // ---------------------------------------------------------------------------

  companion object {
    /** JS bridge object name injected into the sign-in WebView pages. */
    const val BRIDGE_NAME = "AndroidBridge"

    // Launch extras
    const val EXTRA_AUTHORIZE_URL = "io.capkit.authentication.EXTRA_AUTHORIZE_URL"
    const val EXTRA_REDIRECT_URI = "io.capkit.authentication.EXTRA_REDIRECT_URI"
    const val EXTRA_EXPECTED_STATE = "io.capkit.authentication.EXTRA_EXPECTED_STATE"
    const val EXTRA_EXPECTED_NONCE = "io.capkit.authentication.EXTRA_EXPECTED_NONCE"

    // Result extras
    const val EXTRA_PAYLOAD = "io.capkit.authentication.EXTRA_PAYLOAD"
    const val EXTRA_ERROR_CODE = "io.capkit.authentication.EXTRA_ERROR_CODE"
    const val EXTRA_ERROR_MESSAGE = "io.capkit.authentication.EXTRA_ERROR_MESSAGE"

    // Raw SDK error codes emitted by this Activity
    const val SDK_ERROR_UNEXPECTED_HOST = "unexpected_host"
    const val SDK_ERROR_INVALID_PAYLOAD = "invalid_payload"

    /**
     * Builds the launch intent for the Apple sign-in Activity.
     *
     * @param authorizeUrl the URL produced by [AppleAuthUrlBuilder.buildAuthUrl].
     * @param redirectUri  the registered redirect URI, prefix-matched by the
     *                     WebView client as the form_post landing target.
     * @param expectedState the OAuth `state` issued in the authorization URL.
     * @param expectedNonce the OIDC `nonce` issued in the authorization URL.
     */
    fun newIntent(
      context: Context,
      authorizeUrl: String,
      redirectUri: String,
      expectedState: String,
      expectedNonce: String?,
    ): Intent =
      Intent(context, AppleSignInActivity::class.java).apply {
        putExtra(EXTRA_AUTHORIZE_URL, authorizeUrl)
        putExtra(EXTRA_REDIRECT_URI, redirectUri)
        putExtra(EXTRA_EXPECTED_STATE, expectedState)
        putExtra(EXTRA_EXPECTED_NONCE, expectedNonce)
      }

    /**
     * Injected on every `onPageStarted`: installs a capture wrapper (once per
     * page) that serializes the form_post fields — percent-encoding each value,
     * exactly like a real form submission — and hands them to the native bridge
     * BEFORE the auto-submitting form navigates the WebView.
     */
    private const val FORM_CAPTURE_SCRIPT =
      """
      (function () {
        if (window.__capkitAppleFormCaptureInstalled) { return; }
        window.__capkitAppleFormCaptureInstalled = true;

        var serialize = function (form) {
          var pairs = [];
          for (var i = 0; i < form.elements.length; i++) {
            var el = form.elements[i];
            if (!el.name) { continue; }
            pairs.push(encodeURIComponent(el.name) + '=' + encodeURIComponent(el.value || ''));
          }
          return pairs.join('&');
        };

        var capture = function (form) {
          if (window.AndroidBridge) {
            window.AndroidBridge.onFormData(serialize(form));
          }
        };

        var originalSubmit = HTMLFormElement.prototype.submit;
        HTMLFormElement.prototype.submit = function () {
          capture(this);
          // Intentionally do NOT call originalSubmit: the native bridge ends the
          // Activity with the captured payload; the WebView must not navigate.
        };

        document.addEventListener('submit', function (event) {
          capture(event.target);
          event.preventDefault();
          event.stopPropagation();
        }, true);
      })();
      """
  }
}
