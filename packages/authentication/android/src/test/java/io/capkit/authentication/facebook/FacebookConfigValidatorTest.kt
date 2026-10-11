package io.capkit.authentication.facebook

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file FacebookConfigValidatorTest.kt
 * RED-GREEN contract for the Facebook configuration validator:
 * absent `facebook` block → `INIT_FAILED` ("Missing 'facebook' configuration;
 * provide appId."); blank `facebookAppId` → `INVALID_INPUT` (`isBlank` parity);
 * client token optional but non-blank when present; scopes non-empty after the
 * DTO defaults are applied. Mirrors `AppleConfigValidator` / `GoogleConfigValidator`
 * semantics (configuration-integrity-guard).
 */
class FacebookConfigValidatorTest {
  @Test
  fun `validator rejects missing facebook config with INIT_FAILED`() {
    val error = FacebookConfigValidator.validate(null)
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InitFailed)
    assertEquals("Missing 'facebook' configuration; provide appId.", error!!.errorMessage)
  }

  @Test
  fun `validator rejects blank app id with INVALID_INPUT`() {
    val error = FacebookConfigValidator.validate(FacebookConfig(facebookAppId = "  "))
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `validator rejects blank client token with INVALID_INPUT`() {
    val error =
      FacebookConfigValidator.validate(
        FacebookConfig(facebookAppId = "1234567890", facebookClientToken = "   "),
      )
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `validator rejects empty scopes when explicitly provided`() {
    val error =
      FacebookConfigValidator.validate(
        FacebookConfig(facebookAppId = "1234567890", scopes = emptyList()),
      )
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `validator accepts a minimal config with only the app id`() {
    assertNull(FacebookConfigValidator.validate(FacebookConfig(facebookAppId = "1234567890")))
  }

  @Test
  fun `validator accepts a fully configured facebook config`() {
    assertNull(
      FacebookConfigValidator.validate(
        FacebookConfig(
          facebookAppId = "1234567890",
          facebookClientToken = "ct-1",
          facebookVersion = "v17.0",
          scopes = listOf("public_profile", "email"),
        ),
      ),
    )
  }
}
