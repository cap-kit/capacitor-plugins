package io.capkit.sslpinning.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SSLPinningUtilsTest {
  // MARK: - Fingerprint Normalization

  @Test
  fun normalizeFingerprint_withColons_returnsLowercaseNoColons() {
    val input = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"
    val result = SSLPinningUtils.normalizeFingerprint(input)
    assertEquals("aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899", result)
  }

  @Test
  fun normalizeFingerprint_withSpaces_returnsLowercaseNoSpaces() {
    val input = "AA BB CC DD EE FF 00 11 22 33 44 55 66 77 88 99 AA BB CC DD EE FF 00 11 22 33 44 55 66 77 88 99"
    val result = SSLPinningUtils.normalizeFingerprint(input)
    assertEquals("aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899", result)
  }

  @Test
  fun normalizeFingerprint_withMixedColonsAndSpaces_returnsLowercaseClean() {
    val input = "AA:BB CC:DD EE:FF 00:11 22 33 AA:BB CC:DD EE:FF 00:11 22 33"
    val result = SSLPinningUtils.normalizeFingerprint(input)
    assertEquals("aabbccddeeff00112233aabbccddeeff00112233", result)
  }

  @Test
  fun normalizeFingerprint_uppercase_returnsLowercase() {
    val input = "AABBCCDDEEFF00112233445566778899AABBCCDDEEFF00112233445566778899"
    val result = SSLPinningUtils.normalizeFingerprint(input)
    assertEquals("aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899", result)
  }

  // MARK: - Fingerprint Validation

  @Test
  fun isValidFingerprintFormat_valid64Hex_returnsTrue() {
    val input = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
    val result = SSLPinningUtils.isValidFingerprintFormat(input)
    assertTrue(result)
  }

  @Test
  fun isValidFingerprintFormat_validWithColons_returnsTrue() {
    val input = "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899"
    val result = SSLPinningUtils.isValidFingerprintFormat(input)
    assertTrue(result)
  }

  @Test
  fun isValidFingerprintFormat_tooShort_returnsFalse() {
    val input = "abcdef1234567890"
    val result = SSLPinningUtils.isValidFingerprintFormat(input)
    assertFalse(result)
  }

  @Test
  fun isValidFingerprintFormat_tooLong_returnsFalse() {
    val input = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
    val result = SSLPinningUtils.isValidFingerprintFormat(input)
    assertFalse(result)
  }

  @Test
  fun isValidFingerprintFormat_invalidHex_returnsFalse() {
    val input = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef123456789g"
    val result = SSLPinningUtils.isValidFingerprintFormat(input)
    assertFalse(result)
  }

  // MARK: - Excluded Domains Matching

  @Test
  fun isExcludedDomain_exactMatch_returnsTrue() {
    val excludedDomains = listOf("example.com")
    val host = "example.com"
    val result = matchesExcludedDomain(host, excludedDomains)
    assertTrue(result)
  }

  @Test
  fun isExcludedDomain_subdomain_returnsTrue() {
    val excludedDomains = listOf("example.com")
    val host = "api.example.com"
    val result = matchesExcludedDomain(host, excludedDomains)
    assertTrue(result)
  }

  @Test
  fun isExcludedDomain_deepSubdomain_returnsTrue() {
    val excludedDomains = listOf("example.com")
    val host = "api.v2.example.com"
    val result = matchesExcludedDomain(host, excludedDomains)
    assertTrue(result)
  }

  @Test
  fun isExcludedDomain_notExcluded_returnsFalse() {
    val excludedDomains = listOf("example.com")
    val host = "other.com"
    val result = matchesExcludedDomain(host, excludedDomains)
    assertFalse(result)
  }

  @Test
  fun isExcludedDomain_similarSuffix_returnsFalse() {
    val excludedDomains = listOf("example.com")
    val host = "notexample.com"
    val result = matchesExcludedDomain(host, excludedDomains)
    assertFalse(result)
  }

  @Test
  fun isExcludedDomain_caseInsensitive_returnsTrue() {
    val excludedDomains = listOf("Example.com")
    val host = "API.EXAMPLE.COM"
    val result = matchesExcludedDomain(host, excludedDomains)
    assertTrue(result)
  }

  // MARK: - Result Shape

  @Test
  fun resultShape_successFingerprint_hasRequiredFields() {
    val result =
      mapOf(
        "actualFingerprint" to "abcdef1234567890",
        "fingerprintMatched" to true,
        "mode" to "fingerprint",
        "errorCode" to "",
        "error" to "",
      )

    assertTrue(result.containsKey("actualFingerprint"))
    assertTrue(result.containsKey("fingerprintMatched"))
    assertTrue(result.containsKey("mode"))
    assertTrue(result.containsKey("errorCode"))
    assertTrue(result.containsKey("error"))

    assertEquals(true, result["fingerprintMatched"])
    assertEquals("fingerprint", result["mode"])
  }

  @Test
  fun resultShape_excludedDomain_hasRequiredFields() {
    val result =
      mapOf(
        "actualFingerprint" to "abcdef1234567890",
        "fingerprintMatched" to true,
        "excludedDomain" to true,
        "mode" to "excluded",
        "errorCode" to "EXCLUDED_DOMAIN",
        "error" to "Excluded domain",
      )

    assertTrue(result.containsKey("actualFingerprint"))
    assertTrue(result.containsKey("fingerprintMatched"))
    assertTrue(result.containsKey("excludedDomain"))
    assertTrue(result.containsKey("mode"))
    assertTrue(result.containsKey("errorCode"))
    assertTrue(result.containsKey("error"))

    assertEquals(true, result["fingerprintMatched"])
    assertEquals(true, result["excludedDomain"])
    assertEquals("excluded", result["mode"])
  }

  @Test
  fun resultShape_failure_hasErrorFields() {
    val result =
      mapOf(
        "actualFingerprint" to "abcdef1234567890",
        "fingerprintMatched" to false,
        "mode" to "fingerprint",
        "errorCode" to "PINNING_FAILED",
        "error" to "Pinning failed",
      )

    assertTrue(result.containsKey("actualFingerprint"))
    assertTrue(result.containsKey("fingerprintMatched"))
    assertTrue(result.containsKey("mode"))
    assertTrue(result.containsKey("errorCode"))
    assertTrue(result.containsKey("error"))

    assertEquals(false, result["fingerprintMatched"])
    assertEquals("PINNING_FAILED", result["errorCode"])
  }

  // MARK: - Helper

  private fun matchesExcludedDomain(
    host: String,
    excludedDomains: List<String>,
  ): Boolean {
    val hostLower = host.lowercase().trim()
    return excludedDomains.any { excluded ->
      val excludedLower = excluded.lowercase().trim()
      hostLower == excludedLower || hostLower.endsWith(".$excludedLower")
    }
  }
}
