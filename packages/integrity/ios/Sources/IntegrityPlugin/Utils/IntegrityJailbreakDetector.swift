import Foundation

/**
 Performs filesystem-based jailbreak detection.
 All checks are synchronous and deterministic.
 */
struct IntegrityJailbreakDetector {

    /**
     Performs filesystem-based jailbreak detection.

     This detector relies exclusively on the presence of well-known
     filesystem paths commonly introduced by jailbreak tools.

     Characteristics:
     - Deterministic
     - Synchronous
     - No side effects
     - Safe to execute at application boot time

     IMPORTANT:
     - This detector intentionally emits at most ONE signal.
     - The first matching path short-circuits further checks
     to minimize noise and overhead.
     */
    static func detect(
        includeDebug: Bool
    ) -> [[String: Any]] {

        // Well-known filesystem paths associated with common jailbreaks.
        // This list is intentionally conservative and may evolve over time.
        let suspiciousPaths = [
            "/Applications/Cydia.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/bin/bash",
            "/usr/sbin/sshd",
            "/etc/apt",
            "/usr/bin/ssh",
            "/private/var/lib/apt",
            "/private/var/lib/cydia",
            "/usr/libexec/sftp-server",
            "/Applications/Sileo.app",
            "/Applications/Zebra.app"
        ]

        // Iterate through known paths and emit a signal
        // as soon as a single match is found.
        for path in suspiciousPaths
        where FileManager.default.fileExists(atPath: path) {

            return [[
                "id": "ios_jailbreak_path",
                "category": "jailbreak",
                "confidence": "high",

                // Description is included only when debug output is enabled.
                "description": includeDebug
                    ? "Filesystem contains paths commonly associated with jailbroken devices"
                    : nil,

                // Include the matched path for diagnostic purposes only.
                "metadata": ["path": path]
            ].compactMapValues { $0 }]
        }

        // No jailbreak-related filesystem indicators detected.
        return []
    }
}
