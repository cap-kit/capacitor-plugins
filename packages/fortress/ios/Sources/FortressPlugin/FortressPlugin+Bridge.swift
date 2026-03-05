import Foundation
import Capacitor

extension FortressPlugin {

    // MARK: - Version

    /// Retrieves the plugin version synchronized from package.json.
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    @objc func getRuntimeConfig(_ call: CAPPluginCall) {
        guard let runtimeConfig = self.config else {
            call.reject(ErrorMessages.initFailed, NativeError.initFailed(ErrorMessages.initFailed).errorCode)
            return
        }

        call.resolve(runtimeConfigSnapshot(runtimeConfig))
    }

    @objc func configure(_ call: CAPPluginCall) {
        guard var runtimeConfig = self.config else {
            call.reject(ErrorMessages.initFailed, NativeError.initFailed(ErrorMessages.initFailed).errorCode)
            return
        }

        applyRuntimeConfigOverrides(call: call, to: &runtimeConfig)

        self.config = runtimeConfig
        implementation.configure(runtimeConfig)

        implementation.setRuntimeEnablePrivacyScreen(runtimeConfig.enablePrivacyScreen)

        if runtimeConfig.enablePrivacyScreen {
            let isLocked = (try? implementation.isLocked()) ?? true
            implementation.setPrivacyScreenVisible(isLocked)
        } else {
            implementation.setPrivacyScreenVisible(false)
        }

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
                if self?.implementation.isPrivacyScreenActive() == true {
                    self?.implementation.setPrivacyScreenVisible(false)
                }
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
            if implementation.isPrivacyScreenActive() {
                implementation.setPrivacyScreenVisible(true)
            }
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

}
