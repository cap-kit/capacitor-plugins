import Foundation
import LocalAuthentication
import Security

/**
 Biometry-bound secure key-value store on the iOS Keychain.

 Mechanism (Capgo parity with a fork-owned identity):
 - Values are stored as `kSecClassGenericPassword` items under the
   fork-owned `service` constant, keyed by the caller key as account.
 - Writes attach a `SecAccessControl` (`biometryAny` /
   `biometryCurrentSet`) so the system enforces biometric
   authentication on every read; no prompt fires at write time.
 - Reads reuse an `LAContext` (`kSecUseAuthenticationContext`) so the
   Keychain itself presents the biometric prompt.
 - Existence checks disable interaction on the context so protected
   items report `errSecInteractionNotAllowed` instead of prompting.

 Architectural rules:
 - MUST NOT reference Capacitor.
 - MUST NOT reference CAPPluginCall.
 - Failures are thrown as `BiometricsError`.
 - The read blocks the calling thread while the prompt is up;
   callers MUST dispatch `readValue` off the main thread.
 */
enum BiometricsSecureStore {

    // MARK: - Identity

    /// Fork-owned Keychain service for biometry-bound items.
    static let service = "CapKitBiometricsSecureData"

    /// Keychain account for a logical key (the key is used verbatim).
    static func account(for key: String) -> String {
        return key
    }

    // MARK: - Access control

    /**
     Maps the JS `accessControl` value to `SecAccessControlCreateFlags`.

     Returns nil when the value is missing or unsupported: the store
     is biometry-bound by design and has no insecure fallback.
     */
    static func accessControlFlags(for raw: String?) -> SecAccessControlCreateFlags? {
        switch raw {
        case "biometryAny":
            return .biometryAny
        case "biometryCurrentSet":
            return .biometryCurrentSet
        default:
            return nil
        }
    }

    /**
     Creates the access control object applied to stored items.

     Items require the device passcode and are bound to this device
     only; the biometric flags decide enrollment-change behavior.
     */
    static func makeAccessControl(flags: SecAccessControlCreateFlags) -> SecAccessControl? {
        return SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
            flags,
            nil
        )
    }

    // MARK: - Query builders

    /// Query that reads the stored payload, prompting through `context`.
    static func readQuery(key: String, context: LAContext) -> [String: Any] {
        return [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account(for: key),
            kSecMatchLimit as String: kSecMatchLimitOne,
            kSecReturnAttributes as String: true,
            kSecReturnData as String: true,
            kSecUseAuthenticationContext as String: context
        ]
    }

    /// Query that reports existence without ever prompting.
    static func existenceQuery(key: String) -> [String: Any] {
        let context = LAContext()
        context.interactionNotAllowed = true

        return [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account(for: key),
            kSecMatchLimit as String: kSecMatchLimitOne,
            kSecReturnAttributes as String: true,
            kSecUseAuthenticationContext as String: context
        ]
    }

    /// Query that deletes the item stored for `key`.
    static func deleteQuery(key: String) -> [String: Any] {
        return [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account(for: key)
        ]
    }

    /// Query that stores `valueData` under the biometric `access` control.
    static func writeQuery(key: String, valueData: Data, access: SecAccessControl) -> [String: Any] {
        return [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account(for: key),
            kSecValueData as String: valueData,
            kSecAttrAccessControl as String: access
        ]
    }

    // MARK: - Status mapping

    /**
     Existence semantics for a Keychain lookup status.

     `errSecInteractionNotAllowed` means the item exists but its
     access control refused interaction: for existence purposes the
     item counts as present.
     */
    static func indicatesItemExists(status: OSStatus) -> Bool {
        return status == errSecSuccess || status == errSecInteractionNotAllowed
    }

    /**
     Maps a read status to a `BiometricsError`, or nil on success.

     Prompt failures reuse the authenticate error buckets: user
     cancel and authentication failure map to `.cancelled`,
     lockout-style `errSecInteractionNotAllowed` maps to
     `.unavailable`, and a missing item maps to `.notFound`.
     */
    static func readError(for status: OSStatus) -> BiometricsError? {
        switch status {
        case errSecSuccess:
            return nil
        case errSecItemNotFound:
            return .notFound(BiometricsErrorMessages.secureDataNotFound)
        case errSecUserCanceled, errSecAuthFailed:
            return .cancelled(BiometricsErrorMessages.cancelled)
        case errSecInteractionNotAllowed:
            return .unavailable(BiometricsErrorMessages.unavailable)
        default:
            BiometricsLogger.error("Secure store read failed. OSStatus:", status)
            return .initFailed(BiometricsErrorMessages.internalError)
        }
    }

    // MARK: - Operations

    /**
     Stores `value` under `key`, bound to the biometric `accessControl`.

     An existing item for the key is replaced (delete + add, Capgo
     mechanism) because access control attributes cannot be updated
     in place. No prompt fires at write time.
     */
    static func storeValue(_ value: String, key: String, accessControl: SecAccessControlCreateFlags) throws {
        guard let valueData = value.data(using: .utf8) else {
            throw BiometricsError.initFailed(BiometricsErrorMessages.secureDataInvalid)
        }

        guard let access = makeAccessControl(flags: accessControl) else {
            BiometricsLogger.error("SecAccessControlCreateWithFlags failed.")
            throw BiometricsError.initFailed(BiometricsErrorMessages.internalError)
        }

        var status = SecItemAdd(writeQuery(key: key, valueData: valueData, access: access) as CFDictionary, nil)

        if status == errSecDuplicateItem {
            SecItemDelete(deleteQuery(key: key) as CFDictionary)
            status = SecItemAdd(writeQuery(key: key, valueData: valueData, access: access) as CFDictionary, nil)
        }

        guard status == errSecSuccess else {
            BiometricsLogger.error("Secure store write failed. OSStatus:", status)
            throw BiometricsError.initFailed(BiometricsErrorMessages.internalError)
        }
    }

    /**
     Reads the value stored for `key`, presenting the biometric prompt.

     The Keychain read blocks the calling thread while the prompt is
     up; dispatch this off the main thread.
     */
    static func readValue(key: String, reason: String?, fallbackTitle: String?) throws -> String {
        let context = LAContext()
        if let reason = reason {
            context.localizedReason = reason
        }
        if let fallbackTitle = fallbackTitle {
            context.localizedFallbackTitle = fallbackTitle
        }

        var item: CFTypeRef?
        let status = SecItemCopyMatching(readQuery(key: key, context: context) as CFDictionary, &item)

        if let error = readError(for: status) {
            throw error
        }

        guard let existingItem = item as? [String: Any],
              let valueData = existingItem[kSecValueData as String] as? Data,
              let value = String(data: valueData, encoding: .utf8)
        else {
            throw BiometricsError.initFailed(BiometricsErrorMessages.secureDataInvalid)
        }

        return value
    }

    /**
     Deletes the item stored for `key` without prompting.

     Deleting a missing key is a no-op success (idempotent).
     */
    static func deleteValue(key: String) throws {
        let status = SecItemDelete(deleteQuery(key: key) as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            BiometricsLogger.error("Secure store delete failed. OSStatus:", status)
            throw BiometricsError.initFailed(BiometricsErrorMessages.internalError)
        }
    }

    /**
     Reports whether an item exists for `key`, never prompting.

     `errSecInteractionNotAllowed` counts as present: the item exists
     but is protected by an access control.
     */
    static func itemExists(key: String) -> Bool {
        var item: CFTypeRef?
        let status = SecItemCopyMatching(existenceQuery(key: key) as CFDictionary, &item)
        return indicatesItemExists(status: status)
    }
}
