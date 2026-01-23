package io.capkit.sslpinning

import com.getcapacitor.Plugin

class SSLPinningConfig(plugin: Plugin) {
  val verboseLogging: Boolean
  val fingerprint: String?
  val fingerprints: List<String>

  init {
    val config = plugin.getConfig()

    verboseLogging = config.getBoolean("verboseLogging", false)

    val fp = config.getString("fingerprint")
    fingerprint = if (fp.isNullOrBlank()) null else fp

    fingerprints =
      config.getArray("fingerprints")?.toList()?.mapNotNull { it as? String }
        ?: emptyList()
  }
}
