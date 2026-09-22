package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file StateNonceValidatorTest.kt
 * RED-GREEN contract for OAuth state and OpenID nonce validation:
 * a mismatched `state` or `nonce` claim must map
 * to the plugin `INVALID_INPUT` error, never to any other code.
 */
class StateNonceValidatorTest {
  @Test
  fun `accepts matching state and nonce`() {
    assertNull(StateNonceValidator.validate(state = "st-1", expectedState = "st-1"))
    assertNull(
      StateNonceValidator.validate(
        state = "st-1",
        expectedState = "st-1",
        nonce = "nc-1",
        expectedNonce = "nc-1",
      ),
    )
  }

  @Test
  fun `rejects state mismatch with INVALID_INPUT`() {
    val error = StateNonceValidator.validate(state = "st-other", expectedState = "st-1")
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `rejects missing state when state was expected with INVALID_INPUT`() {
    val error = StateNonceValidator.validate(state = null, expectedState = "st-1")
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `rejects nonce mismatch with INVALID_INPUT`() {
    val error =
      StateNonceValidator.validate(
        state = "st-1",
        expectedState = "st-1",
        nonce = "nc-other",
        expectedNonce = "nc-1",
      )
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `rejects missing nonce claim when nonce was expected with INVALID_INPUT`() {
    val error =
      StateNonceValidator.validate(
        state = "st-1",
        expectedState = "st-1",
        nonce = null,
        expectedNonce = "nc-1",
      )
    assertNotNull(error)
    assertTrue(error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `error message mentions the mismatched parameter`() {
    val stateError = StateNonceValidator.validate(state = "st-other", expectedState = "st-1")
    assertNotNull(stateError)
    assertTrue(stateError!!.errorMessage.contains("state"))
    val nonceError =
      StateNonceValidator.validate(
        state = "st-1",
        expectedState = "st-1",
        nonce = "nc-other",
        expectedNonce = "nc-1",
      )
    assertNotNull(nonceError)
    assertTrue(nonceError!!.errorMessage.contains("nonce"))
  }

  @Test
  fun `state and nonce are independently validated`() {
    val nonceOnly =
      StateNonceValidator.validate(
        expectedState = "st-1",
        expectedNonce = "nc-1",
        state = "st-1",
        nonce = "nc-other",
      )
    val stateOnly =
      StateNonceValidator.validate(
        expectedState = "st-1",
        expectedNonce = "nc-1",
        state = "st-other",
        nonce = "nc-1",
      )
    assertEquals(
      "INVALID_INPUT",
      io.capkit.authentication.utils.ErrorCodeMapper
        .mapCode(nonceOnly!!),
    )
    assertEquals(
      "INVALID_INPUT",
      io.capkit.authentication.utils.ErrorCodeMapper
        .mapCode(stateOnly!!),
    )
  }
}
