package io.capkit.splashmotion.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * JVM unit tests for the native error model of the SplashMotion plugin.
 *
 * Errors are conveyed to JavaScript exclusively via message + code; these
 * tests lock the canonical message strings (which MUST stay identical to the
 * iOS `SplashMotionErrorMessages` enum) and the per-subclass message
 * passthrough.
 */
class SplashMotionErrorTest {
  // ---------------------------------------------------------------------------
  // Canonical messages (cross-platform parity lock)
  // ---------------------------------------------------------------------------

  @Test
  fun canonicalErrorMessagesMatchIOS() {
    assertEquals("Feature is unavailable on this device or configuration.", SplashMotionErrorMessages.UNAVAILABLE)
    assertEquals("Operation was cancelled.", SplashMotionErrorMessages.CANCELLED)
    assertEquals("Required permission is denied or not granted.", SplashMotionErrorMessages.PERMISSION_DENIED)
    assertEquals("Native initialization failed.", SplashMotionErrorMessages.INIT_FAILED)
    assertEquals("Invalid or missing input parameters.", SplashMotionErrorMessages.INVALID_INPUT)
    assertEquals("Unsupported or invalid input type.", SplashMotionErrorMessages.UNKNOWN_TYPE)
    assertEquals("Requested resource not found.", SplashMotionErrorMessages.NOT_FOUND)
    assertEquals("Operation conflict with current state.", SplashMotionErrorMessages.CONFLICT)
    assertEquals("Operation timed out.", SplashMotionErrorMessages.TIMEOUT)
    assertEquals("Internal error.", SplashMotionErrorMessages.INTERNAL_ERROR)
    assertEquals("Unexpected native error in plugin operation.", SplashMotionErrorMessages.UNEXPECTED_NATIVE_ERROR)
  }

  // ---------------------------------------------------------------------------
  // Subclass message passthrough
  // ---------------------------------------------------------------------------

  @Test
  fun errorSubclassesExposeTheirMessageThroughThrowable() {
    val message = "custom native failure detail"

    val errors: List<SplashMotionError> =
      listOf(
        SplashMotionError.Unavailable(message),
        SplashMotionError.Cancelled(message),
        SplashMotionError.PermissionDenied(message),
        SplashMotionError.InitFailed(message),
        SplashMotionError.InvalidInput(message),
        SplashMotionError.UnknownType(message),
        SplashMotionError.NotFound(message),
        SplashMotionError.Conflict(message),
        SplashMotionError.Timeout(message),
      )

    for (error in errors) {
      assertEquals(message, error.errorMessage)
      assertEquals(message, error.message)
      assertSame(error.errorMessage, error.message)
    }
  }
}
