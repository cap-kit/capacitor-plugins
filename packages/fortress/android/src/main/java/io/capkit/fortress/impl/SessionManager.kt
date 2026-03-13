package io.capkit.fortress.impl

data class SessionState(
  val isLocked: Boolean,
  val lastActiveAt: Long,
)

/**
 * Thread-safe session manager.
 *
 * Responsibilities:
 * - Maintain lock state
 * - Track last activity timestamp
 * - Provide atomic state transitions
 *
 * This class contains NO Capacitor references and NO side effects.
 */
class SessionManager {
  companion object {
    private const val TOUCH_THROTTLE_MS = 1000L
  }

  private val lock = Any()

  private enum class VaultState {
    LOCKED,
    UNLOCKING,
    UNLOCKED,
    EXPIRED,
  }

  private var state: VaultState = VaultState.LOCKED
  private var lastActiveAt: Long = 0L
  private var lastTouchAt: Long = 0L

  // Timestamp to track background duration
  private var backgroundTimestamp: Long = 0L

  // Callback for the Plugin layer
  var onLockStatusChanged: ((Boolean) -> Unit)? = null

  private fun isLockedState(currentState: VaultState): Boolean = currentState != VaultState.UNLOCKED

  /**
   * Phase 4: Record background entry
   */
  fun setBackgroundTimestamp() {
    synchronized(lock) {
      backgroundTimestamp = System.currentTimeMillis()
    }
  }

  /**
   * Phase 4: Evaluate if the app was in background longer than timeout
   */
  fun evaluateBackgroundGracePeriod(lockAfterMs: Long) {
    val currentTime = System.currentTimeMillis()
    var shouldNotify = false

    synchronized(lock) {
      if (!isLockedState(state) && backgroundTimestamp > 0) {
        val elapsed = currentTime - backgroundTimestamp
        if (elapsed < 0 || elapsed > lockAfterMs) {
          state = VaultState.EXPIRED
          lastActiveAt = 0L
          lastTouchAt = 0L
          shouldNotify = true
        }
      }
      backgroundTimestamp = 0L
    }

    if (shouldNotify) {
      onLockStatusChanged?.invoke(true)
    }
  }

  /**
   * Returns a snapshot of the current session state.
   * Atomic read.
   */
  fun getSession(): SessionState {
    synchronized(lock) {
      return SessionState(
        isLocked = isLockedState(state),
        lastActiveAt = lastActiveAt,
      )
    }
  }

  /**
   * Unlocks the vault and updates activity timestamp.
   * Atomic write.
   */
  fun unlock() {
    var shouldNotify = false
    synchronized(lock) {
      shouldNotify = isLockedState(state)
      state = VaultState.UNLOCKING
      state = VaultState.UNLOCKED
      lastActiveAt = System.currentTimeMillis()
      lastTouchAt = lastActiveAt
    }

    if (shouldNotify) {
      onLockStatusChanged?.invoke(false)
    }
  }

  /**
   * Forces locked state.
   * Atomic write.
   */
  fun lock() {
    var shouldNotify = false
    synchronized(lock) {
      shouldNotify = !isLockedState(state)
      state = VaultState.LOCKED
      lastActiveAt = 0L
      lastTouchAt = 0L
      backgroundTimestamp = 0L
    }

    if (shouldNotify) {
      onLockStatusChanged?.invoke(true)
    }
  }

  /**
   * Updates activity timestamp only if unlocked.
   * Atomic write.
   */
  fun touch() {
    synchronized(lock) {
      if (!isLockedState(state)) {
        val now = System.currentTimeMillis()
        if (now - lastTouchAt < TOUCH_THROTTLE_MS) {
          return
        }
        lastTouchAt = now
        lastActiveAt = now
      }
    }
  }

  /**
   * Resets the session state.
   *
   * Forces vault into locked state
   * and clears last activity timestamp.
   *
   * Atomic write.
   */
  fun reset() {
    var shouldNotify = false
    synchronized(lock) {
      shouldNotify = !isLockedState(state)
      state = VaultState.LOCKED
      lastActiveAt = 0L
      lastTouchAt = 0L
      backgroundTimestamp = 0L
    }

    if (shouldNotify) {
      onLockStatusChanged?.invoke(true)
    }
  }

  /**
   * Pure read — no side effects.
   */
  fun isLocked(): Boolean {
    synchronized(lock) {
      return isLockedState(state)
    }
  }

  fun evaluateLockState(lockAfterMs: Long): Boolean {
    val now = System.currentTimeMillis()
    var shouldNotify = false

    val locked =
      synchronized(lock) {
        if (lockAfterMs <= 0) {
          shouldNotify = !isLockedState(state)
          state = VaultState.LOCKED
          lastActiveAt = 0L
          lastTouchAt = 0L
          return@synchronized true
        }

        if (!isLockedState(state) && lastActiveAt == 0L) {
          state = VaultState.EXPIRED
          lastTouchAt = 0L
          shouldNotify = true
          return@synchronized true
        }

        val isExpired = !isLockedState(state) && ((now - lastActiveAt) > lockAfterMs)
        val isClockSkewed = !isLockedState(state) && now < lastActiveAt

        if (isExpired || isClockSkewed) {
          state = VaultState.EXPIRED
          lastActiveAt = 0L
          lastTouchAt = 0L
          shouldNotify = true
        }

        isLockedState(state)
      }

    if (shouldNotify) {
      onLockStatusChanged?.invoke(true)
    }

    return locked
  }
}
