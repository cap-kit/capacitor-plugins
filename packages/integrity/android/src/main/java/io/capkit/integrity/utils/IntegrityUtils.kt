package io.capkit.integrity.utils

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject

/**
 * Utility helpers for the Integrity plugin.
 *
 * PURPOSE:
 * - Convert Kotlin data structures into Capacitor-compatible JSObject / JSArray.
 *
 * SCOPE:
 * - This utility is intended ONLY for JSON-like data structures composed of:
 *   - Map
 *   - List
 *   - String / Boolean / Number / null
 *
 * WARNING:
 * - Passing non JSON-safe types (e.g. custom objects, enums, exceptions)
 *   will result in a string fallback via `toString()`.
 * - Cyclic data structures are NOT supported and will cause a StackOverflowError.
 */
object IntegrityUtils {
  /**
   * Recursively converts a Kotlin Map into a Capacitor JSObject.
   *
   * CONTRACT:
   * - Keys are converted to String using `toString()`.
   * - Supported value types:
   *   - Map<*, *>
   *   - List<*>
   *   - String / Boolean / Number / null
   *
   * - Any unsupported value type is converted using `toString()`
   *   to avoid runtime crashes.
   *
   * NOTE:
   * - This method assumes acyclic data structures.
   * - Designed for deterministic, inspection-safe payloads.
   */
  fun toJSObject(map: Map<*, *>): JSObject {
    val js = JSObject()

    map.forEach { (key, value) ->
      val k = key?.toString() ?: "null"

      when (value) {
        null -> js.put(k, null)

        is Map<*, *> -> js.put(k, toJSObject(value))

        is List<*> -> js.put(k, toJSArray(value))

        is String,
        is Boolean,
        is Number,
        -> js.put(k, value)

        else -> {
          // Fallback for non JSON-safe types
          js.put(k, value.toString())
        }
      }
    }

    return js
  }

  /**
   * Recursively converts a Kotlin List into a Capacitor JSArray.
   *
   * CONTRACT:
   * - Supported element types:
   *   - Map<*, *>
   *   - List<*>
   *   - String / Boolean / Number / null
   *
   * - Unsupported element types are converted using `toString()`.
   */
  private fun toJSArray(list: List<*>): JSArray {
    val array = JSArray()

    list.forEach { item ->
      when (item) {
        null -> array.put(null)

        is Map<*, *> -> array.put(toJSObject(item))

        is List<*> -> array.put(toJSArray(item))

        is String,
        is Boolean,
        is Number,
        -> array.put(item)

        else -> {
          // Fallback for non JSON-safe types
          array.put(item.toString())
        }
      }
    }

    return array
  }
}
