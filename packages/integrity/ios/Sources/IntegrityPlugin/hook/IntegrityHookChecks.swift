import Foundation
import MachO

/**
 Runtime integrity checks related to instrumentation frameworks (e.g., Frida).
 */
struct IntegrityHookChecks {

    /**
     Scans loaded dynamic libraries for suspicious instrumentation frameworks.
     */
    static func hookingSignal(
        includeDebug: Bool
    ) -> [String: Any]? {

        // Total number of images currently loaded by dyld.
        let imageCount = _dyld_image_count()

        // Substrings commonly found in instrumentation or hooking frameworks.
        // Matching is intentionally heuristic-based.
        let suspicious = ["frida", "gadget", "substrate", "cydia", "substitute"]

        for index in 0..<imageCount {
            guard let cName = _dyld_get_image_name(index) else { continue }
            let imageName = String(cString: cName).lowercased()

            // Optimization: skip standard system libraries to reduce
            // both runtime cost and false positives.
            if imageName.contains("/usr/lib/")
                || imageName.contains("/system/library/") {

                // Still inspect non-standard subpaths for suspicious fragments.
                if !suspicious.contains(where: { imageName.contains($0) }) {
                    continue
                }
            }

            // Emit a hooking signal as soon as a suspicious library is found.
            // Only the first match is reported to limit noise.
            if suspicious.contains(where: { imageName.contains($0) }) {
                return [
                    "id": "ios_hooking_library_detected",
                    "category": "hook",
                    "confidence": "high",
                    "description": includeDebug
                        ? "Detected suspicious instrumentation library: \(imageName)"
                        : nil,
                    "metadata": ["library_path": imageName]
                ].compactMapValues { $0 }
            }
        }

        return nil
    }

    /**
     Detects if a known instrumentation port (Frida) is open on localhost.
     */
    static func isFridaPortOpen() -> Bool {

        // Known default Frida server port.
        // This check is intentionally limited to a single, well-known port.
        var serverAddress = sockaddr_in()
        serverAddress.sin_family = sa_family_t(AF_INET)
        serverAddress.sin_addr.s_addr = inet_addr("127.0.0.1")
        serverAddress.sin_port = UInt16(27042).bigEndian

        // Create a TCP socket to test local connectivity.
        let sock = socket(AF_INET, SOCK_STREAM, 0)
        if sock < 0 { return false }
        defer { close(sock) }

        // Attempt to connect to the local Frida server port.
        // A successful connection suggests an active instrumentation service.
        return withUnsafePointer(to: &serverAddress) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                connect(
                    sock,
                    $0,
                    socklen_t(MemoryLayout<sockaddr_in>.size)
                ) == 0
            }
        }
    }
}
