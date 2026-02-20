package io.capkit.sslpinning.error

/**
 * Canonical error messages shared across platforms.
 * These strings must remain byte-identical on iOS and Android.
 */
object SSLPinningErrorMessages {
  const val URL_REQUIRED = "url is required"
  const val INVALID_URL_MUST_BE_HTTPS = "Invalid URL. Must be https."
  const val INVALID_URL = "Invalid URL."
  const val NO_FINGERPRINTS_PROVIDED = "No fingerprints provided"
  const val NO_CERTS_PROVIDED = "No certs provided"
  const val NO_HOST_FOUND_IN_URL = "No host found in URL"
  const val UNSUPPORTED_HOST = "Unsupported host: %s"
  const val PINNING_FAILED = "Pinning failed"
  const val EXCLUDED_DOMAIN = "Excluded domain"
  const val TIMEOUT = "Timeout"
  const val NETWORK_ERROR = "Network error"
  const val INTERNAL_ERROR = "Internal error"

  @JvmStatic
  fun unsupportedHost(value: String): String = String.format(UNSUPPORTED_HOST, value)
}
