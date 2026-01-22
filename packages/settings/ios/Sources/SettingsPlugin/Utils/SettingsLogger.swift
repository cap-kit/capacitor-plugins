import Capacitor

/**
 Centralized logger for the Settings plugin.

 This logger mirrors the Android Logger pattern and provides:
 - a single logging entry point
 - runtime-controlled verbose logging
 - consistent log formatting

 Business logic MUST NOT perform configuration checks directly.
 */
enum SettingsLogger {

    /**
     Controls whether debug logs are printed.

     This value is set once during plugin load
     based on configuration values.
     */
    static var verbose: Bool = false

    /**
     Prints a verbose / debug log message.

     This method is intended for development-time diagnostics
     and is automatically silenced when verbose logging is disabled.
     */
    static func debug(_ items: Any...) {
        guard verbose else { return }
        log(items)
    }

    /**
     Prints an error log message.

     Error logs are always printed regardless of verbosity.
     */
    static func error(_ items: Any...) {
        log(items)
    }
}

/**
 Low-level log printer with a consistent prefix.

 This function should not be used directly outside this file.
 */
private func log(_ items: Any..., separator: String = " ", terminator: String = "\n") {
    CAPLog.print("⚡️  Settings -", terminator: separator)
    for (itemIndex, item) in items.enumerated() {
        CAPLog.print(
            item,
            terminator: itemIndex == items.count - 1 ? terminator : separator
        )
    }
}
