import Foundation
import Capacitor

/**
 Capacitor bridge for the Biometrics plugin.

 Responsibilities:
 - Parse JavaScript input
 - Call the native implementation
 - Resolve or reject CAPPluginCall
 - Map native errors to JS-facing error codes
 */
@objc(BiometricsPlugin)
public final class BiometricsPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "BiometricsPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "Biometrics"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "authenticate", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "cancelAuthentication", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isAvailable", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isEnrolled", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getBiometricType", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getBiometricTypes", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hasDeviceCredential", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isDeviceSecure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getBiometricStrengthLevel", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAuthenticationType", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "enroll", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setSecureData", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSecureData", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteSecureData", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isSecureDataSaved", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    let implementation = BiometricsImpl()

    /// Configuration instance
    var config: BiometricsConfig?

    // MARK: - Lifecycle

    override public func load() {
        let cfg = BiometricsConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        BiometricsLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    // MARK: - Version

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Biometrics

    @objc func authenticate(_ call: CAPPluginCall) {
        let reason = call.getString("reason")
            ?? call.getString("description")
            ?? "Authenticate"
        let cancelTitle = call.getString("cancelButtonText") ?? "Cancel"
        let allowPasscode = call.getBool("allowDeviceCredential", true)

        if call.getString("iosFallbackButtonText") != nil {
            BiometricsLogger.debug("iosFallbackButtonText is managed by the system fallback title")
        }

        implementation.authenticate(
            reason: reason,
            cancelTitle: cancelTitle,
            allowPasscode: allowPasscode
        ) { [weak self] result in
            switch result {
            case .success:
                call.resolve()
            case .failure(let error):
                self?.handleError(call, error)
            }
        }
    }

    @objc func cancelAuthentication(_ call: CAPPluginCall) {
        implementation.cancelActiveAuthentication()
        call.resolve()
    }

    @objc func isAvailable(_ call: CAPPluginCall) {
        call.resolve(["isAvailable": implementation.status()["isBiometricsAvailable"] as? Bool ?? false])
    }

    @objc func isEnrolled(_ call: CAPPluginCall) {
        call.resolve(["isEnrolled": implementation.status()["isBiometricsEnabled"] as? Bool ?? false])
    }

    @objc func getBiometricType(_ call: CAPPluginCall) {
        call.resolve(["biometryType": implementation.status()["biometryType"] as? String ?? "none"])
    }

    @objc func getBiometricTypes(_ call: CAPPluginCall) {
        call.resolve(["biometryTypes": implementation.status()["biometryTypes"] as? [String] ?? []])
    }

    @objc func hasDeviceCredential(_ call: CAPPluginCall) {
        call.resolve(["hasDeviceCredential": implementation.status()["isDeviceSecure"] as? Bool ?? false])
    }

    @objc func isDeviceSecure(_ call: CAPPluginCall) {
        call.resolve(["isDeviceSecure": implementation.status()["isDeviceSecure"] as? Bool ?? false])
    }

    @objc func getBiometricStrengthLevel(_ call: CAPPluginCall) {
        let status = implementation.status()
        let strong = status["strongBiometryIsAvailable"] as? Bool ?? false
        let available = status["isBiometricsAvailable"] as? Bool ?? false
        call.resolve(["strengthLevel": strong ? "strong" : (available ? "weak" : "none")])
    }

    @objc func getAuthenticationType(_ call: CAPPluginCall) {
        call.resolve(["authenticationType": implementation.lastAuthenticationType])
    }

    @objc func enroll(_ call: CAPPluginCall) {
        // iOS offers no public biometric enrollment API.
        call.reject(
            BiometricsErrorMessages.unavailable,
            BiometricsError.unavailable(BiometricsErrorMessages.unavailable).errorCode
        )
    }

    // MARK: - Secure storage (biometry-bound)

    /**
     Stores a value bound to biometric access control.

     Android-only prompt options (`title`, `negativeButtonText`,
     `maxAttempts`) are accepted and ignored on iOS: the write never
     prompts, the system applies the access control instead.
     */
    @objc func setSecureData(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            reject(call, error: .invalidInput(BiometricsErrorMessages.invalidInput))
            return
        }
        guard let value = call.getString("value") else {
            reject(call, error: .invalidInput(BiometricsErrorMessages.invalidInput))
            return
        }
        guard let accessControl = BiometricsSecureStore.accessControlFlags(for: call.getString("accessControl")) else {
            reject(call, error: .invalidInput(BiometricsErrorMessages.invalidInput))
            return
        }

        implementation.setSecureData(key: key, value: value, accessControl: accessControl) { [weak self] result in
            switch result {
            case .success:
                call.resolve()
            case .failure(let error):
                self?.handleError(call, error)
            }
        }
    }

    /**
     Reads a biometry-protected value, prompting on the Keychain read.

     Android-only prompt options (`title`, `subtitle`, `description`,
     `negativeButtonText`, `maxAttempts`) are accepted and ignored on
     iOS; `reason` and `fallbackTitle` customize the system prompt.
     */
    @objc func getSecureData(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            reject(call, error: .invalidInput(BiometricsErrorMessages.invalidInput))
            return
        }

        implementation.getSecureData(
            key: key,
            reason: call.getString("reason"),
            fallbackTitle: call.getString("fallbackTitle")
        ) { [weak self] result in
            switch result {
            case .success(let value):
                call.resolve(["value": value])
            case .failure(let error):
                self?.handleError(call, error)
            }
        }
    }

    /**
     Deletes the value stored for the key without prompting.
     */
    @objc func deleteSecureData(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            reject(call, error: .invalidInput(BiometricsErrorMessages.invalidInput))
            return
        }

        implementation.deleteSecureData(key: key) { [weak self] result in
            switch result {
            case .success:
                call.resolve()
            case .failure(let error):
                self?.handleError(call, error)
            }
        }
    }

    /**
     Reports whether a value exists for the key, without prompting.
     */
    @objc func isSecureDataSaved(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            reject(call, error: .invalidInput(BiometricsErrorMessages.invalidInput))
            return
        }

        implementation.isSecureDataSaved(key: key) { [weak self] result in
            switch result {
            case .success(let isSaved):
                call.resolve(["isSaved": isSaved])
            case .failure(let error):
                self?.handleError(call, error)
            }
        }
    }

    // MARK: - Error Mapping

    func reject(
        _ call: CAPPluginCall,
        error: BiometricsError
    ) {
        call.reject(error.message, error.errorCode)
    }

    func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let biometricsError = error as? BiometricsError {
            call.reject(biometricsError.message, biometricsError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }
}
