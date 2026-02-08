import Foundation

/**
 Filesystem integrity checks related to sandbox escape
 and suspicious redirections.
 */
struct IntegrityFilesystemChecks {

    /**
     Attempts to detect sandbox escape by testing write access
     to a restricted filesystem location.

     Characteristics:
     - Best-effort heuristic
     - Synchronous
     - No persistent side effects (temporary file is removed)

     IMPORTANT:
     - A successful write strongly suggests a sandbox escape.
     - A failure does NOT guarantee a secure environment.
     */
    static func canEscapeSandbox() -> Bool {

        // Target path intentionally chosen inside a protected directory.
        let testPath = "/private/integrity_test.txt"
        let testString = "integrity_test"

        do {
            // Attempt to write a temporary file outside the app sandbox.
            try testString.write(
                toFile: testPath,
                atomically: true,
                encoding: .utf8
            )

            // Cleanup if the write unexpectedly succeeds.
            try? FileManager.default.removeItem(atPath: testPath)
            return true
        } catch {
            // Write failed as expected on non-compromised devices.
            return false
        }
    }

    /**
     Detects suspicious symbolic links that may indicate
     filesystem redirection caused by a jailbreak.

     Characteristics:
     - Read-only inspection
     - No recursion
     - Limited to well-known system paths

     NOTES:
     - This check intentionally avoids deep filesystem traversal
     to reduce overhead and false positives.
     */
    static func hasSuspiciousSymlinks() -> Bool {

        // Well-known system directories that should never be symbolic links.
        let pathsToCheck = [
            "/Applications",
            "/Library",
            "/usr/libexec",
            "/usr/share"
        ]

        for path in pathsToCheck {
            do {
                let attributes =
                    try FileManager.default.attributesOfItem(atPath: path)

                // Any symbolic link at these locations is highly suspicious.
                if attributes[.type] as? FileAttributeType == .typeSymbolicLink {
                    return true
                }
            } catch {
                // Ignore inaccessible paths and continue checking others.
                continue
            }
        }

        // No suspicious filesystem redirections detected.
        return false
    }
}
