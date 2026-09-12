package io.capkit.authentication.apple

/**
 * @file AppleHostAllowlist.kt
 * Pure host allowlist consulted by the [AppleSignInActivity] WebView for every
 * navigation. Only Apple-owned authentication
 * hosts — and their subdomains — may load in the sign-in WebView; look-alike
 * domains (`appleid.apple.com.evil.com`) are rejected, and the WebView is
 * stopped for everything else.
 *
 * Matching is suffix-safe: a host is allowed iff it equals an allowlisted host
 * or ends with `.` + an allowlisted host, so `appleid.apple.com.evil.com` can
 * never pass. Pure and side-effect free.
 */
object AppleHostAllowlist {
  /** Apple-owned hosts participating in the sign-in/account flows. */
  private val ALLOWED_HOSTS =
    setOf(
      "appleid.apple.com",
      "idmsa.apple.com",
      "iforgot.apple.com",
    )

  /**
   * @param host the host part of a URL being loaded in the sign-in WebView.
   * @return true when the host is an allowlisted Apple host or one of its
   *         subdomains; false for null, blank, look-alike and unknown hosts.
   */
  fun isAllowed(host: String?): Boolean {
    if (host.isNullOrBlank()) return false
    val normalized = host.trim().lowercase()
    return ALLOWED_HOSTS.any { allowed -> normalized == allowed || normalized.endsWith(".$allowed") }
  }
}
