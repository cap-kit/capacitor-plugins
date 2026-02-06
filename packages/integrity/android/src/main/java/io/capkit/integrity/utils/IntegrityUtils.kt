package io.capkit.integrity.utils

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject

/**
 * Utility helpers for the Integrity plugin.
 *
 * This layer handles data transformation and mapping logic, ensuring
 * that native Kotlin types are correctly converted to Capacitor-compatible
 * JSON structures.
 */
object IntegrityUtils {
  /**
   * Recursively converts a Kotlin Map into a Capacitor JSObject.
   * * This ensures that nested Maps and Lists are correctly serialized
   * instead of being converted to simple strings.
   */
  fun toJSObject(map: Map<*, *>): JSObject {
    val js = JSObject()
    map.forEach { (key, value) ->
      val k = key.toString()
      when (value) {
        is Map<*, *> -> js.put(k, toJSObject(value))
        is List<*> -> {
          val array = JSArray()
          value.forEach { item ->
            if (item is Map<*, *>) {
              array.put(toJSObject(item))
            } else {
              array.put(item)
            }
          }
          js.put(k, array)
        }
        else -> js.put(k, value)
      }
    }
    return js
  }
}
