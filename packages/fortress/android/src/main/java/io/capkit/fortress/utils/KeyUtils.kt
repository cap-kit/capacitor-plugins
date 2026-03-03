package io.capkit.fortress.utils

object KeyUtils {
  fun obfuscate(
    key: String,
    prefix: String,
  ): String = "${prefix}$key"
}
