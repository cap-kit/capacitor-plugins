package io.capkit.privacyscreen

import io.capkit.privacyscreen.utils.PrivacyScreenValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Headless JVM tests for the pure validation helpers of the plugin.
 */
class PrivacyScreenValidatorsTest {
  @Test
  fun hexColorAcceptsSixDigitWithHash() {
    assertTrue(PrivacyScreenValidators.isValidHexColor("#0B2E4F"))
  }

  @Test
  fun hexColorAcceptsEightDigitWithHash() {
    assertTrue(PrivacyScreenValidators.isValidHexColor("#FF0B2E4F"))
  }

  @Test
  fun hexColorAcceptsMissingHash() {
    assertTrue(PrivacyScreenValidators.isValidHexColor("0B2E4F"))
  }

  @Test
  fun hexColorAcceptsLowercaseDigits() {
    assertTrue(PrivacyScreenValidators.isValidHexColor("#0b2e4f"))
  }

  @Test
  fun hexColorRejectsNamesAndShortShapes() {
    assertFalse(PrivacyScreenValidators.isValidHexColor("white"))
    assertFalse(PrivacyScreenValidators.isValidHexColor("#fff"))
    assertFalse(PrivacyScreenValidators.isValidHexColor("#GGGGGG"))
    assertFalse(PrivacyScreenValidators.isValidHexColor(null))
    assertFalse(PrivacyScreenValidators.isValidHexColor(""))
  }

  @Test
  fun parseHexColorExpandsSixDigitsToOpaque() {
    assertEquals(-1, PrivacyScreenValidators.parseHexColor("#FFFFFF"))
    assertEquals(
      0xFF0B2E4F.toInt(),
      PrivacyScreenValidators.parseHexColor("#0B2E4F"),
    )
  }

  @Test
  fun parseHexColorKeepsAlphaChannel() {
    assertEquals(
      0x80FFFFFF.toInt(),
      PrivacyScreenValidators.parseHexColor("#80FFFFFF"),
    )
  }
}
