import Foundation
import Capacitor

/**
 Capacitor bridge for the Fortress plugin.

 Responsibilities:
 - Parse JavaScript input
 - Call the native implementation
 - Resolve or reject CAPPluginCall
 - Map native errors to JS-facing error codes
 */
@objc(FortressPlugin)
public final class FortressPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "FortressPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "Fortress"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     - `getPluginVersion`: A method that returns the version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getRuntimeConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "configure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resetRuntimeConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clearAll", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "unlock", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "lock", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isLocked", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSession", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resetSession", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "touchSession", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "biometricKeysExist", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createKeys", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteKeys", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createSignature", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "registerWithChallenge", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "authenticateWithChallenge", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "generateChallengePayload", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setInsecureValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getInsecureValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeInsecureValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getObfuscatedKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hasKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setMany", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkStatus", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setBiometryType", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setBiometryIsEnrolled", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setDeviceIsSecure", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    let implementation: Fortress = Fortress()

    /// Configuration instance
    var config: Config?
    var staticConfigBaseline: Config?
    let runtimeConfigStore = RuntimeConfigStore()
    var lastSecurityStatus: [String: Any]?
    private var isPrivacyTapUnlockInProgress = false
    private let privacyTapNotification = Notification.Name("FortressPrivacyScreenTapUnlock")
    private let reasonSecurityStateChanged = "security_state_changed"
    let reasonKeypairInvalidated = "keypair_invalidated"
    let reasonKeysDeleted = "keys_deleted"

    // MARK: - Lifecycle

    /**
     Plugin lifecycle entry point.

     Called once when the plugin is loaded by the Capacitor bridge.
     This is the correct place to:
     - read static configuration
     - initialize native resources
     - configure the implementation
     */
    override public func load() {
        // Initialize Config with the correct type
        let cfg = initialRuntimeConfig()
        self.config = cfg
        implementation.applyConfig(cfg)

        // We use classic selectors to avoid the Sendability limitations of closures in Swift 6.
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleWillResignActive),
            name: UIApplication.willResignActiveNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleWillEnterForeground),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleDidBecomeActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handlePrivacyScreenTapUnlock),
            name: privacyTapNotification,
            object: nil
        )

        implementation.setSessionLockCallback { [weak self] isLocked in
            if isLocked {
                self?.notifyListeners("sessionLocked", data: nil)
            } else {
                self?.notifyListeners("sessionUnlocked", data: nil)
            }

            self?.notifyListeners("onLockStatusChanged", data: [
                "isLocked": isLocked
            ])
        }

        // Set up callback for when user taps on privacy screen
        implementation.setPrivacyScreenTapCallback { [privacyTapNotification] in
            NotificationCenter.default.post(name: privacyTapNotification, object: nil)
        }

        lastSecurityStatus = implementation.checkBiometricStatus()

        // Log if verbose logging is enabled
        Logger.info("Plugin loaded. Version: ", PluginVersion.number)
    }

    @objc private func handleDidEnterBackground() {
        // Register app background timestamp for grace-period timeout checks.
        implementation.setSessionBackgroundTimestamp()

        if implementation.isPrivacyScreenActive() {
            implementation.setPrivacyScreenVisible(true)
        }
    }

    @objc private func handleWillResignActive() {
        if implementation.isPrivacyScreenActive() {
            implementation.setPrivacyScreenVisible(true)
        }
    }

    @objc private func handleWillEnterForeground() {
        // 1. Evaluate lock status after background grace period.
        let timeout = Int64(config?.lockAfterMs ?? 60000)
        implementation.evaluateSessionBackgroundGracePeriod(lockAfterMs: timeout)

        if implementation.isPrivacyScreenActive() {
            // 2. Keep privacy overlay only while vault is locked.
            let locked = (try? implementation.isLocked()) ?? true
            implementation.setPrivacyScreenVisible(locked)
        }

        notifySecurityStateIfChanged()
        notifyListeners("onAppResume", data: nil)
    }

    @objc private func handleDidBecomeActive() {
        if implementation.isPrivacyScreenActive() {
            let locked = (try? implementation.isLocked()) ?? true
            implementation.setPrivacyScreenVisible(locked)
        }
    }

    @MainActor
    @objc private func handlePrivacyScreenTapUnlock() {
        triggerBiometricUnlock()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    private func notifySecurityStateIfChanged() {
        let currentStatus = implementation.checkBiometricStatus()

        guard let previousStatus = lastSecurityStatus else {
            lastSecurityStatus = currentStatus
            return
        }

        if !areSecurityStatusesEqual(previous: previousStatus, current: currentStatus) {
            notifyListeners("onSecurityStateChanged", data: currentStatus)

            if didSecurityPostureDowngrade(previous: previousStatus, current: currentStatus) {
                notifyListeners("onVaultInvalidated", data: [
                    "reason": reasonSecurityStateChanged
                ])
            }

            lastSecurityStatus = currentStatus
        }
    }

    @MainActor
    private func triggerBiometricUnlock() {
        guard !isPrivacyTapUnlockInProgress else { return }

        let locked = (try? implementation.isLocked()) ?? true
        if !locked {
            implementation.setPrivacyScreenVisible(false)
            return
        }

        isPrivacyTapUnlockInProgress = true

        implementation.unlock(promptOptions: nil, promptMessage: nil) { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                self.isPrivacyTapUnlockInProgress = false

                if case .failure(let error) = result {
                    Logger.warn("Privacy tap unlock failed:", error.localizedDescription)
                }
            }
        }
    }

    private func areSecurityStatusesEqual(previous: [String: Any], current: [String: Any]) -> Bool {
        let previousAvailable = previous["isBiometricsAvailable"] as? Bool
        let currentAvailable = current["isBiometricsAvailable"] as? Bool

        let previousEnabled = previous["isBiometricsEnabled"] as? Bool
        let currentEnabled = current["isBiometricsEnabled"] as? Bool

        let previousDeviceSecure = previous["isDeviceSecure"] as? Bool
        let currentDeviceSecure = current["isDeviceSecure"] as? Bool

        let previousType = previous["biometryType"] as? String
        let currentType = current["biometryType"] as? String

        return previousAvailable == currentAvailable &&
            previousEnabled == currentEnabled &&
            previousDeviceSecure == currentDeviceSecure &&
            previousType == currentType
    }

    private func didSecurityPostureDowngrade(previous: [String: Any], current: [String: Any]) -> Bool {
        let wasDeviceSecure = previous["isDeviceSecure"] as? Bool ?? false
        let isDeviceSecure = current["isDeviceSecure"] as? Bool ?? false

        let wasBiometricsEnabled = previous["isBiometricsEnabled"] as? Bool ?? false
        let isBiometricsEnabled = current["isBiometricsEnabled"] as? Bool ?? false

        return (wasDeviceSecure && !isDeviceSecure) || (wasBiometricsEnabled && !isBiometricsEnabled)
    }

    private func initialRuntimeConfig() -> Config {
        let baselineConfig = Config(plugin: self)
        staticConfigBaseline = baselineConfig

        var config = baselineConfig
        if let persistedOverrides = runtimeConfigStore.loadOverrides() {
            config.applyRuntimeOverrides(persistedOverrides)
        }

        return config
    }

}
