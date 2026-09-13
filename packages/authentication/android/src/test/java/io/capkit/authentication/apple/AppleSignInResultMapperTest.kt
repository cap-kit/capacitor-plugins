package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file AppleSignInResultMapperTest.kt
 * RED-GREEN contract for mapping the [AppleSignInActivity] result surfaced through
 * the bridge `@ActivityCallback` into a typed sign-in outcome for the
 * implementation layer. The mapper composes the existing
 * pure classes — [FormPostParser], [StateNonceValidator], [AppleErrorMapper] —
 * and maps cancellation to `USER_CANCELLED` with no new error codes.
 * Pure, no Android runtime deps.
 */
class AppleSignInResultMapperTest {
  @Test
  fun `maps a successful result code with a valid payload to success`() {
    val payload =
      FormPostValueDecoder.toFormPostJson(
        mapOf(
          "code" to "auth-code-1",
          "id_token" to "header.payload.signature",
          "state" to "st-1",
        ),
      )

    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_OK,
        payload = payload,
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.Success)
    val success = outcome as AppleSignInResultMapper.Outcome.Success
    assertEquals("auth-code-1", success.authorizationCode)
    assertEquals("header.payload.signature", success.idToken)
    assertEquals("st-1", success.state)
  }

  @Test
  fun `maps a successful result with a user payload to a user in the outcome`() {
    val payload =
      FormPostValueDecoder.toFormPostJson(
        mapOf(
          "code" to "auth-code-1",
          "state" to "st-1",
          "user" to """{"email":"ada@example.com","name":{"firstName":"Ada","lastName":"Lovelace"}}""",
        ),
      )

    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_OK,
        payload = payload,
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.Success)
    val success = outcome as AppleSignInResultMapper.Outcome.Success
    assertEquals("ada@example.com", success.user?.email)
    assertEquals(
      "first name present",
      "Ada",
      (success.user?.name as? io.capkit.authentication.model.NameParts)?.firstName,
    )
  }

  @Test
  fun `rejects a state mismatch with INVALID_INPUT`() {
    val payload =
      FormPostValueDecoder.toFormPostJson(
        mapOf("code" to "auth-code-1", "state" to "st-2"),
      )

    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_OK,
        payload = payload,
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.Failed)
    val failed = outcome as AppleSignInResultMapper.Outcome.Failed
    assertTrue(failed.error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `rejects a nonce mismatch with INVALID_INPUT`() {
    val payload =
      FormPostValueDecoder.toFormPostJson(
        mapOf("code" to "auth-code-1", "state" to "st-1"),
      )

    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_OK,
        payload = payload,
        expectedState = "st-1",
        expectedNonce = "nonce-expected",
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.Failed)
    val failed = outcome as AppleSignInResultMapper.Outcome.Failed
    assertTrue(failed.error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `maps a malformed payload to a failed outcome without crashing`() {
    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_OK,
        payload = "not-json",
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.Failed)
  }

  @Test
  fun `maps cancellation with no payload to a user cancelled outcome`() {
    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_CANCELED,
        payload = null,
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.UserCancelled)
  }

  @Test
  fun `maps cancellation carrying an error to the mapped failure`() {
    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_CANCELED,
        payload = null,
        errorCode = "invalid_nonce",
        errorMessage = "Nonce did not match",
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.Failed)
    val failed = outcome as AppleSignInResultMapper.Outcome.Failed
    assertTrue(failed.error is AuthenticationError.InvalidInput)
  }

  @Test
  fun `maps a cancellation error surfaced by the sdk to user cancelled`() {
    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_CANCELED,
        payload = null,
        errorCode = "user_cancelled_authorize",
        errorMessage = "The user closed the window",
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.UserCancelled)
  }

  @Test
  fun `maps an unexpected result code as user cancelled`() {
    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = 42,
        payload = null,
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.UserCancelled)
  }

  @Test
  fun `keeps an absent optional user on the success outcome when omitted`() {
    val payload =
      FormPostValueDecoder.toFormPostJson(
        mapOf("code" to "auth-code-1", "state" to "st-1"),
      )

    val outcome =
      AppleSignInResultMapper.mapResult(
        resultCode = AppleSignInResultMapper.RESULT_OK,
        payload = payload,
        expectedState = "st-1",
        expectedNonce = null,
      )

    assertTrue(outcome is AppleSignInResultMapper.Outcome.Success)
    assertNull((outcome as AppleSignInResultMapper.Outcome.Success).user)
  }

  @Test
  fun `exposes the expected activity result code constants`() {
    // Android contract: RESULT_OK==-1, RESULT_CANCELED==0 (android.app.Activity).
    assertEquals(-1, AppleSignInResultMapper.RESULT_OK)
    assertEquals(0, AppleSignInResultMapper.RESULT_CANCELED)
    assertNotNull(AppleSignInResultMapper.Outcome.Success::class)
  }
}
