package io.capkit.securestorage.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the pure key-name codecs of [SecureStorageUtils].
 *
 * These run on the JVM (no Android runtime needed) because the utils
 * were ported to java.util.Base64, which behaves identically to the
 * previous android.util.Base64 usage on device.
 */
class SecureStorageUtilsTest {
  @Test
  fun encodeB64IsUrlSafeAndUnpadded() {
    val encoded = SecureStorageUtils.encodeB64("clave Usuario#1")
    assertFalse(encoded.contains('+'))
    assertFalse(encoded.contains('/'))
    assertFalse(encoded.contains('='))
  }

  @Test
  fun encodeDecodeRoundTrip() {
    val original = "clave Usuario#1/ñ+accent"
    val encoded = SecureStorageUtils.encodeB64(original)
    assertEquals(original, SecureStorageUtils.decodeB64(encoded))
  }

  @Test
  fun decodeB64RejectsPlainNames() {
    // Plain (non-encoded) names must never be misdecoded.
    assertNull(SecureStorageUtils.decodeB64("plain_key_name"))
    assertNull(SecureStorageUtils.decodeB64("user@domain.com"))
  }

  @Test
  fun decodeB64RejectsInvalidSequences() {
    assertNull(SecureStorageUtils.decodeB64("!!!"))
    // An empty value round-trips cleanly on both platforms (Swift does
    // the same), so it decodes to the empty string rather than null.
    assertEquals("", SecureStorageUtils.decodeB64(""))
  }

  @Test
  fun encodedValuesMatchIosImplementation() {
    // Cross-platform parity: the Swift implementation uses the same
    // URL-safe alphabet and padding stripping, so both platforms must
    // produce identical obfuscated names for the same key.
    assertEquals("a2V5X25hbWU", SecureStorageUtils.encodeB64("key_name"))
    assertEquals("dXN1YXJpbw", SecureStorageUtils.encodeB64("usuario"))
  }
}
