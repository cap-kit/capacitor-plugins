import Foundation
import MachO
import DeviceCheck // Required for DCAppAttestService stub

/**
 Native iOS implementation for the Integrity plugin.

 ROLE:
 This type acts as a pure orchestration layer.

 It coordinates:
 - execution order of integrity checks
 - aggregation of integrity signals
 - correlation between independent detections
 - final report assembly via helper builders

 It does NOT:
 - implement low-level detection logic directly
 - own UI or Capacitor bridge concerns
 - perform configuration reads on demand

 Responsibilities:
 - Invoke platform-specific integrity detectors
 (jailbreak, emulator, debug, hooking)
 - Interact with iOS runtime APIs only through
 dedicated helper types
 - Produce structured, JS-bridge-safe payloads

 Forbidden:
 - Accessing PluginCall or Capacitor-specific bridge APIs
 - Referencing configuration directly (must be injected)
 - Performing long-running or asynchronous work
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
     Static buffer for signals captured during early boot.
     These are merged into the first JavaScript-triggered report.
     */
    private static var bootSignals: [[String: Any]] = []

    /**
     Native entry point for AppDelegate.didFinishLaunchingWithOptions.
     Allows capturing security signals before the Capacitor bridge is initialized.
     */
    @objc public static func onAppLaunch() {
        // Capture jailbreak-related filesystem signals immediately at boot.
        // This uses pure, deterministic checks and does not depend on plugin configuration.
        let signals =
            IntegrityJailbreakDetector.detect(
                includeDebug: false
            )

        self.bootSignals.append(contentsOf: signals)
    }

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once.
     */
    func applyConfig(_ config: IntegrityConfig) {
        // Defensive programming:
        // This method MUST be called exactly once by the Plugin layer.
        // A second invocation indicates a programming error and is caught early in development.
        precondition(
            self.config == nil,
            "IntegrityImpl.applyConfig(_:) must be called exactly once"
        )

        self.config = config
        IntegrityLogger.verbose = config.verboseLogging

        IntegrityLogger.debug(
            "Integrity configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Remote Attestation Stubs

    /**
     Stub for Apple App Attest integration.
     To be implemented in a future evolution step.
     */
    func getAppAttestSignal() {
        // Placeholder for DCAppAttestService logic
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

        // Aggregated list of integrity signals produced during this execution.
        // Signals are appended in a deterministic order.
        var signals: [[String: Any]] = []

        // Whether diagnostic descriptions should be included in signals.
        // This flag only affects verbosity, never detection behavior.
        let includeDebug = options.includeDebugInfo ?? false

        // Merge integrity signals captured during early application boot
        // (before the Capacitor bridge and plugin lifecycle are initialized).
        signals.append(contentsOf: IntegrityImpl.bootSignals)
        IntegrityImpl.bootSignals.removeAll()

        // Execute BASIC integrity checks.
        // These checks are deterministic, low-cost, and always executed.
        // The return value indicates whether the app is running in a simulator.
        let isSimulator = performBasicChecks(
            signals: &signals,
            includeDebug: includeDebug
        )

        // Execute additional checks for STANDARD and STRICT levels.
        // These checks may include runtime inspection and instrumentation heuristics
        // and are intentionally skipped for BASIC level.
        if (options.level ?? "basic") != "basic" {
            performStandardChecks(
                signals: &signals,
                includeDebug: includeDebug
            )
        }

        // Assemble the final integrity report.
        // This step computes the overall score and produces a JS-bridge-safe payload.
        return IntegrityReportBuilder.buildReport(
            signals: signals,
            isEmulator: isSimulator,
            platform: "ios"
        )
    }

    // MARK: - Basic Checks

    /**
     Executes BASIC integrity checks.
     Returns whether the app is running in a simulator.
     */
    private func performBasicChecks(
        signals: inout [[String: Any]],
        includeDebug: Bool
    ) -> Bool {

        signals.append(
            contentsOf: IntegrityJailbreakDetector.detect(
                includeDebug: includeDebug
            )
        )

        if IntegrityFilesystemChecks.canEscapeSandbox() {
            signals.append(
                IntegrityUtils.buildSignal(
                    id: "ios_sandbox_escaped",
                    category: "jailbreak",
                    confidence: "high",
                    description: "Successfully wrote to a protected system directory",
                    includeDebug: includeDebug
                )
            )
        }

        if IntegrityFilesystemChecks.hasSuspiciousSymlinks() {
            signals.append(
                IntegrityUtils.buildSignal(
                    id: "ios_suspicious_symlink",
                    category: "jailbreak",
                    confidence: "high",
                    description: "System directories are redirected via symbolic links",
                    includeDebug: includeDebug
                )
            )
        }

        let simulator = isSimulator()
        if simulator {
            signals.append(
                IntegrityUtils.buildSignal(
                    id: "ios_simulator",
                    category: "emulator",
                    confidence: "high",
                    description: "Application is running in an iOS simulator environment",
                    metadata: ["type": "apple_simulator"],
                    includeDebug: includeDebug
                )
            )
        }

        return simulator
    }

    // MARK: - Standard & Strict Checks

    /**
     Executes STANDARD and STRICT integrity checks.
     */
    private func performStandardChecks(
        signals: inout [[String: Any]],
        includeDebug: Bool
    ) {

        signals.append(
            contentsOf: IntegrityRuntimeChecks.debugSignals(
                includeDebug: includeDebug
            )
        )

        let hookingDetected =
            IntegrityRuntimeChecks.hookingSignal(
                includeDebug: includeDebug
            )

        if let hookingDetected {
            signals.append(hookingDetected)
        }

        let portDetected =
            IntegrityRuntimeChecks.isFridaPortOpen()

        if portDetected {
            signals.append(
                IntegrityUtils.buildSignal(
                    id: "ios_frida_port_detected",
                    category: "hook",
                    confidence: "medium",
                    description: "Known instrumentation service port is reachable on localhost",
                    metadata: ["port": 27042],
                    includeDebug: includeDebug
                )
            )
        }

        if hookingDetected != nil && portDetected {
            signals.append(
                IntegrityUtils.buildSignal(
                    id: "ios_frida_correlation_confirmed",
                    category: "hook",
                    confidence: "high",
                    description: "Multiple instrumentation indicators detected simultaneously",
                    metadata: ["source": "library+port"],
                    includeDebug: includeDebug
                )
            )
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
}
