package io.capkit.redsys.utils

import android.util.Base64
import com.getcapacitor.JSObject
import com.redsys.tpvvinapplibrary.TPVVConstants
import java.util.HashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Redsys Utility Helpers (Android)
 *
 * This object belongs to the Utils Layer.
 * It provides stateless helper functions for:
 * - Enum conversion
 * - Data transformation
 * - Cryptographic operations
 *
 * Architectural rules:
 * - MUST NOT access PluginCall
 * - MUST NOT perform UI operations
 * - MUST remain side-effect free
 */
object RedsysUtils {
  // ---------------------------------------------------------------------------
  // Enum Mapping
  // ---------------------------------------------------------------------------

  /**
   * Maps public JS transaction type values to SDK-specific constants.
   * Ensures the JS API remains platform-agnostic.
   */
  fun mapTransactionType(type: String?): String =
    when (type?.lowercase()) {
      "preauthorization" -> TPVVConstants.PAYMENT_TYPE_PREAUTHORIZATION
      "traditional" -> TPVVConstants.PAYMENT_TYPE_TRADITIONAL
      "authentication" -> TPVVConstants.PAYMENT_TYPE_AUTHENTICATION
      else -> TPVVConstants.PAYMENT_TYPE_NORMAL
    }

  // ---------------------------------------------------------------------------
  // Data Conversion
  // ---------------------------------------------------------------------------

  /**
   * Converts a JSObject into a HashMap<String, String>.
   * Used for mapping extraParams between JS and SDK.
   */
  fun toHashMap(obj: JSObject?): HashMap<String, String>? {
    if (obj == null) return null
    val map = HashMap<String, String>()
    val keys = obj.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      map[key] = obj.getString(key) ?: ""
    }
    return map
  }

  // ---------------------------------------------------------------------------
  // Card Masking
  // ---------------------------------------------------------------------------

  /**
   * Masks a card number using a specific pattern.
   * Matches the reference implementation logic.
   */
  fun maskCardNumber(
    cardNumber: String,
    mask: String = "xxxx-xxxx-xxxx-####",
  ): String {
    var index = 0
    val maskedNumber = StringBuilder()
    for (i in 0 until mask.length) {
      val c = mask[i]
      when (c) {
        '#' -> {
          maskedNumber.append(cardNumber[index])
          index++
        }
        'x' -> {
          maskedNumber.append(c)
          index++
        }
        else -> {
          maskedNumber.append(c)
        }
      }
    }
    return maskedNumber.toString()
  }

  // ---------------------------------------------------------------------------
  // Cryptographic Utilities
  // ---------------------------------------------------------------------------

  /**
   * Computes HMAC using a Base64 encoded key.
   * Matches Redsys requirement for WebView signature generation.
   */
  fun calculateHMAC(
    data: String,
    keyBase64: String,
    algorithm: String,
  ): String? =
    try {
      val javaAlg = if (algorithm.contains("512")) "HmacSHA512" else "HmacSHA256"
      // Use NO_WRAP/DEFAULT based on common Base64 merchant key formats
      val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
      val mac = Mac.getInstance(javaAlg)
      mac.init(SecretKeySpec(keyBytes, javaAlg))
      val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
      Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
      RedsysLogger.error("HMAC calculation failed", e)
      null
    }
}
