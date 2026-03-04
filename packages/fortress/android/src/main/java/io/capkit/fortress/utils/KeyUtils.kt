package io.capkit.fortress.utils

object KeyUtils {
  /**
   * Applies both the global prefix and the obfuscation prefix.
   */
  fun obfuscate(
    key: String,
    obfuscationPrefix: String,
    globalPrefix: String = "",
  ): String = "${globalPrefix}${obfuscationPrefix}$key"

  /**
   * Applies only the global prefix for secure storage keys.
   */
  fun formatSecureKey(
    key: String,
    globalPrefix: String = "",
  ): String = "${globalPrefix}$key"
}
