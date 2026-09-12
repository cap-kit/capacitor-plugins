package io.capkit.authentication.google

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file GoogleConfigTest.kt
 * RED-GREEN contract for the `google` configuration sub-object resolved from the
 * plugin's raw Capacitor config JSON (config-integrity: native-only read, never
 * mutated at runtime, nothing leaked to JS).
 */
class GoogleConfigTest {
  private val json =
    kotlinx.serialization.json.Json {
      ignoreUnknownKeys = true
    }

  @Test
  fun `resolve applies default scopes, autoSelect false and null nonce when only client id given`() {
    val config =
      GoogleConfigResolver.resolve(
        """{"google":{"serverClientId":"123.apps.googleusercontent.com"}}""",
      )

    assertNotNull(config)
    config!!
    assertEquals("123.apps.googleusercontent.com", config.serverClientId)
    assertEquals(listOf("openid", "email", "profile"), config.scopes)
    assertEquals(false, config.autoSelect)
    assertNull(config.nonce)
  }

  @Test
  fun `resolve respects explicit scopes autoSelect and nonce`() {
    val config =
      GoogleConfigResolver.resolve(
        """{"google":{"serverClientId":"123.apps.googleusercontent.com","scopes":["email"],"autoSelect":true,"nonce":"n-1"}}""",
      )

    assertNotNull(config)
    config!!
    assertEquals(listOf("email"), config.scopes)
    assertEquals(true, config.autoSelect)
    assertEquals("n-1", config.nonce)
  }

  @Test
  fun `resolve returns null when google block is absent`() {
    assertNull(GoogleConfigResolver.resolve("""{"verboseLogging":true}"""))
    assertNull(GoogleConfigResolver.resolve("""{}"""))
  }

  @Test
  fun `resolve returns null on malformed top-level JSON`() {
    assertNull(GoogleConfigResolver.resolve("not-json"))
  }

  @Test
  fun `unknown keys inside google block are ignored`() {
    val config =
      GoogleConfigResolver.resolve(
        """{"google":{"serverClientId":"123.apps.googleusercontent.com","futureKey":42}}""",
      )
    assertNotNull(config)
    assertEquals("123.apps.googleusercontent.com", config!!.serverClientId)
  }

  @Test
  fun `validator rejects missing google config with INIT_FAILED`() {
    val error = GoogleConfigValidator.validate(null)
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InitFailed)
  }

  @Test
  fun `validator rejects blank client id with INVALID_INPUT`() {
    val error = GoogleConfigValidator.validate(GoogleConfig(serverClientId = "  "))
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `validator accepts a well formed google config`() {
    assertNull(GoogleConfigValidator.validate(GoogleConfig(serverClientId = "123.apps.googleusercontent.com")))
  }
}
