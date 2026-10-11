package io.capkit.authentication.google

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file SignInGateTest.kt
 * RED-GREEN single-flight gate for native sign-in: overlapping calls are rejected
 * with CONFLICT (web single-flight parity), never executed concurrently.
 */
class SignInGateTest {
  @Test
  fun `first caller acquires the gate`() {
    val gate = SignInGate()
    assertTrue(gate.tryAcquire())
    gate.release()
  }

  @Test
  fun `second caller is rejected while sign-in is in flight`() {
    val gate = SignInGate()
    assertTrue(gate.tryAcquire())
    assertFalse(gate.tryAcquire())
    assertFalse(gate.tryAcquire())
  }

  @Test
  fun `gate opens again after release`() {
    val gate = SignInGate()
    assertTrue(gate.tryAcquire())
    gate.release()
    assertTrue(gate.tryAcquire())
    gate.release()
  }
}
