import Foundation
import Capacitor

/**
 Capacitor bridge for the SecureStorage plugin.

 Responsibilities:
 - Parse JavaScript input
 - Call the native implementation
 - Resolve or reject CAPPluginCall
 - Map native errors to JS-facing error codes
 */
@objc(SecureStoragePlugin)
public final class SecureStoragePlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "SecureStoragePlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "SecureStorage"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "set", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "get", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "remove", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "has", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "keys", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getMany", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clear", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setSynchronize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSynchronize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setDefaultKeychainAccess", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "configure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setInsecure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getInsecure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeInsecure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hasInsecure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "keysInsecure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clearInsecure", returnType: CAPPluginReturnPromise)
    ]

    /// Valid `KeychainAccess` names.
    static let keychainAccessNames = [
        "whenUnlocked",
        "whenUnlockedThisDeviceOnly",
        "afterFirstUnlock",
        "afterFirstUnlockThisDeviceOnly",
        "whenPasscodeSetThisDeviceOnly"
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    let implementation = SecureStorageImpl()

    /// Configuration instance
    var config: SecureStorageConfig?

    // MARK: - Lifecycle

    override public func load() {
        let cfg = SecureStorageConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        SecureStorageLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    // MARK: - Version

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Key/value operations

    @objc func set(_ call: CAPPluginCall) {
        guard let key = call.getString("key"), let value = call.getString("value") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }

        if let access = call.getString("access"), !Self.keychainAccessNames.contains(access) {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }

        do {
            try implementation.set(
                key: key,
                value: value,
                access: call.getString("access")
            )
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func get(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }

        do {
            call.resolve(["value": try implementation.get(key: key) as Any])
        } catch {
            handleError(call, error)
        }
    }

    @objc func remove(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }

        do {
            call.resolve(["existed": try implementation.remove(key: key)])
        } catch {
            handleError(call, error)
        }
    }

    @objc func has(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }

        do {
            call.resolve(["exists": try implementation.has(key: key)])
        } catch {
            handleError(call, error)
        }
    }

    @objc func keys(_ call: CAPPluginCall) {
        do {
            call.resolve(["keys": try implementation.keys()])
        } catch {
            handleError(call, error)
        }
    }

    @objc func getMany(_ call: CAPPluginCall) {
        guard let keys = call.getArray("keys", String.self) else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }

        do {
            var values: [String: Any] = [:]
            for (key, value) in try implementation.getMany(keys: keys) {
                values[key] = value ?? NSNull()
            }
            call.resolve(["values": values])
        } catch {
            handleError(call, error)
        }
    }

    @objc func clear(_ call: CAPPluginCall) {
        do {
            try implementation.clear()
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    @objc func setSynchronize(_ call: CAPPluginCall) {
        implementation.setSynchronize(call.getBool("synchronize", false))
        call.resolve()
    }

    @objc func getSynchronize(_ call: CAPPluginCall) {
        call.resolve(["synchronize": implementation.isSynchronized()])
    }

    @objc func setDefaultKeychainAccess(_ call: CAPPluginCall) {
        guard let access = call.getString("access"),
              Self.keychainAccessNames.contains(access) else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }
        implementation.setDefaultAccess(access)
        call.resolve()
    }

    // MARK: - Insecure tier (plaintext, group-namespaced)

    @objc func configure(_ call: CAPPluginCall) {
        implementation.configure(
            group: call.getString("group"),
            obfuscateKeys: call.options["obfuscateKeys"] as? Bool,
            obfuscationPrefix: call.getString("obfuscationPrefix")
        )
        call.resolve()
    }

    @objc func setInsecure(_ call: CAPPluginCall) {
        guard let key = call.getString("key"), let value = call.getString("value") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }
        implementation.setInsecure(key: key, value: value)
        call.resolve()
    }

    @objc func getInsecure(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }
        call.resolve(["value": implementation.getInsecure(key: key) as Any])
    }

    @objc func removeInsecure(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }
        call.resolve(["existed": implementation.removeInsecure(key: key)])
    }

    @objc func hasInsecure(_ call: CAPPluginCall) {
        guard let key = call.getString("key") else {
            call.reject(
                SecureStorageErrorMessages.invalidInput,
                SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput).errorCode
            )
            return
        }
        call.resolve(["exists": implementation.hasInsecure(key: key)])
    }

    @objc func keysInsecure(_ call: CAPPluginCall) {
        call.resolve(["keys": implementation.keysInsecure()])
    }

    @objc func clearInsecure(_ call: CAPPluginCall) {
        implementation.clearInsecure()
        call.resolve()
    }

    // MARK: - Error Mapping

    func reject(
        _ call: CAPPluginCall,
        error: SecureStorageError
    ) {
        call.reject(error.message, error.errorCode)
    }

    func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let storageError = error as? SecureStorageError {
            call.reject(storageError.message, storageError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }
}
