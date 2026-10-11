package io.capkit.fortress.logger

import android.util.Log

/**
 * Centralized logging utility for the Fortress plugin.
 *
 * This logging provides a single entry point for all native logs
 * and supports runtime-controlled verbose logging.
 *
 * The goal is to avoid scattering `if (verbose)` checks across
 * business logic and keep logging behavior consistent.
 */
object Logger {
  enum class Level(
    val priority: Int,
  ) {
    ERROR(Log.ERROR),
    WARN(Log.WARN),
    INFO(Log.INFO),
    DEBUG(Log.DEBUG),
    VERBOSE(Log.VERBOSE),
  }

  /**
   * Logcat tag used for all plugin logs.
   * Helps filtering logs during debugging.
   */
  private const val TAG = "⚡️ Fortress"

  /**
   * Controls whether debug logs are printed.
   *
   * This flag should be set once during plugin initialization
   * based on configuration values.
   */
  var verbose: Boolean = false
    set(value) {
      field = value
      level = if (value) Level.DEBUG else Level.INFO
    }

  var level: Level = Level.INFO

  /**
   * Sets logger level from a configuration string.
   *
   * Unknown values fall back to `INFO`.
   */
  fun setLevel(levelName: String?) {
    val normalized = levelName?.trim()?.lowercase()
    level =
      when (normalized) {
        "error" -> Level.ERROR
        "warn" -> Level.WARN
        "debug" -> Level.DEBUG
        "verbose" -> Level.VERBOSE
        else -> Level.INFO
      }
  }

  /**
   * Prints a debug / verbose log message.
   *
   * This method should be used for development-time diagnostics
   * and is automatically silenced when [verbose] is false.
   *
   * @param messages One or more message fragments to be concatenated.
   */
  fun debug(vararg messages: String) {
    if (level.priority <= Log.DEBUG) {
      log(TAG, Log.DEBUG, *messages)
    }
  }

  /**
   * Prints an info log message.
   *
   * Info logs are always printed regardless of [verbose] state.
   * Used for one-time notifications like hook activation.
   *
   * @param message Human-readable info description.
   */
  fun info(message: String) {
    if (level.priority <= Log.INFO) {
      log(TAG, Log.INFO, message)
    }
  }

  /**
   * Prints a warning log message.
   *
   * Warning logs are always printed regardless of [verbose] state.
   * Used for potentially problematic conditions.
   *
   * @param message Human-readable warning description.
   */
  fun warn(message: String) {
    if (level.priority <= Log.WARN) {
      log(TAG, Log.WARN, message)
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
    if (level.priority > Log.ERROR) {
      return
    }

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
