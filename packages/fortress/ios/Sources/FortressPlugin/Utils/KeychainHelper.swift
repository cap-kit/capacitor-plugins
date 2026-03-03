import Foundation
import Security

/**
 Helper for interacting with iOS Keychain.

 This implementation provides secure storage using the iOS Keychain
 with hardware-backed encryption when available.

 Architectural rules:
 - Pure Swift, no Capacitor dependencies
 - Stateless utility functions
 - Returns Result type for operation outcomes
 */
enum KeychainHelper {

    // MARK: - Constants

    private static let service = "io.capkit.fortress"

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

    /**
     Saves data to the Keychain.

     - Parameters:
     - data: The data to store.
     - account: The unique key identifier.
     - Throws: KeychainError if the operation fails.
     */
    static func save(_ data: Data, for account: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

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
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        let attributes: [String: Any] = [
            kSecValueData as String: data
        ]

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
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

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
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

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
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: false,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        let status = SecItemCopyMatching(query as CFDictionary, nil)

        switch status {
        case errSecSuccess:
            return true
        case errSecItemNotFound:
            return false
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
            kSecAttrService as String: service
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
}
