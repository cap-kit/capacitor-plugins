import Capacitor
import Foundation

/**
 Centralized native logger for the Fortress plugin.

 Responsibilities:
 - Provide a single logging entry point
 - Support runtime-controlled verbose logging
 - Keep logging behavior consistent across files

 Forbidden:
 - Controlling application logic
 - Being queried for flow decisions
 */
enum Logger {

    enum Level: Int {
        case error = 0
        case warn = 1
        case info = 2
        case debug = 3
        case verbose = 4
    }

    private static let lock = NSLock()

    nonisolated(unsafe) private static var rawVerbose: Bool = false
    nonisolated(unsafe) private static var rawLevel: Level = .info

    /**
     Controls whether debug logs are printed.

     This value MUST be set once during plugin initialization
     based on static configuration.
     */
    static var verbose: Bool {
        get {
            lock.lock()
            defer { lock.unlock() }
            return rawVerbose
        }
        set {
            lock.lock()
            rawVerbose = newValue
            rawLevel = newValue ? .debug : .info
            lock.unlock()
        }
    }

    static var level: Level {
        get {
            lock.lock()
            defer { lock.unlock() }
            return rawLevel
        }
        set {
            lock.lock()
            rawLevel = newValue
            rawVerbose = newValue.rawValue >= Level.debug.rawValue
            lock.unlock()
        }
    }

    static func setLevel(_ levelName: String?) {
        switch levelName?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "error":
            level = .error
        case "warn":
            level = .warn
        case "debug":
            level = .debug
        case "verbose":
            level = .verbose
        default:
            level = .info
        }
    }

    /**
     Prints a verbose / debug log message.

     Debug logs are automatically silenced
     when `verbose` is false.
     */
    static func debug(_ items: Any...) {
        guard level.rawValue >= Level.debug.rawValue else { return }
        log(items)
    }

    /**
     Prints an error log message.

     Error logs are always printed regardless
     of the verbose flag.
     */
    static func error(_ items: Any...) {
        guard level.rawValue >= Level.error.rawValue else { return }
        log(items)
    }

    /**
     Prints an info log message.

     Info logs are always printed regardless
     of the verbose flag.
     */
    static func info(_ items: Any...) {
        guard level.rawValue >= Level.info.rawValue else { return }
        log(items)
    }

    static func warn(_ items: Any...) {
        guard level.rawValue >= Level.warn.rawValue else { return }
        log(items)
    }
}

// MARK: - Internal log printer

/**
 Low-level log printer with a consistent prefix.

 This function MUST NOT be used outside this file.
 */
private func log(
    _ items: [Any],
    separator: String = " ",
    terminator: String = "\n"
) {
    CAPLog.print("⚡️ Fortress -", terminator: separator)

    for (index, item) in items.enumerated() {
        CAPLog.print(
            item,
            terminator: index == items.count - 1
                ? terminator
                : separator
        )
    }
}
