package io.capkit.authentication.apple

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file AppleHostAllowlistTest.kt
 * RED-GREEN contract for the WebView host allowlist:
 * the [AppleSignInActivity] MUST only navigate to Apple-owned authentication
 * hosts; any other host (including suffix-look-alike domains) is rejected.
 * Pure, no Android runtime deps.
 */
class AppleHostAllowlistTest {
  @Test
  fun `allows the apple authorization endpoint host`() {
    assertTrue(AppleHostAllowlist.isAllowed("appleid.apple.com"))
  }

  @Test
  fun `allows the apple authentication host`() {
    assertTrue(AppleHostAllowlist.isAllowed("idmsa.apple.com"))
  }

  @Test
  fun `allows the apple account recovery host`() {
    assertTrue(AppleHostAllowlist.isAllowed("iforgot.apple.com"))
  }

  @Test
  fun `allows subdomains of allowed hosts`() {
    assertTrue(AppleHostAllowlist.isAllowed("auth.idmsa.apple.com"))
  }

  @Test
  fun `rejects a look alike domain with the same suffix`() {
    assertFalse(AppleHostAllowlist.isAllowed("appleid.apple.com.evil.com"))
  }

  @Test
  fun `rejects a plain evil domain`() {
    assertFalse(AppleHostAllowlist.isAllowed("evil.com"))
  }

  @Test
  fun `rejects apple com look alike`() {
    assertFalse(AppleHostAllowlist.isAllowed("appleid-apple-com.evil.com"))
  }

  @Test
  fun `rejects null and empty hosts`() {
    assertFalse(AppleHostAllowlist.isAllowed(null))
    assertFalse(AppleHostAllowlist.isAllowed(""))
    assertFalse(AppleHostAllowlist.isAllowed("   "))
  }

  @Test
  fun `rejects an apple com subdomain of an unlisted parent`() {
    assertFalse(AppleHostAllowlist.isAllowed("apple.com"))
    assertFalse(AppleHostAllowlist.isAllowed("idmsa.com"))
  }
}
