package io.capkit.tlsfingerprint.logger

import android.util.Log

/**
 * Centralized logging utility for the TLSFingerprint plugin.
 *
 * This logging provides a single entry point for all native logs
 * and supports runtime-controlled verbose logging.
 *
 * The goal is to avoid scattering `if (verbose)` checks across
 * business logic and keep logging behavior consistent.
 */
object TLSFingerprintLogger {
  /**
   * Logcat tag used for all plugin logs.
   * Helps filtering logs during debugging.
   */
  private const val TAG = "⚡️ TLSFingerprint"

  /**
   * Controls whether debug logs are printed.
   *
   * This flag should be set once during plugin initialization
   * based on configuration values.
   */
  var verbose: Boolean = false

  /**
   * Prints a debug / verbose log message.
   *
   * This method should be used for development-time diagnostics
   * and is automatically silenced when [verbose] is false.
   *
   * @param messages One or more message fragments to be concatenated.
   */
  fun debug(vararg messages: String) {
    if (verbose) {
      log(TAG, Log.DEBUG, *messages)
    }
  }

  /**
   * Prints an error log message.
   *
   * Error logs are always printed regardless of [verbose] state.
   *
   * @param message Human-readable error description.
   * @param e Optional exception for stack trace logging.
   */
  fun error(
    message: String,
    e: Throwable? = null,
  ) {
    val sb = StringBuilder(message)
    if (e != null) {
      sb.append(" | Error: ").append(e.message)
    }
    Log.e(TAG, sb.toString(), e)
  }

  /**
   * Internal low-level log dispatcher.
   *
   * Joins message fragments and forwards them to Android's Log API
   * using the specified priority.
   */
  fun log(
    tag: String,
    level: Int,
    vararg messages: String,
  ) {
    val sb = StringBuilder()
    for (msg in messages) {
      sb.append(msg).append(" ")
    }
    when (level) {
      Log.DEBUG -> Log.d(tag, sb.toString())
      Log.INFO -> Log.i(tag, sb.toString())
      Log.WARN -> Log.w(tag, sb.toString())
      Log.ERROR -> Log.e(tag, sb.toString())
      else -> Log.v(tag, sb.toString())
    }
  }
}
