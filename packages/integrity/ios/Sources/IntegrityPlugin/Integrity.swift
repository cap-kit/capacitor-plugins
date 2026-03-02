import Foundation
import MachO

/**
 Native iOS implementation for the Integrity plugin.
 */
@objc
public final class Integrity: NSObject {

    // MARK: - Configuration

    /// Immutable plugin configuration injected by the Plugin layer.
    private var config: Config?

    /// Buffer for integrity signals captured during early app boot.
    /// Flushed on the first explicit integrity check.
    private static var bootSignals: [[String: Any]] = []

    // Negative cache for expensive integrity checks.
    // Caches only "no-signal" results for a short time window.
    private struct NegativeCacheEntry {
        let timestampMs: Int
    }

    private var negativeCache: [String: NegativeCacheEntry] = [:]

    private let negativeCacheTTLms = 30_000

    // MARK: - Early Boot Hooks

    /// Entry point invoked from AppDelegate during application launch.
    @objc public static func onAppLaunch() {
        // Capture jailbreak-related filesystem signals immediately at boot.
        let signals = JailbreakDetector.detect(includeDebug: false)
        self.bootSignals.append(contentsOf: signals)
    }

    // MARK: - Configuration Injection

    /// Applies static plugin configuration.
    func applyConfig(_ config: Config) {
        precondition(
            self.config == nil,
            "Integrity.applyConfig(_:) must be called exactly once"
        )

        self.config = config
        Logger.verbose = config.verboseLogging

        Logger.debug(
            "Integrity configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Remote Attestation (Stub)

    /// Placeholder for future Apple App Attest integration.
    func getAppAttestSignal(options: CheckOptions) -> [String: Any]? {
        return RemoteAttestor.getAppAttestSignal(options: options)
    }

    // MARK: - Integrity Check Orchestration

    /**
     Executes integrity checks according to the requested options
     and returns a fully assembled integrity report.
     */
    func performCheck(
        options: CheckOptions
    ) throws -> [String: Any] {

        // Apply negative cache only for standard / strict levels.
        // Cached results represent a recent "no-signal" execution.
        if let level = options.level,
           level != "basic",
           isNegativeCacheValid(level: level) {

            Logger.debug(
                "Negative cache hit for integrity check:",
                level
            )

            return ReportBuilder.buildReport(
                signals: [],
                isEmulator: false,
                platform: "ios"
            )
        }

        var signals: [[String: Any]] = []
        let includeDebug = options.includeDebugInfo ?? false

        mergeBootSignals(into: &signals)

        let isSimulator = runChecks(
            options: options,
            signals: &signals,
            includeDebug: includeDebug
        )

        appendCorrelations(
            signals: &signals,
            includeDebug: includeDebug
        )

        // Update negative cache only when no integrity signals are detected.
        // Any detected signal invalidates the cached clean state.
        if let level = options.level, level != "basic" {
            if signals.isEmpty {
                updateNegativeCache(level: level)
            } else {
                clearNegativeCache(level: level)
            }
        }

        return ReportBuilder.buildReport(
            signals: signals,
            isEmulator: isSimulator,
            platform: "ios"
        )
    }

}

private extension Integrity {
    // MARK: - BASIC Checks

    /// Executes BASIC integrity checks.
    func performBasicChecks(
        signals: inout [[String: Any]],
        includeDebug: Bool
    ) -> Bool {

        appendFilesystemJailbreakSignals(
            to: &signals,
            includeDebug: includeDebug
        )

        let isSimulator = appendSimulatorSignalIfNeeded(
            to: &signals,
            includeDebug: includeDebug
        )

        appendUrlSchemeJailbreakSignalIfNeeded(
            to: &signals,
            includeDebug: includeDebug
        )

        return isSimulator
    }

    // MARK: - STANDARD / STRICT Checks

    /**
     Executes STANDARD and STRICT integrity checks,
     including runtime and instrumentation heuristics.
     */
    func performStandardChecks(
        signals: inout [[String: Any]],
        includeDebug: Bool
    ) {

        signals.append(
            contentsOf: RuntimeChecks.debugSignals(
                includeDebug: includeDebug
            )
        )

        let hookingDetected =
            HookChecks.hookingSignal(
                includeDebug: includeDebug
            )

        if let hookingDetected {
            signals.append(hookingDetected)
        }

        let portDetected =
            HookChecks.isFridaPortOpen()

        if portDetected {
            signals.append(
                Utils.buildSignal(
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
                Utils.buildSignal(
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

    // MARK: - Orchestration Helpers

    /// Merges early boot signals into the current execution context.
    func mergeBootSignals(
        into signals: inout [[String: Any]]
    ) {
        signals.append(contentsOf: Integrity.bootSignals)
        Integrity.bootSignals.removeAll()
    }

    /// Runs integrity checks according to the selected strictness level.
    func runChecks(
        options: CheckOptions,
        signals: inout [[String: Any]],
        includeDebug: Bool
    ) -> Bool {

        let isSimulator = performBasicChecks(
            signals: &signals,
            includeDebug: includeDebug
        )

        if (options.level ?? "basic") != "basic" {
            performStandardChecks(
                signals: &signals,
                includeDebug: includeDebug
            )

            // Entitlement & Provisioning Verification (RASP)
            // Added check to verify if the production app allows debugging via entitlements
            if let entData = EntitlementChecks.checkEntitlements() {
                if let isDebuggable = entData["debuggable"] as? Bool, isDebuggable, options.level == "strict" {
                    signals.append(
                        Utils.buildSignal(
                            id: "ios_entitlement_debuggable",
                            category: "tamper",
                            confidence: "high",
                            description: "Production app has 'get-task-allow' enabled in provisioning profile",
                            metadata: entData,
                            includeDebug: includeDebug
                        )
                    )
                }

                if let hasKeychain = entData["has_keychain_access"] as? Bool, !hasKeychain, options.level == "strict" {
                    signals.append(
                        Utils.buildSignal(
                            id: "ios_keychain_entitlement_missing",
                            category: "tamper",
                            confidence: "medium",
                            description: "Expected keychain-access-groups are missing from provisioning profile",
                            metadata: entData,
                            includeDebug: includeDebug
                        )
                    )
                }
            }
        }

        if options.level == "strict",
           let attest = getAppAttestSignal(options: options) {
            signals.append(attest)
        }

        return isSimulator
    }

    /// Appends derived correlation signals based on collected indicators.
    func appendCorrelations(
        signals: inout [[String: Any]],
        includeDebug: Bool
    ) {
        if let jailbreakCorrelation =
            CorrelationUtils.jailbreakCorrelation(
                from: signals,
                includeDebug: includeDebug
            ) {
            signals.append(jailbreakCorrelation)
        }

        if let jailbreakAndHookCorrelation =
            CorrelationUtils.jailbreakAndHookCorrelation(
                from: signals,
                includeDebug: includeDebug
            ) {
            signals.append(jailbreakAndHookCorrelation)
        }
    }

    // MARK: - BASIC Check Helpers

    /// Appends filesystem-based jailbreak indicators.
    func appendFilesystemJailbreakSignals(
        to signals: inout [[String: Any]],
        includeDebug: Bool
    ) {
        signals.append(
            contentsOf: JailbreakDetector.detect(
                includeDebug: includeDebug
            )
        )

        if FilesystemChecks.canEscapeSandbox() {
            signals.append(
                Utils.buildSignal(
                    id: "ios_sandbox_escaped",
                    category: "tamper",
                    confidence: "high",
                    description: "Successfully wrote to a protected system directory (Sandbox violation)",
                    metadata: ["path": "/private/integrity_test.txt"],
                    includeDebug: includeDebug
                )
            )
        }

        if FilesystemChecks.hasSuspiciousSymlinks() {
            signals.append(
                Utils.buildSignal(
                    id: "ios_suspicious_symlink",
                    category: "jailbreak",
                    confidence: "high",
                    description: "System directories are redirected via symbolic links",
                    includeDebug: includeDebug
                )
            )
        }
    }

    /// Appends simulator signal if the app is running in a simulator.
    func appendSimulatorSignalIfNeeded(
        to signals: inout [[String: Any]],
        includeDebug: Bool
    ) -> Bool {

        let isSimulator = SimulatorChecks.isSimulator()

        if isSimulator {
            signals.append(
                Utils.buildSignal(
                    id: "ios_simulator",
                    category: "emulator",
                    confidence: "high",
                    description: "Application is running in an iOS simulator environment",
                    metadata: ["type": "apple_simulator"],
                    includeDebug: includeDebug
                )
            )
        }

        return isSimulator
    }

    /// Appends optional jailbreak URL scheme detection signal.
    func appendUrlSchemeJailbreakSignalIfNeeded(
        to signals: inout [[String: Any]],
        includeDebug: Bool
    ) {
        guard
            let schemeConfig = config?.jailbreakUrlSchemes,
            schemeConfig.enabled,
            let schemeSignal =
                JailbreakUrlSchemeDetector.detect(
                    schemes: schemeConfig.schemes,
                    includeDebug: includeDebug
                )
        else { return }

        signals.append(schemeSignal)
    }

    func cacheKey(level: String) -> String {
        return "ios:\(level)"
    }

    func isNegativeCacheValid(level: String) -> Bool {
        guard let entry = negativeCache[cacheKey(level: level)] else {
            return false
        }

        let now = Int(Date().timeIntervalSince1970 * 1000)
        return now - entry.timestampMs <= negativeCacheTTLms
    }

    func updateNegativeCache(level: String) {
        let now = Int(Date().timeIntervalSince1970 * 1000)
        negativeCache[cacheKey(level: level)] = NegativeCacheEntry(timestampMs: now)
    }

    func clearNegativeCache(level: String) {
        negativeCache.removeValue(forKey: cacheKey(level: level))
    }
}
