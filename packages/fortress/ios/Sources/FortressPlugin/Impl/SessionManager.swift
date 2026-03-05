import Foundation

/**
 Thread-safe session manager.

 Responsibilities:
 - Maintain lock state
 - Track last activity timestamp
 - Provide atomic state transitions

 This class contains NO Capacitor references and NO side effects.
 */
final class SessionManager {

    private static let touchThrottleMs: Int64 = 1_000

    private enum VaultState {
        case locked
        case unlocking
        case unlocked
        case expired
    }

    struct SessionState {
        let isLocked: Bool
        let lastActiveAt: Int64
    }

    private let stateQueue = DispatchQueue(label: "io.capkit.fortress.session")

    // Queue-specific marker to avoid deadlocks on re-entrant sync calls.
    private let queueKey = DispatchSpecificKey<Void>()

    private var _state: VaultState = .locked
    private var _lastActiveAt: Int64 = 0
    private var _lastTouchAt: Int64 = 0
    private var _backgroundTimestamp: Int64 = 0
    var onLockStatusChanged: ((Bool) -> Void)?

    private func isLockedState(_ state: VaultState) -> Bool {
        state != .unlocked
    }

    init() {
        stateQueue.setSpecific(key: queueKey, value: ())
    }

    private func sync<T>(_ block: () -> T) -> T {
        // If we're already on stateQueue, run inline to avoid deadlock.
        if DispatchQueue.getSpecific(key: queueKey) != nil {
            return block()
        }
        return stateQueue.sync(execute: block)
    }

    private func nowMs() -> Int64 {
        SessionManager.currentTimeMillis()
    }

    func getSession() -> SessionState {
        return sync {
            SessionState(
                isLocked: isLockedState(_state),
                lastActiveAt: _lastActiveAt
            )
        }
    }

    func unlock() {
        var shouldNotify = false
        sync {
            shouldNotify = isLockedState(_state)
            _state = .unlocking
            _state = .unlocked
            _lastActiveAt = nowMs()
            _lastTouchAt = _lastActiveAt
        }

        if shouldNotify {
            onLockStatusChanged?(false)
        }
    }

    func lock() {
        var shouldNotify = false
        sync {
            shouldNotify = !isLockedState(_state)
            _state = .locked
            _lastActiveAt = 0
            _lastTouchAt = 0
            // Explicitly invalidate background timestamp so a later touch
            // cannot resurrect an expired session.
            _backgroundTimestamp = 0
        }

        if shouldNotify {
            onLockStatusChanged?(true)
        }
    }

    func setBackgroundTimestamp() {
        sync { _backgroundTimestamp = SessionManager.currentTimeMillis() }
    }

    func evaluateBackgroundGracePeriod(lockAfterMs: Int64) {
        let now = SessionManager.currentTimeMillis()
        var notify = false

        sync {
            if !isLockedState(_state) && _backgroundTimestamp > 0 {
                let elapsed = now - _backgroundTimestamp
                if elapsed < 0 || elapsed > lockAfterMs {
                    _state = .expired
                    _lastActiveAt = 0
                    _lastTouchAt = 0
                    notify = true
                }
            }
            _backgroundTimestamp = 0
        }

        if notify { onLockStatusChanged?(true) }
    }

    func touch() {
        sync {
            if !isLockedState(_state) {
                let now = nowMs()
                if (now - _lastTouchAt) < SessionManager.touchThrottleMs {
                    return
                }
                _lastTouchAt = now
                _lastActiveAt = now
            }
        }
    }

    func reset() {
        var shouldNotify = false
        sync {
            shouldNotify = !isLockedState(_state)
            _state = .locked
            _lastActiveAt = 0
            _lastTouchAt = 0
            _backgroundTimestamp = 0
        }

        if shouldNotify {
            onLockStatusChanged?(true)
        }
    }

    func isLocked() -> Bool {
        return sync { isLockedState(_state) }
    }

    /**
     Atomically evaluates expiration and locks if needed.

     - Parameters:
     - lockAfterMs: Session timeout in milliseconds.
     - nowMs: The current time in milliseconds (injectable for deterministic tests).
     - Returns: The effective locked state after evaluation.
     */
    func evaluateLockState(lockAfterMs: Int64, nowMs: Int64 = SessionManager.currentTimeMillis()) -> Bool {
        var shouldNotify = false
        let locked = sync {
            // Fail-safe: non-positive timeout means "immediately locked".
            if lockAfterMs <= 0 {
                shouldNotify = !isLockedState(_state)
                _state = .locked
                _lastActiveAt = 0
                _lastTouchAt = 0
                return true
            }

            // Fail-safe: unlocked state with zero activity timestamp is inconsistent.
            if !isLockedState(_state) && _lastActiveAt == 0 {
                _state = .expired
                _lastTouchAt = 0
                shouldNotify = true
                return true
            }

            let isExpired = (nowMs - _lastActiveAt) > lockAfterMs
            let isClockSkewed = nowMs < _lastActiveAt

            if !isLockedState(_state) && (isExpired || isClockSkewed) {
                _state = .expired
                _lastActiveAt = 0
                _lastTouchAt = 0
                shouldNotify = true
            }

            return isLockedState(_state)
        }

        if shouldNotify {
            onLockStatusChanged?(true)
        }

        return locked
    }

    private static func currentTimeMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
