import Foundation
// swiftlint:disable file_length

/**
 Native iOS implementation for the Fortress plugin.
 */
@objc
public final class Fortress: NSObject {

    private static let defaultBiometricKeyAlias = "biometric_keypair"

    struct SessionState {
        let isLocked: Bool
        let lastActiveAt: Int64
    }

    struct PromptOptions {
        let title: String?
        let subtitle: String?
        let description: String?
        let negativeButtonText: String?
        let confirmationRequired: Bool?
    }

    private struct ChallengePayload: Codable {
        let nonce: String
        let timestamp: Int64
        let deviceIdentifierHash: String
    }

    // MARK: - Configuration

    /// Immutable plugin configuration injected by the Plugin layer.
    private var config: Config?
    private let secureStorage = SecureStorage()
    private let standardStorage = StandardStorage()
    private let biometricAuth = BiometricAuth()
    private let sessionManager = SessionManager()
    private let privacyScreen = PrivacyScreen()
    private var lastSuccessfulAuthAtMs: Int64 = 0
    private var failedBiometricAttempts: Int = 0
    private var lockoutUntilMs: Int64 = 0

    // Initializer
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once
     from the Plugin layer during `load()`.

     Responsibilities:
     - Store immutable configuration
     - Configure runtime logging behavior
     */
    func applyConfig(_ config: Config) {
        self.config = config

        // Synchronize logger state
        Logger.verbose = config.verboseLogging
        Logger.setLevel(config.logLevel)
        KeychainHelper.setSynchronizable(config.enableICloudKeychainSync)

        Logger.debug(
            "Configuration applied. Log level:",
            config.logLevel
        )
    }

    func configure(_ config: Config) {
        applyConfig(config)
    }

    private func shouldUseCachedAuthentication() -> Bool {
        guard let config else {
            return false
        }

        guard config.allowCachedAuthentication else {
            return false
        }

        guard config.cachedAuthenticationTimeoutMs > 0 else {
            return false
        }

        if config.requireFreshAuthenticationMs > 0 {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            if now - lastSuccessfulAuthAtMs > Int64(config.requireFreshAuthenticationMs) {
                return false
            }
        }

        guard lastSuccessfulAuthAtMs > 0 else {
            return false
        }

        let now = Int64(Date().timeIntervalSince1970 * 1000)
        return (now - lastSuccessfulAuthAtMs) <= Int64(config.cachedAuthenticationTimeoutMs)
    }

    private func markAuthenticationSuccess() {
        lastSuccessfulAuthAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        failedBiometricAttempts = 0
        lockoutUntilMs = 0
    }

    private func clearAuthenticationCache() {
        lastSuccessfulAuthAtMs = 0
        failedBiometricAttempts = 0
        lockoutUntilMs = 0
    }

    private func assertNotLockedOut() throws {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        if now < lockoutUntilMs {
            throw NativeError.securityViolation(ErrorMessages.securityViolation)
        }
    }

    private func recordBiometricFailure(_ error: Error) {
        guard let config else {
            return
        }

        let maxAttempts = config.maxBiometricAttempts
        let lockoutDuration = config.lockoutDurationMs
        guard maxAttempts > 0, lockoutDuration > 0 else {
            return
        }

        if let nativeError = error as? NativeError,
           case .cancelled = nativeError {
            return
        }

        failedBiometricAttempts += 1
        if failedBiometricAttempts >= maxAttempts {
            lockoutUntilMs = Int64(Date().timeIntervalSince1970 * 1000) + Int64(lockoutDuration)
            failedBiometricAttempts = 0
        }
    }

    private func resolvePromptReason(
        promptMessage: String?,
        promptOptions: PromptOptions?,
        fallback: String
    ) -> String {
        if let promptMessage, !promptMessage.isEmpty {
            return promptMessage
        }

        var components: [String] = []
        if let title = promptOptions?.title, !title.isEmpty {
            components.append(title)
        }
        if let subtitle = promptOptions?.subtitle, !subtitle.isEmpty {
            components.append(subtitle)
        }
        if let description = promptOptions?.description, !description.isEmpty {
            components.append(description)
        }

        if components.isEmpty {
            return fallback
        }

        return components.joined(separator: "\n")
    }

