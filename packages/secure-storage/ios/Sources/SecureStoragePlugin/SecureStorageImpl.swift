import Foundation
import Security

/**
 Native iOS implementation for the SecureStorage plugin.

 Hardware-backed key/value storage on the iOS Keychain, isolated from the
 Capacitor bridge:
 - MUST NOT access CAPPluginCall.
 - MUST NOT depend on Capacitor bridge APIs directly.

 Transparent store: no vault, no lock state, no authentication. Reads are
 always available (subject to the item's accessibility class).
 */
final class SecureStorageImpl: @unchecked Sendable {

    // MARK: - Properties

    /// Cached plugin configuration containing logging and sync flags.
    var config: SecureStorageConfig?

    /// Session default accessibility name (see `KeychainAccess`).
    private var defaultAccess: String?

    private static let service = "io.capkit.securestorage"

    private let syncLock = NSLock()
    private var synchronizableEnabled: Bool = false

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     MUST be called exactly once from the Plugin bridge layer during `load()`.
     */
    func applyConfig(_ config: SecureStorageConfig) {
        self.config = config
        SecureStorageLogger.verbose = config.verboseLogging
        notePrefix(config.obfuscationPrefix)
        setSynchronize(config.enableICloudKeychainSync)

        SecureStorageLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Key/value operations

    func set(key: String, value: String, access: String?) throws {
        guard let data = value.data(using: .utf8) else {
            throw SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput)
        }
        try save(data, for: encodeName(key), accessible: resolveAccessible(access))
    }

