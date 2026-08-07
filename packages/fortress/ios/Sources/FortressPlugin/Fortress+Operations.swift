import Foundation

extension Fortress {

    // MARK: - Internal

    func setValue(key: String, value: String) throws {
        try ensureSecureVaultAccessible()
        let globalPrefix = config?.prefix ?? ""
        let secureKey = KeyUtils.formatSecureKey(key, globalPrefix: globalPrefix)
        try secureStorage.set(key: secureKey, value: value)
    }

    // MARK: - Atomic Multi-Set

    func setMany(values: [[String: Any]]) throws {
        let operations = try values.map { item in
            guard let key = item["key"] as? String,
                  let value = item["value"] as? String else {
                throw NativeError.invalidInput(ErrorMessages.invalidInput)
            }

            let secure = item["secure"] as? Bool ?? true
            return SetManyOperation(key: key, value: value, secure: secure)
        }

        if operations.contains(where: { $0.secure }) {
            try ensureSecureVaultAccessible()
        }

        var snapshot = [String: SnapshotValue]()
        var capturedMarkers = Set<String>()
        for operation in operations {
            let marker = operationMarker(for: operation)
            if !capturedMarkers.contains(marker) {
                let previousValue = try readOperationValue(operation)
                snapshot[marker] = previousValue.map(SnapshotValue.value) ?? .empty
                capturedMarkers.insert(marker)
            }
        }

        do {
            for operation in operations {
                if operation.secure {
                    let secureKey = secureStorageKey(for: operation.key)
                    try secureStorage.set(key: secureKey, value: operation.value)
                } else {
                    try setInsecureValue(key: operation.key, value: operation.value)
                }
            }
        } catch {
            rollbackSetMany(snapshot)
            throw error
        }
    }

    // MARK: - Biometric Status

    func checkBiometricStatus() -> [String: Any] {
        applySecurityOverrides(biometricAuth.checkStatus())
    }

    func setBiometryType(_ biometryType: String) {
        overrideBiometryType = biometryType

        if biometryType == "none" {
            overrideIsBiometricsAvailable = false
            overrideIsBiometricsEnabled = false
        } else {
            overrideIsBiometricsAvailable = true
        }
    }

    func setBiometryIsEnrolled(_ isBiometricsEnabled: Bool) {
        overrideIsBiometricsEnabled = isBiometricsEnabled

        if isBiometricsEnabled {
            overrideIsBiometricsAvailable = true
            if overrideBiometryType == "none" {
                overrideBiometryType = "fingerprint"
            }
        }
    }

    func setDeviceIsSecure(_ isDeviceSecure: Bool) {
        overrideIsDeviceSecure = isDeviceSecure
    }

    func getValue(key: String) throws -> String? {
        try ensureSecureVaultAccessible()
        let globalPrefix = config?.prefix ?? ""
        let secureKey = KeyUtils.formatSecureKey(key, globalPrefix: globalPrefix)
        return try secureStorage.get(key: secureKey)
    }

    func removeValue(key: String) throws {
        let globalPrefix = config?.prefix ?? ""
        let secureKey = KeyUtils.formatSecureKey(key, globalPrefix: globalPrefix)
        try secureStorage.remove(key: secureKey)
    }

    func clearAll() throws {
        try secureStorage.clearAll()
    }

    func unlock(
        promptOptions: PromptOptions?,
        promptMessage: String?,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        do {
            try assertNotLockedOut()
        } catch {
            completion(.failure(error))
            return
        }

        if shouldUseCachedAuthentication() {
            if isPrivacyScreenEnabled() {
                privacyScreen.unlock()
            }
            sessionManager.unlock()
            completion(.success(()))
            return
        }

        let reason = resolvePromptReason(
            promptMessage: promptMessage,
            promptOptions: promptOptions,
            fallback: "Authenticate to access your secure vault"
        )
        let allowPasscode = resolveAllowPasscode()
        let cancelTitle = promptOptions?.negativeButtonText ?? config?.biometricPromptText ?? "Cancel"

        biometricAuth.authenticate(
            reason: reason,
            cancelTitle: cancelTitle,
            allowPasscode: allowPasscode
        ) { [weak self] result in
            guard let self else { return }

            switch result {
            case .success:
                self.markAuthenticationSuccess()
                if self.isPrivacyScreenEnabled() {
                    self.privacyScreen.unlock()
                }
                self.sessionManager.unlock()
                completion(.success(()))
            case .failure(let error):
                self.recordBiometricFailure(error)
                completion(.failure(error))
            }
        }
    }

    func lock() throws {
        if isPrivacyScreenEnabled() {
            privacyScreen.lock()
        }
        sessionManager.lock()
        clearAuthenticationCache()
    }