    // MARK: - Session Lifecycle Delegates

    func setSessionLockCallback(_ callback: @escaping (Bool) -> Void) {
        sessionManager.onLockStatusChanged = callback
    }

    func setSessionBackgroundTimestamp() {
        sessionManager.setBackgroundTimestamp()
    }

    func evaluateSessionBackgroundGracePeriod(lockAfterMs: Int64) {
        sessionManager.evaluateBackgroundGracePeriod(lockAfterMs: lockAfterMs)
    }

    func setPrivacyScreenVisible(_ visible: Bool) {
        if !isPrivacyScreenEnabled() {
            privacyScreen.unlock()
            return
        }

        if visible {
            privacyScreen.lock()
        } else {
            privacyScreen.unlock()
        }
    }

}

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
                if let previousValue {
                    snapshot[marker] = .value(previousValue)
                } else {
                    snapshot[marker] = .empty
                }
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
        return biometricAuth.checkStatus()
    }

    // MARK: -

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
        // Safely access config or use defaults
        let allowPasscode = config?.allowDevicePasscode ?? true
        let cancelTitle = promptOptions?.negativeButtonText ?? config?.biometricPromptText ?? "Cancel"

        biometricAuth.authenticate(
            reason: reason,
            cancelTitle: cancelTitle,
            allowPasscode: allowPasscode,
            ) { [weak self] result in
            guard let self = self else { return }

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
        // External side effects belong here (UI/privacy screen).
        if isPrivacyScreenEnabled() {
            privacyScreen.lock()
        }
        sessionManager.lock() // Transition internal state to LOCKED
        clearAuthenticationCache()
    }

    func isLocked() throws -> Bool {
        let timeout = Int64(config?.lockAfterMs ?? 60_000)
        let locked = sessionManager.evaluateLockState(lockAfterMs: timeout)

        if locked {
            // Keep external side effects (UI/privacy screen) in Fortress, not in SessionManager.
            if isPrivacyScreenEnabled() {
                privacyScreen.lock()
            }
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
        // Reset session state and enforce locked state
        sessionManager.reset()
        if isPrivacyScreenEnabled() {
            privacyScreen.lock()
        }
        clearAuthenticationCache()
    }

    /**
     Updates the session timestamp or locks the vault if the timeout has passed.
     */
    func touchSession() throws {
        let timeout = Int64(config?.lockAfterMs ?? 60_000)
        let locked = sessionManager.evaluateLockState(lockAfterMs: timeout)

        if locked {
            // Keep external side effects (UI/privacy screen) in Fortress, not in SessionManager.
            if isPrivacyScreenEnabled() {
                privacyScreen.lock()
            }
            return
        }

        sessionManager.touch()
    }

    // swiftlint:disable function_body_length
    func createSignature(
        payload: String,
        keyAlias: String?,
        promptMessage: String?,
        promptOptions: PromptOptions?,
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        do {
            try assertNotLockedOut()
        } catch {
            completion(.failure(error))
            return
        }

        guard !payload.isEmpty else {
            completion(.failure(NativeError.invalidInput(ErrorMessages.invalidInput)))
            return
        }

        do {
            try ensureSecureVaultAccessible()
        } catch {
            completion(.failure(error))
            return
        }

        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        // Explicit registration state validation
        let isRegistered = KeychainHelper.hasKeyPair(alias: resolvedAlias)
        guard isRegistered else {
            completion(.failure(NativeError.notFound(ErrorMessages.notFound)))
            return
        }

        let reason = resolvePromptReason(
            promptMessage: promptMessage,
            promptOptions: promptOptions,
            fallback: "Authenticate to sign payload"
        )
        let cancelTitle = promptOptions?.negativeButtonText ?? config?.biometricPromptText ?? "Cancel"
        let allowPasscode = config?.allowDevicePasscode ?? true

        biometricAuth.authenticate(
            reason: reason,
            cancelTitle: cancelTitle,
            allowPasscode: allowPasscode
        ) { result in
            switch result {
            case .success:
                self.markAuthenticationSuccess()
                do {
                    let signatureData = try KeychainHelper.sign(data: Data(payload.utf8), with: resolvedAlias)

                    // Update activity timestamp after successful protected operation.
                    self.sessionManager.touch()

                    completion(.success(signatureData.base64EncodedString()))
                } catch {
                    if let keychainError = error as? KeychainHelper.KeychainError,
                       case .itemNotFound = keychainError {
                        completion(.failure(NativeError.notFound(ErrorMessages.notFound)))
                        return
                    }
                    completion(.failure(NativeError.securityViolation(ErrorMessages.securityViolation)))
                }
            case .failure(let error):
                self.recordBiometricFailure(error)
                completion(.failure(error))
            }
        }
    }
    // swiftlint:enable function_body_length

    func biometricKeysExist(keyAlias: String?) -> Bool {
        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        return KeychainHelper.hasKeyPair(alias: resolvedAlias)
    }

    func createKeys(keyAlias: String?) throws -> String {
        if config?.cryptoStrategy == "rsa" {
            throw NativeError.unavailable(ErrorMessages.unavailable)
        }

        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias

        try? KeychainHelper.deleteKeyPair(alias: resolvedAlias)

        let publicKeyData = try KeychainHelper.generateKeyPair(alias: resolvedAlias)
        guard let publicKey = String(data: publicKeyData, encoding: .utf8), !publicKey.isEmpty else {
            throw NativeError.initFailed(ErrorMessages.initFailed)
        }
        return publicKey
    }

    func deleteKeys(keyAlias: String?) throws {
        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        try? KeychainHelper.deleteKeyPair(alias: resolvedAlias)
    }

    func registerWithChallenge(
        challenge: String,
        keyAlias: String?,
        promptMessage: String?,
        promptOptions: PromptOptions?,
        completion: @escaping (Result<[String: String], Error>) -> Void
    ) {
        do {
            try assertNotLockedOut()
        } catch {
            completion(.failure(error))
            return
        }

        guard !challenge.isEmpty else {
            completion(.failure(NativeError.invalidInput(ErrorMessages.invalidInput)))
            return
        }

        do {
            let publicKey = try createKeys(keyAlias: keyAlias)
            authenticateWithChallenge(
                challenge: challenge,
                keyAlias: keyAlias,
                promptMessage: promptMessage,
                promptOptions: promptOptions
            ) { result in
                switch result {
                case .success(let signature):
                    completion(.success([
                        "publicKey": publicKey,
                        "signature": signature
                    ]))
                case .failure(let error):
                    completion(.failure(error))
                }
            }
        } catch {
            completion(.failure(error))
        }
    }

    func authenticateWithChallenge(
        challenge: String,
        keyAlias: String?,
        promptMessage: String?,
        promptOptions: PromptOptions?,
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        do {
            try assertNotLockedOut()
        } catch {
            completion(.failure(error))
            return
        }

        guard !challenge.isEmpty else {
            completion(.failure(NativeError.invalidInput(ErrorMessages.invalidInput)))
            return
        }

        let resolvedAlias = keyAlias ?? Fortress.defaultBiometricKeyAlias
        guard KeychainHelper.hasKeyPair(alias: resolvedAlias) else {
            completion(.failure(NativeError.notFound(ErrorMessages.notFound)))
            return
        }

        let reason = resolvePromptReason(
            promptMessage: promptMessage,
            promptOptions: promptOptions,
            fallback: "Authenticate to sign payload"
        )
        let cancelTitle = promptOptions?.negativeButtonText ?? config?.biometricPromptText ?? "Cancel"
        let allowPasscode = config?.allowDevicePasscode ?? true

        biometricAuth.authenticate(
            reason: reason,
            cancelTitle: cancelTitle,
            allowPasscode: allowPasscode
        ) { result in
            switch result {
            case .success:
                self.markAuthenticationSuccess()
                do {
                    let signatureData = try KeychainHelper.sign(data: Data(challenge.utf8), with: resolvedAlias)
                    completion(.success(signatureData.base64EncodedString()))
                } catch {
                    if let keychainError = error as? KeychainHelper.KeychainError,
                       case .itemNotFound = keychainError {
                        completion(.failure(NativeError.notFound(ErrorMessages.notFound)))
                        return
                    }
                    completion(.failure(NativeError.securityViolation(ErrorMessages.securityViolation)))
                }
            case .failure(let error):
                self.recordBiometricFailure(error)
                completion(.failure(error))
            }
        }
    }

    func generateChallengePayload(nonce: String) throws -> String {
        guard !nonce.isEmpty else {
            throw NativeError.invalidInput(ErrorMessages.invalidInput)
        }

        // Use a fixed timestamp to ensure no millisecond rounding differences
        let currentTimestamp = Int64(Date().timeIntervalSince1970 * 1000)

        let payload = ChallengePayload(
            nonce: nonce,
            timestamp: currentTimestamp,
            deviceIdentifierHash: Utils.deviceIdentifierHash()
        )

        let encoder = JSONEncoder()
        // Mandatory for Capacitor v8 / iOS 15+ target
        encoder.outputFormatting = [.sortedKeys, .withoutEscapingSlashes]

        guard let payloadString = String(data: try encoder.encode(payload), encoding: .utf8) else {
            throw NativeError.initFailed(ErrorMessages.initFailed)
        }

        return payloadString
    }

    func setInsecureValue(key: String, value: String) throws {
        try standardStorage.set(
            key: getObfuscatedKeyValue(key),
            value: value
        )
    }

    func getInsecureValue(key: String) throws -> String? {
        return standardStorage.get(key: getObfuscatedKeyValue(key))
    }

    func removeInsecureValue(key: String) throws {
        try standardStorage.remove(key: getObfuscatedKeyValue(key))
    }

    func getObfuscatedKey(key: String) throws -> String {
        return getObfuscatedKeyValue(key)
    }

    func hasKey(key: String, secure: Bool) throws -> Bool {
        if secure {
            return try secureStorage.hasKey(key: key)
        }
        return standardStorage.hasKey(key: getObfuscatedKeyValue(key))
    }

    private func getObfuscatedKeyValue(_ key: String) -> String {
        let obfuscationPrefix = config?.obfuscationPrefix ?? "ftrss_"
        let globalPrefix = config?.prefix ?? ""
        return KeyUtils.obfuscate(key, prefix: obfuscationPrefix, globalPrefix: globalPrefix)
    }

    private struct SetManyOperation {
        let key: String
        let value: String
        let secure: Bool
    }

    private func secureStorageKey(for key: String) -> String {
        let globalPrefix = config?.prefix ?? ""
        return KeyUtils.formatSecureKey(key, globalPrefix: globalPrefix)
    }

    private func operationMarker(for operation: SetManyOperation) -> String {
        if operation.secure {
            return "secure:\(operation.key)"
        }
        return "insecure:\(operation.key)"
    }

    private func readOperationValue(_ operation: SetManyOperation) throws -> String? {
        if operation.secure {
            return try secureStorage.get(key: secureStorageKey(for: operation.key))
        }
        return standardStorage.get(key: getObfuscatedKeyValue(operation.key))
    }

    private enum SnapshotValue {
        case value(String)
        case empty
    }

    private func rollbackSetMany(_ snapshot: [String: SnapshotValue]) {
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

    private func isPrivacyScreenEnabled() -> Bool {
        return config?.enablePrivacyScreen ?? true
    }

    private func ensureSecureVaultAccessible() throws {
        if let freshTimeout = config?.requireFreshAuthenticationMs,
           freshTimeout > 0 {
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