    func get(key: String) throws -> String? {
        for name in nameCandidates(key) {
            guard let data = try read(for: name) else {
                continue
            }
            guard let string = String(data: data, encoding: .utf8) else {
                throw SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput)
            }
            return string
        }
        return nil
    }

    func remove(key: String) throws -> Bool {
        var existed = false
        for name in nameCandidates(key) {
            existed = (try? has(storedName: name)) == true || existed
            try delete(for: name)
        }
        return existed
    }

    func has(key: String) throws -> Bool {
        return try nameCandidates(key).contains { try has(storedName: $0) }
    }

    private func has(storedName name: String) throws -> Bool {
        var query = itemQuery(account: name, synchronizableAny: true)
        query[kSecReturnAttributes as String] = false
        query[kSecReturnData as String] = false
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        let status = SecItemCopyMatching(query as CFDictionary, nil)
        switch status {
        case errSecSuccess:
            return true
        case errSecItemNotFound:
            return false
        default:
            throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (\(status))")
        }
    }

    func keys() throws -> [String] {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.service,
            kSecAttrSynchronizable as String: kSecAttrSynchronizableAny,
            kSecReturnAttributes as String: true,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        switch status {
        case errSecSuccess:
            guard let items = result as? [[String: Any]] else {
                return []
            }
            // An item may exist in both the local and iCloud keychains
            // (kSecAttrSynchronizableAny matches both); report each key once.
            var seen = Set<String>()
            return items.compactMap { $0[kSecAttrAccount as String] as? String }
                .map(decodeName)
                .filter { seen.insert($0).inserted }
        case errSecItemNotFound:
            return []
        default:
            throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (\(status))")
        }
    }

    func getMany(keys: [String]) throws -> [String: String?] {
        var values: [String: String?] = [:]
        for key in keys {
            values[key] = try get(key: key)
        }
        return values
    }

    func clear() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.service,
            kSecAttrSynchronizable as String: kSecAttrSynchronizableAny
        ]

        // Some platforms (e.g. macOS) remove a single matching item per
        // SecItemDelete call; loop until the service is empty so `clear()`
        // is total everywhere. One iteration on iOS.
        var attempts = 0
        while attempts < 1024 {
            let status = SecItemDelete(query as CFDictionary)
            switch status {
            case errSecSuccess:
                attempts += 1
            case errSecItemNotFound:
                return
            default:
                throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (\(status))")
            }
        }
        throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (clear did not converge)")
    }

    // MARK: - Synchronization

    /// Runtime insecure group. Mirrors the official Preferences contract.
    var insecureGroup = "CapacitorStorage"

    /// Session obfuscation overrides (see `configure`). Nil falls back to
    /// the static configuration values.
    var runtimeObfuscateKeys: Bool?
    var runtimeObfuscationPrefix: String?

    /// Obfuscation prefixes observed this session (static boot value
    /// noted at configuration time).
    var seenPrefixes = Set<String>()

    // MARK: - Insecure tier (see SecureStorageImpl+Insecure.swift)

    func setSynchronize(_ enabled: Bool) {
        syncLock.lock()
        defer { syncLock.unlock() }
        synchronizableEnabled = enabled
    }

    func isSynchronized() -> Bool {
        syncLock.lock()
        defer { syncLock.unlock() }
        return synchronizableEnabled
    }

    // MARK: - Accessibility

    func setDefaultAccess(_ access: String) {
        defaultAccess = access
    }

    // MARK: - Name encoding (see SecureStorageImpl+Insecure.swift)

    // MARK: - Private Keychain mechanics

    private func isSyncEnabled() -> Bool {
        syncLock.lock()
        defer { syncLock.unlock() }
        return synchronizableEnabled
    }

    private func itemQuery(account: String, synchronizableAny: Bool) -> [String: Any] {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.service,
            kSecAttrAccount as String: account
        ]
        if synchronizableAny {
            query[kSecAttrSynchronizable as String] = kSecAttrSynchronizableAny
        }
        return query
    }

    private func resolvedProtection(accessible: CFString?) -> (synchronizable: Bool, accessible: CFString) {
        guard let accessible else {
            return isSyncEnabled()
                ? (true, kSecAttrAccessibleAfterFirstUnlock)
                : (false, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }

        let supportsSync = (accessible as String) == (kSecAttrAccessibleAfterFirstUnlock as String)
        return (supportsSync && isSyncEnabled(), accessible)
    }

    func resolveAccessible(_ access: String?) -> CFString? {
        switch access ?? defaultAccess {
        case "whenUnlocked":
            return kSecAttrAccessibleWhenUnlocked
        case "whenUnlockedThisDeviceOnly":
            return kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        case "afterFirstUnlock":
            return kSecAttrAccessibleAfterFirstUnlock
        case "afterFirstUnlockThisDeviceOnly":
            return kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        case "whenPasscodeSetThisDeviceOnly":
            return kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
        default:
            return nil
        }
    }

    private func save(_ data: Data, for account: String, accessible: CFString? = nil) throws {
        var query = itemQuery(account: account, synchronizableAny: false)
        query[kSecValueData as String] = data

        let protection = resolvedProtection(accessible: accessible)
        if protection.synchronizable {
            query[kSecAttrSynchronizable as String] = kCFBooleanTrue
        }
        query[kSecAttrAccessible as String] = protection.accessible

        let status = SecItemAdd(query as CFDictionary, nil)
        switch status {
        case errSecSuccess:
            return
        case errSecDuplicateItem:
            try update(data, for: account, accessible: accessible)
        default:
            throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (\(status))")
        }
    }

    private func update(_ data: Data, for account: String, accessible: CFString? = nil) throws {
        var query = itemQuery(account: account, synchronizableAny: true)
        let protection = resolvedProtection(accessible: accessible)
        if protection.synchronizable {
            query[kSecAttrSynchronizable as String] = kCFBooleanTrue
        }

        let attributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: protection.accessible
        ]

        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        guard status == errSecSuccess else {
            throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (\(status))")
        }
    }

    private func read(for account: String) throws -> Data? {
        var query = itemQuery(account: account, synchronizableAny: true)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        switch status {
        case errSecSuccess:
            guard let data = result as? Data else {
                throw SecureStorageError.invalidInput(SecureStorageErrorMessages.invalidInput)
            }
            return data
        case errSecItemNotFound:
            return nil
        default:
            throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (\(status))")
        }
    }

    private func delete(for account: String) throws {
        let query = itemQuery(account: account, synchronizableAny: true)
        let status = SecItemDelete(query as CFDictionary)
        switch status {
        case errSecSuccess, errSecItemNotFound:
            return
        default:
            throw SecureStorageError.initFailed("\(SecureStorageErrorMessages.internalError) (\(status))")
        }
    }
}
