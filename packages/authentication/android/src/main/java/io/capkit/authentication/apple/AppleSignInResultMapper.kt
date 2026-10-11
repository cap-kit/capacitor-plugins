package io.capkit.authentication.apple

import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.model.SocialAuthResultUser

/**
 * @file AppleSignInResultMapper.kt
 * Pure mapping of the [AppleSignInActivity] result — surfaced through the
 * plugin's `@ActivityCallback` — into a typed sign-in outcome for the
 * implementation layer.
 *
 * The mapper composes the existing pure classes only: [FormPostParser] to parse
 * the callback payload, [JwtDecoder] to extract the `nonce` claim from the
 * `id_token`, [StateNonceValidator] to reject state/nonce mismatches, and
 * [AppleErrorMapper] to classify SDK errors — so no new error codes exist and
 * cancellation always maps to `USER_CANCELLED`. Pure and
 * side-effect free.
 */
object AppleSignInResultMapper {
  /** Mirrors `android.app.Activity.RESULT_OK` without an Android runtime dep. */
  const val RESULT_OK = -1

  /** Mirrors `android.app.Activity.RESULT_CANCELED` without an Android runtime dep. */
  const val RESULT_CANCELED = 0

  /** Typed outcome of an Apple sign-in Activity result. */
  sealed class Outcome {
    /** The WebView delivered (and validated) a form_post credential. */
    data class Success(
      val authorizationCode: String?,
      val idToken: String?,
      val state: String?,
      val user: SocialAuthResultUser?,
    ) : Outcome()

    /** The flow failed with a classified [AuthenticationError]. */
    data class Failed(
      val error: AuthenticationError,
    ) : Outcome()

    /** The user cancelled the interactive flow (→ `USER_CANCELLED`). */
    object UserCancelled : Outcome()
  }

  /**
   * Maps an Activity result to a typed [Outcome].
   *
   * @param resultCode the Activity result code from the callback.
   * @param payload the decoded form_post JSON body (see [FormPostValueDecoder]),
   *        or `null` when the Activity returned no credential.
   * @param expectedState the `state` issued in the authorization URL.
   * @param expectedNonce the `nonce` issued in the authorization URL; `null`
   *        when the configuration did not issue one.
   * @param errorCode the raw SDK error code, when the Activity failed with one.
   * @param errorMessage the raw SDK error message, when one was surfaced.
   */
  fun mapResult(
    resultCode: Int,
    payload: String?,
    expectedState: String,
    expectedNonce: String? = null,
    errorCode: String? = null,
    errorMessage: String? = null,
  ): Outcome {
    if (resultCode != RESULT_OK) {
      return mapFailure(errorCode, errorMessage)
    }

    val parsed =
      FormPostParser.parse(payload.orEmpty()) ?: return Outcome.Failed(
        AuthenticationError.InvalidInput("The Apple sign-in returned a malformed form_post payload."),
      )

    val nonce = JwtDecoder.decode(parsed.idToken.orEmpty())?.nonce
    StateNonceValidator
      .validate(
        state = parsed.state,
        expectedState = expectedState,
        nonce = nonce,
        expectedNonce = expectedNonce,
      )?.let { return Outcome.Failed(it) }

    return Outcome.Success(
      authorizationCode = parsed.authorizationCode,
      idToken = parsed.idToken,
      state = parsed.state,
      user = parsed.user,
    )
  }

  private fun mapFailure(
    errorCode: String?,
    errorMessage: String?,
  ): Outcome {
    if (errorCode == null) return Outcome.UserCancelled
    val mapped = AppleErrorMapper.mapSdkError(errorCode, errorMessage)
    return if (mapped is AuthenticationError.UserCancelled) {
      Outcome.UserCancelled
    } else {
      Outcome.Failed(mapped)
    }
  }
}
