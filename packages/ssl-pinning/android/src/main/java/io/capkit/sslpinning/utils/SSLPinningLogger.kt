package io.capkit.sslpinning.utils

import android.util.Log

/**
 * Centralized native logger for the SSLPinning plugin (Android).
 *
 * Responsibilities:
 * - Provide a single logging entry point
 * - Support runtime-controlled verbose logging
 * - Keep logging behavior consistent across files
 *
 * Forbidden:
 * - Controlling application logic
 * - Being queried for flow decisions
 */
object SSLPinningLogger {
  /**
   * Logcat tag used for all plugin logs.
   */
  private const val TAG = "⚡️ SSLPinning"

  /**
   * Controls whether debug logs are printed.
   *
   * This flag MUST be set once during plugin initialization
   * based on static configuration.
   */
  var verbose: Boolean = false

  /**
   * Prints a debug / verbose log message.
   *
   * Debug logs are automatically silenced
   * when `verbose` is false.
   */
  fun debug(vararg messages: String) {
    if (!verbose) return
    log(Log.DEBUG, *messages)
  }

  /**
   * Prints an error log message.
   *
   * Error logs are always printed regardless
   * of the verbose flag.
   */
  fun error(
    message: String,
    throwable: Throwable? = null,
  ) {
    if (throwable != null) {
      Log.e(TAG, message, throwable)
    } else {
      Log.e(TAG, message)
    }
  }

  /**
   * Internal low-level log dispatcher.
   */
  private fun log(
    level: Int,
    vararg messages: String,
  ) {
    val text =
      messages.joinToString(separator = " ")

    when (level) {
      Log.DEBUG -> Log.d(TAG, text)
      Log.INFO -> Log.i(TAG, text)
      Log.WARN -> Log.w(TAG, text)
      Log.ERROR -> Log.e(TAG, text)
      else -> Log.v(TAG, text)
    }
  }
}
