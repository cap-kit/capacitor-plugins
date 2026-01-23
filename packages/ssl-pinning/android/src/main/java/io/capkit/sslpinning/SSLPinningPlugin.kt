package io.capkit.sslpinning

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.capkit.sslpinning.utils.SSLPinningLogger

@CapacitorPlugin(name = "SSLPinning")
class SSLPinningPlugin : Plugin() {
  private lateinit var implementation: SSLPinningImpl

  override fun load() {
    val config = SSLPinningConfig(this)
    SSLPinningLogger.verbose = config.verboseLogging
    implementation = SSLPinningImpl(config)
  }

  @PluginMethod
  fun checkCertificate(call: PluginCall) {
    val url = call.getString("url") ?: ""
    val fingerprint = call.getString("fingerprint")

    if (url.isEmpty()) {
      val result = JSObject()
      result.put("fingerprintMatched", false)
      result.put("error", "Missing url")
      call.resolve(result)
      return
    }

    execute {
      implementation.checkCertificate(url, fingerprint) { data ->
        val result = JSObject()
        for (entry in data.entries) {
          result.put(entry.key, entry.value)
        }
        call.resolve(result)
      }
    }
  }

  @PluginMethod
  fun checkCertificates(call: PluginCall) {
    val url = call.getString("url") ?: ""

    val jsArray: JSArray? = call.getArray("fingerprints")
    val fingerprints: List<String>? =
      if (jsArray != null && jsArray.length() > 0) {
        val list = ArrayList<String>()
        for (i in 0 until jsArray.length()) {
          val value = jsArray.getString(i)
          if (!value.isNullOrEmpty()) {
            list.add(value)
          }
        }
        if (list.isNotEmpty()) list else null
      } else {
        null
      }

    if (url.isEmpty()) {
      val result = JSObject()
      result.put("fingerprintMatched", false)
      result.put("error", "Missing url")
      call.resolve(result)
      return
    }

    execute {
      implementation.checkCertificates(url, fingerprints) { data ->
        val result = JSObject()
        for (entry in data.entries) {
          result.put(entry.key, entry.value)
        }
        call.resolve(result)
      }
    }
  }
}
