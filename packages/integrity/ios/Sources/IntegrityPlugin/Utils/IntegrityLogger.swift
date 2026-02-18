import Foundation

enum IntegrityLogger {
    static var verbose = false

    static func debug(_ items: Any...) {
        guard verbose else { return }
        let message = items.map { String(describing: $0) }.joined(separator: " ")
        print("[Integrity][DEBUG] \(message)")
    }

    static func error(_ items: Any...) {
        let message = items.map { String(describing: $0) }.joined(separator: " ")
        print("[Integrity][ERROR] \(message)")
    }
}
