package io.capkit.privacyscreen.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * JVM unit tests for the native error model of the PrivacyScreen plugin.
 *
 * Errors are conveyed to JavaScript exclusively via message + code; these
 * tests lock the canonical message strings (which MUST stay identical to the
 * iOS `PrivacyScreenErrorMessages` enum) and the per-subclass message
 * passthrough.
 */
class PrivacyScreenErrorTest {
  // ---------------------------------------------------------------------------
  // Canonical messages (cross-platform parity lock)
  // ---------------------------------------------------------------------------

  @Test
  fun canonicalErrorMessagesMatchIOS() {
    assertEquals("Feature is unavailable on this device or configuration.", PrivacyScreenErrorMessages.UNAVAILABLE)
    assertEquals("Operation was cancelled.", PrivacyScreenErrorMessages.CANCELLED)
    assertEquals("Required permission is denied or not granted.", PrivacyScreenErrorMessages.PERMISSION_DENIED)
    assertEquals("Native initialization failed.", PrivacyScreenErrorMessages.INIT_FAILED)
    assertEquals("Invalid or missing input parameters.", PrivacyScreenErrorMessages.INVALID_INPUT)
    assertEquals("Unsupported or invalid input type.", PrivacyScreenErrorMessages.UNKNOWN_TYPE)
    assertEquals("Requested resource not found.", PrivacyScreenErrorMessages.NOT_FOUND)
    assertEquals("Operation conflict with current state.", PrivacyScreenErrorMessages.CONFLICT)
    assertEquals("Operation timed out.", PrivacyScreenErrorMessages.TIMEOUT)
    assertEquals("Internal error.", PrivacyScreenErrorMessages.INTERNAL_ERROR)
    assertEquals("Unexpected native error in plugin operation.", PrivacyScreenErrorMessages.UNEXPECTED_NATIVE_ERROR)
  }

  // ---------------------------------------------------------------------------
  // Subclass message passthrough
  // ---------------------------------------------------------------------------

  @Test
  fun errorSubclassesExposeTheirMessageThroughThrowable() {
    val message = "custom native failure detail"

    val errors: List<PrivacyScreenError> =
      listOf(
        PrivacyScreenError.Unavailable(message),
        PrivacyScreenError.Cancelled(message),
        PrivacyScreenError.PermissionDenied(message),
        PrivacyScreenError.InitFailed(message),
        PrivacyScreenError.InvalidInput(message),
        PrivacyScreenError.UnknownType(message),
        PrivacyScreenError.NotFound(message),
        PrivacyScreenError.Conflict(message),
        PrivacyScreenError.Timeout(message),
      )

    for (error in errors) {
      assertEquals(message, error.errorMessage)
      assertEquals(message, error.message)
      assertSame(error.errorMessage, error.message)
    }
  }
}
