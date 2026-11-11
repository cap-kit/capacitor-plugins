package io.capkit.splashmotion.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Headless JVM tests for the dotLottie reader.
 *
 * Archives are built in-memory with ZipOutputStream, matching the real
 * dotLottie container layout (manifest.json plus animation entries under
 * the animations folder).
 */
class SplashMotionZipReaderTest {
  @Test
  fun extractsFirstAnimationsEntry() {
    val zip =
      dotLottieZip(
        "manifest.json" to "{\"version\":\"1.0\"}",
        "animations/splash.json" to "{\"v\":\"5.0\"}",
        "animations/other.json" to "{\"v\":\"5.1\"}",
      )
    assertEquals(
      "{\"v\":\"5.0\"}",
      SplashMotionZipReader.firstAnimationJson(ByteArrayInputStream(zip)),
    )
  }

  @Test
  fun fallsBackToFirstNonManifestJson() {
    val zip =
      dotLottieZip(
        "manifest.json" to "{}",
        "root-animation.json" to "{\"v\":\"5.0\"}",
      )
    assertEquals(
      "{\"v\":\"5.0\"}",
      SplashMotionZipReader.firstAnimationJson(ByteArrayInputStream(zip)),
    )
  }

  @Test
  fun returnsNullForArchiveWithoutAnimationJson() {
    val zip =
      dotLottieZip(
        "manifest.json" to "{}",
        "images/logo.png" to "bytes",
      )
    assertNull(SplashMotionZipReader.firstAnimationJson(ByteArrayInputStream(zip)))
  }

  @Test
  fun returnsNullForNonZipStream() {
    assertNull(SplashMotionZipReader.firstAnimationJson(ByteArrayInputStream(byteArrayOf(1, 2, 3))))
  }

  private fun dotLottieZip(vararg entries: Pair<String, String>): ByteArray {
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
      entries.forEach { (name, content) ->
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
      }
    }
    return bytes.toByteArray()
  }
}
