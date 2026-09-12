package io.capkit.authentication.facebook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file FacebookConfigTest.kt
 * RED-GREEN contract for the `facebook` configuration sub-object resolved from the
 * plugin's raw Capacitor config JSON (configuration-integrity: native-only read,
 * per-provider `facebook` block, absent/malformed treated as an initialization
 * failure). Mirrors the `AppleConfigResolver` /
 * `GoogleConfigResolver` conventions (config-integrity-guard).
 */
class FacebookConfigTest {
  @Test
  fun `resolve applies default version v170 and default scopes when only app id given`() {
    val config =
      FacebookConfigResolver.resolve(
        """{"facebook":{"facebookAppId":"1234567890"}}""",
      )

    assertNotNull(config)
    config!!
    assertEquals("1234567890", config.facebookAppId)
    assertEquals(FacebookConfig.DEFAULT_VERSION, config.facebookVersion)
    assertEquals(listOf("public_profile", "email"), config.scopes)
    assertNull(config.facebookClientToken)
  }

  @Test
  fun `resolve respects explicit client token version and scopes`() {
    val config =
      FacebookConfigResolver.resolve(
        """{"facebook":{"facebookAppId":"1234567890","facebookClientToken":"ct-1","facebookVersion":"v19.0","scopes":["email"]}}""",
      )

    assertNotNull(config)
    config!!
    assertEquals("ct-1", config.facebookClientToken)
    assertEquals("v19.0", config.facebookVersion)
    assertEquals(listOf("email"), config.scopes)
  }

  @Test
  fun `resolve returns null when facebook block is absent`() {
    assertNull(FacebookConfigResolver.resolve("""{"verboseLogging":true}"""))
    assertNull(FacebookConfigResolver.resolve("""{}"""))
  }

  @Test
  fun `resolve returns null on malformed top-level JSON`() {
    assertNull(FacebookConfigResolver.resolve("not-json"))
  }

  @Test
  fun `unknown keys inside facebook block are ignored`() {
    val config =
      FacebookConfigResolver.resolve(
        """{"facebook":{"facebookAppId":"1234567890","futureKey":42}}""",
      )
    assertNotNull(config)
    assertEquals("1234567890", config!!.facebookAppId)
  }

  @Test
  fun `resolve returns null when the facebook block lacks the required app id`() {
    assertNull(FacebookConfigResolver.resolve("""{"facebook":{}}"""))
  }
}
