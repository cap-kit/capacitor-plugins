import Foundation
import Security

/**
 * Secure per-provider token vault backed by the iOS Keychain
 * (`kSecClassGenericPassword`).
 *
 * Security contract (secure-token-storage spec):
 * - Native-only: raw token material never reaches JavaScript.
 * - Per-provider namespace: a distinct Keychain service per provider isolates
 *   tokens between providers.
 * - Default access control (kSecAttrAccessibleAfterFirstUnlock) keeps data
 *   readable after the first device unlock without a UI prompt.
 * - The token set is stored as a single JSON blob (Codable) under the account
 *   key, so `store` is exact-state: a stale payload is fully replaced.
 */
struct KeychainVault {

    /// The Keychain service name, namespaced per provider (e.g. `capkit.auth.google`).
    let service: String

    /// The Keychain account key under which the token JSON is stored.
    let account: String

    init(provider: String) {
        self.service = "capkit.auth.\(provider)"
        self.account = "oauth_tokens"
    }

    /**
     * Persists the token set, replacing any previously stored set for this service.
     *
     * - Parameter tokens: the token set to persist.
     * - Throws: `AuthenticationError.initFailed` when encoding or the Keychain
     *   write fails.
     */
    func store(_ tokens: TokenSet) throws {
        let data = try JSONEncoder().encode(tokens)

        // Delete any existing item first (exact-state; stale payload replaced).
        let deleteQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(deleteQuery as CFDictionary)

        let addQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock,
            kSecValueData as String: data
        ]
        let status = SecItemAdd(addQuery as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw AuthenticationError.initFailed("Keychain store failed: \(status)")
        }
    }

    /**
     * @return the persisted token set, or `nil` when none is stored.
     */
    func read() throws -> TokenSet? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess else {
            if status == errSecItemNotFound {
                return nil
            }
            throw AuthenticationError.initFailed("Keychain read failed: \(status)")
        }
        guard let data = result as? Data else {
            return nil
        }
        return try JSONDecoder().decode(TokenSet.self, from: data)
    }

    /**
     * Deletes every token for the vault service.
     */
    func delete() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
    }
}
