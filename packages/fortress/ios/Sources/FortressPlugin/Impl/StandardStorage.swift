import Foundation

/**
 Standard (insecure) storage implementation using UserDefaults.
 *
 * This layer provides non-encrypted storage for non-sensitive data
 * using iOS UserDefaults.
 *
 * Use for:
 * - UI theme preferences
 * - Non-sensitive flags
 * - Public configuration
 *
 * DO NOT use for:
 * - Passwords
 * - Authentication tokens
 * - Any sensitive data
 *
 * Architectural rules:
 * - Pure Swift implementation
 * - Stateless - no internal state
 * - Delegates to UserDefaults for persistence
 */
struct StandardStorage {

    // MARK: - Constants

    private static let suiteName = "io.capkit.fortress.standard"

    // MARK: - Public API

    /**
     Stores a value in standard (insecure) storage.

     - Parameters:
     - key: The unique key identifier.
     - value: The string value to store.
     - Throws: NativeError if storage fails.
     */
    func set(key: String, value: String) throws {
        let defaults = UserDefaults.standard
        defaults.set(value, forKey: key)
    }

    /**
     Retrieves a value from standard (insecure) storage.

     - Parameter key: The unique key identifier.
     - Returns: The stored string value, or nil if not found.
     */
    func get(key: String) -> String? {
        let defaults = UserDefaults.standard
        return defaults.string(forKey: key)
    }

    /**
     Removes a value from standard (insecure) storage.

     - Parameter key: The unique key identifier.
     */
    func remove(key: String) throws {
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: key)
    }

    /**
     Clears all values from standard storage.

     Note: This only clears keys that start with the fortress prefix.
     */
    func clearAll() throws {
        let defaults = UserDefaults.standard
        let allKeys = defaults.dictionaryRepresentation().keys

        for key in allKeys where key.hasPrefix("ftrss_") || key.hasPrefix("fortress_") {
            defaults.removeObject(forKey: key)
        }
    }

    /**
     Checks whether a key exists in standard storage.

     - Parameter key: The unique key identifier.
     - Returns: true if the key exists, false otherwise.
     */
    func hasKey(key: String) -> Bool {
        let defaults = UserDefaults.standard
        return defaults.object(forKey: key) != nil
    }
}
