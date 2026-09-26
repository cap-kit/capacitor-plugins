import Capacitor

/**
 Centralized native logger for the Rank plugin.

 Responsibilities:
 - Provide a single logging entry point
 - Support runtime-controlled verbose logging
 - Keep logging behavior consistent across files

 Forbidden:
 - Controlling application logic
 - Being queried for flow decisions
 */
enum RankLogger {

    /**
     Controls whether debug logs are printed.

     This value MUST be set once during plugin initialization
     based on static configuration.
     */
    static var verbose: Bool = false

    /**
     Prints a verbose / debug log message.

     Debug logs are automatically silenced
     when `verbose` is false.
     */
    static func debug(_ items: Any...) {
        guard verbose else { return }
        log(items)
    }

    /**
     Prints an error log message.

     Error logs are always printed regardless
     of the verbose flag.
     */
    static func error(_ items: Any...) {
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
    CAPLog.print("⚡️ Rank -", terminator: separator)

    for (index, item) in items.enumerated() {
        CAPLog.print(
            item,
            terminator: index == items.count - 1
                ? terminator
                : separator
        )
    }
}
