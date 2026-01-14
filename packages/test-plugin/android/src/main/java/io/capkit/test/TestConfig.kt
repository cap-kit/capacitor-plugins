package io.capkit.test

import com.getcapacitor.Plugin

/**
 * Helper class to manage the plugin configuration.
 * It parses the values from capacitor.config.json
 */
class TestConfig(plugin: Plugin) {
  val customMessage: String

  init {
    // "customMessage" must match the key in capacitor.config.ts
    customMessage = plugin.getConfig().getString("customMessage") ?: " (from config)"
  }
}
