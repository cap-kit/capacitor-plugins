package io.capkit.integrity.config

import com.getcapacitor.Plugin

/**
 * Plugin configuration container.
 *
 * This class is responsible for reading and exposing
 * static configuration values defined under the
 * `Integrity` key in capacitor.config.ts.
 *
 * Configuration rules:
 * - Read once during plugin initialization
 * - Treated as immutable runtime input
 * - Accessible only from native code
 *
 * @property verboseLogging Enables verbose native logging.
 * @property blockPage Optional block page configuration.
 */
class Config(
  plugin: Plugin,
) {
  // -----------------------------------------------------------------------------
  // Configuration Keys
  // -----------------------------------------------------------------------------

  /**
   * Centralized definition of configuration keys.
   * Avoids string duplication and typos.
   */
  private object Keys {
    const val VERBOSE_LOGGING = "verboseLogging"
    const val BLOCK_PAGE = "blockPage"
    const val BLOCK_PAGE_ENABLED = "enabled"
    const val BLOCK_PAGE_URL = "url"
  }

  // -----------------------------------------------------------------------------
  // Public Configuration Values
  // -----------------------------------------------------------------------------

  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information
   * is printed to Logcat.
   *
   * @default false
   */
  val verboseLogging: Boolean

  /**
   * Optional configuration for the integrity block page.
   *
   * Controls the availability and source of a developer-provided
   * HTML page that may be presented to the end user when the host
   * application decides to do so.
   *
   * @see BlockPageConfig
   */
  val blockPage: BlockPageConfig?

  // ---------------------------------------------------------------------------
  // Initialization
  // ---------------------------------------------------------------------------

  init {
    val config = plugin.config

    // Verbose logging flag
    verboseLogging =
      config.getBoolean(Keys.VERBOSE_LOGGING, false)

    val blockPageConfig = config.getObject(Keys.BLOCK_PAGE)

    blockPage =
      if (blockPageConfig != null) {
        BlockPageConfig(
          enabled =
            if (blockPageConfig.has(Keys.BLOCK_PAGE_ENABLED)) {
              blockPageConfig.getBoolean(Keys.BLOCK_PAGE_ENABLED)
            } else {
              false
            },
          url =
            if (blockPageConfig.has(Keys.BLOCK_PAGE_URL)) {
              blockPageConfig.getString(Keys.BLOCK_PAGE_URL)
            } else {
              null
            },
        )
      } else {
        null
      }
  }
}

// -----------------------------------------------------------------------------
// Block Page Config
// -----------------------------------------------------------------------------

/**
 * Configuration for the optional integrity block page.
 *
 * This configuration controls the availability and source
 * of a developer-provided HTML page that may be presented
 * to the end user when the host application decides to do so.
 *
 * @property enabled Enables the block page feature.
 * @property url URL or local path of the HTML page to present.
 */
data class BlockPageConfig(
  val enabled: Boolean,
  val url: String?,
)
