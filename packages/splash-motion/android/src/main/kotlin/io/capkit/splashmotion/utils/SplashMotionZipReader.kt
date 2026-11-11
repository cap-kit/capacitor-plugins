package io.capkit.splashmotion.utils

import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Minimal dotLottie reader.
 *
 * A dotLottie file is a zip archive containing a `manifest.json` plus one or
 * more animation JSONs under `animations/`. Lottie's composition parsers only
 * accept raw JSON, so this reader streams the archive and extracts the JSON
 * payload of the selected animation entry (see
 * [SplashMotionValidators.selectDotLottieEntry] for the selection rule).
 *
 * Pure JVM code: readable from an arbitrary stream, unit-testable headless.
 */
object SplashMotionZipReader {
  /**
   * Extracts the JSON payload of the selected animation entry.
   *
   * @return the raw JSON text, or null when the stream is not a readable
   * zip archive or contains no animation JSON.
   */
  fun firstAnimationJson(stream: InputStream): String? {
    val names = mutableListOf<String>()
    val payloads = mutableMapOf<String, String>()

    ZipInputStream(stream.buffered()).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val name = entry.name
          names.add(name)
          if (name.endsWith(".json")) {
            payloads[name] = zip.readBytes().toString(Charsets.UTF_8)
          }
        }
        zip.closeEntry()
        entry = zip.nextEntry
      }
    }

    val selected = SplashMotionValidators.selectDotLottieEntry(names) ?: return null
    return payloads[selected]
  }
}
