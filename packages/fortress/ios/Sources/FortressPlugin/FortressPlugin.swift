import Foundation
import Capacitor
// swiftlint:disable file_length

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
        CAPPluginMethod(name: "configure", returnType: CAPPluginReturnPromise),
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
        CAPPluginMethod(name: "checkStatus", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    private let implementation: Fortress = Fortress()

    /// Configuration instance
    private var config: Config?
    private var lastSecurityStatus: [String: Any]?
    private let reasonSecurityStateChanged = "security_state_changed"
    private let reasonKeypairInvalidated = "keypair_invalidated"
    private let reasonKeysDeleted = "keys_deleted"

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
        let cfg = Config(plugin: self)
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
            selector: #selector(handleWillEnterForeground),
            name: UIApplication.willEnterForegroundNotification,
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

        lastSecurityStatus = implementation.checkBiometricStatus()

        // Log if verbose logging is enabled
        Logger.info("Plugin loaded. Version: ", PluginVersion.number)
    }

    @objc private func handleDidEnterBackground() {
        // Register app background timestamp for grace-period timeout checks.
        implementation.setSessionBackgroundTimestamp()

        if config?.enablePrivacyScreen == true {
            implementation.setPrivacyScreenVisible(true)
        }
    }

    @objc private func handleWillEnterForeground() {
        // 1. Evaluate lock status after background grace period.
        let timeout = Int64(config?.lockAfterMs ?? 60000)
        implementation.evaluateSessionBackgroundGracePeriod(lockAfterMs: timeout)

        if config?.enablePrivacyScreen == true {
            // 2. Keep privacy overlay only while vault is locked.
            let locked = (try? implementation.isLocked()) ?? true
            implementation.setPrivacyScreenVisible(locked)
        }

        notifySecurityStateIfChanged()
        notifyListeners("onAppResume", data: nil)
    }

    private func parsePromptOptions(_ call: CAPPluginCall) -> Fortress.PromptOptions? {
        guard let promptOptions = call.getObject("promptOptions") else {
            return nil
        }

        return Fortress.PromptOptions(
            title: promptOptions["title"] as? String,
            subtitle: promptOptions["subtitle"] as? String,
            description: promptOptions["description"] as? String,
            negativeButtonText: promptOptions["negativeButtonText"] as? String,
            confirmationRequired: promptOptions["confirmationRequired"] as? Bool
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: - Error mapping

    /**
     Maps native `NativeError` values to JS-facing error codes.

     CONTRACT:
     - Error codes MUST be stable and documented
     - Error codes MUST match across platforms
     - Platform-specific error codes are FORBIDDEN
     */
    private func reject(
        _ call: CAPPluginCall,
        error: NativeError
    ) {
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let nativeError = error as? NativeError {
            reject(call, error: nativeError)
        } else {
            let message = error.localizedDescription.isEmpty
                ? ErrorMessages.unexpectedNativeError
                : error.localizedDescription
            reject(call, error: .initFailed(message))
        }
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

}

extension FortressPlugin {

    // MARK: - Version

    /// Retrieves the plugin version synchronized from package.json.
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    @objc func configure(_ call: CAPPluginCall) {
        guard let cfg = self.config else {
            call.reject(ErrorMessages.initFailed, NativeError.initFailed(ErrorMessages.initFailed).errorCode)
            return
        }

        implementation.configure(cfg)
        call.resolve()
    }

    @objc func setValue(_ call: CAPPluginCall) {
        guard let key = call.getString("key"), let value = call.getString("value") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            try implementation.setValue(key: key, value: value)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func setMany(_ call: CAPPluginCall) {
        guard let values = call.getArray("values", [String: Any].self) else {
            reject(call, error: .invalidInput(ErrorMessages.invalidInput))
            return
        }

        do {
            try implementation.setMany(values: values)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func getValue(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            let value = try implementation.getValue(key: key)
            call.resolve(["value": value as Any])
        } catch {
            handleError(call, error)
        }
    }

    @objc func removeValue(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            try implementation.removeValue(key: key)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func clearAll(_ call: CAPPluginCall) {
        do {
            try implementation.clearAll()
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func unlock(_ call: CAPPluginCall) {
        let promptMessage = call.getString("promptMessage")
        let promptOptions = parsePromptOptions(call)

        implementation.unlock(promptOptions: promptOptions, promptMessage: promptMessage) { [weak self] result in
            switch result {
            case .success:
                call.resolve()
            case .failure(let error):
                if let nativeError = error as? NativeError,
                   case .notFound = nativeError {
                    self?.notifyListeners("onVaultInvalidated", data: [
                        "reason": self?.reasonKeypairInvalidated ?? "keypair_invalidated"
                    ])
                }
                self?.handleError(call, error)
            }
        }
    }

    @objc func lock(_ call: CAPPluginCall) {
        do {
            try implementation.lock()
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func isLocked(_ call: CAPPluginCall) {
        do {
            call.resolve(["isLocked": try implementation.isLocked()])
        } catch {
            handleError(call, error)
        }
    }

    @objc func getSession(_ call: CAPPluginCall) {
        let session = implementation.getSession()

        call.resolve([
            "isLocked": session.isLocked,
            "lastActiveAt": session.lastActiveAt
        ])
    }

    @objc func resetSession(_ call: CAPPluginCall) {
        do {
            try implementation.resetSession()
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func touchSession(_ call: CAPPluginCall) {
        do {
            try implementation.touchSession()
            // implementation.sessionManager.touchSession()
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func createSignature(_ call: CAPPluginCall) {
        guard let payload = call.getString("payload"), !payload.isEmpty else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        let keyAlias = call.getString("keyAlias")
        let promptMessage = call.getString("promptMessage")
        let promptOptions = parsePromptOptions(call)

        implementation.createSignature(
            payload: payload,
            keyAlias: keyAlias,
            promptMessage: promptMessage,
            promptOptions: promptOptions
        ) { [weak self] result in
            switch result {
            case .success(let signature):
                call.resolve([
                    "success": true,
                    "signature": signature
                ])
            case .failure(let error):
                if let nativeError = error as? NativeError,
                   case .notFound = nativeError {
                    self?.notifyListeners("onVaultInvalidated", data: [
                        "reason": self?.reasonKeypairInvalidated ?? "keypair_invalidated"
                    ])
                }
                self?.handleError(call, error)
            }
        }
    }

    @objc func biometricKeysExist(_ call: CAPPluginCall) {
        let keyAlias = call.getString("keyAlias")
        let keysExist = implementation.biometricKeysExist(keyAlias: keyAlias)
        call.resolve(["keysExist": keysExist])
    }

    @objc func createKeys(_ call: CAPPluginCall) {
        do {
            let keyAlias = call.getString("keyAlias")
            let publicKey = try implementation.createKeys(keyAlias: keyAlias)
            call.resolve(["publicKey": publicKey])
        } catch {
            handleError(call, error)
        }
    }

    @objc func deleteKeys(_ call: CAPPluginCall) {
        do {
            let keyAlias = call.getString("keyAlias")
            let hadKeys = implementation.biometricKeysExist(keyAlias: keyAlias)
            try implementation.deleteKeys(keyAlias: keyAlias)

            if hadKeys {
                notifyListeners("onVaultInvalidated", data: [
                    "reason": reasonKeysDeleted
                ])
            }

            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func registerWithChallenge(_ call: CAPPluginCall) {
        guard let challenge = call.getString("challenge"), !challenge.isEmpty else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        let keyAlias = call.getString("keyAlias")
        let promptMessage = call.getString("promptMessage")
        let promptOptions = parsePromptOptions(call)

        implementation.registerWithChallenge(
            challenge: challenge,
            keyAlias: keyAlias,
            promptMessage: promptMessage,
            promptOptions: promptOptions
        ) { [weak self] result in
            switch result {
            case .success(let data):
                call.resolve(data)
            case .failure(let error):
                if let nativeError = error as? NativeError,
                   case .notFound = nativeError {
                    self?.notifyListeners("onVaultInvalidated", data: [
                        "reason": self?.reasonKeypairInvalidated ?? "keypair_invalidated"
                    ])
                }
                self?.handleError(call, error)
            }
        }
    }

    @objc func authenticateWithChallenge(_ call: CAPPluginCall) {
        guard let challenge = call.getString("challenge"), !challenge.isEmpty else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        let keyAlias = call.getString("keyAlias")
        let promptMessage = call.getString("promptMessage")
        let promptOptions = parsePromptOptions(call)

        implementation.authenticateWithChallenge(
            challenge: challenge,
            keyAlias: keyAlias,
            promptMessage: promptMessage,
            promptOptions: promptOptions
        ) { [weak self] result in
            switch result {
            case .success(let signature):
                call.resolve(["signature": signature])
            case .failure(let error):
                if let nativeError = error as? NativeError,
                   case .notFound = nativeError {
                    self?.notifyListeners("onVaultInvalidated", data: [
                        "reason": self?.reasonKeypairInvalidated ?? "keypair_invalidated"
                    ])
                }
                self?.handleError(call, error)
            }
        }
    }

    @objc func generateChallengePayload(_ call: CAPPluginCall) {
        guard let nonce = call.getString("nonce"), !nonce.isEmpty else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            let payload = try implementation.generateChallengePayload(nonce: nonce)
            call.resolve(["payload": payload])
        } catch {
            handleError(call, error)
        }
    }

    @objc func setInsecureValue(_ call: CAPPluginCall) {
        guard let key = call.getString("key"), let value = call.getString("value") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            try implementation.setInsecureValue(key: key, value: value)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func getInsecureValue(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            let value = try implementation.getInsecureValue(key: key)
            call.resolve(["value": value as Any])
        } catch {
            handleError(call, error)
        }
    }

    @objc func removeInsecureValue(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            try implementation.removeInsecureValue(key: key)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func getObfuscatedKey(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        do {
            let obfuscated = try implementation.getObfuscatedKey(key: key)
            call.resolve(["obfuscated": obfuscated])
        } catch {
            handleError(call, error)
        }
    }

    @objc func hasKey(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }

        let secure = call.getBool("secure", true)

        do {
            let exists = try implementation.hasKey(key: key, secure: secure)
            call.resolve(["exists": exists])
        } catch {
            handleError(call, error)
        }
    }

    @objc func checkStatus(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        lastSecurityStatus = status
        call.resolve(status)
    }
}
