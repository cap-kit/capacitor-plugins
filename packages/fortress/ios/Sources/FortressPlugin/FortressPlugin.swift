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
        CAPPluginMethod(name: "setInsecureValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getInsecureValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeInsecureValue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getObfuscatedKey", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hasKey", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    private let implementation = Fortress()

    /// Configuration instance
    private var config: Config?

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

        // Log if verbose logging is enabled
        Logger.info("Plugin loaded. Version: ", PluginVersion.number)
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
        implementation.unlock { [weak self] result in
            switch result {
            case .success:
                call.resolve()
            case .failure(let error):
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
        do {
            let session = try implementation.getSession()
            call.resolve([
                "isLocked": session.isLocked,
                "lastActiveAt": session.lastActiveAt
            ])
        } catch {
            handleError(call, error)
        }
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
            call.resolve()
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

}
