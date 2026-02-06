import Foundation
import MachO

/**
 Native iOS implementation for the Integrity plugin.

 Responsibilities:
 - Perform platform-specific integrity checks (Jailbreak, Simulator, Debug, Hooking).
 - Interact with iOS system APIs and runtime.
 - Produce structured integrity signals with diagnostic metadata.

 Forbidden:
 - Accessing PluginCall or Capacitor-specific bridge APIs.
 - Referencing configuration directly (must be injected).
 */
@objc
public final class IntegrityImpl: NSObject {

    // MARK: - Configuration

    /**
     Immutable plugin configuration.

     This configuration MUST be injected exactly once
     from the Plugin layer during load().
     */
    private var config: IntegrityConfig?

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once.
     */
    func applyConfig(_ config: IntegrityConfig) {
        // WARNING:
        // This method is not protected against multiple invocations.
        // A second call would silently override configuration and logger state.
        // The Plugin layer MUST guarantee single invocation during load().

        self.config = config
        IntegrityLogger.verbose = config.verboseLogging

        IntegrityLogger.debug(
            "Integrity configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Execution Orchestrator

    /**
     Executes the requested integrity checks and aggregates signals.

     - IMPORTANT:
     This method assumes configuration has already been applied.
     If `applyConfig(_:)` was not called, behavior is undefined.

     - NOTE:
     This method performs synchronous checks only.
     It MUST NOT be called on the main thread if future checks
     introduce blocking I/O.
     */
    func performCheck(
        options: IntegrityCheckOptions
    ) throws -> [String: Any] {
        // NOTE:
        // Consider explicitly throwing an error if `config == nil`
        // to make the precondition failure explicit and diagnosable.

        var signals: [[String: Any]] = []

        // --- BASIC -----------------------------------------------------------
        // BASIC level MUST include only deterministic, low-cost checks.
        // These checks are expected to be safe to cache.
        signals.append(contentsOf: try checkJailbreakSignals())

        let isSimulator = isSimulator()
        if isSimulator {
            signals.append([
                "id": "ios_simulator",
                "category": "emulator",
                "confidence": "high",
                "metadata": ["type": "apple_simulator"]
            ])
        }

        // --- STANDARD & STRICT -----------------------------------------------
        // Levels above BASIC may include:
        // - Runtime inspection
        // - Debug / tracing detection
        // - Hooking and instrumentation heuristics
        //
        // These checks may produce false positives and MUST be scored accordingly.
        if options.level != "basic" {
            // Debug detection
            signals.append(contentsOf: checkDebugSignals(options: options))

            // Hooking detection (Frida/Substrate)
            let hookingSignal = checkHookingSignals(options: options)
            if let signal = hookingSignal {
                signals.append(signal)
            }

            // Port-based detection
            let portDetected = checkFridaPort()
            if portDetected {
                signals.append([
                    "id": "ios_frida_port_detected",
                    "category": "hook",
                    "confidence": "medium",
                    "metadata": ["port": 27042]
                ])
            }

            // --- SIGNAL CORRELATION ------------------------------------------
            // If both library and port are detected, emit a high-confidence
            // correlation signal to confirm active instrumentation.
            if hookingSignal != nil && portDetected {
                signals.append([
                    "id": "ios_frida_correlation_confirmed",
                    "category": "hook",
                    "confidence": "high",
                    "metadata": ["source": "library+port"]
                ])
            }
        }

        let score = computeScore(signals)

        return [
            "signals": signals,
            "score": score,
            "compromised": score >= 30,
            "environment": [
                "platform": "ios",
                "isEmulator": isSimulator,
                "isDebugBuild": false
            ],
            "timestamp": Int(Date().timeIntervalSince1970 * 1000)
        ]
    }

    // MARK: - Jailbreak Detection

    /**
     Cached jailbreak-related signals.

     Jailbreak checks are deterministic and relatively expensive,
     therefore they are cached for the lifetime of the process.

     - WARNING:
     Cached results are NOT invalidated if filesystem state changes
     during runtime (e.g. after dynamic instrumentation).
     */
    private var cachedJailbreakSignals: [[String: Any]]?

    /**
     Performs jailbreak detection using expanded filesystem heuristics.

     - NOTE:
     This method intentionally avoids:
     - fork()
     - syscalls requiring entitlements
     - write attempts outside sandbox

     - LIMITATION:
     This does NOT detect:
     - Rootless jailbreaks reliably
     - Runtime-only jailbreak environments
     */
    func checkJailbreakSignals() throws -> [[String: Any]] {
        if let cached = cachedJailbreakSignals {
            return cached
        }

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

        var signals: [[String: Any]] = []

        for path in suspiciousPaths
        where FileManager.default.fileExists(atPath: path) {

            signals.append([
                "id": "ios_jailbreak_path",
                "category": "jailbreak",
                "confidence": "high",
                "metadata": ["path": path]
            ])
            break
        }

        cachedJailbreakSignals = signals
        return signals
    }

    // MARK: - Debug & Hooking Detection

    /**
     Detects if a debugger is attached or if the process is being traced.

     - NOTE:
     This implementation uses `sysctl` + `P_TRACED` only.

     - LIMITATION:
     Does NOT detect:
     - ptrace-based evasion
     - LLDB attach after launch
     - Kernel-level debugging
     */
    func checkDebugSignals(options: IntegrityCheckOptions) -> [[String: Any]] {
        var signals: [[String: Any]] = []

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
                    "metadata": ["method": "sysctl_p_traced"]
                ])
            }
        }

        return signals
    }

    /**
     Inspects loaded dyld images to detect hooking frameworks like Frida.

     - WARNING:
     This method may produce false positives if:
     - App bundles legitimately include similarly named libraries
     - Vendor SDKs embed substrings like "gadget"

     - SCORING:
     Results from this method SHOULD always be weighted,
     never treated as a single-source compromise signal.
     */
    func checkHookingSignals(options: IntegrityCheckOptions) -> [String: Any]? {
        let imageCount = _dyld_image_count()

        for index in 0..<imageCount {
            if let cName = _dyld_get_image_name(index) {
                let imageName = String(cString: cName).lowercased()
                if imageName.contains("frida")
                    || imageName.contains("gadget")
                    || imageName.contains("substrate") {

                    return [
                        "id": "ios_hooking_library_detected",
                        "category": "hook",
                        "confidence": "high",
                        "metadata": ["library_path": imageName]
                    ]
                }
            }
        }

        return nil
    }

    /**
     Checks for active Frida server ports on the local interface.

     - WARNING:
     This performs an actual socket connection attempt.

     - LIMITATION:
     - May fail on hardened devices even if Frida is present
     - May succeed on non-compromised devices with port reuse

     - SECURITY:
     This MUST never be extended to scan arbitrary ports.
     */
    func checkFridaPort() -> Bool {
        var serverAddress = sockaddr_in()
        serverAddress.sin_family = sa_family_t(AF_INET)
        serverAddress.sin_addr.s_addr = inet_addr("127.0.0.1")
        serverAddress.sin_port = UInt16(27042).bigEndian

        let sock = socket(AF_INET, SOCK_STREAM, 0)
        if sock < 0 { return false }
        defer { close(sock) }

        return withUnsafePointer(to: &serverAddress) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                connect(sock, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) == 0
            }
        }
    }

    // MARK: - Helpers

    /**
     Determines whether the current process is running under the iOS Simulator.

     - NOTE:
     This uses compile-time environment checks and is not spoofable at runtime.
     */
    func isSimulator() -> Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    // MARK: - Scoring

    /**
     Computes a numeric risk score based on signal confidence.

     - NOTE:
     This scoring model is heuristic-based and intentionally simple.

     - POLICY:
     - high   → 30 points
     - medium → 15 points
     - low    → 5 points

     - WARNING:
     This method MUST remain platform-agnostic.
     Platform-specific weighting is FORBIDDEN here.
     */
    private func computeScore(
        _ signals: [[String: Any]]
    ) -> Int {
        return signals.reduce(0) { acc, signal in
            guard let confidence = signal["confidence"] as? String else {
                return acc
            }

            switch confidence {
            case "high": return acc + 30
            case "medium": return acc + 15
            case "low": return acc + 5
            default: return acc
            }
        }
    }
}
