import Foundation
import Capacitor

extension FortressPlugin {
    // MARK: - Secure Storage enumeration & sync (Aparajita-style parity)

    /// Valid `KeychainAccess` names accepted by `setValue` and
    /// `setDefaultKeychainAccess`.
    static let keychainAccessNames = [
        "whenUnlocked",
        "whenUnlockedThisDeviceOnly",
        "afterFirstUnlock",
        "afterFirstUnlockThisDeviceOnly",
        "whenPasscodeSetThisDeviceOnly"
    ]

    @objc func keys(_ call: CAPPluginCall) {
        let secure = call.getBool("secure", true)
        do {
            call.resolve(["keys": try implementation.keys(secure: secure)])
        } catch {
            handleError(call, error)
        }
    }

    @objc func getMany(_ call: CAPPluginCall) {
        guard let keys = call.getArray("keys", String.self) else {
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }
        let secure = call.getBool("secure", true)
        do {
            var values: [String: Any] = [:]
            for (key, value) in try implementation.getMany(keys: keys, secure: secure) {
                values[key] = value ?? NSNull()
            }
            call.resolve(["values": values])
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
            call.reject(ErrorMessages.invalidInput, NativeError.invalidInput(ErrorMessages.invalidInput).errorCode)
            return
        }
        implementation.setDefaultKeychainAccess(access)
        call.resolve()
    }
}
