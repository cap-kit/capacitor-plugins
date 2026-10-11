package io.capkit.authentication.restore

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file RestoreRequestValidatorTest.kt
 * RED-GREEN contract for createRestoreCredential input: `requestJson` must be a
 * non-blank JSON object (WebAuthn-style); everything else is INVALID_INPUT.
 */
class RestoreRequestValidatorTest {
  @Test
  fun `accepts a well formed JSON object request`() {
    assertNull(RestoreRequestValidator.validate("""{"challenge":"abc","rpId":"example.com"}"""))
  }

  @Test
  fun `rejects null or blank requestJson`() {
    assertRejects(RestoreRequestValidator.validate(null))
    assertRejects(RestoreRequestValidator.validate(""))
    assertRejects(RestoreRequestValidator.validate("   "))
  }

  @Test
  fun `rejects malformed JSON`() {
    assertRejects(RestoreRequestValidator.validate("not-json"))
    assertRejects(RestoreRequestValidator.validate("""{"challenge":}"""))
  }

  @Test
  fun `rejects valid JSON that is not an object`() {
    assertRejects(RestoreRequestValidator.validate("""[1,2,3]"""))
    assertRejects(RestoreRequestValidator.validate(""""string""""))
  }

  private fun assertRejects(error: AuthenticationError?) {
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }
}
