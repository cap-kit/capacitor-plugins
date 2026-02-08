import Foundation

/**
 Runtime integrity checks related to debugging
 and instrumentation frameworks.
 */
struct IntegrityRuntimeChecks {

    static func debugSignals(
        includeDebug: Bool
    ) -> [[String: Any]] {

        // Collected runtime debug-related signals.
        var signals: [[String: Any]] = []

        // 1. Check if a debugger is attached via sysctl
        var info = kinfo_proc()
        var size = MemoryLayout.size(ofValue: info)
        var mib: [Int32] = [
            CTL_KERN,
            KERN_PROC,
            KERN_PROC_PID,
            getpid()
        ]

        if sysctl(&mib, UInt32(mib.count), &info, &size, nil, 0) == 0 {
            if (info.kp_proc.p_flag & P_TRACED) != 0 {
                signals.append([
                    "id": "ios_debugger_attached",
                    "category": "debug",
                    "confidence": "high",
                    "description": includeDebug
                        ? "A debugger is currently attached to the running process"
                        : nil,
                    "metadata": ["method": "sysctl_p_traced"]
                ].compactMapValues { $0 })
            }
        }

        // 2. Check for development/debug provisioning profile
        // This mirrors the Android FLAG_DEBUGGABLE check.
        if let path = Bundle.main.path(forResource: "embedded", ofType: "mobileprovision") {
            do {
                let profileContent = try String(contentsOfFile: path, encoding: .ascii)
                if profileContent.contains("<key>get-task-allow</key>\n\t\t<true/>") {
                    signals.append([
                        "id": "ios_runtime_debuggable",
                        "category": "debug",
                        "confidence": "medium",
                        "description": includeDebug
                            ? "Application is signed with a profile that allows debugging"
                            : nil,
                        "metadata": ["entitlement": "get-task-allow"]
                    ].compactMapValues { $0 })
                }
            } catch {
                // Ignore errors reading the profile
            }
        }

        return signals
    }
}
