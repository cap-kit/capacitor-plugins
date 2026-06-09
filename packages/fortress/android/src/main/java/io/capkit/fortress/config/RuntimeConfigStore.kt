package io.capkit.fortress.config

import android.content.Context
import com.getcapacitor.JSObject
import org.json.JSONObject

class RuntimeConfigStore(
  context: Context,
) {
  private companion object {
    const val PREFS_NAME = "fortress_runtime_config"
    const val KEY_PAYLOAD = "payload"
    const val PAYLOAD_VERSION = 1

    val ALLOWED_KEYS =
      setOf(
        "verboseLogging",
        "logLevel",
        "lockAfterMs",
        "enablePrivacyScreen",
        "privacyOverlayText",
        "privacyOverlayImageName",
        "privacyOverlayShowText",
        "privacyOverlayShowImage",
        "privacyOverlayTextColor",
        "privacyOverlayBackgroundOpacity",
        "privacyOverlayTheme",
        "fallbackStrategy",
        "allowCachedAuthentication",
        "cachedAuthenticationTimeoutMs",
        "maxBiometricAttempts",
        "lockoutDurationMs",
        "requireFreshAuthenticationMs",
        "encryptionAlgorithm",
        "persistSessionState",
      )
  }

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun loadOverrides(): JSObject? {
    val rawPayload = prefs.getString(KEY_PAYLOAD, null) ?: return null

    val payload = runCatching { JSONObject(rawPayload) }.getOrNull() ?: return null
    if (payload.optInt("version", -1) != PAYLOAD_VERSION) {
      return null
    }

    val rawOverrides = payload.optJSONObject("overrides") ?: return null
    return sanitize(rawOverrides)
  }

  fun saveOverrides(rawOverrides: JSObject) {
    val sanitized = sanitize(rawOverrides)
    val payload =
      JSONObject()
        .put("version", PAYLOAD_VERSION)
        .put("updatedAt", System.currentTimeMillis())
        .put("overrides", sanitized)

    prefs.edit().putString(KEY_PAYLOAD, payload.toString()).apply()
  }

  fun clearOverrides() {
    prefs.edit().remove(KEY_PAYLOAD).apply()
  }

  private fun sanitize(source: JSONObject): JSObject {
    val result = JSObject()

    val keys = source.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      if (!ALLOWED_KEYS.contains(key)) {
        continue
      }

      val value = source.opt(key)
      if (value is Boolean || value is Int || value is Long || value is Double || value is String) {
        result.put(key, value)
      }
    }

    return result
  }
}
