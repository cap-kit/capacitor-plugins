import Foundation

final class SessionManager {
    private var isLocked = true
    private var lastActiveAt: Int64 = 0

    func getSession() -> Fortress.SessionState {
        Fortress.SessionState(isLocked: isLocked, lastActiveAt: lastActiveAt)
    }
}
