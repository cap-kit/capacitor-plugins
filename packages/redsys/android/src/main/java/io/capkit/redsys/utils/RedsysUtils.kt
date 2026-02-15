package io.capkit.redsys.utils

import android.util.Base64
import com.getcapacitor.JSObject
import com.redsys.tpvvinapplibrary.ResultResponse
import com.redsys.tpvvinapplibrary.TPVVConstants
import org.json.JSONObject
import java.util.HashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Redsys Utility Helpers (Android)
 *
 * This object belongs to the Utils Layer.
 * It provides stateless helper functions for:
 * - SDK → JS response mapping
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
  // Response Mapping
  // ---------------------------------------------------------------------------

  /**
   * Converts SDK ResultResponse into a standardized JS-compatible object.
   * Maintains a stable cross-platform response shape.
   */
  fun resultToJSObject(res: ResultResponse): JSObject {
    val js = JSObject()

    // Base result info
    js.put("code", res.responseCode?.toIntOrNull() ?: 0)
    js.put("desc", "") // Description is usually handled in the reject path for errors

    // Transaction details
    js.put("amount", res.amount ?: "")
    js.put("currency", res.currency ?: "")
    js.put("order", res.order ?: "")
    js.put("merchantCode", res.merchantCode ?: "")
    js.put("terminal", res.terminal ?: "")
    js.put("responseCode", res.responseCode ?: "")
    js.put("authorisationCode", res.authorisationCode ?: "")
    js.put("transactionType", res.transactionType ?: "")
    js.put("securePayment", res.securePayment ?: "")
    js.put("signature", res.signature ?: "")

    // Card details (ensuring empty string if null for JS parity)
    js.put("cardNumber", if (res.cardNumber != null) maskCardNumber(res.cardNumber) else "")
    js.put("cardBrand", res.cardBrand ?: "")
    js.put("cardCountry", res.cardCountry ?: "")
    js.put("cardType", res.cardType ?: "")
    js.put("expiryDate", res.expiryDate ?: "")

    // Merchant and metadata
    js.put("merchantIdentifier", res.identifier ?: "")
    js.put("consumerLanguage", res.language ?: "")
    js.put("date", res.date ?: "")
    js.put("hour", res.hour ?: "")
    js.put("merchantData", res.merchantData ?: "")

    // Add extraParams if present
    val extraParamsJS = JSObject()
    if (!res.extraParams.isNullOrEmpty()) {
      try {
        // Parse JSON string manually to avoid SDK-specific utility errors
        val json = JSONObject(res.extraParams)
        val keys = json.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          extraParamsJS.put(key, json.get(key).toString())
        }
      } catch (e: Exception) {
        // Fallback to empty object on parse error
      }
    }
    js.put("extraParams", extraParamsJS)

    return js
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
