package io.capkit.authentication.google

import java.util.concurrent.atomic.AtomicBoolean

/**
 * @file SignInGate.kt
 * Single-flight gate for native sign-in (web single-flight parity): only one
 * interactive credential flow may run at a time; overlapping calls are rejected by
 * the caller with CONFLICT. Thread-safe via [AtomicBoolean].
 */
class SignInGate {
  private val inFlight = AtomicBoolean(false)

  /**
   * @return `true` and marks the gate busy when no flow is running; `false` when
   *         a sign-in is already in flight.
   */
  fun tryAcquire(): Boolean = inFlight.compareAndSet(false, true)

  /** Releases the gate so a new sign-in can start. */
  fun release() {
    inFlight.set(false)
  }
}
