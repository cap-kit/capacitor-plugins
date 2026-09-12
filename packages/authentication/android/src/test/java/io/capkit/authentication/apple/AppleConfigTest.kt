package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file AppleConfigTest.kt
 * RED-GREEN contract for the `apple` configuration sub-object resolved from the
 * plugin's raw Capacitor config JSON (config-integrity: native-only read, per-provider
 * block, absent/malformed treated as an initialization failure).
 * Mirrors the Google `GoogleConfigResolver`/`GoogleConfigValidator`
 * conventions.
 */
class AppleConfigTest {
  @Test
  fun `resolve applies default scopes name email and null nonce state when only client id given`() {
    val config =
      AppleConfigResolver.resolve(
        """{"apple":{"clientId":"com.example.service"}}""",
      )

    assertNotNull(config)
    config!!
    assertEquals("com.example.service", config.clientId)
    assertEquals(listOf("name", "email"), config.scopes)
    assertNull(config.nonce)
    assertNull(config.state)
  }

  @Test
  fun `resolve respects explicit scopes nonce and state`() {
    val config =
      AppleConfigResolver.resolve(
        """{"apple":{"clientId":"com.example.service","scopes":["email"],"nonce":"n-1","state":"s-1"}}""",
      )

    assertNotNull(config)
    config!!
    assertEquals(listOf("email"), config.scopes)
    assertEquals("n-1", config.nonce)
    assertEquals("s-1", config.state)
  }

  @Test
  fun `resolve reads redirectURI when provided`() {
    val config =
      AppleConfigResolver.resolve(
        """{"apple":{"clientId":"com.example.service","redirectURI":"https://app.example.com/oauth/callback"}}""",
      )

    assertNotNull(config)
    assertEquals("https://app.example.com/oauth/callback", config!!.redirectURI)
  }

  @Test
  fun `resolve returns null when apple block is absent`() {
    assertNull(AppleConfigResolver.resolve("""{"verboseLogging":true}"""))
    assertNull(AppleConfigResolver.resolve("""{}"""))
  }

  @Test
  fun `resolve returns null on malformed top-level JSON`() {
    assertNull(AppleConfigResolver.resolve("not-json"))
  }

  @Test
  fun `unknown keys inside apple block are ignored`() {
    val config =
      AppleConfigResolver.resolve(
        """{"apple":{"clientId":"com.example.service","futureKey":42}}""",
      )
    assertNotNull(config)
    assertEquals("com.example.service", config!!.clientId)
  }

  @Test
  fun `validator rejects missing apple config with INIT_FAILED`() {
    val error = AppleConfigValidator.validate(null)
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InitFailed)
  }

  @Test
  fun `validator rejects blank client id with INVALID_INPUT`() {
    val error = AppleConfigValidator.validate(AppleConfig(clientId = "  "))
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `validator rejects blank redirect uri with INVALID_INPUT`() {
    val error = AppleConfigValidator.validate(AppleConfig(clientId = "com.example.service", redirectURI = "  "))
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `validator accepts a well formed apple config`() {
    assertNull(
      AppleConfigValidator.validate(
        AppleConfig(clientId = "com.example.service", redirectURI = "https://app.example.com/oauth/callback"),
      ),
    )
  }
}
