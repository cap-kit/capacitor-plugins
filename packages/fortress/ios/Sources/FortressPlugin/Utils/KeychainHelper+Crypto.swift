import Foundation
import Security
import LocalAuthentication

extension KeychainHelper {

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

        if let pemString = try? PublicKeyEncoder.p256RawToSpkiPem(publicKeyData) {
            return pemString.data(using: .utf8) ?? publicKeyData
        }

        return publicKeyData
    }

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
        guard let signature = SecKeyCreateSignature(
            secPrivateKey,
            .ecdsaSignatureMessageX962SHA256,
            data as CFData,
            &signError
        ) else {
            throw KeychainError.unexpectedError(errSecParam)
        }

        return signature as Data
    }

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

    static func hasKeyPair(alias: String) -> Bool {
        let tag = alias.data(using: .utf8)!

        let context = LAContext()
        context.interactionNotAllowed = true

        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: false,
            kSecUseAuthenticationContext as String: context
        ]

        let status = SecItemCopyMatching(query as CFDictionary, nil)
        return status == errSecSuccess || status == errSecInteractionNotAllowed
    }
}