    func isLocked() throws -> Bool {
        let timeout = Int64(config?.lockAfterMs ?? 60_000)
        let locked = sessionManager.evaluateLockState(lockAfterMs: timeout)

        if locked, isPrivacyScreenEnabled() {
            privacyScreen.lock()
        }

        return locked
    }

    func getSession() -> Fortress.SessionState {
        let internalState = sessionManager.getSession()

        return Fortress.SessionState(
            isLocked: internalState.isLocked,
            lastActiveAt: internalState.lastActiveAt
        )
    }

    func resetSession() throws {
        sessionManager.reset()
        if isPrivacyScreenEnabled() {
            privacyScreen.lock()
        }
        clearAuthenticationCache()
    }

    func touchSession() throws {
        let timeout = Int64(config?.lockAfterMs ?? 60_000)
        let locked = sessionManager.evaluateLockState(lockAfterMs: timeout)

        if locked {
            if isPrivacyScreenEnabled() {
                privacyScreen.lock()
            }
            return
        }

        sessionManager.touch()
    }

    func isPrivacyScreenEnabled() -> Bool {
        runtimeEnablePrivacyScreenOverride ?? (config?.enablePrivacyScreen ?? true)
    }

    func resolveAllowPasscode() -> Bool {
        guard let config else {
            return true
        }

        switch config.fallbackStrategy {
        case "deviceCredential":
            return true
        case "none":
            return false
        default:
            return config.allowDevicePasscode
        }
    }

    func applySecurityOverrides(_ status: [String: Any]) -> [String: Any] {
        return [
            "isBiometricsAvailable":
                overrideIsBiometricsAvailable ?? (status["isBiometricsAvailable"] as? Bool ?? false),
            "isBiometricsEnabled": overrideIsBiometricsEnabled ?? (status["isBiometricsEnabled"] as? Bool ?? false),
            "isDeviceSecure": overrideIsDeviceSecure ?? (status["isDeviceSecure"] as? Bool ?? false),
            "biometryType": overrideBiometryType ?? (status["biometryType"] as? String ?? "none")
        ]
    }

    func ensureSecureVaultAccessible() throws {
        if let freshTimeout = config?.requireFreshAuthenticationMs, freshTimeout > 0 {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            let stale = lastSuccessfulAuthAtMs <= 0 || (now - lastSuccessfulAuthAtMs) > Int64(freshTimeout)
            if stale {
                sessionManager.lock()
                if isPrivacyScreenEnabled() {
                    privacyScreen.lock()
                }
                throw NativeError.vaultLocked(ErrorMessages.vaultLocked)
            }
        }

        let timeout = Int64(config?.lockAfterMs ?? 60_000)
        let locked = sessionManager.evaluateLockState(lockAfterMs: timeout)
        if locked {
            if isPrivacyScreenEnabled() {
                privacyScreen.lock()
            }
            throw NativeError.vaultLocked(ErrorMessages.vaultLocked)
        }
    }
}

private extension Fortress {

    struct SetManyOperation {
        let key: String
        let value: String
        let secure: Bool
    }

    enum SnapshotValue {
        case value(String)
        case empty
    }

    func secureStorageKey(for key: String) -> String {
        let globalPrefix = config?.prefix ?? ""
        return KeyUtils.formatSecureKey(key, globalPrefix: globalPrefix)
    }

    func operationMarker(for operation: SetManyOperation) -> String {
        operation.secure ? "secure:\(operation.key)" : "insecure:\(operation.key)"
    }

    func readOperationValue(_ operation: SetManyOperation) throws -> String? {
        if operation.secure {
            return try secureStorage.get(key: secureStorageKey(for: operation.key))
        }

        return standardStorage.get(key: getObfuscatedKeyValue(operation.key))
    }

    func rollbackSetMany(_ snapshot: [String: SnapshotValue]) {
        for (marker, previousValue) in snapshot {
            let secure = marker.hasPrefix("secure:")
            let key = String(marker.split(separator: ":", maxSplits: 1, omittingEmptySubsequences: false).last ?? "")

            do {
                if secure {
                    let secureKey = secureStorageKey(for: key)
                    if case .value(let value) = previousValue {
                        try secureStorage.set(key: secureKey, value: value)
                    } else {
                        try secureStorage.remove(key: secureKey)
                    }
                } else {
                    let obfuscatedKey = getObfuscatedKeyValue(key)
                    if case .value(let value) = previousValue {
                        try standardStorage.set(key: obfuscatedKey, value: value)
                    } else {
                        try standardStorage.remove(key: obfuscatedKey)
                    }
                }
            } catch {
            }
        }
    }
}
