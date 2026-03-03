package io.capkit.fortress.impl

data class SessionState(
  val isLocked: Boolean,
  val lastActiveAt: Long,
)

class SessionManager {
  fun getSession(): SessionState =
    SessionState(
      isLocked = true,
      lastActiveAt = 0,
    )
}
