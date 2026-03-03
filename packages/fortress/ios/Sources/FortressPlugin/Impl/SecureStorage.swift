import Foundation

/**
 Secure storage implementation using iOS Keychain.

 This layer provides encrypted storage using the iOS Keychain
 with hardware-backed security when available.

 Architectural rules:
 - Pure Swift implementation
 - Stateless - no internal state
 - Delegates actual Keychain operations to KeychainHelper
 - Throws NativeError for all failure cases
 */
struct SecureStorage {

    // MARK: - Public API

    /**
     Stores a secure value in the encrypted vault.

     - Parameters:
     - key: The unique key identifier.
     - value: The string value to store securely.
     - Throws: NativeError if storage fails.
     */
    func set(key: String, value: String) throws {
        do {
            try KeychainHelper.saveString(value, for: key)
        } catch let error as KeychainHelper.KeychainError {
            throw mapKeychainError(error)
        }
    }

    /**
     Retrieves a secure value from the encrypted vault.

     - Parameter key: The unique key identifier.
     - Returns: The stored string value, or nil if not found.
     - Throws: NativeError if retrieval fails unexpectedly.
     */
    func get(key: String) throws -> String? {
        do {
            return try KeychainHelper.readString(for: key)
        } catch let error as KeychainHelper.KeychainError {
            throw mapKeychainError(error)
        }
    }

    /**
     Removes a secure value from the encrypted vault.

     - Parameter key: The unique key identifier.
     - Throws: NativeError if deletion fails unexpectedly.
     */
    func remove(key: String) throws {
        do {
            try KeychainHelper.delete(for: key)
        } catch let error as KeychainHelper.KeychainError {
            throw mapKeychainError(error)
        }
    }

    /**
     Clears all secure values from the vault.

     - Throws: NativeError if clear operation fails.
     */
    func clearAll() throws {
        do {
            try KeychainHelper.clearAll()
        } catch let error as KeychainHelper.KeychainError {
            throw mapKeychainError(error)
        }
    }

    /**
     Checks whether a key exists in the secure vault.

     - Parameter key: The unique key identifier.
     - Returns: true if the key exists, false otherwise.
     - Throws: NativeError if the check fails unexpectedly.
     */
    func hasKey(key: String) throws -> Bool {
        do {
            return try KeychainHelper.hasValue(for: key)
        } catch let error as KeychainHelper.KeychainError {
            throw mapKeychainError(error)
        }
    }

    // MARK: - Error Mapping

    private func mapKeychainError(_ error: KeychainHelper.KeychainError) -> NativeError {
        switch error {
        case .unableToSave(let status):
            return .initFailed("Failed to save to Keychain: \(status)")
        case .unableToRead(let status):
            return .initFailed("Failed to read from Keychain: \(status)")
        case .unableToDelete(let status):
            return .initFailed("Failed to delete from Keychain: \(status)")
        case .dataConversionFailed:
            return .invalidInput("Data conversion failed")
        case .itemNotFound:
            return .notFound(ErrorMessages.notFound)
        case .unexpectedError(let status):
            return .initFailed("Unexpected Keychain error: \(status)")
        }
    }
}
