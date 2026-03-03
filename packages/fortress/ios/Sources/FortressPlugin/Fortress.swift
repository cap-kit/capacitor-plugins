import Foundation

/**
 Native iOS implementation for the Fortress plugin.
 */
@objc
public final class Fortress: NSObject {

    struct SessionState {
        let isLocked: Bool
        let lastActiveAt: Int64
    }

    // MARK: - Configuration

    /// Immutable plugin configuration injected by the Plugin layer.
    private var config: Config?
    private let secureStorage = SecureStorage()
    private let standardStorage = StandardStorage()
    private let biometricAuth = BiometricAuth()
    private let sessionManager = SessionManager()
    private let privacyScreen = PrivacyScreen()

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

        Logger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    func configure(_ config: Config) {
        applyConfig(config)
    }

    func setValue(key: String, value: String) throws {
        try secureStorage.set(key: key, value: value)
    }

    func getValue(key: String) throws -> String? {
        return try secureStorage.get(key: key)
    }

    func removeValue(key: String) throws {
        try secureStorage.remove(key: key)
    }

    func clearAll() throws {
        try secureStorage.clearAll()
    }

    func unlock(completion: @escaping (Result<Void, Error>) -> Void) {
        let reason = "Authenticate to access your secure vault"
        biometricAuth.authenticate(reason: reason, allowPasscode: true) { [self] result in
            switch result {
            case .success:
                self.privacyScreen.unlock()
                completion(.success(()))
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }

    func lock() throws {
        privacyScreen.lock()
    }

    func isLocked() throws -> Bool {
        return sessionManager.getSession().isLocked
    }

    func getSession() throws -> SessionState {
        return sessionManager.getSession()
    }

    func resetSession() throws {
        _ = sessionManager.getSession()
    }

    func touchSession() throws {
        _ = sessionManager.getSession()
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
        let prefix = config?.obfuscationPrefix ?? "ftrss_"
        return KeyUtils.obfuscate(key, prefix: prefix)
    }
}
