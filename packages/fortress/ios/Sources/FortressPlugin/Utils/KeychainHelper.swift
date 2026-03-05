import Foundation
import Security
import LocalAuthentication

// Helper for interacting with iOS Keychain.
//
// This implementation provides secure storage using the iOS Keychain
// with hardware-backed encryption when available.
//
// Architectural rules:
// - Pure Swift, no Capacitor dependencies
// - Stateless utility functions
// - Returns Result type for operation outcomes
enum KeychainHelper {

    // MARK: - Constants

    private static let service = "io.capkit.fortress"
    private static let syncLock = NSLock()
    nonisolated(unsafe) private static var synchronizableEnabled: Bool = false

    static func setSynchronizable(_ enabled: Bool) {
        syncLock.lock()
        synchronizableEnabled = enabled
        syncLock.unlock()
    }

    private static var isSynchronizableEnabled: Bool {
        syncLock.lock()
        defer { syncLock.unlock() }
        return synchronizableEnabled
    }

    // MARK: - Error Types

    enum KeychainError: Swift.Error {
        case unableToSave(OSStatus)
        case unableToRead(OSStatus)
        case unableToDelete(OSStatus)
        case dataConversionFailed
        case itemNotFound
        case unexpectedError(OSStatus)
    }

    // MARK: - Public API

    private static func genericPasswordQuery(
        account: String,
        includeSynchronizableAny: Bool = false
    ) -> [String: Any] {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        if includeSynchronizableAny {
            query[kSecAttrSynchronizable as String] = kSecAttrSynchronizableAny
        }

        return query
    }

    static func castSecKey(_ value: CFTypeRef) throws -> SecKey {
        guard CFGetTypeID(value) == SecKeyGetTypeID() else {
            throw KeychainError.dataConversionFailed
        }

        return unsafeBitCast(value, to: SecKey.self)
    }

    /**
     Saves data to the Keychain.

     - Parameters:
     - data: The data to store.
     - account: The unique key identifier.
     - Throws: KeychainError if the operation fails.
     */
    static func save(_ data: Data, for account: String) throws {
        var query = genericPasswordQuery(account: account)
        query[kSecValueData as String] = data

        if isSynchronizableEnabled {
            query[kSecAttrSynchronizable as String] = kCFBooleanTrue
            query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        } else {
            // Ensure hardware-backed security is enforced
            query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        }

        let status = SecItemAdd(query as CFDictionary, nil)

        switch status {
        case errSecSuccess:
            return
        case errSecDuplicateItem:
            try update(data, for: account)
        default:
            throw KeychainError.unableToSave(status)
        }
    }

    /**
     Updates existing data in the Keychain.

     - Parameters:
     - data: The new data to store.
     - account: The unique key identifier.
     - Throws: KeychainError if the operation fails.
     */
    private static func update(_ data: Data, for account: String) throws {
        var query = genericPasswordQuery(account: account, includeSynchronizableAny: true)
        if isSynchronizableEnabled {
            query[kSecAttrSynchronizable as String] = kCFBooleanTrue
        }

        var attributes: [String: Any] = [
            kSecValueData as String: data
        ]

        if isSynchronizableEnabled {
            attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        } else {
            attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        }

        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)

        guard status == errSecSuccess else {
            throw KeychainError.unableToSave(status)
        }
    }

    /**
     Reads data from the Keychain.

     - Parameter account: The unique key identifier.
     - Returns: The stored data, or nil if not found.
     - Throws: KeychainError if the operation fails unexpectedly.
     */
    static func read(for account: String) throws -> Data? {
        var query = genericPasswordQuery(account: account, includeSynchronizableAny: true)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        switch status {
        case errSecSuccess:
            guard let data = result as? Data else {
                throw KeychainError.dataConversionFailed
            }
            return data
        case errSecItemNotFound:
            return nil
        default:
            throw KeychainError.unableToRead(status)
        }
    }

    /**
     Deletes data from the Keychain.

     - Parameter account: The unique key identifier.
     - Throws: KeychainError if the operation fails.
     */
    static func delete(for account: String) throws {
        let query = genericPasswordQuery(account: account, includeSynchronizableAny: true)

        let status = SecItemDelete(query as CFDictionary)

        switch status {
        case errSecSuccess, errSecItemNotFound:
            return
        default:
            throw KeychainError.unableToDelete(status)
        }
    }

    /**
     Checks whether a key exists in the Keychain.

     - Parameter account: The unique key identifier.
     - Returns: true if the key exists, false otherwise.
     - Throws: KeychainError if the operation fails unexpectedly.
     */
    static func hasValue(for account: String) throws -> Bool {
        // We use LAContext to handle modern UI interaction blocking
        let context = LAContext()
        context.interactionNotAllowed = true

        var query = genericPasswordQuery(account: account, includeSynchronizableAny: true)
        query[kSecReturnAttributes as String] = false
        query[kSecReturnData as String] = false
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        // We pass the context to instruct Keychain not to show biometric prompts
        query[kSecUseAuthenticationContext as String] = context

        let status = SecItemCopyMatching(query as CFDictionary, nil)

        switch status {
        case errSecSuccess:
            return true
        case errSecItemNotFound:
            return false
        // This state confirms that the item exists but requires authentication (which we have forbidden)
        case errSecInteractionNotAllowed:
            return true
        default:
            throw KeychainError.unexpectedError(status)
        }
    }

    /**
     Clears all items stored by this plugin in the Keychain.

     This removes only items belonging to the Fortress service.

     - Throws: KeychainError if the operation fails.
     */
    static func clearAll() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrSynchronizable as String: kSecAttrSynchronizableAny
        ]

        let status = SecItemDelete(query as CFDictionary)

        switch status {
        case errSecSuccess, errSecItemNotFound:
            return
        default:
            throw KeychainError.unableToDelete(status)
        }
    }

    // MARK: - String Convenience Methods

    /**
     Saves a string to the Keychain.

     - Parameters:
     - value: The string to store.
     - account: The unique key identifier.
     - Throws: KeychainError if the operation fails.
     */
    static func saveString(_ value: String, for account: String) throws {
        guard let data = value.data(using: .utf8) else {
            throw KeychainError.dataConversionFailed
        }
        try save(data, for: account)
    }

    /**
     Reads a string from the Keychain.

     - Parameter account: The unique key identifier.
     - Returns: The stored string, or nil if not found.
     - Throws: KeychainError if the operation fails or data conversion fails.
     */
    static func readString(for account: String) throws -> String? {
        guard let data = try read(for: account) else {
            return nil
        }
        guard let string = String(data: data, encoding: .utf8) else {
            throw KeychainError.dataConversionFailed
        }
        return string
    }

    // Crypto key pair operations are implemented in KeychainHelper+Crypto.swift
}
