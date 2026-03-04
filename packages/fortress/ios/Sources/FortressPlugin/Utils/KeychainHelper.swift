import Foundation
import Security
import LocalAuthentication
// swiftlint:disable file_length

// Helper for interacting with iOS Keychain.
//
// This implementation provides secure storage using the iOS Keychain
// with hardware-backed encryption when available.
//
// Architectural rules:
// - Pure Swift, no Capacitor dependencies
// - Stateless utility functions
// - Returns Result type for operation outcomes
// swiftlint:disable:next type_body_length
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

    private static func castSecKey(_ value: CFTypeRef) throws -> SecKey {
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

    // MARK: - Cryptographic Key Pairs (Secure Enclave)

    /**
     Supported elliptic curves for key pair generation.
     */
    enum EllipticCurve {
        case p256
        case p384
        case p521

        var algorithm: SecKeyAlgorithm {
            switch self {
            case .p256: return .ecdsaSignatureMessageX962SHA256
            case .p384: return .ecdsaSignatureMessageX962SHA384
            case .p521: return .ecdsaSignatureMessageX962SHA512
            }
        }

        var secKeyType: CFString {
            switch self {
            case .p256, .p384, .p521: return kSecAttrKeyTypeECSECPrimeRandom
            }
        }

        var keySizeInBits: Int {
            switch self {
            case .p256: return 256
            case .p384: return 384
            case .p521: return 521
            }
        }
    }

    /**
     Checks whether Secure Enclave is available on this device.

     - Returns: true if Secure Enclave is available, false otherwise.
     */
    static func isSecureEnclaveAvailable() -> Bool {
        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits as String: 256,
            kSecAttrTokenID as String: kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String: false
            ]
        ]

        var error: Unmanaged<CFError>?
        let key = SecKeyCreateRandomKey(attributes as CFDictionary, &error)
        return key != nil
    }

    /**
     Generates a cryptographic key pair in the Secure Enclave.

     - Parameters:
     - alias: Unique identifier for the key pair
     - userPresenceRequired: If true, requires biometric or passcode to use the key
     - Returns: The public key raw bytes (ANSI X9.63 / external representation).
     IMPORTANT: This is NOT a PEM string. Backend helpers should convert to SPKI PEM if needed.
     - Throws: KeychainError if generation fails.
     */
    static func generateKeyPair(alias: String, userPresenceRequired: Bool = true) throws -> Data {
        let accessControl: SecAccessControl?

        if userPresenceRequired {
            var error: Unmanaged<CFError>?
            accessControl = SecAccessControlCreateWithFlags(
                kCFAllocatorDefault,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                [.privateKeyUsage, .biometryCurrentSet],
                &error
            )

            if error != nil {
                throw KeychainError.unexpectedError(errSecParam)
            }
        } else {
            var error: Unmanaged<CFError>?
            accessControl = SecAccessControlCreateWithFlags(
                kCFAllocatorDefault,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                .privateKeyUsage,
                &error
            )

            if error != nil {
                throw KeychainError.unexpectedError(errSecParam)
            }
        }

        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits as String: 256,
            kSecAttrTokenID as String: kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String: true,
                kSecAttrApplicationTag as String: alias.data(using: .utf8)!,
                kSecAttrAccessControl as String: accessControl as Any
            ]
        ]

        var keyError: Unmanaged<CFError>?
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &keyError) else {
            throw KeychainError.unexpectedError(errSecParam)
        }

        guard let publicKey = SecKeyCopyPublicKey(privateKey) else {
            throw KeychainError.unexpectedError(errSecParam)
        }

        guard let publicKeyData = SecKeyCopyExternalRepresentation(publicKey, &keyError) as Data? else {
            throw KeychainError.unexpectedError(errSecParam)
        }

        // Logic Improvement: Standardize on SPKI PEM format internally
        // to ensure the string returned to the Bridge is immediately usable by backends.
        if let pemString = try? PublicKeyEncoder.p256RawToSpkiPem(publicKeyData) {
            return pemString.data(using: .utf8) ?? publicKeyData
        }

        // NOTE: publicKeyData is raw ANSI X9.63 representation.
        // If/when we expose public keys to JS/backend, standardize to SPKI PEM using PublicKeyEncoder.
        return publicKeyData
    }

    /**
     Signs data using a private key stored in the Keychain (or Secure Enclave).

     - Parameters:
     - data: The data to sign
     - alias: The key pair identifier
     - Returns: The signature data
     - Throws: KeychainError if signing fails.
     */
    static func sign(data: Data, with alias: String) throws -> Data {
        let tag = alias.data(using: .utf8)!

        let context = LAContext()
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: true,
            kSecUseAuthenticationContext as String: context
        ]

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let privateKey = result else {
            throw KeychainError.itemNotFound
        }

        let secPrivateKey = try castSecKey(privateKey)

        var signError: Unmanaged<CFError>?
        // Note: SecKeyCreateSignature returns a DER-encoded signature for ECDSA
        guard let signature = SecKeyCreateSignature(
            secPrivateKey,
            .ecdsaSignatureMessageX962SHA256, // This algorithm produces ASN.1 DER
            data as CFData,
            &signError
        ) else {
            throw KeychainError.unexpectedError(errSecParam)
        }

        return signature as Data
    }

    /**
     Verifies a signature using the public key from a key pair.

     - Parameters:
     - signature: The signature to verify
     - data: The original data that was signed
     - alias: The key pair identifier
     - Returns: true if the signature is valid
     - Throws: KeychainError if verification fails.
     */
    static func verify(signature: Data, for data: Data, with alias: String) throws -> Bool {
        let tag = alias.data(using: .utf8)!

        let context = LAContext()
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: true,
            kSecUseAuthenticationContext as String: context
        ]

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let privateKey = result else {
            throw KeychainError.itemNotFound
        }

        let secPrivateKey = try castSecKey(privateKey)

        guard let publicKey = SecKeyCopyPublicKey(secPrivateKey) else {
            throw KeychainError.unexpectedError(errSecParam)
        }

        var verifyError: Unmanaged<CFError>?
        let isValid = SecKeyVerifySignature(
            publicKey,
            .ecdsaSignatureMessageX962SHA256,
            data as CFData,
            signature as CFData,
            &verifyError
        )

        return isValid
    }

    /**
     Deletes a key pair from the Keychain.

     - Parameter alias: The key pair identifier.
     - Throws: KeychainError if deletion fails.
     */
    static func deleteKeyPair(alias: String) throws {
        let tag = alias.data(using: .utf8)!

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom
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
     Checks whether a key pair exists in the Keychain.
     Uses interactionNotAllowed to prevent unwanted biometric prompts during check.

     - Parameter alias: The key pair identifier.
     - Returns: true if the key pair exists, false otherwise.
     */
    static func hasKeyPair(alias: String) -> Bool {
        let tag = alias.data(using: .utf8)!

        // Use LAContext to suppress UI prompts during existence check
        let context = LAContext()
        context.interactionNotAllowed = true

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: false, // We only care about existence
            kSecUseAuthenticationContext as String: context
        ]

        let status = SecItemCopyMatching(query as CFDictionary, nil)

        // errSecInteractionNotAllowed means the item exists but requires biometrics
        return status == errSecSuccess || status == errSecInteractionNotAllowed
    }
}
