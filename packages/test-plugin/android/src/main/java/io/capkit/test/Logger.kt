package io.capkit.test

import android.util.Log

/**
 * Utility object for logging messages with a custom prefix.
 */
object Logger {
  private const val TAG = "⚡️ Test"

  fun debug(vararg messages: String) {
    log(TAG, *messages)
  }

  fun log(
    tag: String,
    vararg messages: String,
  ) {
    val sb = StringBuilder()
    for (msg in messages) {
      sb.append(msg).append(" ")
    }
    Log.d(tag, sb.toString())
  }
}
