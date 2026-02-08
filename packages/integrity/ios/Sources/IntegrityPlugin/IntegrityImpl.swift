import Foundation
import MachO

/**
 Native iOS implementation for the Integrity plugin.
 */
@objc
public final class IntegrityImpl: NSObject {

    // MARK: - Configuration

    /// Immutable plugin configuration injected by the Plugin layer.
    private var config: IntegrityConfig?

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
        let signals = IntegrityJailbreakDetector.detect(includeDebug: false)
        self.bootSignals.append(contentsOf: signals)
    }

    // MARK: - Configuration Injection

    /// Applies static plugin configuration.
    func applyConfig(_ config: IntegrityConfig) {
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

    // MARK: - Remote Attestation (Stub)

    /// Placeholder for future Apple App Attest integration.
    func getAppAttestSignal(options: IntegrityCheckOptions) -> [String: Any]? {
        return IntegrityRemoteAttestor.getAppAttestSignal(options: options)
    }

    // MARK: - Integrity Check Orchestration

    /**
     Executes integrity checks according to the requested options
     and returns a fully assembled integrity report.
     */
    func performCheck(
        options: IntegrityCheckOptions
    ) throws -> [String: Any] {

        // Apply negative cache only for standard / strict levels.
        // Cached results represent a recent "no-signal" execution.
        if let level = options.level,
           level != "basic",
           isNegativeCacheValid(level: level) {

            IntegrityLogger.debug(
                "Negative cache hit for integrity check:",
                level
            )

            return IntegrityReportBuilder.buildReport(
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

        return IntegrityReportBuilder.buildReport(
            signals: signals,
            isEmulator: isSimulator,
            platform: "ios"
        )
    }

    // MARK: - BASIC Checks

    /// Executes BASIC integrity checks.
    private func performBasicChecks(
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
            IntegrityHookChecks.hookingSignal(
                includeDebug: includeDebug
            )

        if let hookingDetected {
            signals.append(hookingDetected)
        }

        let portDetected =
            IntegrityHookChecks.isFridaPortOpen()

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

    // MARK: - Orchestration Helpers

    /// Merges early boot signals into the current execution context.
    private func mergeBootSignals(
        into signals: inout [[String: Any]]
    ) {
        signals.append(contentsOf: IntegrityImpl.bootSignals)
        IntegrityImpl.bootSignals.removeAll()
    }

    /// Runs integrity checks according to the selected strictness level.
    private func runChecks(
        options: IntegrityCheckOptions,
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
            if let entData = IntegrityEntitlementChecks.checkEntitlements() {
                if let isDebuggable = entData["debuggable"] as? Bool, isDebuggable, options.level == "strict" {
                    signals.append(
                        IntegrityUtils.buildSignal(
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
                        IntegrityUtils.buildSignal(
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
    private func appendCorrelations(
        signals: inout [[String: Any]],
        includeDebug: Bool
    ) {
        if let jailbreakCorrelation =
            IntegrityCorrelationUtils.jailbreakCorrelation(
                from: signals,
                includeDebug: includeDebug
            ) {
            signals.append(jailbreakCorrelation)
        }

        if let jailbreakAndHookCorrelation =
            IntegrityCorrelationUtils.jailbreakAndHookCorrelation(
                from: signals,
                includeDebug: includeDebug
            ) {
            signals.append(jailbreakAndHookCorrelation)
        }
    }

    // MARK: - BASIC Check Helpers

    /// Appends filesystem-based jailbreak indicators.
    private func appendFilesystemJailbreakSignals(
        to signals: inout [[String: Any]],
        includeDebug: Bool
    ) {
        signals.append(
            contentsOf: IntegrityJailbreakDetector.detect(
                includeDebug: includeDebug
            )
        )

        if IntegrityFilesystemChecks.canEscapeSandbox() {
            signals.append(
                IntegrityUtils.buildSignal(
                    id: "ios_sandbox_escaped",
                    category: "tamper",
                    confidence: "high",
                    description: "Successfully wrote to a protected system directory (Sandbox violation)",
                    metadata: ["path": "/private/integrity_test.txt"],
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
    }

    /// Appends simulator signal if the app is running in a simulator.
    private func appendSimulatorSignalIfNeeded(
        to signals: inout [[String: Any]],
        includeDebug: Bool
    ) -> Bool {

        let isSimulator = IntegritySimulatorChecks.isSimulator()

        if isSimulator {
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

        return isSimulator
    }

    /// Appends optional jailbreak URL scheme detection signal.
    private func appendUrlSchemeJailbreakSignalIfNeeded(
        to signals: inout [[String: Any]],
        includeDebug: Bool
    ) {
        guard
            let schemeConfig = config?.jailbreakUrlSchemes,
            schemeConfig.enabled,
            let schemeSignal =
                IntegrityJailbreakUrlSchemeDetector.detect(
                    schemes: schemeConfig.schemes,
                    includeDebug: includeDebug
                )
        else { return }

        signals.append(schemeSignal)
    }

    private func cacheKey(level: String) -> String {
        return "ios:\(level)"
    }

    private func isNegativeCacheValid(level: String) -> Bool {
        guard let entry = negativeCache[cacheKey(level: level)] else {
            return false
        }

        let now = Int(Date().timeIntervalSince1970 * 1000)
        return now - entry.timestampMs <= negativeCacheTTLms
    }

    private func updateNegativeCache(level: String) {
        let now = Int(Date().timeIntervalSince1970 * 1000)
        negativeCache[cacheKey(level: level)] = NegativeCacheEntry(timestampMs: now)
    }

    private func clearNegativeCache(level: String) {
        negativeCache.removeValue(forKey: cacheKey(level: level))
    }
}
